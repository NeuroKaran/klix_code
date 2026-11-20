package com.klix.code.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klix.code.R
import com.klix.code.data.agent.model.ModelProvider
import com.klix.code.data.sandbox.SandboxExecutionProvider
import com.klix.code.data.sandbox.SandboxManager
import com.klix.code.data.sandbox.SandboxStatus
import com.klix.code.data.security.KeyManager
import com.klix.code.data.security.KeyManagerImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ToolchainOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val logoRes: Int,
    val packages: List<String>
) {
    PYTHON(
        id = "python3",
        title = "Python 3",
        subtitle = "python3 • pip",
        logoRes = R.drawable.ic_logo_python,
        packages = listOf("python3", "py3-pip")
    ),
    NODEJS(
        id = "nodejs",
        title = "Node.js",
        subtitle = "nodejs • npm",
        logoRes = R.drawable.ic_logo_nodejs,
        packages = listOf("nodejs", "npm")
    ),
    GIT(
        id = "git",
        title = "Git",
        subtitle = "git • ssh",
        logoRes = R.drawable.ic_logo_git,
        packages = listOf("git", "openssh")
    ),
    CPP(
        id = "c/c++",
        title = "C / C++",
        subtitle = "build-base",
        logoRes = R.drawable.ic_logo_cpp,
        packages = listOf("build-base")
    )
}

sealed class SandboxInstallState {
    object Idle : SandboxInstallState()
    data class DownloadingRootFs(val progress: Float, val statusMessage: String) : SandboxInstallState()
    data class ExtractingRootFs(val progress: Float, val statusMessage: String) : SandboxInstallState()
    data class InstallingPackages(
        val currentPackage: String,
        val progress: Float,
        val statusMessage: String
    ) : SandboxInstallState()
    object Completed : SandboxInstallState()
    data class Error(val message: String) : SandboxInstallState()
}

class OnboardingViewModel : ViewModel() {

    // Page 2: Selected Toolchains (Python 3 selected by default as in screenshot)
    private val _selectedTools = MutableStateFlow<Set<ToolchainOption>>(setOf(ToolchainOption.PYTHON))
    val selectedTools: StateFlow<Set<ToolchainOption>> = _selectedTools.asStateFlow()

    private val _installState = MutableStateFlow<SandboxInstallState>(SandboxInstallState.Idle)
    val installState: StateFlow<SandboxInstallState> = _installState.asStateFlow()

    // Page 3: AI Provider & Key Store
    private val _selectedProvider = MutableStateFlow(ModelProvider.GEMINI)
    val selectedProvider: StateFlow<ModelProvider> = _selectedProvider.asStateFlow()

    private val _apiKeyInput = MutableStateFlow("")
    val apiKeyInput: StateFlow<String> = _apiKeyInput.asStateFlow()

    private val _showKeyInputDialog = MutableStateFlow(false)
    val showKeyInputDialog: StateFlow<Boolean> = _showKeyInputDialog.asStateFlow()

    fun toggleTool(tool: ToolchainOption) {
        val current = _selectedTools.value.toMutableSet()
        if (current.contains(tool)) {
            current.remove(tool)
        } else {
            current.add(tool)
        }
        _selectedTools.value = current
    }

    fun selectProvider(provider: ModelProvider) {
        _selectedProvider.value = provider
    }

    fun setApiKeyInput(key: String) {
        _apiKeyInput.value = key
    }

    fun setShowKeyInputDialog(show: Boolean) {
        _showKeyInputDialog.value = show
    }

