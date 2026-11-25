package com.solerunner26.zeusreader.reader.epub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EpubActiveContentSanitizerTest {

    @Test
    fun stripsPublicationScriptsEventHandlersAndJavascriptUrisWhileKeepingContent() {
        val dirtyXhtml = """
            <html xmlns="http://www.w3.org/1999/xhtml">
            <head>
              <script src="malicious.js"></script>
              <script>alert('xss')</script>
            </head>
            <body onload="stealCookies()">
              <p onclick="track()">Capillary action draws water upward.</p>
              <a href="javascript:evil()">Bad link</a>
              <iframe src="https://evil.example/track"></iframe>
              <img src="images/fig4_2.png" alt="Diagram"/>
            </body>
            </html>
        """.trimIndent()

        val cleaned = EpubActiveContentSanitizer.sanitizeXhtml(dirtyXhtml)
        assertFalse(cleaned.contains("<script", ignoreCase = true))
        assertFalse(cleaned.contains("alert(", ignoreCase = true))
        assertFalse(cleaned.contains("onload=", ignoreCase = true))
        assertFalse(cleaned.contains("onclick=", ignoreCase = true))
        assertFalse(cleaned.contains("javascript:", ignoreCase = true))
        assertFalse(cleaned.contains("<iframe", ignoreCase = true))
        assertTrue(cleaned.contains("Capillary action draws water upward."))
        assertTrue(cleaned.contains("images/fig4_2.png"))
    }

    @Test
    fun blocksJavascriptResourceHrefsAndMimeTypes() {
        assertTrue(EpubActiveContentSanitizer.shouldBlockPublicationResource("OEBPS/js/book.js"))
        assertTrue(EpubActiveContentSanitizer.shouldBlockPublicationResource("OEBPS/app.mjs"))
        assertTrue(EpubActiveContentSanitizer.shouldBlockPublicationResource("OEBPS/x", "application/javascript"))
        assertFalse(EpubActiveContentSanitizer.shouldBlockPublicationResource("OEBPS/ch1.xhtml", "application/xhtml+xml"))
        assertFalse(EpubActiveContentSanitizer.shouldBlockPublicationResource("OEBPS/style.css", "text/css"))
    }
}
