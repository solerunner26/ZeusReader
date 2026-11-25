package com.solerunner26.zeusreader

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solerunner26.zeusreader.reader.crash.ReaderCrashGuard
import com.solerunner26.zeusreader.reader.crash.ReaderCrashReport
import com.solerunner26.zeusreader.reader.prefs.AppTheme
import com.solerunner26.zeusreader.ui.theme.AppUiConfigViewModel
import com.solerunner26.zeusreader.ui.theme.HueAuraBackground
import com.solerunner26.zeusreader.ui.theme.ReaderSurface
import com.solerunner26.zeusreader.ui.theme.ZeusReaderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val uiConfigViewModel: AppUiConfigViewModel by viewModels()
    private var pendingSharedUris by mutableStateOf<List<Uri>>(emptyList())
    private var crashReportToShow by mutableStateOf<ReaderCrashReport?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingShareIntent(intent)
        enableEdgeToEdge()
        setContent {
            val uiConfig by uiConfigViewModel.config.collectAsStateWithLifecycle()
            val effectiveHue by uiConfigViewModel.effectiveHue.collectAsStateWithLifecycle()
            val darkTheme = when (uiConfig.appTheme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }
            ZeusReaderTheme(
                hue = effectiveHue,
                darkTheme = darkTheme,
                pureBlackOled = uiConfig.readerSurface == ReaderSurface.OLED,
                animateHue = uiConfig.masterAnimations && uiConfig.readerSurface != ReaderSurface.EINK
            ) {
                HueAuraBackground(
                    config = uiConfig.copy(themeHue = effectiveHue),
                    darkTheme = darkTheme,
                    modifier = Modifier.fillMaxSize()
                ) {
                    MainNavigation(
                        sharedImportUris = pendingSharedUris,
                        onSharedImportsHandled = { pendingSharedUris = emptyList() }
                    )

                    crashReportToShow?.let { report ->
                        AlertDialog(
                            onDismissRequest = { crashReportToShow = null },
                            title = { Text("Reader closed safely") },
                            text = {
                                Text(
                                    "\"${report.bookTitle}\" (${report.format}) encountered a document error " +
                                        "in the isolated reader process and returned safely to your library.\n\n" +
                                        report.errorSummary
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = { crashReportToShow = null }) {
                                    Text("OK")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val report = ReaderCrashGuard.consumeCrashReport(this)
        if (report != null) {
            crashReportToShow = report
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingShareIntent(intent)
    }

    private fun handleIncomingShareIntent(intent: Intent?) {
        if (intent == null) return
        val extracted: List<Uri> = when (intent.action) {
            Intent.ACTION_SEND -> listOfNotNull(extractParcelableUri(intent))
            Intent.ACTION_SEND_MULTIPLE -> extractParcelableUriList(intent)
            else -> emptyList()
        }
        if (extracted.isNotEmpty()) {
            pendingSharedUris = extracted
        }
    }

    private fun extractParcelableUri(intent: Intent): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }

    private fun extractParcelableUriList(intent: Intent): List<Uri> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
        }
}
