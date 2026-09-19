package com.comet.download.storage

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.comet.data.db.DownloadEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Storage bridge (Part 8.4).
 *
 * Work dir: app-private external storage (no permission needed, survives until uninstall).
 * Final: MediaStore insert so files appear in gallery apps — the API 29+ branch uses
 * RELATIVE_PATH + IS_PENDING; the API 28 branch writes the legacy public directory and
 * registers the file with MediaStore. Both branches are implemented (spec: forward-compat).
 */
@Singleton
class StorageBridge @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun workDirFor(taskId: String): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, "comet-work/$taskId")

    fun freeBytes(): Long = runCatching {
        StatFs(workRoot().absolutePath).availableBytes
    }.getOrDefault(0L)

    /** Total size of the storage volume holding the work dir (for the S11 stats bar). */
    fun totalBytes(): Long = runCatching {
        StatFs(workRoot().absolutePath).totalBytes
    }.getOrDefault(0L)

    /** Pre-download space check (Part 8.4): block under 100MB free; warn under 1GB (UI). */
    fun hasSpaceFor(estimatedBytes: Long?): Boolean {
        val free = freeBytes()
        if (free < MIN_FREE_BYTES) return false
        if (estimatedBytes != null && estimatedBytes > 0) return free > estimatedBytes + SAFETY_MARGIN
        return true
    }

    private fun workRoot(): File =
        context.getExternalFilesDir(null) ?: context.filesDir

    fun hasLegacyStoragePermission(): Boolean =
        Build.VERSION.SDK_INT >= 29 ||
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    /**
     * Moves a finished work file into the public library (Movies/COMET or Music/COMET).
     * @return the final absolute path, or null on failure.
     */
    fun moveToLibrary(entity: DownloadEntity, source: File, displayName: String): String? {
        val mime = mimeTypeFor(source.extension)
        return try {
            if (Build.VERSION.SDK_INT >= 29 || hasLegacyStoragePermission()) {
                insertViaMediaStore(entity, source, displayName, mime)
            } else {
                // Permission denied on API 28: fall back to app-scoped storage so the
                // download is never lost (MediaStore still indexes it for the picker).
                moveToAppScoped(entity, source, displayName, mime)
            }
        } catch (e: Exception) {
            Timber.e(e, "moveToLibrary failed")
            null
        }
    }

    private fun collectionUri(isAudio: Boolean) =
        if (isAudio) MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        else MediaStore.Video.Media.EXTERNAL_CONTENT_URI

    private fun insertViaMediaStore(
        entity: DownloadEntity,
        source: File,
        displayName: String,
        mime: String,
    ): String? {
        val resolver = context.contentResolver
        val collection = collectionUri(entity.isAudio)

        if (Build.VERSION.SDK_INT >= 29) {
            val dir = if (entity.isAudio) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "$dir/COMET")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(collection, values) ?: return null
            resolver.openOutputStream(uri)?.use { out ->
                source.inputStream().use { it.copyTo(out) }
            } ?: run {
                resolver.delete(uri, null, null)
                return null
            }
            val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
            source.delete()
            return queryDataPath(uri)
        }

        // API 28 legacy branch: direct write to the public dir + MediaStore registration.
        val publicDir = File(
            Environment.getExternalStoragePublicDirectory(
                if (entity.isAudio) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES
            ),
            "COMET",
        ).apply { mkdirs() }
        val target = uniqueFile(publicDir, displayName, source.extension)
        source.copyTo(target, overwrite = true)
        source.delete()
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.TITLE, entity.title)
            put(MediaStore.MediaColumns.DISPLAY_NAME, target.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            @Suppress("DEPRECATION")
            put(MediaStore.MediaColumns.DATA, target.absolutePath)
        }
        runCatching { resolver.insert(collection, values) }
        return target.absolutePath
    }

    private fun moveToAppScoped(
        entity: DownloadEntity,
        source: File,
        displayName: String,
        mime: String,
    ): String? {
        val dir = File(
            context.getExternalFilesDir(
                if (entity.isAudio) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES
            ),
            "COMET",
        ).apply { mkdirs() }
        val target = uniqueFile(dir, displayName, source.extension)
        source.copyTo(target, overwrite = true)
        source.delete()
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.TITLE, entity.title)
            put(MediaStore.MediaColumns.DISPLAY_NAME, target.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            @Suppress("DEPRECATION")
            put(MediaStore.MediaColumns.DATA, target.absolutePath)
        }
        runCatching { context.contentResolver.insert(collectionUri(entity.isAudio), values) }
        return target.absolutePath
    }

    private fun queryDataPath(uri: android.net.Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
    }.getOrNull()

    private fun uniqueFile(dir: File, displayName: String, ext: String): File {
        val base = displayName.substringBeforeLast('.')
        val extension = if (displayName.endsWith(".$ext")) ext else ext
        var candidate = File(dir, "$base.$extension")
        var i = 1
        while (candidate.exists()) {
            candidate = File(dir, "$base ($i).$extension")
            i++
        }
        return candidate
    }

    private fun mimeTypeFor(extension: String): String = when (extension.lowercase()) {
        "mp4", "m4v" -> "video/mp4"
        "webm" -> "video/webm"
        "mkv" -> "video/x-matroska"
        "mp3" -> "audio/mpeg"
        "m4a" -> "audio/mp4"
        "opus" -> "audio/opus"
        "ogg" -> "audio/ogg"
        "flac" -> "audio/flac"
        "wav" -> "audio/wav"
        else -> if (extension.startsWith("mp", true)) "video/mp4" else "application/octet-stream"
    }

    companion object {
        const val MIN_FREE_BYTES = 100L * 1024 * 1024 // block new downloads under 100MB free
        const val SAFETY_MARGIN = 50L * 1024 * 1024
    }
}
