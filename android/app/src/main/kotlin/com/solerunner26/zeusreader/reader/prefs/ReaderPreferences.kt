package com.solerunner26.zeusreader.reader.prefs

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.solerunner26.zeusreader.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

internal val Context.readerDataStore: DataStore<Preferences> by preferencesDataStore(name = "reader_prefs")

enum class ReaderThemeMode(
    val label: String,
    val backgroundColor: Color,
    val textColor: Color,
    val surfaceColor: Color
) {
    DAY("Day", Color(0xFFFAF8F3), Color(0xFF1C1C19), Color(0xFFF0EDE4)),
    SEPIA("Sepia", Color(0xFFF4ECD8), Color(0xFF3B2F23), Color(0xFFE9DEC3)),
    NIGHT("Night", Color(0xFF181A1B), Color(0xFFE2E0DB), Color(0xFF232628)),
    PURE_BLACK_OLED("OLED", Color(0xFF000000), Color(0xFFE6E4DF), Color(0xFF121212)),
    E_INK("E-Ink", Color(0xFFFFFFFF), Color(0xFF000000), Color(0xFFF2F2F2))
}

enum class ReaderFontChoice(val label: String, val cssName: String) {
    LITERATA("Literata", "Literata"),
    ATKINSON("Atkinson", "Atkinson Hyperlegible"),
    NOTO_SERIF("Noto Serif", "serif"),
    NOTO_SANS("Noto Sans", "sans-serif"),
    NOTO_DEVANAGARI("Devanagari", "Noto Sans Devanagari"),
    NOTO_GUJARATI("Gujarati", "Noto Sans Gujarati");

    fun toComposeFontFamily(): FontFamily = when (this) {
        LITERATA -> FontFamily(Font(R.font.literata_regular))
        ATKINSON -> FontFamily(Font(R.font.atkinson_hyperlegible_regular))
        NOTO_SERIF -> FontFamily.Serif
        NOTO_SANS -> FontFamily.SansSerif
        NOTO_DEVANAGARI -> FontFamily(Font(R.font.noto_sans_devanagari_regular))
        NOTO_GUJARATI -> FontFamily(Font(R.font.noto_sans_gujarati_regular))
    }
}

enum class PdfFitMode(val label: String) {
    FIT_PAGE("Fit Page"),
    FIT_WIDTH("Fit Width")
}

data class ReaderSettings(
    val theme: ReaderThemeMode = ReaderThemeMode.DAY,
    val fontChoice: ReaderFontChoice = ReaderFontChoice.LITERATA,
    val fontSizePercent: Int = 100,
    val lineHeightMultiplier: Float = 1.45f,
    val horizontalMarginDp: Int = 20,
    val rotationLocked: Boolean = false,
    val volumeKeyPageTurn: Boolean = true,
    val edgeBrightnessSwipe: Boolean = true,
    val warmLightStrength: Float = 0f,
    val pdfAutoCropMargins: Boolean = false,
    val pdfRetainZoomAcrossPages: Boolean = true,
    val pdfFitMode: PdfFitMode = PdfFitMode.FIT_PAGE,
    val pdfReflowMode: Boolean = false
)

