package com.solerunner26.zeusreader.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubAppUpdaterTest {

    @Test
    fun `isNewerVersion compares semantic versions accurately`() {
        assertTrue(GitHubAppUpdater.isNewerVersion("v0.5.0-private", "0.4.0-private"))
        assertTrue(GitHubAppUpdater.isNewerVersion("v0.4.1-private", "0.4.0-private"))
        assertTrue(GitHubAppUpdater.isNewerVersion("v1.0.0", "0.4.0-private"))
        assertFalse(GitHubAppUpdater.isNewerVersion("v0.4.0-private", "0.4.0-private"))
        assertFalse(GitHubAppUpdater.isNewerVersion("v0.3.0-private", "0.4.0-private"))
    }

    @Test
    fun `parseLatestReleaseJson extracts apk asset and release notes when newer`() {
        val sampleJson = """
            {
              "tag_name": "v0.5.0-private",
              "name": "ZeusReader 0.5.0",
              "body": "- Faster PDF OCR\n- New Cloudflare AI summaries",
              "draft": false,
              "published_at": "2026-12-01T10:00:00Z",
              "assets": [
                {
                  "name": "zeusreader-0.5.0-private.apk",
                  "size": 150000000,
                  "browser_download_url": "https://github.com/solerunner26/ZeusReader/releases/download/v0.5.0-private/zeusreader-0.5.0-private.apk"
                }
              ]
            }
        """.trimIndent()

        val parsed = GitHubAppUpdater.parseLatestReleaseJson(sampleJson, "0.4.0-private")
        assertNotNull(parsed)
        assertEquals("v0.5.0-private", parsed?.tagName)
        assertTrue(parsed?.releaseNotes?.contains("Faster PDF OCR") == true)
        assertEquals(150000000L, parsed?.apkSizeBytes)

        val sameVersion = GitHubAppUpdater.parseLatestReleaseJson(sampleJson, "0.5.0-private")
        assertNull(sameVersion)
    }
}
