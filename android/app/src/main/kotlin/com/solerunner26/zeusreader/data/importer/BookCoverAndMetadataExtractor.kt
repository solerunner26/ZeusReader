package com.solerunner26.zeusreader.data.importer

import android.content.Context
import android.graphics.Bitmap
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

data class ExtractedBookMetadata(
    val coverPath: String? = null,
    val authors: String? = null,
    val category: String = "General"
)

/**
 * Extracts embedded covers from EPUB/PDF files and optionally enriches
 * missing author/category/cover from Open Library when online.
 */
@Singleton
class BookCoverAndMetadataExtractor @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val coversDir: File by lazy {
        File(context.filesDir, "covers").apply { mkdirs() }
    }

    suspend fun extractAndEnrich(
        editionId: String,
        title: String,
        existingAuthors: String,
        format: String,
        localPath: String?,
        allowNetworkLookup: Boolean = true
    ): ExtractedBookMetadata = withContext(Dispatchers.IO) {
        val coverFile = File(coversDir, "$editionId.jpg")
        var resolvedCoverPath: String? = if (coverFile.exists() && coverFile.length() > 0L) {
            coverFile.absolutePath
        } else {
            null
        }
        var resolvedAuthors = existingAuthors.takeIf { it.isNotBlank() }
        var resolvedCategory = inferCategoryFromTitle(title)

        if (resolvedCoverPath == null && !localPath.isNullOrBlank()) {
            val sourceFile = File(localPath)
            if (sourceFile.exists()) {
                when (format.uppercase()) {
                    "EPUB" -> {
                        if (extractEpubCover(sourceFile, coverFile)) {
                            resolvedCoverPath = coverFile.absolutePath
                        }
                    }
                    "PDF", "XPS", "OXPS" -> {
                        if (extractPdfPageZeroCover(sourceFile, coverFile)) {
                            resolvedCoverPath = coverFile.absolutePath
                        }
                    }
                }
            }
        }

        if (allowNetworkLookup && (resolvedCoverPath == null || resolvedAuthors == null || resolvedCategory == "General")) {
            val remote = fetchOpenLibraryMetadata(title)
            if (remote != null) {
                if (resolvedAuthors == null && !remote.author.isNullOrBlank()) {
                    resolvedAuthors = remote.author
                }
                if (resolvedCategory == "General" && remote.category != "General") {
                    resolvedCategory = remote.category
                }
                if (resolvedCoverPath == null && remote.coverId != null) {
                    if (downloadOpenLibraryCover(remote.coverId, coverFile)) {
                        resolvedCoverPath = coverFile.absolutePath
                    }
                }
            }
        }

        ExtractedBookMetadata(
            coverPath = resolvedCoverPath,
            authors = resolvedAuthors,
            category = resolvedCategory
        )
    }

    private fun extractEpubCover(epubFile: File, targetFile: File): Boolean = runCatching {
        ZipFile(epubFile).use { zip ->
            val entries = zip.entries().asSequence().toList()
            val coverEntry = entries.firstOrNull { entry ->
                val name = entry.name.lowercase()
                !entry.isDirectory &&
                    (name.contains("cover") || name.endsWith("titlepage.jpg") || name.endsWith("titlepage.png")) &&
                    (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp"))
            } ?: entries.firstOrNull { entry ->
                val name = entry.name.lowercase()
                !entry.isDirectory && (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png"))
            } ?: return@runCatching false

            zip.getInputStream(coverEntry).use { input ->
                FileOutputStream(targetFile).use { out -> input.copyTo(out) }
            }
            targetFile.exists() && targetFile.length() > 0L
        }
    }.getOrDefault(false)

    private fun extractPdfPageZeroCover(pdfFile: File, targetFile: File): Boolean = runCatching {
        val doc = Document.openDocument(pdfFile.absolutePath)
        try {
            if (doc.countPages() <= 0) return@runCatching false
            val page = doc.loadPage(0)
            try {
                val bounds = page.bounds
                val width = (bounds.x1 - bounds.x0).coerceAtLeast(1f)
                val scale = (360f / width).coerceIn(0.25f, 2f)
                val bitmap = AndroidDrawDevice.drawPage(page, Matrix(scale))
                try {
                    FileOutputStream(targetFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 86, out)
                    }
                } finally {
                    bitmap.recycle()
                }
                targetFile.exists() && targetFile.length() > 0L
            } finally {
                page.destroy()
            }
        } finally {
            doc.destroy()
        }
    }.getOrDefault(false)

    private data class OpenLibraryHit(
        val author: String?,
        val coverId: Int?,
        val category: String
    )

    private fun fetchOpenLibraryMetadata(title: String): OpenLibraryHit? = runCatching {
        val cleanTitle = title.replace(Regex("\\([^)]*\\)"), "").trim()
        if (cleanTitle.length < 3) return@runCatching null
        val encoded = URLEncoder.encode(cleanTitle, "UTF-8")
        val conn = (URL("https://openlibrary.org/search.json?title=$encoded&limit=1").openConnection() as HttpURLConnection).apply {
            connectTimeout = 4_000
            readTimeout = 4_000
        }
        try {
            if (conn.responseCode !in 200..299) return@runCatching null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val docs = JSONObject(body).optJSONArray("docs") ?: return@runCatching null
            if (docs.length() == 0) return@runCatching null
            val doc = docs.getJSONObject(0)
            val author = doc.optJSONArray("author_name")?.optString(0)?.takeIf { it.isNotBlank() }
            val coverId = if (doc.has("cover_i")) doc.optInt("cover_i") else null
            val subjects = buildString {
                val arr = doc.optJSONArray("subject")
                if (arr != null) {
                    for (i in 0 until minOf(arr.length(), 8)) {
                        append(arr.optString(i).lowercase()).append(' ')
                    }
                }
            }
            OpenLibraryHit(
                author = author,
                coverId = coverId,
                category = inferCategoryFromText("$cleanTitle $subjects")
            )
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    private fun downloadOpenLibraryCover(coverId: Int, targetFile: File): Boolean = runCatching {
        val conn = (URL("https://covers.openlibrary.org/b/id/$coverId-M.jpg").openConnection() as HttpURLConnection).apply {
            connectTimeout = 4_000
            readTimeout = 6_000
        }
        try {
            if (conn.responseCode !in 200..299) return@runCatching false
            conn.inputStream.use { input ->
                FileOutputStream(targetFile).use { out -> input.copyTo(out) }
            }
            targetFile.exists() && targetFile.length() > 100L
        } finally {
            conn.disconnect()
        }
    }.getOrDefault(false)

    companion object {
        val RACK_CATEGORIES = listOf(
            "All",
            "Fiction",
            "Non-Fiction",
            "Self-Help",
            "History",
            "Autobiography",
            "Science",
            "General"
        )

        fun inferCategoryFromTitle(title: String): String = inferCategoryFromText(title)

        fun inferCategoryFromText(text: String): String {
            val lower = text.lowercase()
            return when {
                "autobiography" in lower || "biography" in lower || "lincoln" in lower || "memoir" in lower -> "Autobiography"
                "history" in lower || "empire" in lower || "war" in lower || "civilization" in lower -> "History"
                "habit" in lower || "self-help" in lower || "mindset" in lower || "psychology" in lower || "atomic" in lower -> "Self-Help"
                "science" in lower || "physics" in lower || "math" in lower || "biology" in lower || "cosmos" in lower -> "Science"
                "fiction" in lower || "novel" in lower || "alice" in lower || "2 states" in lower || "stories" in lower -> "Fiction"
                "essay" in lower || "economics" in lower || "philosophy" in lower || "non-fiction" in lower -> "Non-Fiction"
                else -> "General"
            }
        }
    }
}
