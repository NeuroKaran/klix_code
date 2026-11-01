package com.klix.code.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.klix.code.BuildConfig
import com.klix.code.data.telemetry.KlixTelemetry
import com.klix.code.data.update.UpdateChecker
import com.klix.code.data.update.UpdateDownloader
import com.klix.code.data.update.UpdateInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UpdateUIState {
    object Idle : UpdateUIState()
    object Checking : UpdateUIState()
    data class Available(val updateInfo: UpdateInfo) : UpdateUIState()
    data class UpToDate(val versionName: String) : UpdateUIState()
    data class Error(val message: String, val updateInfo: UpdateInfo? = null) : UpdateUIState()
}

class UpdateViewModel @JvmOverloads constructor(
    application: Application,
    private val checker: UpdateChecker = UpdateChecker(),
    private val downloader: UpdateDownloader = UpdateDownloader()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<UpdateUIState>(UpdateUIState.Idle)
    val uiState: StateFlow<UpdateUIState> = _uiState.asStateFlow()

    private val _isDialogVisible = MutableStateFlow(false)
    val isDialogVisible: StateFlow<Boolean> = _isDialogVisible.asStateFlow()

    private val _snackMessage = MutableStateFlow<String?>(null)
    val snackMessage: StateFlow<String?> = _snackMessage.asStateFlow()

    val currentVersionCode: Int = BuildConfig.VERSION_CODE
    val currentVersionName: String = BuildConfig.VERSION_NAME

    fun clearSnackMessage() {
        _snackMessage.value = null
    }

    fun checkForUpdates(context: Context, isManual: Boolean = false) {
        if (!isManual && !UpdateChecker.shouldCheckAutomatically(context)) {
            return
        }

        if (isManual) {
            _uiState.value = UpdateUIState.Checking
        }

        viewModelScope.launch {
            KlixTelemetry.recordBreadcrumb(
                category = "update",
                message = "Checking for update (manual=$isManual, current=$currentVersionName)"
            )

            val result = checker.checkForUpdate(
                currentVersionCode = currentVersionCode,
                currentVersionName = currentVersionName
            )

            UpdateChecker.recordCheckTimestamp(context)

            result.fold(
                onSuccess = { info ->
                    if (info.isUpdateAvailable) {
                        val isPostponed = UpdateChecker.isVersionPostponed(context, info.latestVersionCode)
                        if (!isManual && isPostponed && !info.forceUpdate) {
                            // Don't disturb user if they chose "later" for this version
                            _uiState.value = UpdateUIState.Idle
                        } else {
                            _uiState.value = UpdateUIState.Available(info)
                            _isDialogVisible.value = true
                        }
                    } else {
                        if (isManual) {
                            _uiState.value = UpdateUIState.UpToDate(currentVersionName)
                            _snackMessage.value = "Klix Code is up to date (v$currentVersionName)"
                        } else {
                            _uiState.value = UpdateUIState.Idle
                        }
                    }
                },
                onFailure = { error ->
                    KlixTelemetry.recordError(error, "Check for update failed", mapOf("action" to "check_for_update"))
                    if (isManual) {
                        val errMsg = error.message ?: "Failed to check for updates"
                        _uiState.value = UpdateUIState.Error(errMsg)
                        _snackMessage.value = errMsg
                    } else {
                        _uiState.value = UpdateUIState.Idle
                    }
                }
            )
        }
    }

    fun openInBrowser(context: Context, url: String) {
        downloader.openInBrowser(context, url).onFailure { error ->
            KlixTelemetry.recordError(error, "Failed to open update URL", mapOf("url" to url))
            _snackMessage.value = "Failed to open browser: ${error.message}"
        }
        _isDialogVisible.value = false
    }

    fun dismissDialog(context: Context, postpone: Boolean = false) {
        val currentState = _uiState.value
        if (postpone && currentState is UpdateUIState.Available) {
            UpdateChecker.postponeVersion(context, currentState.updateInfo.latestVersionCode)
        }
        _isDialogVisible.value = false
    }
}
