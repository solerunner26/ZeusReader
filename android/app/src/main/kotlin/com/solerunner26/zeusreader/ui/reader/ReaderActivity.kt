package com.solerunner26.zeusreader.ui.reader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.solerunner26.zeusreader.reader.crash.ReaderCrashGuard
import com.solerunner26.zeusreader.reader.prefs.ReaderPreferencesRepository
import com.solerunner26.zeusreader.ui.theme.ZeusReaderTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.system.exitProcess

/**
 * Isolated document reader host running in `android:process=":reader"`.
 * Native MuPDF / Readium / WebView crashes stay isolated from the Library process and record
 * diagnostics for safe recovery in [com.solerunner26.zeusreader.MainActivity].
 */
@AndroidEntryPoint
class ReaderActivity : FragmentActivity() {

    @Inject
    lateinit var preferencesRepository: ReaderPreferencesRepository

    private val epubViewModel: EpubReaderViewModel by viewModels()
    private val pdfViewModel: PdfReaderViewModel by viewModels()
    private val imageBookViewModel: ImageBookReaderViewModel by viewModels()

    private val _volumeTurnEvents = MutableSharedFlow<Int>(extraBufferCapacity = 8)
    private val volumeTurnEvents = _volumeTurnEvents.asSharedFlow()
    private var volumeTurnEnabled: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            preferencesRepository.settings.collect { settings ->
                volumeTurnEnabled = settings.volumeKeyPageTurn
            }
        }

        val editionId = intent.getStringExtra(EXTRA_EDITION_ID).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Reader" }
        val format = intent.getStringExtra(EXTRA_FORMAT).orEmpty().uppercase()
        val localPath = intent.getStringExtra(EXTRA_LOCAL_PATH).orEmpty()

        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            ReaderCrashGuard.recordCrash(
                context = applicationContext,
                bookTitle = title,
                format = format,
                throwable = throwable
            )
            if (previousHandler != null && thread.name != "main") {
                previousHandler.uncaughtException(thread, throwable)
            } else {
                exitProcess(1)
            }
        }

        enableEdgeToEdge()
        setContent {
            ZeusReaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        format == "EPUB" -> {
                            EpubReaderScreen(
                                editionId = editionId,
                                title = title,
                                localPath = localPath,
                                activity = this,
                                viewModel = epubViewModel,
                                volumeTurnEvents = volumeTurnEvents,
                                onBack = { finish() }
                            )
                        }

                        format == "IMAGE_BOOK" || format in setOf("JPG", "JPEG", "PNG") -> {
                            ImageBookReaderScreen(
                                editionId = editionId,
                                title = title,
                                viewModel = imageBookViewModel,
                                onBack = { finish() }
                            )
                        }

                        else -> {
                            PdfReaderScreen(
                                editionId = editionId,
                                title = title,
                                localPath = localPath,
                                viewModel = pdfViewModel,
                                volumeTurnEvents = volumeTurnEvents,
                                onBack = { finish() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (volumeTurnEnabled) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    _volumeTurnEvents.tryEmit(1)
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    _volumeTurnEvents.tryEmit(-1)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        const val EXTRA_EDITION_ID = "extra_edition_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_FORMAT = "extra_format"
        const val EXTRA_LOCAL_PATH = "extra_local_path"

        fun createIntent(
            context: Context,
            editionId: String,
            title: String,
            format: String,
            localPath: String?
        ): Intent = Intent(context, ReaderActivity::class.java).apply {
            putExtra(EXTRA_EDITION_ID, editionId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_FORMAT, format)
            putExtra(EXTRA_LOCAL_PATH, localPath.orEmpty())
        }
    }
}
