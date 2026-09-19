package com.comet.data.repo

import com.comet.data.db.CometDatabase
import com.comet.data.db.DownloadEntity
import com.comet.data.db.PlaylistEntity
import com.comet.data.db.WaveformEntity
import com.comet.data.model.DownloadState
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed download queue (Part 8.1/9.1). All queue mutations funnel through here so the
 * DB remains the single source of truth; transient progress is kept in-memory by QueueManager.
 */
@Singleton
class DownloadRepository @Inject constructor(
    private val db: CometDatabase,
) {
    private val dao = db.downloadDao()
    private val playlistDao = db.playlistDao()

    fun queueFlow(): Flow<List<DownloadEntity>> = dao.queueFlow()
    fun libraryFlow(): Flow<List<DownloadEntity>> = dao.libraryFlow()
    fun failedFlow(): Flow<List<DownloadEntity>> = dao.failedFlow()
    fun byIdFlow(id: String): Flow<DownloadEntity?> = dao.byIdFlow(id)
    fun activeCountFlow(): Flow<Int> = dao.activeCountFlow()

    suspend fun byId(id: String): DownloadEntity? = dao.byId(id)
    suspend fun findExistingByUrl(url: String): DownloadEntity? = dao.findExistingByUrl(url)

    /** One-shot non-flow snapshot of everything still in the queue (queued/active/paused). */
    suspend fun queueSnapshot(): List<DownloadEntity> = dao.all().filter { !it.state.isTerminal }

    suspend fun enqueue(
        id: String,
        workPath: String,
        url: String,
        title: String,
        site: String,
        thumbnailPath: String?,
        durationSec: Long?,
        formatId: String,
        qualityLabel: String,
        isAudio: Boolean,
        sizeBytes: Long?,
        playlistId: String? = null,
    ): DownloadEntity {
        val entity = DownloadEntity(
            id = id,
            url = url,
            title = title,
            site = site,
            thumbnailPath = thumbnailPath,
            durationSec = durationSec,
            formatId = formatId,
            qualityLabel = qualityLabel,
            isAudio = isAudio,
            sizeBytes = sizeBytes,
            downloadedBytes = 0L,
            state = DownloadState.QUEUED,
            errorMessage = null,
            workPath = workPath,
            finalPath = null,
            playlistId = playlistId,
            position = (dao.maxPosition() ?: -1) + 1,
            createdAt = System.currentTimeMillis(),
            completedAt = null,
        )
        dao.upsert(entity)
        return entity
    }

    suspend fun upsertPlaylist(playlist: PlaylistEntity) = playlistDao.upsert(playlist)
    suspend fun playlistById(id: String): PlaylistEntity? = playlistDao.byId(id)

    suspend fun updateState(id: String, state: DownloadState, errorMessage: String? = null) {
        val e = dao.byId(id) ?: return
        dao.upsert(
            e.copy(
                state = state,
                errorMessage = errorMessage ?: e.errorMessage.takeIf { state != DownloadState.FAILED },
                completedAt = if (state == DownloadState.DONE) System.currentTimeMillis() else e.completedAt,
            )
        )
    }

    suspend fun updateProgress(id: String, downloadedBytes: Long, sizeBytes: Long?) {
        val e = dao.byId(id) ?: return
        dao.upsert(e.copy(downloadedBytes = downloadedBytes, sizeBytes = sizeBytes ?: e.sizeBytes))
    }

    suspend fun updateFinalPath(id: String, finalPath: String?) {
        val e = dao.byId(id) ?: return
        dao.upsert(e.copy(finalPath = finalPath))
    }

    suspend fun moveToFront(id: String) {
        val e = dao.byId(id) ?: return
        val others = dao.all()
            .filter { it.id != id && it.state.isPending }
            .sortedBy { it.position }
        others.forEachIndexed { index, item -> dao.updatePosition(item.id, index + 1) }
        dao.updatePosition(id, 0)
        // refresh entity state to QUEUED so the runner picks it up
        dao.upsert(e.copy(state = DownloadState.QUEUED))
    }

    suspend fun pause(id: String) = updateState(id, DownloadState.PAUSED)
    suspend fun resume(id: String) = moveToFront(id)

    suspend fun cancel(id: String) {
        dao.delete(id)
        dao.deleteWaveform(id)
    }

    suspend fun retry(id: String) {
        val e = dao.byId(id) ?: return
        dao.upsert(e.copy(state = DownloadState.QUEUED, errorMessage = null))
    }

    suspend fun requeueInterrupted() = dao.requeueInterrupted()
    suspend fun pendingCount(): Int = dao.pendingCount()
    suspend fun clearFinished() = dao.clearFinished()

    suspend fun saveWaveform(downloadId: String, amplitudes: IntArray) {
        dao.upsertWaveform(WaveformEntity(downloadId, amplitudes))
    }

    suspend fun waveform(downloadId: String): IntArray? = dao.waveform(downloadId)?.amplitudes

    suspend fun deleteLibraryItem(id: String) {
        dao.delete(id)
        dao.deleteWaveform(id)
    }
}
