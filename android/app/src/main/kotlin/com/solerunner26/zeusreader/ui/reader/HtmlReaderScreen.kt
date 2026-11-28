package com.solerunner26.zeusreader.ui.reader

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solerunner26.zeusreader.R
import com.solerunner26.zeusreader.reader.html.HtmlSandboxPolicy
import com.solerunner26.zeusreader.reader.prefs.ReaderPreferencesRepository
import com.solerunner26.zeusreader.reader.prefs.ReaderSettings
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HtmlReaderScreen(
    title: String,
    localPath: String,
    preferencesRepository: ReaderPreferencesRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by preferencesRepository.settings.collectAsStateWithLifecycle(initialValue = ReaderSettings())
    val scope = rememberCoroutineScope()
    var showTypographySheet by remember { mutableStateOf(false) }

    val htmlFile = remember(localPath) { File(localPath) }
    val baseDir = remember(htmlFile) { htmlFile.parentFile ?: htmlFile }
    val sanitizedHtml = remember(localPath) {
        val raw = runCatching { htmlFile.readText(Charsets.UTF_8) }.getOrDefault("<p>Unable to read HTML file.</p>")
        HtmlSandboxPolicy.sanitizeHtmlDocument(raw)
    }

    val bgHex = String.format("#%06X", 0xFFFFFF and settings.theme.backgroundColor.toArgb())
    val textHex = String.format("#%06X", 0xFFFFFF and settings.theme.textColor.toArgb())
    val styledHtml = remember(sanitizedHtml, settings) {
        """
        <html><head><style>
        body {
          background-color: $bgHex;
          color: $textHex;
          font-size: ${settings.fontSizePercent}%;
          line-height: ${settings.lineHeightMultiplier};
          padding: 16px ${settings.horizontalMarginDp}px;
          font-family: ${settings.fontChoice.cssName}, sans-serif;
        }
        img { max-width: 100%; height: auto; }
        </style></head><body>$sanitizedHtml</body></html>
        """.trimIndent()
    }

    var lastLoadedHtml by remember { mutableStateOf<String?>(null) }
    StormlightReaderFrame(
        title = title,
        meta = "HTML",
        paperColor = settings.theme.backgroundColor,
        onBack = onBack,
        progressFraction = null,
        progressLabel = "Scroll to read",
        tools = listOf(OrbTool("Vibe & type", Icons.Rounded.Tune) { showTypographySheet = true }),
        modifier = modifier
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    val webSettings = this.settings
                    webSettings.javaScriptEnabled = false
                    webSettings.blockNetworkLoads = true
                    webSettings.blockNetworkImage = true
                    webSettings.allowFileAccess = true
                    @Suppress("DEPRECATION")
                    webSettings.allowFileAccessFromFileURLs = false
                    @Suppress("DEPRECATION")
                    webSettings.allowUniversalAccessFromFileURLs = false

                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val url = request?.url?.toString().orEmpty()
                            if (!HtmlSandboxPolicy.isRequestAllowed(url, baseDir)) {
                                return WebResourceResponse(
                                    "text/plain",
                                    "utf-8",
                                    403,
                                    "Blocked by ZeusReader sandbox",
                                    emptyMap(),
                                    ByteArrayInputStream(ByteArray(0))
                                )
                            }
                            return super.shouldInterceptRequest(view, request)
                        }
                    }
                }
            },
            update = { webView ->
                // Reload only when the styled document actually changes (keeps scroll position stable).
                if (lastLoadedHtml != styledHtml) {
                    lastLoadedHtml = styledHtml
                    webView.loadDataWithBaseURL(baseDir.toURI().toString(), styledHtml, "text/html", "utf-8", null)
                }
            }
        )
        WarmLightOverlay(settings.warmLightStrength)
    }

    if (showTypographySheet) {
        TypographyBottomSheet(
            settings = settings,
            isPdfMode = false,
            onSettingsChange = { transform ->
                scope.launch { preferencesRepository.update(transform) }
            },
            onDismiss = { showTypographySheet = false }
        )
    }
}
