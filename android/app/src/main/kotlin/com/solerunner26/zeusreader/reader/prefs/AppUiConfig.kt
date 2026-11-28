package com.solerunner26.zeusreader.reader.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.solerunner26.zeusreader.ui.theme.HuePreset
import com.solerunner26.zeusreader.ui.theme.ReaderSurface
import com.solerunner26.zeusreader.ui.theme.normalizeHue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appUiConfigDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_ui_config")

enum class AppTheme(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System")
}

enum class AppLanguage(val tag: String, val label: String) {
    EN("en", "English"),
    HI("hi", "हिन्दी"),
    GU("gu", "ગુજરાતી")
}

data class AppUiConfig(
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val themeHue: Float = HuePreset.DEFAULT_HUE,
    val hueFromCover: Boolean = false,
    val auraIntensity: Float = 0.4f,
    val readerSurface: ReaderSurface = ReaderSurface.PAPER,
    val focusRibbon: Boolean = false,
    val glassUi: Boolean = true,
    val aestheticElements: Boolean = true,
    val animatedGlare: Boolean = true,
    val animatedBorders: Boolean = true,
    val masterAnimations: Boolean = true,
    val embossedEffects: Boolean = false,
    val pointerHoverEffects: Boolean = true,
    val appLanguage: AppLanguage = AppLanguage.EN
)

@Singleton
class AppUiConfigRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private object Keys {
        val APP_THEME = stringPreferencesKey("app_theme")
        val THEME_HUE = floatPreferencesKey("theme_hue")
        val HUE_FROM_COVER = booleanPreferencesKey("hue_from_cover")
        val AURA_INTENSITY = floatPreferencesKey("aura_intensity")
        val READER_SURFACE = stringPreferencesKey("reader_surface")
        val FOCUS_RIBBON = booleanPreferencesKey("focus_ribbon")
        val GLASS_UI = booleanPreferencesKey("glass_ui")
        val AESTHETIC_ELEMENTS = booleanPreferencesKey("aesthetic_elements")
        val ANIMATED_GLARE = booleanPreferencesKey("animated_glare")
        val ANIMATED_BORDERS = booleanPreferencesKey("animated_borders")
        val MASTER_ANIMATIONS = booleanPreferencesKey("master_animations")
        val EMBOSSED_EFFECTS = booleanPreferencesKey("embossed_effects")
        val POINTER_HOVER_EFFECTS = booleanPreferencesKey("pointer_hover_effects")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val V1_MIGRATED = booleanPreferencesKey("v1_migrated")
    }

    private val legacyThemeKey = stringPreferencesKey("theme")

    val config: Flow<AppUiConfig> = context.appUiConfigDataStore.data.map { prefs ->
        fromPreferences(prefs)
    }

    /**
     * Migrates v1 `reader_prefs` theme choice into `app_ui_config` once, preserving:
     * Day -> Paper, Sepia -> Dusk, Night -> Night, OLED -> OLED, E-ink -> E-ink.
     */
    suspend fun migrateFromV1IfNeeded() {
        val current = context.appUiConfigDataStore.data.first()
        if (current[Keys.V1_MIGRATED] == true) return

        val legacyPrefs = context.readerDataStore.data.first()
        val legacyThemeName = legacyPrefs[legacyThemeKey]
        val migratedSurface = mapLegacyThemeNameToSurface(legacyThemeName)

        context.appUiConfigDataStore.edit { prefs ->
            if (prefs[Keys.V1_MIGRATED] != true) {
                if (prefs[Keys.READER_SURFACE] == null) {
                    prefs[Keys.READER_SURFACE] = migratedSurface.name
                }
                prefs[Keys.V1_MIGRATED] = true
            }
        }
    }

    suspend fun update(transform: (AppUiConfig) -> AppUiConfig) {
        migrateFromV1IfNeeded()
        context.appUiConfigDataStore.edit { prefs ->
            val next = transform(fromPreferences(prefs))
            prefs[Keys.APP_THEME] = next.appTheme.name
            prefs[Keys.THEME_HUE] = normalizeHue(next.themeHue)
            prefs[Keys.HUE_FROM_COVER] = next.hueFromCover
            prefs[Keys.AURA_INTENSITY] = next.auraIntensity.coerceIn(0f, 1f)
            prefs[Keys.READER_SURFACE] = next.readerSurface.name
            prefs[Keys.FOCUS_RIBBON] = next.focusRibbon
            prefs[Keys.GLASS_UI] = next.glassUi
            prefs[Keys.AESTHETIC_ELEMENTS] = next.aestheticElements
            prefs[Keys.ANIMATED_GLARE] = next.animatedGlare
            prefs[Keys.ANIMATED_BORDERS] = next.animatedBorders
            prefs[Keys.MASTER_ANIMATIONS] = next.masterAnimations
            prefs[Keys.EMBOSSED_EFFECTS] = next.embossedEffects
            prefs[Keys.POINTER_HOVER_EFFECTS] = next.pointerHoverEffects
            prefs[Keys.APP_LANGUAGE] = next.appLanguage.name
            prefs[Keys.V1_MIGRATED] = true
        }
    }

    private fun fromPreferences(prefs: Preferences): AppUiConfig = AppUiConfig(
        appTheme = prefs[Keys.APP_THEME]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() }
            ?: AppTheme.SYSTEM,
        themeHue = prefs[Keys.THEME_HUE]?.let(::normalizeHue) ?: HuePreset.DEFAULT_HUE,
        hueFromCover = prefs[Keys.HUE_FROM_COVER] ?: false,
        auraIntensity = (prefs[Keys.AURA_INTENSITY] ?: 0.4f).coerceIn(0f, 1f),
        readerSurface = prefs[Keys.READER_SURFACE]?.let { runCatching { ReaderSurface.valueOf(it) }.getOrNull() }
            ?: ReaderSurface.PAPER,
        focusRibbon = prefs[Keys.FOCUS_RIBBON] ?: false,
        glassUi = prefs[Keys.GLASS_UI] ?: true,
        aestheticElements = prefs[Keys.AESTHETIC_ELEMENTS] ?: true,
        animatedGlare = prefs[Keys.ANIMATED_GLARE] ?: true,
        animatedBorders = prefs[Keys.ANIMATED_BORDERS] ?: true,
        masterAnimations = prefs[Keys.MASTER_ANIMATIONS] ?: true,
        embossedEffects = prefs[Keys.EMBOSSED_EFFECTS] ?: false,
        pointerHoverEffects = prefs[Keys.POINTER_HOVER_EFFECTS] ?: true,
        appLanguage = prefs[Keys.APP_LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
            ?: AppLanguage.EN
    )

    companion object {
        fun mapLegacyThemeNameToSurface(legacyName: String?): ReaderSurface {
            val mode = legacyName?.let { runCatching { ReaderThemeMode.valueOf(it) }.getOrNull() }
                ?: ReaderThemeMode.DAY
            return ReaderSurface.fromLegacyThemeMode(mode)
        }
    }
}
