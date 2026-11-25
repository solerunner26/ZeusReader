package com.solerunner26.zeusreader.ui.reader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.solerunner26.zeusreader.reader.crash.ReaderCrashGuard
import com.solerunner26.zeusreader.ui.theme.ZeusReaderTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlin.system.exitProcess

/**
 * Isolated document reader host running in `android:process=":reader"`.
 * Native MuPDF / Readium / WebView crashes stay isolated from the Library process and record
 * diagnostics for safe recovery in [com.solerunner26.zeusreader.MainActivity].
 */
@AndroidEntryPoint
class ReaderActivity : FragmentActivity() {

    private val epubViewModel: EpubReaderViewModel by viewModels()
    private val pdfViewModel: PdfReaderViewModel by viewModels()
    private val imageBookViewModel: ImageBookReaderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                                onBack = { finish() }
                            )
                        }
                    }
                }
            }
        }
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
