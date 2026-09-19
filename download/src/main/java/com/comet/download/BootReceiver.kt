package com.comet.download

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.comet.data.repo.DownloadRepository
import com.comet.download.service.DownloadService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Resumes the interrupted queue after boot (Part 10). */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: DownloadRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.requeueInterrupted()
                if (repository.pendingCount() > 0) {
                    DownloadService.start(context)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
