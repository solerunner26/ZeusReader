package com.solerunner26.zeusreader.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.github.takahirom.roborazzi.captureRoboImage
import com.solerunner26.zeusreader.reader.prefs.AppUiConfig
import com.solerunner26.zeusreader.ui.settings.SettingsScreenContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w412dp-h915dp-xxhdpi")
class Phase3RRoborazziTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `settings screen renders across Saffron, Moss, and Indigo Dusk hues`() {
        var hue by mutableFloatStateOf(32f)
        var dark by mutableStateOf(false)

        composeRule.setContent {
            ZeusReaderTheme(hue = hue, darkTheme = dark, animateHue = false) {
                SettingsScreenContent(
                    config = AppUiConfig(themeHue = hue, auraIntensity = 0f),
                    darkTheme = dark,
                    onBack = {},
                    onSelectAppTheme = {},
                    onSelectLanguage = {},
                    onSelectPreset = {},
                    onToggleHueFromCover = {},
                    onAuraIntensityChange = {},
                    onSelectReaderSurface = {},
                    onToggleFocusRibbon = {},
                    onToggleGlassUi = {},
                    onToggleAestheticElements = {},
                    onToggleMasterAnimations = {}
                )
            }
        }

        for (h in listOf(32f, 140f, 250f)) {
            for (d in listOf(false, true)) {
                hue = h
                dark = d
                composeRule.waitForIdle()
                composeRule.onNode(androidx.compose.ui.test.hasTestTag("settings_screen_root"))
                    .captureRoboImage("src/test/snapshots/settings_h${h.toInt()}_d$d.png")
            }
        }
    }
}
