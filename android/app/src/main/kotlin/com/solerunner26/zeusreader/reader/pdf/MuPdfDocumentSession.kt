package com.solerunner26.zeusreader.reader.pdf

import android.graphics.Bitmap
import com.artifex.mupdf.fitz.Cookie
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.Outline
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import com.solerunner26.zeusreader.reader.core.ReaderCapabilities
import com.solerunner26.zeusreader.reader.core.ReaderEngine
import com.solerunner26.zeusreader.reader.core.ReaderEngineType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class PdfOutlineEntry(
    val title: String,
    val pageIndex: Int,
    val depth: Int
)

data class PdfSearchHit(
    val pageIndex: Int,
    val matchCount: Int
)

data class NormalizedSearchQuad(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

interface PdfSession : ReaderEngine, AutoCloseable {
    val pageCount: Int
    suspend fun renderPage(pageIndex: Int, targetWidthPx: Int, autoCropMargins: Boolean = false): Result<Bitmap>
    suspend fun extractReflowedPageText(pageIndex: Int): Result<String>
    suspend fun loadOutline(): List<PdfOutlineEntry>
    suspend fun searchText(query: String, maxHits: Int = 50): List<PdfSearchHit>
    suspend fun findNormalizedSearchQuads(pageIndex: Int, query: String): List<NormalizedSearchQuad> = emptyList()
}

/**
 * MuPDF-backed PDF renderer with:
 * - Serialized native access via [Mutex]
 * - Max 3 cached page bitmaps + 48 MB memory budget via [PageBitmapCache]
 * - Per-page decoded-bitmap budget downsampling, optional auto-crop, structured-text reflow,
 *   and [OutOfMemoryError] recovery.
 */
class MuPdfDocumentSession(
    private val filePath: String,
    private val cache: PageBitmapCache<Bitmap> = PageBitmapCache.forAndroidBitmaps(),
    private val maxSinglePageBytes: Long = MAX_SINGLE_PAGE_BITMAP_BYTES
) : PdfSession {

    override val engineType: ReaderEngineType = ReaderEngineType.PDF_MUPDF
    override val capabilities: ReaderCapabilities = ReaderCapabilities.PDF

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

    override suspend fun renderPage(
        pageIndex: Int,
        targetWidthPx: Int,
        autoCropMargins: Boolean
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        val cacheKey = if (autoCropMargins) -(pageIndex + 1) else pageIndex
        cache.get(cacheKey)?.let { existing ->
            if (!existing.isRecycled) return@withContext Result.success(existing)
        }

        mutex.withLock {
            cache.get(cacheKey)?.let { existing ->
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

                    val (outW, outH) = computeBoundedDimensions(
                        pageWidthPts = pageW,
                        pageHeightPts = pageH,
                        requestedWidthPx = targetWidthPx,
                        maxBytes = maxSinglePageBytes
                    )
                    val scale = outW.toFloat() / pageW

                    val rawBitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
                    val ctm = Matrix(scale, scale).translate(-bounds.x0, -bounds.y0)
                    drawDevice = AndroidDrawDevice(rawBitmap, 0, 0, 0, 0, outW, outH)
                    page.run(drawDevice, ctm, Cookie())
                    drawDevice.close()

                    val finalBitmap = if (autoCropMargins) cropLightMargins(rawBitmap) else rawBitmap
                    cache.put(cacheKey, finalBitmap)
                    Result.success(finalBitmap)
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

    override suspend fun extractReflowedPageText(pageIndex: Int): Result<String> = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val doc = document ?: Document.openDocument(filePath).also {
                    document = it
                    cachedPageCount = it.countPages()
                }
                val count = if (cachedPageCount >= 0) cachedPageCount else doc.countPages()
                if (pageIndex < 0 || pageIndex >= count) {
                    error("Invalid page $pageIndex")
                }
                val page = doc.loadPage(pageIndex)
                var sText: com.artifex.mupdf.fitz.StructuredText? = null
                try {
                    sText = page.toStructuredText()
                    val blocks = sText.blocks ?: emptyArray()
                    val paragraphs = blocks.mapNotNull { block ->
                        val lines = block.lines?.mapNotNull { line ->
                            line.chars?.joinToString("") { ch -> ch.c.toChar().toString() }?.trim()
                        }.orEmpty().filter { it.isNotBlank() }
                        if (lines.isEmpty()) null else dehyphenateLines(lines)
                    }
                    paragraphs.joinToString("\n\n").ifBlank {
                        "No selectable text detected on this page (scanned image)."
                    }
                } finally {
                    runCatching { sText?.destroy() }
                    runCatching { page.destroy() }
                }
            }
        }
    }

    override suspend fun loadOutline(): List<PdfOutlineEntry> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val doc = document ?: return@withLock emptyList()
            val out = mutableListOf<PdfOutlineEntry>()
            fun walk(nodes: Array<Outline>?, depth: Int) {
                if (nodes == null) return
                for (node in nodes) {
                    val loc = runCatching { doc.resolveLink(node) }.getOrNull()
                    val pageIdx = loc?.page ?: 0
                    if (!node.title.isNullOrBlank()) {
                        out.add(PdfOutlineEntry(title = node.title, pageIndex = pageIdx, depth = depth))
                    }
                    walk(node.down, depth + 1)
                }
            }
            runCatching { walk(doc.loadOutline(), 0) }
            out
        }
    }

    override suspend fun searchText(query: String, maxHits: Int): List<PdfSearchHit> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        mutex.withLock {
            val doc = document ?: return@withLock emptyList()
            val count = if (cachedPageCount >= 0) cachedPageCount else doc.countPages()
            val hits = mutableListOf<PdfSearchHit>()
            for (idx in 0 until count) {
                if (hits.size >= maxHits) break
                val page = doc.loadPage(idx)
                try {
                    val quads = page.search(query)
                    if (!quads.isNullOrEmpty()) {
                        hits.add(PdfSearchHit(pageIndex = idx, matchCount = quads.size))
                    }
                } finally {
                    runCatching { page.destroy() }
                }
            }
            hits
        }
    }

    override suspend fun findNormalizedSearchQuads(pageIndex: Int, query: String): List<NormalizedSearchQuad> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext emptyList()
            mutex.withLock {
                val doc = document ?: return@withLock emptyList()
                val count = if (cachedPageCount >= 0) cachedPageCount else doc.countPages()
                if (pageIndex < 0 || pageIndex >= count) return@withLock emptyList()
                val page = doc.loadPage(pageIndex)
                try {
                    val bounds = page.bounds
                    val w = max(1f, bounds.x1 - bounds.x0)
                    val h = max(1f, bounds.y1 - bounds.y0)
                    val hits = page.search(query) ?: return@withContext emptyList()
                    hits.flatMap { it.toList() }.map { q ->
                        val r = q.toRect()
                        NormalizedSearchQuad(
                            left = ((r.x0 - bounds.x0) / w).coerceIn(0f, 1f),
                            top = ((r.y0 - bounds.y0) / h).coerceIn(0f, 1f),
                            right = ((r.x1 - bounds.x0) / w).coerceIn(0f, 1f),
                            bottom = ((r.y1 - bounds.y0) / h).coerceIn(0f, 1f)
                        )
                    }
                } finally {
                    runCatching { page.destroy() }
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

        /** Joins wrapped PDF lines while de-hyphenating line-break word splits cleanly. */
        fun dehyphenateLines(lines: List<String>): String {
            if (lines.isEmpty()) return ""
            val sb = StringBuilder()
            lines.forEachIndexed { idx, rawLine ->
                val line = rawLine.trim()
                if (line.isEmpty()) return@forEachIndexed
                if (idx == 0) {
                    sb.append(line)
                } else if (sb.endsWith("-") && line.first().isLowerCase()) {
                    sb.deleteCharAt(sb.length - 1)
                    sb.append(line)
                } else {
                    sb.append(' ').append(line)
                }
            }
            return sb.toString()
        }

        /** Trims near-white borders from a rendered PDF bitmap with small padding. */
        fun cropLightMargins(source: Bitmap, whiteThreshold: Int = 246, paddingPx: Int = 12): Bitmap {
            val w = source.width
            val h = source.height
            if (w < 64 || h < 64) return source

            val step = 4
            var minX = w
            var minY = h
            var maxX = 0
            var maxY = 0
            val rowPixels = IntArray(w)

            for (y in 0 until h step step) {
                source.getPixels(rowPixels, 0, w, 0, y, w, 1)
                for (x in 0 until w step step) {
                    val px = rowPixels[x]
                    val r = (px shr 16) and 0xFF
                    val g = (px shr 8) and 0xFF
                    val b = px and 0xFF
                    if (r < whiteThreshold || g < whiteThreshold || b < whiteThreshold) {
                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                        if (y < minY) minY = y
                        if (y > maxY) maxY = y
                    }
                }
            }

            if (minX >= maxX || minY >= maxY) return source
            val left = (minX - paddingPx).coerceAtLeast(0)
            val top = (minY - paddingPx).coerceAtLeast(0)
            val right = (maxX + paddingPx).coerceAtMost(w)
            val bottom = (maxY + paddingPx).coerceAtMost(h)
            val cropW = right - left
            val cropH = bottom - top
            if (cropW <= 32 || cropH <= 32 || (cropW >= w - 16 && cropH >= h - 16)) {
                return source
            }
            return Bitmap.createBitmap(source, left, top, cropW, cropH)
        }
    }
}
