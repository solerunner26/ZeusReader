package com.solerunner26.zeusreader.ui.sunset

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Sunset lock-in: screen code must use tokens from ui/theme.
 * Fails on `Color(0x…)`, hex colour strings and raw `.dp` / `.sp` literals outside ui/theme/.
 */
class ZeusTokenGuardTest {

    private val root: File = listOf(
        File("src/main/kotlin/com/solerunner26/zeusreader"),
        File("app/src/main/kotlin/com/solerunner26/zeusreader")
    ).first { it.exists() }

    private val rawColor = Regex("""Color\(\s*0x""")
    private val hexString = Regex(""""#[0-9A-Fa-f]{6,8}"""")
    private val rawDpSp = Regex("""(?<![\w.])\d+(\.\d+)?f?\.(dp|sp)\b""")

    private fun screenFiles(): List<File> {
        val ui = File(root, "ui").walkTopDown().filter { it.isFile && it.extension == "kt" }
            .filterNot { it.path.replace('\\', '/').contains("/ui/theme/") }
            .toList()
        return ui + File(root, "Navigation.kt")
    }

    @Test
    fun `screen code uses Stormlight tokens only`() {
        val violations = mutableListOf<String>()
        screenFiles().forEach { file ->
            file.readLines().forEachIndexed { idx, raw ->
                val line = raw.substringBefore("//")
                listOf(rawColor to "raw Color(0x…)", hexString to "hex colour string", rawDpSp to "raw .dp/.sp literal")
                    .forEach { (regex, what) ->
                        if (regex.containsMatchIn(line)) {
                            violations += "${file.relativeTo(root)}:${idx + 1}: $what → ${raw.trim()}"
                        }
                    }
            }
        }
        assertTrue(
            "Use Sunset tokens (ZeusSpacing/ZeusSizes/ZeusType/ZeusColors) instead:\n" + violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun `guard scans the real source tree`() {
        assertTrue("Token guard found no screen files", screenFiles().size > 20)
    }
}