@Singleton
class ReaderPreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val FONT = stringPreferencesKey("font_choice")
        val FONT_SIZE = intPreferencesKey("font_size_percent")
        val LINE_HEIGHT = floatPreferencesKey("line_height_multiplier")
        val MARGIN_DP = intPreferencesKey("horizontal_margin_dp")
        val ROTATION_LOCK = booleanPreferencesKey("rotation_locked")
        val VOLUME_KEYS = booleanPreferencesKey("volume_key_page_turn")
        val BRIGHTNESS_SWIPE = booleanPreferencesKey("edge_brightness_swipe")
        val WARM_LIGHT = floatPreferencesKey("warm_light_strength")
        val PDF_AUTO_CROP = booleanPreferencesKey("pdf_auto_crop_margins")
        val PDF_RETAIN_ZOOM = booleanPreferencesKey("pdf_retain_zoom_across_pages")
        val PDF_FIT_MODE = stringPreferencesKey("pdf_fit_mode")
        val PDF_REFLOW = booleanPreferencesKey("pdf_reflow_mode")
    }

    val settings: Flow<ReaderSettings> = context.readerDataStore.data.map { prefs ->
        ReaderSettings(
            theme = prefs[Keys.THEME]?.let { runCatching { ReaderThemeMode.valueOf(it) }.getOrNull() }
                ?: ReaderThemeMode.DAY,
            fontChoice = prefs[Keys.FONT]?.let { runCatching { ReaderFontChoice.valueOf(it) }.getOrNull() }
                ?: ReaderFontChoice.LITERATA,
            fontSizePercent = (prefs[Keys.FONT_SIZE] ?: 100).coerceIn(80, 220),
            lineHeightMultiplier = (prefs[Keys.LINE_HEIGHT] ?: 1.45f).coerceIn(1.1f, 2.0f),
            horizontalMarginDp = (prefs[Keys.MARGIN_DP] ?: 20).coerceIn(8, 48),
            rotationLocked = prefs[Keys.ROTATION_LOCK] ?: false,
            volumeKeyPageTurn = prefs[Keys.VOLUME_KEYS] ?: true,
            edgeBrightnessSwipe = prefs[Keys.BRIGHTNESS_SWIPE] ?: true,
            warmLightStrength = (prefs[Keys.WARM_LIGHT] ?: 0f).coerceIn(0f, 0.45f),
            pdfAutoCropMargins = prefs[Keys.PDF_AUTO_CROP] ?: false,
            pdfRetainZoomAcrossPages = prefs[Keys.PDF_RETAIN_ZOOM] ?: true,
            pdfFitMode = prefs[Keys.PDF_FIT_MODE]?.let { runCatching { PdfFitMode.valueOf(it) }.getOrNull() }
                ?: PdfFitMode.FIT_PAGE,
            pdfReflowMode = prefs[Keys.PDF_REFLOW] ?: false
        )
    }

    suspend fun update(transform: (ReaderSettings) -> ReaderSettings) {
        context.readerDataStore.edit { prefs ->
            val current = ReaderSettings(
                theme = prefs[Keys.THEME]?.let { runCatching { ReaderThemeMode.valueOf(it) }.getOrNull() }
                    ?: ReaderThemeMode.DAY,
                fontChoice = prefs[Keys.FONT]?.let { runCatching { ReaderFontChoice.valueOf(it) }.getOrNull() }
                    ?: ReaderFontChoice.LITERATA,
                fontSizePercent = prefs[Keys.FONT_SIZE] ?: 100,
                lineHeightMultiplier = prefs[Keys.LINE_HEIGHT] ?: 1.45f,
                horizontalMarginDp = prefs[Keys.MARGIN_DP] ?: 20,
                rotationLocked = prefs[Keys.ROTATION_LOCK] ?: false,
                volumeKeyPageTurn = prefs[Keys.VOLUME_KEYS] ?: true,
                edgeBrightnessSwipe = prefs[Keys.BRIGHTNESS_SWIPE] ?: true,
                warmLightStrength = prefs[Keys.WARM_LIGHT] ?: 0f,
                pdfAutoCropMargins = prefs[Keys.PDF_AUTO_CROP] ?: false,
                pdfRetainZoomAcrossPages = prefs[Keys.PDF_RETAIN_ZOOM] ?: true,
                pdfFitMode = prefs[Keys.PDF_FIT_MODE]?.let { runCatching { PdfFitMode.valueOf(it) }.getOrNull() }
                    ?: PdfFitMode.FIT_PAGE,
                pdfReflowMode = prefs[Keys.PDF_REFLOW] ?: false
            )
            val next = transform(current)
            prefs[Keys.THEME] = next.theme.name
            prefs[Keys.FONT] = next.fontChoice.name
            prefs[Keys.FONT_SIZE] = next.fontSizePercent.coerceIn(80, 220)
            prefs[Keys.LINE_HEIGHT] = next.lineHeightMultiplier.coerceIn(1.1f, 2.0f)
            prefs[Keys.MARGIN_DP] = next.horizontalMarginDp.coerceIn(8, 48)
            prefs[Keys.ROTATION_LOCK] = next.rotationLocked
            prefs[Keys.VOLUME_KEYS] = next.volumeKeyPageTurn
            prefs[Keys.BRIGHTNESS_SWIPE] = next.edgeBrightnessSwipe
            prefs[Keys.WARM_LIGHT] = next.warmLightStrength.coerceIn(0f, 0.45f)
            prefs[Keys.PDF_AUTO_CROP] = next.pdfAutoCropMargins
            prefs[Keys.PDF_RETAIN_ZOOM] = next.pdfRetainZoomAcrossPages
            prefs[Keys.PDF_FIT_MODE] = next.pdfFitMode.name
            prefs[Keys.PDF_REFLOW] = next.pdfReflowMode
        }
    }
}
