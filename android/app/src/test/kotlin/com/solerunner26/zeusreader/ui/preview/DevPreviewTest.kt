package com.solerunner26.zeusreader.ui.preview

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.solerunner26.zeusreader.ui.home.HomeContent
import com.solerunner26.zeusreader.ui.theme.ZeusPaper
import com.solerunner26.zeusreader.ui.theme.ZeusSky
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Developer previews written to build/preview (not baselines). Run with -Pzeus.preview=true. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class DevPreviewTest {
    @get:Rule
    val rule = createComposeRule()

    private fun out(name: String) = "build/preview/$name.png"

    @Test
    fun home() {
        var sky = ZeusSky.STORM
        var light = false
        rule.setContent {
            PreviewSky(sky = sky, paper = if (light) ZeusPaper.CLOUD else ZeusPaper.NIGHT, daylight = light) {
                HomeContent(
                    books = PreviewFixtures.books,
                    featuredSparkText = "Every gram of healthy soil is a city of living things.",
                    streakDays = 12,
                    totalBooks = 5,
                    booksInProgress = 2,
                    dueVocabWords = 6,
                    glassEnabled = true,
                    onOpenBook = {}, onOpenLibrary = {}, onOpenPassages = {}, onOpenVocab = {},
                    onOpenOnline = {}, onOpenStats = {}, onOpenPrepareOffline = {}, onOpenSettings = {}
                )
            }
        }
        rule.onRoot().captureRoboImage(out("home_storm_dark"))
    }
}
