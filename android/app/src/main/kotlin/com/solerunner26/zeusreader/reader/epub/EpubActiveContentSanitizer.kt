package com.solerunner26.zeusreader.reader.epub

/**
 * Strips publication-supplied active content (scripts, inline event handlers, javascript: URIs,
 * iframe/object/embed tags, and external script files) while preserving Readium's own internal
 * navigation scripts injected later by EpubNavigatorFragment.
 */
object EpubActiveContentSanitizer {

    private val SCRIPT_TAG_REGEX = Regex(
        pattern = "<script\\b[^>]*>[\\s\\S]*?</script>|<script\\b[^>]*/?>",
        options = setOf(RegexOption.IGNORE_CASE)
    )
    private val ACTIVE_EMBED_TAG_REGEX = Regex(
        pattern = "<(iframe|object|embed|applet)\\b[^>]*>([\\s\\S]*?</\\1>)?",
        options = setOf(RegexOption.IGNORE_CASE)
    )
    private val INLINE_EVENT_HANDLER_REGEX = Regex(
        pattern = "\\s+on[a-z]+\\s*=\\s*(?:\"[^\"]*\"|'[^']*'|[^\\s>]+)",
        options = setOf(RegexOption.IGNORE_CASE)
    )
    private val JAVASCRIPT_URI_REGEX = Regex(
        pattern = "(href|src|action)\\s*=\\s*([\"'])\\s*javascript:[^\"']*\\2",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    fun shouldBlockPublicationResource(href: String, mediaType: String? = null): Boolean {
        val lowerHref = href.lowercase()
        val lowerType = mediaType?.lowercase().orEmpty()
        return lowerHref.endsWith(".js") ||
            lowerHref.endsWith(".mjs") ||
            "javascript" in lowerType ||
            "ecmascript" in lowerType
    }

    fun sanitizeXhtml(xhtml: String): String {
        var cleaned = SCRIPT_TAG_REGEX.replace(xhtml, "")
        cleaned = ACTIVE_EMBED_TAG_REGEX.replace(cleaned, "")
        cleaned = INLINE_EVENT_HANDLER_REGEX.replace(cleaned, "")
        cleaned = JAVASCRIPT_URI_REGEX.replace(cleaned, "$1=$2#$2")
        return cleaned
    }
}
