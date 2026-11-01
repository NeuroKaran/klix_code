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
import java.io.File

sealed class UpdateUIState {
    object Idle : UpdateUIState()
    object Checking : UpdateUIState()
    data class Available(val updateInfo: UpdateInfo) : UpdateUIState()
    data class UpToDate(val versionName: String) : UpdateUIState()
    data class Downloading(
        val updateInfo: UpdateInfo,
        val progressPercent: Int,
        val bytesRead: Long,
        val totalBytes: Long
    ) : UpdateUIState()
    data class ReadyToInstall(val updateInfo: UpdateInfo, val apkFile: File) : UpdateUIState()
    data class PermissionRequired(val updateInfo: UpdateInfo, val apkFile: File) : UpdateUIState()
    data class Error(val message: String, val updateInfo: UpdateInfo? = null) : UpdateUIState()
}

class UpdateViewModel(
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

    fun startDownload(context: Context, updateInfo: UpdateInfo) {
        _uiState.value = UpdateUIState.Downloading(
            updateInfo = updateInfo,
            progressPercent = 0,
            bytesRead = 0,
            totalBytes = 0
        )
        _isDialogVisible.value = true

        viewModelScope.launch {
            KlixTelemetry.recordBreadcrumb(
                category = "update",
                message = "Starting APK download: v${updateInfo.latestVersionName}"
            )

            val downloadResult = downloader.downloadApk(
                context = context,
                updateInfo = updateInfo,
                onProgress = { percent, bytesRead, totalBytes ->
                    _uiState.value = UpdateUIState.Downloading(
                        updateInfo = updateInfo,
                        progressPercent = percent,
                        bytesRead = bytesRead,
                        totalBytes = totalBytes
                    )
                }
            )

            downloadResult.fold(
                onSuccess = { apkFile ->
                    if (downloader.canRequestPackageInstalls(context)) {
                        _uiState.value = UpdateUIState.ReadyToInstall(updateInfo, apkFile)
                        // Automatically trigger install prompt
                        installApk(context, apkFile)
                    } else {
                        _uiState.value = UpdateUIState.PermissionRequired(updateInfo, apkFile)
                    }
                },
                onFailure = { error ->
                    KlixTelemetry.recordError(error, "APK download failed", mapOf("action" to "download_apk"))
                    _uiState.value = UpdateUIState.Error(
                        message = error.message ?: "Download failed",
                        updateInfo = updateInfo
                    )
                }
            )
        }
    }

    fun installApk(context: Context, apkFile: File) {
        if (!downloader.canRequestPackageInstalls(context)) {
            val currentInfo = (_uiState.value as? UpdateUIState.ReadyToInstall)?.updateInfo
                ?: (_uiState.value as? UpdateUIState.PermissionRequired)?.updateInfo
            if (currentInfo != null) {
                _uiState.value = UpdateUIState.PermissionRequired(currentInfo, apkFile)
            }
            downloader.openInstallPermissionSettings(context)
            return
        }

        val result = downloader.installApk(context, apkFile)
        result.onFailure { error ->
            KlixTelemetry.recordError(error, "APK install launch failed", mapOf("action" to "install_apk"))
            _snackMessage.value = "Failed to launch installer: ${error.message}"
        }
    }

    fun requestInstallPermission(context: Context) {
        downloader.openInstallPermissionSettings(context)
    }

    fun openInBrowser(context: Context, url: String) {
        downloader.openInBrowser(context, url)
        _isDialogVisible.value = false
    }

    fun dismissDialog(context: Context, postpone: Boolean = false) {
        val currentState = _uiState.value
        if (postpone) {
            when (currentState) {
                is UpdateUIState.Available -> {
                    UpdateChecker.postponeVersion(context, currentState.updateInfo.latestVersionCode)
                }
                is UpdateUIState.Downloading -> {
                    UpdateChecker.postponeVersion(context, currentState.updateInfo.latestVersionCode)
                }
                is UpdateUIState.ReadyToInstall -> {
                    UpdateChecker.postponeVersion(context, currentState.updateInfo.latestVersionCode)
                }
                is UpdateUIState.PermissionRequired -> {
                    UpdateChecker.postponeVersion(context, currentState.updateInfo.latestVersionCode)
                }
                else -> {}
            }
        }
        _isDialogVisible.value = false
    }
}
