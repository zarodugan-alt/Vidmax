package com.comet.engine

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * aria2 path for direct file URLs (Part 7.3): non-yt-dlp links such as plain mp4/mkv/binaries
 * intercepted from the browser. Runs the aria2c binary that ships inside the aria2 AAR
 * (extracted by [com.yausername.aria2c.Aria2c.init]).
 */
@Singleton
class Aria2DirectEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    @Volatile
    private var initialized = false

    suspend fun ensureInit() = withContext(Dispatchers.IO) {
        if (initialized) return@withContext
        runCatching { com.yausername.aria2c.Aria2c.getInstance().init(context) }
            .onFailure { Timber.e(it, "aria2c init failed") }
        initialized = true
    }

    /** Locates the extracted aria2c executable inside the app-private package dir. */
    private fun findBinary(): File? {
        val base = File(context.noBackupFilesDir, "youtubedl-android/packages/aria2c")
        if (!base.exists()) return null
        return base.walkTopDown().firstOrNull { it.isFile && it.name == "aria2c" }
    }

    fun download(
        task: DownloadTask,
        headers: Map<String, String> = emptyMap(),
    ): Flow<EngineEvent> = callbackFlow {
        launch(Dispatchers.IO) {
            val binary = findBinary()
            if (binary == null) {
                trySend(EngineEvent.Failed(code = 1, message = "aria2 engine unavailable"))
                close()
                return@launch
            }

            val outName = outputFileName(task)
            val args = mutableListOf(
                binary.absolutePath,
                "--summary-interval=1",
                "--continue=true",
                "--allow-overwrite=true",
                "--file-allocation=none",
                "-x", "16",
                "-s", "16",
                "--dir", task.workDir,
                "-o", outName,
                task.url,
            )
            headers.forEach { (k, v) -> args += listOf("--header", "$k: $v") }

            val process = try {
                val pb = ProcessBuilder(args).redirectErrorStream(false)
                pb.environment()["LD_LIBRARY_PATH"] =
                    File(context.noBackupFilesDir, "youtubedl-android/packages/aria2c/usr/lib")
                        .absolutePath
                pb.start()
            } catch (e: Exception) {
                trySend(EngineEvent.Failed(code = 1, message = e.message ?: "aria2 failed to start"))
                close()
                return@launch
            }

            val stdoutJob = launch(Dispatchers.IO) {
                process.inputStream.bufferedReader().useLines { lines ->
                    for (line in lines) {
                        when (val parsed = ProgressParser.parse(line)) {
                            is ParsedLine.Progress -> trySend(
                                EngineEvent.Progress(
                                    downloadedBytes = parsed.downloadedBytes ?: 0L,
                                    totalBytes = parsed.totalBytes,
                                    bytesPerSec = parsed.bytesPerSec ?: 0L,
                                    etaSec = parsed.etaSec,
                                )
                            )

                            else -> Unit
                        }
                    }
                }
            }
            val stderrTail = StringBuilder()
            val stderrJob = launch(Dispatchers.IO) {
                process.errorStream.bufferedReader().useLines { lines ->
                    for (line in lines) {
                        if (stderrTail.length < 4000) {
                            stderrTail.appendLine(line)
                        }
                    }
                }
            }

            val exitCode = runCatching { process.waitFor() }.getOrDefault(-1)
            stdoutJob.join()
            stderrJob.join()

            if (exitCode == 0) {
                val file = File(task.workDir, outName)
                trySend(EngineEvent.Done(file.absolutePath))
            } else {
                // 20 = checksum error etc. Message tail is the honest copy.
                val message = stderrTail.toString().lineSequence()
                    .lastOrNull { it.isNotBlank() } ?: "aria2 exited with code $exitCode"
                trySend(EngineEvent.Failed(code = exitCode, message = message))
            }
            close()
        }

        awaitClose { Unit }
    }

    private fun outputFileName(task: DownloadTask): String {
        val fromUrl = task.url.substringBefore('?').substringAfterLast('/')
        val sanitized = fromUrl.takeIf { it.isNotBlank() && it.length <= 120 } ?: "download"
        return sanitized.replace(Regex("[/\\\\]"), "_")
    }
}
