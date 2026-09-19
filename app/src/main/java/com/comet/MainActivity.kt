package com.comet

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.comet.download.service.DownloadService
import com.comet.home.HomeViewModel
import com.comet.ui.theme.CometTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Single-activity Compose app (Part 4). Share targets per Part 10:
 * ACTION_SEND (text/plain), ACTION_VIEW (http/https), ACTION_PROCESS_TEXT.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            CometTheme {
                CometApp(viewModel)
            }
        }
        observeSideEffects()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                val text = (intent.getCharSequenceExtra(Intent.EXTRA_TEXT) ?: "").toString()
                viewModel.offerExternalUrl(extractUrl(text))
            }

            Intent.ACTION_VIEW -> viewModel.offerExternalUrl(intent.dataString)

            Intent.ACTION_PROCESS_TEXT -> {
                val text = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT).orEmpty()
                viewModel.offerExternalUrl(extractUrl(text))
            }
        }
        // Notification [Play] deep link.
        intent?.getStringExtra(DownloadService.EXTRA_OPEN_DOWNLOAD_ID)?.let { id ->
            viewModel.openPlayer(id)
        }
    }

    private fun extractUrl(text: String): String? =
        text.split(Regex("\\s+")).firstOrNull {
            it.startsWith("http://", ignoreCase = true) ||
                it.startsWith("https://", ignoreCase = true)
        }

    private fun observeSideEffects() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.toast.collect { message ->
                        if (message != null) {
                            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                            viewModel.consumeToast()
                        }
                    }
                }
                launch {
                    viewModel.permissionRequest.collect { requested ->
                        if (requested) {
                            ActivityCompat.requestPermissions(
                                this@MainActivity,
                                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                RC_STORAGE,
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RC_STORAGE) {
            val granted = grantResults.isNotEmpty() &&
                grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
            viewModel.onPermissionResult(granted)
        }
    }

    private companion object {
        const val RC_STORAGE = 4001
    }
}
