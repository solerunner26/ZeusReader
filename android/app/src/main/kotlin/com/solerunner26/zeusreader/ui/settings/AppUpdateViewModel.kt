package com.solerunner26.zeusreader.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solerunner26.zeusreader.BuildConfig
import com.solerunner26.zeusreader.sync.AppUpdateInfo
import com.solerunner26.zeusreader.sync.GitHubAppUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppUpdateUiState(
    val currentVersion: String = BuildConfig.VERSION_NAME,
    val currentVersionCode: Int = BuildConfig.VERSION_CODE,
    val isChecking: Boolean = false,
    val availableUpdate: AppUpdateInfo? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val dismissedForSession: Boolean = false,
    val statusMessage: String? = null
)

@HiltViewModel
class AppUpdateViewModel @Inject constructor(
    private val updater: GitHubAppUpdater
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUpdateUiState())
    val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()

    init {
        checkNow(silentIfUpToDate = true)
    }

    fun checkNow(silentIfUpToDate: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true, statusMessage = null) }
            updater.checkForUpdate(BuildConfig.VERSION_NAME)
                .onSuccess { info ->
                    _uiState.update {
                        it.copy(
                            isChecking = false,
                            availableUpdate = info,
                            dismissedForSession = false,
                            statusMessage = if (info != null) {
                                "Update ${info.tagName} available"
                            } else if (!silentIfUpToDate) {
                                "You are on the latest version (v${BuildConfig.VERSION_NAME})."
                            } else {
                                null
                            }
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isChecking = false,
                            statusMessage = if (!silentIfUpToDate) err.message ?: "Update check failed" else null
                        )
                    }
                }
        }
    }

    fun dismissForSession() {
        _uiState.update { it.copy(dismissedForSession = true) }
    }

    fun downloadAndInstall() {
        val update = _uiState.value.availableUpdate ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true, downloadProgress = 0, statusMessage = "Downloading ${update.tagName}…") }
            updater.downloadAndInstallUpdate(update) { pct ->
                _uiState.update { it.copy(downloadProgress = pct) }
            }.onSuccess { startedInstaller ->
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        statusMessage = if (startedInstaller) {
                            "Installer opened for ${update.tagName}"
                        } else {
                            "Allow 'Install unknown apps' for ZeusReader, then tap Update again."
                        }
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        statusMessage = "Update failed: ${err.message ?: "check network"}"
                    )
                }
            }
        }
    }
}
