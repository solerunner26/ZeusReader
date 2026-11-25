package com.solerunner26.zeusreader.reader.prefs

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.solerunner26.zeusreader.ui.theme.ReaderSurface
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppUiConfigMigrationTest {

    @Test
    fun `v1 theme names map to v2 ReaderSurface without data loss`() {
        assertEquals(ReaderSurface.PAPER, AppUiConfigRepository.mapLegacyThemeNameToSurface("DAY"))
        assertEquals(ReaderSurface.DUSK, AppUiConfigRepository.mapLegacyThemeNameToSurface("SEPIA"))
        assertEquals(ReaderSurface.NIGHT, AppUiConfigRepository.mapLegacyThemeNameToSurface("NIGHT"))
        assertEquals(ReaderSurface.OLED, AppUiConfigRepository.mapLegacyThemeNameToSurface("PURE_BLACK_OLED"))
        assertEquals(ReaderSurface.EINK, AppUiConfigRepository.mapLegacyThemeNameToSurface("E_INK"))
        assertEquals(ReaderSurface.PAPER, AppUiConfigRepository.mapLegacyThemeNameToSurface(null))
    }

    @Test
    fun `migrates legacy reader_prefs Sepia into AppUiConfig Dusk and persists hue`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val legacyRepo = ReaderPreferencesRepository(context)
        legacyRepo.update { it.copy(theme = ReaderThemeMode.SEPIA) }

        val uiConfigRepo = AppUiConfigRepository(context)
        uiConfigRepo.migrateFromV1IfNeeded()
        val migrated = uiConfigRepo.config.first()
        assertEquals(ReaderSurface.DUSK, migrated.readerSurface)

        uiConfigRepo.update { it.copy(themeHue = 140f, focusRibbon = true) }
        val updated = uiConfigRepo.config.first()
        assertEquals(140f, updated.themeHue, 0.01f)
        assertEquals(true, updated.focusRibbon)
    }
}
