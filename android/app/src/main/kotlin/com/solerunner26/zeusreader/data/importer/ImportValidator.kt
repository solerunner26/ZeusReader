package com.solerunner26.zeusreader.data.importer

import java.io.File
import java.io.FileInputStream
import java.util.Locale
import java.util.zip.ZipInputStream

enum class SupportedImportFormat(val extension: String, val dbFormatName: String) {
    EPUB("epub", "EPUB"),
    PDF("pdf", "PDF"),
    XPS("xps", "XPS"),
    OXPS("oxps", "OXPS"),
    CBZ("cbz", "CBZ"),
    HTML("html", "HTML"),
    TIFF("tiff", "TIFF"),
    JPG("jpg", "JPG"),
    PNG("png", "PNG");

    val isImage: Boolean
        get() = this == JPG || this == PNG
}

sealed interface ImportValidationResult {
    data class Valid(val format: SupportedImportFormat) : ImportValidationResult
    data class Rejected(val reason: String, val detectedSignature: String) : ImportValidationResult
}

/**
 * Magic-byte and ZIP container validator for local imports.
 * Never trusts file extensions alone; rejects disguised APKs, executables, DRM, and scripts.
 */
object ImportValidator {

    private val PDF_MAGIC = "%PDF-".toByteArray(Charsets.US_ASCII)
    private val PNG_MAGIC = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    )
    private val JPG_MAGIC = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    private val TIFF_LE_MAGIC = byteArrayOf(0x49, 0x49, 0x2A, 0x00)
    private val TIFF_BE_MAGIC = byteArrayOf(0x4D, 0x4D, 0x00, 0x2A)
    private val ZIP_MAGIC = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
    private val PE_EXE_MAGIC = byteArrayOf(0x4D, 0x5A) // MZ
    private val ELF_MAGIC = byteArrayOf(0x7F, 0x45, 0x4C, 0x46) // \x7fELF
    private val SHEBANG_MAGIC = byteArrayOf(0x23, 0x21) // #!

    fun validateFile(file: File, declaredFileName: String): ImportValidationResult {
        if (!file.exists() || file.length() == 0L) {
            return ImportValidationResult.Rejected(
                reason = "Empty or missing file",
                detectedSignature = "empty"
            )
        }

        val header = ByteArray(2048)
        val readCount = FileInputStream(file).use { input ->
            input.read(header)
        }.coerceAtLeast(0)
        val prefix = header.copyOf(readCount)

        return validateBytes(
            header = prefix,
            declaredFileName = declaredFileName,
            zipMetadataProvider = { readZipContainerInfo(file) }
        )
    }

    internal data class ZipContainerInfo(
        val entryNames: Set<String>,
        val mimetypeContent: String?
    )

    internal fun validateBytes(
        header: ByteArray,
        declaredFileName: String,
        zipMetadataProvider: (() -> ZipContainerInfo)? = null
    ): ImportValidationResult {
        if (header.isEmpty()) {
            return ImportValidationResult.Rejected("Empty file", "empty")
        }

        if (startsWith(header, PE_EXE_MAGIC) || startsWith(header, ELF_MAGIC) || startsWith(header, SHEBANG_MAGIC)) {
            return ImportValidationResult.Rejected(
                reason = "Executable or script signature rejected",
                detectedSignature = "executable_or_script"
            )
        }

        val ext = declaredFileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (ext == "oxp") {
            return ImportValidationResult.Rejected(
                reason = "Unsupported .oxp format (did you mean .oxps?). Retained in Unsupported list.",
                detectedSignature = "oxp"
            )
        }
        if (ext == "kfx") {
            return ImportValidationResult.Rejected(
                reason = "Kindle KFX format is DRM/proprietary and unsupported.",
                detectedSignature = "kfx"
            )
        }
        if (ext in setOf("apk", "exe", "sh", "bat", "ps1")) {
            return ImportValidationResult.Rejected(
                reason = "Unsafe executable/script extension .$ext rejected",
                detectedSignature = ext
            )
        }

        // ZIP-based containers: EPUB, XPS, OXPS, CBZ (and disguised APK detection)
        if (startsWith(header, ZIP_MAGIC)) {
            val zipInfo = runCatching { zipMetadataProvider?.invoke() }.getOrNull()
            val entries = zipInfo?.entryNames.orEmpty()
            if ("AndroidManifest.xml" in entries || "classes.dex" in entries) {
                return ImportValidationResult.Rejected(
                    reason = "Disguised Android APK rejected (expected ${ext.ifBlank { "book" }})",
                    detectedSignature = "android_apk"
                )
            }
            if ("META-INF/encryption.xml" in entries) {
                return ImportValidationResult.Rejected(
                    reason = "DRM-encrypted EPUB detected and rejected.",
                    detectedSignature = "drm_epub"
                )
            }

            if (ext == "epub" || ext.isBlank()) {
                val mimetype = zipInfo?.mimetypeContent?.trim()
                if (mimetype == "application/epub+zip" && "META-INF/container.xml" in entries) {
                    return ImportValidationResult.Valid(SupportedImportFormat.EPUB)
                }
                if (ext == "epub") {
                    return ImportValidationResult.Rejected(
                        reason = "Invalid EPUB container (missing application/epub+zip mimetype or container.xml)",
                        detectedSignature = "zip_non_epub"
                    )
                }
            }

            if (ext == "xps" || ext == "oxps") {
                val isXpsContainer = "[Content_Types].xml" in entries &&
                    entries.any {
                        it.equals("FixedDocumentSequence.fdseq", ignoreCase = true) ||
                            it.endsWith(".fpage", ignoreCase = true) ||
                            it.startsWith("Documents/", ignoreCase = true)
                    }
                if (isXpsContainer) {
                    return ImportValidationResult.Valid(
                        if (ext == "oxps") SupportedImportFormat.OXPS else SupportedImportFormat.XPS
                    )
                }
                return ImportValidationResult.Rejected(
                    reason = "Invalid .$ext container structure",
                    detectedSignature = "zip_invalid_$ext"
                )
            }

            if (ext == "cbz") {
                val hasImages = entries.any { name ->
                    val lower = name.lowercase(Locale.ROOT)
                    lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                        lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".gif")
                }
                if (hasImages) {
                    return ImportValidationResult.Valid(SupportedImportFormat.CBZ)
                }
                return ImportValidationResult.Rejected(
                    reason = "CBZ archive contains no supported comic images",
                    detectedSignature = "cbz_empty"
                )
            }

            return ImportValidationResult.Rejected(
                reason = "Disguised ZIP archive rejected for .$ext import",
                detectedSignature = "zip_container"
            )
        }

        val detected = when {
            startsWith(header, PDF_MAGIC) -> SupportedImportFormat.PDF
            startsWith(header, JPG_MAGIC) -> SupportedImportFormat.JPG
            startsWith(header, PNG_MAGIC) -> SupportedImportFormat.PNG
            startsWith(header, TIFF_LE_MAGIC) || startsWith(header, TIFF_BE_MAGIC) -> SupportedImportFormat.TIFF
            (ext == "htm" || ext == "html") && looksLikeHtml(header) -> SupportedImportFormat.HTML
            else -> null
        } ?: return ImportValidationResult.Rejected(
            reason = "Unrecognized or unsupported file signature",
            detectedSignature = if (ext.isNotBlank()) ext else "unknown"
        )

        val expectedGroup = when (ext) {
            "epub" -> SupportedImportFormat.EPUB
            "pdf" -> SupportedImportFormat.PDF
            "xps" -> SupportedImportFormat.XPS
            "oxps" -> SupportedImportFormat.OXPS
            "cbz" -> SupportedImportFormat.CBZ
            "htm", "html" -> SupportedImportFormat.HTML
            "tif", "tiff" -> SupportedImportFormat.TIFF
            "jpg", "jpeg" -> SupportedImportFormat.JPG
            "png" -> SupportedImportFormat.PNG
            "" -> detected
            else -> null
        }
        if (expectedGroup != null && expectedGroup != detected && !(expectedGroup.isImage && detected.isImage)) {
            return ImportValidationResult.Rejected(
                reason = "File content ($detected) does not match extension .$ext",
                detectedSignature = detected.extension
            )
        }

        return ImportValidationResult.Valid(detected)
    }

    private fun looksLikeHtml(header: ByteArray): Boolean {
        val text = header.toString(Charsets.UTF_8).trimStart('\uFEFF', ' ', '\t', '\r', '\n').lowercase(Locale.ROOT)
        return text.startsWith("<!doctype html") ||
            text.startsWith("<html") ||
            text.startsWith("<head") ||
            text.startsWith("<body") ||
            text.startsWith("<meta") ||
            text.startsWith("<title") ||
            text.startsWith("<h1") ||
            text.startsWith("<p")
    }

    private fun readZipContainerInfo(file: File): ZipContainerInfo {
        val entries = linkedSetOf<String>()
        var mimetypeContent: String? = null
        ZipInputStream(FileInputStream(file)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null && entries.size < 256) {
                entries.add(entry.name)
                if (entry.name == "mimetype") {
                    mimetypeContent = zip.readBytes().toString(Charsets.UTF_8)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return ZipContainerInfo(entries, mimetypeContent)
    }

    private fun startsWith(data: ByteArray, prefix: ByteArray): Boolean {
        if (data.size < prefix.size) return false
        for (i in prefix.indices) {
            if (data[i] != prefix[i]) return false
        }
        return true
    }
}
