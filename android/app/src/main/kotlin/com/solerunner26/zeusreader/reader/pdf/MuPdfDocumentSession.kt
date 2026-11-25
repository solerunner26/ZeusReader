package com.solerunner26.zeusreader.reader.pdf

import android.graphics.Bitmap
import com.artifex.mupdf.fitz.Cookie
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

interface PdfSession : AutoCloseable {
    val pageCount: Int
    suspend fun renderPage(pageIndex: Int, targetWidthPx: Int): Result<Bitmap>
}

/**
 * MuPDF-backed PDF renderer with:
 * - Serialized native access via [Mutex]
 * - Max 3 cached page bitmaps + 48 MB memory budget via [PageBitmapCache]
 * - Per-page decoded-bitmap budget downsampling and [OutOfMemoryError] recovery
 */
class MuPdfDocumentSession(
    private val filePath: String,
    private val cache: PageBitmapCache<Bitmap> = PageBitmapCache.forAndroidBitmaps(),
    private val maxSinglePageBytes: Long = MAX_SINGLE_PAGE_BITMAP_BYTES
) : PdfSession {

    private val mutex = Mutex()
    private var document: Document? = null
    private var cachedPageCount: Int = -1

    override val pageCount: Int
        get() = cachedPageCount.coerceAtLeast(0)

    suspend fun open(): Result<Int> = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val doc = document ?: Document.openDocument(filePath).also { document = it }
                val count = doc.countPages()
                cachedPageCount = count
                count
            }
        }
    }

    override suspend fun renderPage(pageIndex: Int, targetWidthPx: Int): Result<Bitmap> =
        withContext(Dispatchers.IO) {
            cache.get(pageIndex)?.let { existing ->
                if (!existing.isRecycled) return@withContext Result.success(existing)
            }

            mutex.withLock {
                // Re-check inside lock
                cache.get(pageIndex)?.let { existing ->
                    if (!existing.isRecycled) return@withLock Result.success(existing)
                }

                try {
                    val doc = document ?: Document.openDocument(filePath).also {
                        document = it
                        cachedPageCount = it.countPages()
                    }
                    val count = if (cachedPageCount >= 0) cachedPageCount else doc.countPages().also { cachedPageCount = it }
                    if (pageIndex < 0 || pageIndex >= count) {
                        return@withLock Result.failure(IndexOutOfBoundsException("Invalid page $pageIndex of $count"))
                    }

                    val page = doc.loadPage(pageIndex)
                    var drawDevice: AndroidDrawDevice? = null
                    try {
                        val bounds = page.bounds
                        val pageW = max(1f, bounds.x1 - bounds.x0)
                        val pageH = max(1f, bounds.y1 - bounds.y0)

                        val desiredWidth = targetWidthPx.coerceIn(240, 2200).toFloat()
                        var scale = desiredWidth / pageW
                        var outW = max(1, (pageW * scale).roundToInt())
                        var outH = max(1, (pageH * scale).roundToInt())

                        // Enforce single-page decoded ARGB_8888 budget
                        val estimatedBytes = outW.toLong() * outH.toLong() * 4L
                        if (estimatedBytes > maxSinglePageBytes) {
                            val ratio = sqrt(maxSinglePageBytes.toDouble() / estimatedBytes.toDouble()).toFloat()
                            scale *= ratio
                            outW = max(1, (pageW * scale).roundToInt())
                            outH = max(1, (pageH * scale).roundToInt())
                        }

                        val bitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
                        val ctm = Matrix(scale, scale).translate(-bounds.x0, -bounds.y0)
                        drawDevice = AndroidDrawDevice(bitmap, 0, 0, 0, 0, outW, outH)
                        page.run(drawDevice, ctm, Cookie())
                        drawDevice.close()

                        cache.put(pageIndex, bitmap)
                        Result.success(bitmap)
                    } finally {
                        runCatching { drawDevice?.destroy() }
                        runCatching { page.destroy() }
                    }
                } catch (oom: OutOfMemoryError) {
                    cache.evictAll()
                    Result.failure(IllegalStateException("Insufficient memory to decode PDF page", oom))
                } catch (t: Throwable) {
                    Result.failure(t)
                }
            }
        }

    override fun close() {
        cache.evictAll()
        runCatching { document?.destroy() }
        document = null
    }

    companion object {
        const val MAX_SINGLE_PAGE_BITMAP_BYTES = 16L * 1024L * 1024L

        fun computeBoundedDimensions(
            pageWidthPts: Float,
            pageHeightPts: Float,
            requestedWidthPx: Int,
            maxBytes: Long = MAX_SINGLE_PAGE_BITMAP_BYTES
        ): Pair<Int, Int> {
            val safeW = max(1f, pageWidthPts)
            val safeH = max(1f, pageHeightPts)
            val scale = requestedWidthPx.coerceIn(240, 2200).toFloat() / safeW
            var w = max(1, (safeW * scale).roundToInt())
            var h = max(1, (safeH * scale).roundToInt())
            val bytes = w.toLong() * h.toLong() * 4L
            if (bytes > maxBytes) {
                val factor = sqrt(maxBytes.toDouble() / bytes.toDouble())
                w = max(1, (w * factor).roundToInt())
                h = max(1, (h * factor).roundToInt())
            }
            return w to min(h, 8192)
        }
    }
}
