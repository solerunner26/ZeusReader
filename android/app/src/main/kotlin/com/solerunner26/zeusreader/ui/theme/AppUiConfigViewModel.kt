package com.solerunner26.zeusreader.ui.theme

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solerunner26.zeusreader.reader.prefs.AppLanguage
import com.solerunner26.zeusreader.reader.prefs.AppTheme
import com.solerunner26.zeusreader.reader.prefs.AppUiConfig
import com.solerunner26.zeusreader.reader.prefs.AppUiConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActiveCoverHueHolder @Inject constructor() {
    private val _coverHue = MutableStateFlow<Float?>(null)
    val coverHue: StateFlow<Float?> = _coverHue

    fun updateFromBitmap(bitmap: Bitmap?) {
        _coverHue.value = CoverHueExtractor.extractHueOrNull(bitmap)
    }

    fun setCoverHue(hue: Float?) {
        _coverHue.value = hue?.let(::normalizeHue)
    }

    fun clear() {
        _coverHue.value = null
    }
}

@HiltViewModel
class AppUiConfigViewModel @Inject constructor(
    private val repository: AppUiConfigRepository,
    private val coverHueHolder: ActiveCoverHueHolder
) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.migrateFromV1IfNeeded()
        }
    }

    val config: StateFlow<AppUiConfig> = repository.config.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppUiConfig()
    )

    val effectiveHue: StateFlow<Float> = combine(
        repository.config,
        coverHueHolder.coverHue
    ) { cfg, coverHue ->
        if (cfg.hueFromCover && coverHue != null) coverHue else cfg.themeHue
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HuePreset.DEFAULT_HUE
    )

    fun setAppTheme(theme: AppTheme) = update { it.copy(appTheme = theme) }
    fun setPreset(preset: HuePreset) = update { it.copy(themeHue = preset.hue) }
    fun setHueFromCover(enabled: Boolean) = update { it.copy(hueFromCover = enabled) }
    fun setAuraIntensity(intensity: Float) = update { it.copy(auraIntensity = intensity.coerceIn(0f, 1f)) }
    fun setReaderSurface(surface: ReaderSurface) = update { it.copy(readerSurface = surface) }
    fun setFocusRibbon(enabled: Boolean) = update { it.copy(focusRibbon = enabled) }
    fun setGlassUi(enabled: Boolean) = update { it.copy(glassUi = enabled) }
    fun setAestheticElements(enabled: Boolean) = update { it.copy(aestheticElements = enabled) }
    fun setMasterAnimations(enabled: Boolean) = update { it.copy(masterAnimations = enabled) }
    fun setThemePack(sky: ZeusSky) = update { it.copy(themeHue = sky.hue, hueFromCover = false) }
    fun setAnimatedGlare(enabled: Boolean) = update { it.copy(animatedGlare = enabled) }
    fun setAnimatedBorders(enabled: Boolean) = update { it.copy(animatedBorders = enabled) }
    fun setAppLanguage(language: AppLanguage) = update { it.copy(appLanguage = language) }

    private fun update(transform: (AppUiConfig) -> AppUiConfig) {
        viewModelScope.launch {
            repository.update(transform)
        }
    }
}
