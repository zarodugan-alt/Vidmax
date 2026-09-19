package com.comet.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query(
        "SELECT * FROM downloads WHERE state IN ('QUEUED','ANALYZING','DOWNLOADING','PROCESSING','PAUSED') " +
            "ORDER BY position ASC"
    )
    fun queueFlow(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE state = 'DONE' ORDER BY completedAt DESC")
    fun libraryFlow(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE state = 'FAILED' ORDER BY createdAt DESC")
    fun failedFlow(): Flow<List<DownloadEntity>>

    @Query(
        "SELECT * FROM downloads WHERE state IN ('DONE','FAILED') " +
            "AND (completedAt IS NOT NULL AND completedAt >= :since) ORDER BY completedAt DESC"
    )
    fun historyFlow(since: Long): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    fun byIdFlow(id: String): Flow<DownloadEntity?>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun byId(id: String): DownloadEntity?

    @Query(
        "SELECT * FROM downloads WHERE url = :url AND state IN ('QUEUED','ANALYZING','DOWNLOADING','PROCESSING','PAUSED','DONE') " +
            "LIMIT 1"
    )
    suspend fun findExistingByUrl(url: String): DownloadEntity?

    @Query("SELECT MAX(position) FROM downloads")
    suspend fun maxPosition(): Int?

    @Query("SELECT COUNT(*) FROM downloads WHERE state IN ('QUEUED','ANALYZING','DOWNLOADING','PROCESSING')")
    fun activeCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM downloads WHERE state IN ('QUEUED','ANALYZING','DOWNLOADING','PROCESSING','PAUSED')")
    suspend fun pendingCount(): Int

    @Query("SELECT COUNT(*) FROM downloads WHERE playlistId = :playlistId AND state = 'DONE'")
    fun playlistDoneCountFlow(playlistId: String): Flow<Int>

    @Upsert
    suspend fun upsert(entity: DownloadEntity)

    @Upsert
    suspend fun upsertAll(entities: List<DownloadEntity>)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM downloads WHERE state IN ('DONE','CANCELLED','FAILED')")
    suspend fun clearFinished()

    @Query("DELETE FROM waveforms WHERE downloadId = :downloadId")
    suspend fun deleteWaveform(downloadId: String)

    @Query("SELECT * FROM downloads ORDER BY position ASC")
    suspend fun all(): List<DownloadEntity>

    @Query("UPDATE downloads SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: String, position: Int)

    /** Used on service restart: interrupted tasks go back to QUEUED so `--continue` can resume them. */
    @Query("UPDATE downloads SET state = 'QUEUED' WHERE state IN ('ANALYZING','DOWNLOADING','PROCESSING')")
    suspend fun requeueInterrupted()

    @Query("SELECT * FROM waveforms WHERE downloadId = :downloadId")
    suspend fun waveform(downloadId: String): WaveformEntity?

    @Upsert
    suspend fun upsertWaveform(waveform: WaveformEntity)
}

@Dao
interface PlaylistDao {
    @Upsert
    suspend fun upsert(playlist: PlaylistEntity)

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun byId(id: String): PlaylistEntity?

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun allFlow(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    suspend fun all(): List<PlaylistEntity>

    @Transaction
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun playlistsWithDownloadsFlow(): Flow<List<PlaylistWithDownloads>>

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface EngineStateDao {
    @Query("SELECT * FROM engine_state WHERE id = 1")
    suspend fun get(): EngineStateEntity?

    @Query("SELECT * FROM engine_state WHERE id = 1")
    fun observe(): Flow<EngineStateEntity?>

    @Upsert
    suspend fun upsert(state: EngineStateEntity)
}