    fun installSelectedPackages(
        context: Context,
        sandboxManager: SandboxManager,
        onSuccess: () -> Unit
    ) {
        if (_installState.value is SandboxInstallState.DownloadingRootFs ||
            _installState.value is SandboxInstallState.ExtractingRootFs ||
            _installState.value is SandboxInstallState.InstallingPackages
        ) {
            return
        }

        viewModelScope.launch {
            try {
                // Step 1: Install Alpine RootFS if not already present
                if (!sandboxManager.isInstalled()) {
                    _installState.value = SandboxInstallState.DownloadingRootFs(0.05f, "Connecting to Alpine Linux repository...")

                    // Launch monitoring coroutine for progress & statusMessage
                    val monitorJob = launch {
                        while (isActive) {
                            val status = sandboxManager.status.value
                            val prog = sandboxManager.progress.value
                            val msg = sandboxManager.statusMessage.value
                            when (status) {
                                SandboxStatus.DOWNLOADING -> {
                                    _installState.value = SandboxInstallState.DownloadingRootFs(prog, msg)
                                }
                                SandboxStatus.EXTRACTING -> {
                                    _installState.value = SandboxInstallState.ExtractingRootFs(prog, msg)
                                }
                                SandboxStatus.ERROR -> {
                                    _installState.value = SandboxInstallState.Error(msg)
                                    return@launch
                                }
                                else -> {}
                            }
                            delay(100)
                        }
                    }

                    val installResult = withContext(Dispatchers.IO) {
                        sandboxManager.installSandbox()
                    }
                    monitorJob.cancel()

                    if (installResult.isFailure) {
                        val errorMsg = installResult.exceptionOrNull()?.message ?: "Failed to install Alpine Linux"
                        _installState.value = SandboxInstallState.Error(errorMsg)
                        return@launch
                    }
                }

                // Step 2: Install selected packages inside RootFS via PRoot
                val toolsToInstall = _selectedTools.value.toList()
                if (toolsToInstall.isNotEmpty()) {
                    val executionProvider = SandboxExecutionProvider(context, sandboxManager)
                    val totalTools = toolsToInstall.size

                    for ((index, tool) in toolsToInstall.withIndex()) {
                        val baseProgress = (index.toFloat() / totalTools)
                        val pkgList = tool.packages.joinToString(" ")
                        _installState.value = SandboxInstallState.InstallingPackages(
                            currentPackage = tool.title,
                            progress = baseProgress + 0.05f,
                            statusMessage = "Installing ${tool.title} ($pkgList)..."
                        )

                        // Run apk add inside sandbox
                        val cmd = "apk add $pkgList"
                        val result = withContext(Dispatchers.IO) {
                            executionProvider.runCommand(cmd)
                        }

                        _installState.value = SandboxInstallState.InstallingPackages(
                            currentPackage = tool.title,
                            progress = ((index + 1).toFloat() / totalTools),
                            statusMessage = "Configured ${tool.title}"
                        )
                        delay(200)
                    }
                }

                // Refresh sandbox installed tools map so other screens update immediately
                withContext(Dispatchers.IO) {
                    sandboxManager.refreshInstalledTools()
                }

                _installState.value = SandboxInstallState.Completed
                delay(300)
                onSuccess()
            } catch (e: Exception) {
                _installState.value = SandboxInstallState.Error(e.message ?: "Installation error occurred")
            }
        }
    }

    fun retryInstallation(
        context: Context,
        sandboxManager: SandboxManager,
        onSuccess: () -> Unit
    ) {
        _installState.value = SandboxInstallState.Idle
        installSelectedPackages(context, sandboxManager, onSuccess)
    }

    fun saveProviderAndKey(
        context: Context,
        keyManager: KeyManager = KeyManagerImpl(context),
        onDone: () -> Unit
    ) {
        val provider = _selectedProvider.value
        val key = _apiKeyInput.value.trim()

        if (key.isNotBlank()) {
            keyManager.setApiKey(provider, key)
        }

        val currentSettings = keyManager.getSettings()
        keyManager.updateSettings(
            currentSettings.copy(
                activeProvider = provider,
                activeModel = provider.defaultModel
            )
        )

        // Persist active settings in prefs as well
        val prefs = context.getSharedPreferences("klix_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("active_ai_provider", provider.name)
            .putBoolean("offline_mode", false)
            .apply()

        _showKeyInputDialog.value = false
        onDone()
    }

    fun chooseOfflineMode(
        context: Context,
        onDone: () -> Unit
    ) {
        val prefs = context.getSharedPreferences("klix_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("offline_mode", true)
            .apply()
        onDone()
    }

    fun completeOnboarding(
        context: Context,
        onComplete: () -> Unit
    ) {
        val prefs = context.getSharedPreferences("klix_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("onboarding_completed", true)
            .apply()
        onComplete()
    }
}

