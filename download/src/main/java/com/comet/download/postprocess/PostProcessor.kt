package com.comet.download.postprocess

import com.comet.data.datastore.SettingsRepository
import com.comet.data.db.DownloadEntity
import com.comet.download.storage.FilenameTemplate
import com.comet.download.storage.StorageBridge
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Post-processing pipeline (Part 8.3): merge/extract/embed all happen inside the yt-dlp
 * invocation (staged via [com.comet.engine.EngineEvent.Stage]); this class owns the final
 * leg — render the public filename from the template, move from the work dir into the
 * public library via MediaStore, and hand the final path back for the Room update.
 */
@Singleton
class PostProcessor @Inject constructor(
    private val storageBridge: StorageBridge,
    private val settingsRepository: SettingsRepository,
) {

    /**
     * @param srcPath engine-reported output file (or null — falls back to the largest work file).
     * @return public absolute path, or null if the file could not be saved.
     */
    suspend fun process(entity: DownloadEntity, srcPath: String?): String? {
        val source = srcPath?.let(::File)?.takeIf { it.exists() && it.length() > 0 }
            ?: largestWorkFile(entity.workPath)
            ?: return null

        val settings = settingsRepository.settings.value
        val baseName = FilenameTemplate.render(
            template = settings.filenameTemplate,
            title = entity.title,
            channel = null,
            quality = entity.qualityLabel,
            id = entity.playlistId ?: entity.id,
            site = entity.site,
            timestamp = entity.createdAt,
        )
        val displayName = "$baseName.${source.extension}"
        Timber.d("post-process: %s -> %s", source.absolutePath, displayName)
        return storageBridge.moveToLibrary(entity, source, displayName)
    }

    private fun largestWorkFile(workPath: String?): File? =
        workPath?.let { path ->
            File(path).walkTopDown()
                .filter { it.isFile && it.length() > 0 && !it.name.endsWith(".part") }
                .maxByOrNull { it.length() }
        }
}
