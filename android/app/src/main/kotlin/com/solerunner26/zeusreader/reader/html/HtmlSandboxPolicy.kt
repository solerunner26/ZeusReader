package com.solerunner26.zeusreader.reader.html

import com.solerunner26.zeusreader.reader.epub.EpubActiveContentSanitizer
import java.io.File
import java.net.URI

/**
 * Security policy for sandboxed HTML reading (`GEMINI.md §3.1, §4.1, §6`):
 * - JavaScript completely disabled
 * - Zero outbound network requests (`http`, `https`, `ws`, `ftp`, etc. blocked)
 * - Local relative image/stylesheet requests allowed ONLY inside [allowedBaseDir]
 */
object HtmlSandboxPolicy {

    fun isRequestAllowed(requestUrl: String, allowedBaseDir: File): Boolean {
        val lower = requestUrl.lowercase().trim()
        if (lower.startsWith("http://") ||
            lower.startsWith("https://") ||
            lower.startsWith("ws://") ||
            lower.startsWith("wss://") ||
            lower.startsWith("ftp://") ||
            lower.startsWith("javascript:")
        ) {
            return false
        }
        if (lower.startsWith("file:/")) {
            val targetFile = runCatching { File(URI(requestUrl)).canonicalFile }.getOrNull() ?: return false
            val baseCanonical = runCatching { allowedBaseDir.canonicalFile }.getOrNull() ?: return false
            return targetFile.path.startsWith(baseCanonical.path + File.separator) || targetFile == baseCanonical
        }
        return false
    }

    fun sanitizeHtmlDocument(rawHtml: String): String =
        EpubActiveContentSanitizer.sanitizeXhtml(rawHtml)
}
