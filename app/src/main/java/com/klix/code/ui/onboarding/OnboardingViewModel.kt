package com.klix.code.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klix.code.R
import com.klix.code.data.agent.execution.ExecutionProvider
import com.klix.code.data.agent.model.ModelProvider
import com.klix.code.data.sandbox.SandboxExecutionProvider
import com.klix.code.data.sandbox.SandboxManager
import com.klix.code.data.sandbox.SandboxStatus
import com.klix.code.data.security.KeyManager
import com.klix.code.data.security.KeyManagerImpl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

enum class ToolchainOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val logoRes: Int,
    val packages: List<String>,
    val verifyCommand: String
) {
    PYTHON(
        id = "python3",
        title = "Python 3",
        subtitle = "python3 • Built-in",
        logoRes = R.drawable.ic_logo_python,
        packages = listOf("python3"),
        verifyCommand = "python3 --version"
    ),
    NODEJS(
        id = "nodejs",
        title = "Node.js",
        subtitle = "30.5 MB • On-Demand",
        logoRes = R.drawable.ic_logo_nodejs,
        packages = listOf("nodejs"),
        verifyCommand = "node --version"
    ),
    DASH(
        id = "dash",
        title = "Dash Shell",
        subtitle = "sh • Built-in",
        logoRes = R.drawable.ic_icon_terminal_prompt,
        packages = listOf("dash"),
        verifyCommand = "echo \$0"
    ),
    CPP(
        id = "c/c++",
        title = "C / C++",
        subtitle = "71.2 MB • On-Demand",
        logoRes = R.drawable.ic_logo_cpp,
        packages = listOf("clang", "lld"),
        verifyCommand = "clang --version"
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

class OnboardingViewModel @JvmOverloads constructor(
    private val executionProviderFactory: (Context, SandboxManager) -> ExecutionProvider = { context, sandboxManager ->
        SandboxExecutionProvider(context, sandboxManager)
    },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val coroutineScopeOverride: CoroutineScope? = null
) : ViewModel() {

    internal var activeInstallJob: Job? = null
    private val installInProgress = AtomicBoolean(false)

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

    private fun isInProgressInstallState(state: SandboxInstallState): Boolean {
        return state is SandboxInstallState.DownloadingRootFs ||
            state is SandboxInstallState.ExtractingRootFs ||
            state is SandboxInstallState.InstallingPackages
    }

    fun installSelectedPackages(
        context: Context,
        sandboxManager: SandboxManager,
        onSuccess: () -> Unit
    ) {
        if (activeInstallJob?.isActive == true || !installInProgress.compareAndSet(false, true)) {
            return
        }

        val scope = coroutineScopeOverride ?: viewModelScope
        val job = scope.launch(start = CoroutineStart.LAZY) {
            performInstallSelectedPackagesInternal(context, sandboxManager, onSuccess)
        }
        activeInstallJob = job
        job.invokeOnCompletion { cause ->
            if (cause is CancellationException && isInProgressInstallState(_installState.value)) {
                _installState.value = SandboxInstallState.Idle
            }
            if (activeInstallJob === job) {
                activeInstallJob = null
            }
            installInProgress.set(false)
        }
        job.start()
    }

    internal suspend fun performInstallSelectedPackages(
        context: Context,
        sandboxManager: SandboxManager,
        onSuccess: () -> Unit
    ) {
        if (!installInProgress.compareAndSet(false, true)) {
            return
        }
        try {
            performInstallSelectedPackagesInternal(context, sandboxManager, onSuccess)
        } finally {
            installInProgress.set(false)
        }
    }

    private suspend fun performInstallSelectedPackagesInternal(
        context: Context,
        sandboxManager: SandboxManager,
        onSuccess: () -> Unit
    ) {
        try {
            coroutineScope {
                // Step 1: Install Bionic 16 KB Sandbox if not already present
                val alreadyInstalled = withContext(ioDispatcher) { sandboxManager.isInstalled() }
                if (!alreadyInstalled) {
                    _installState.value = SandboxInstallState.DownloadingRootFs(
                        0.05f,
                        "Preparing Bionic 16 KB runtime..."
                    )

                    // Launch monitoring coroutine for progress & statusMessage
                    val monitorJob = launch {
                        while (isActive) {
                            val statusFlow: StateFlow<SandboxStatus>? = sandboxManager.status
                            val progressFlow: StateFlow<Float>? = sandboxManager.progress
                            val messageFlow: StateFlow<String>? = sandboxManager.statusMessage
                            val status = statusFlow?.value
                            val prog = progressFlow?.value ?: 0f
                            val msg = messageFlow?.value ?: ""
                            when (status) {
                                SandboxStatus.DOWNLOADING -> {
                                    _installState.value = SandboxInstallState.DownloadingRootFs(prog, msg)
                                }
                                SandboxStatus.EXTRACTING -> {
                                    _installState.value = SandboxInstallState.ExtractingRootFs(prog, msg)
                                }
                                SandboxStatus.ERROR -> {
                                    _installState.value = SandboxInstallState.Error(
                                        msg.ifBlank { "Failed to initialize Bionic runtime" }
                                    )
                                    return@launch
                                }
                                else -> {}
                            }
                            delay(100)
                        }
                    }

                    val installResult = try {
                        withContext(ioDispatcher) {
                            sandboxManager.installSandbox()
                        }
                    } finally {
                        monitorJob.cancel()
                    }

                    if (installResult.isFailure) {
                        val errorMsg = installResult.exceptionOrNull()?.message
                            ?: "Failed to initialize Bionic runtime"
                        _installState.value = SandboxInstallState.Error(errorMsg)
                        return@coroutineScope
                    }

                    val rootFsVerified = withContext(ioDispatcher) { sandboxManager.isInstalled() }
                    if (!rootFsVerified) {
                        _installState.value = SandboxInstallState.Error(
                            "Sandbox installation completed but verification failed"
                        )
                        return@coroutineScope
                    }
                }

                // Step 2: Install on-demand or verify bundled Bionic toolchains inside sandbox
                val toolsToInstall = _selectedTools.value.toList()
                if (toolsToInstall.isNotEmpty()) {
                    val executionProvider = executionProviderFactory(context, sandboxManager)
                    val totalTools = toolsToInstall.size

                    for ((index, tool) in toolsToInstall.withIndex()) {
                        val baseProgress = index.toFloat() / totalTools
                        val pkgList = tool.packages.joinToString(" ")
                        val isOnDemand = withContext(ioDispatcher) {
                            sandboxManager.isOnDemandToolchain(tool.id) && !sandboxManager.isToolInstalled(tool.id)
                        }

                        if (isOnDemand) {
                            val monitorJob = launch {
                                while (isActive) {
                                    val dl = sandboxManager.activeDownload.value
                                    if (dl != null && dl.toolId == tool.id) {
                                        val stepProgress = baseProgress + (dl.progress / totalTools)
                                        _installState.value = SandboxInstallState.DownloadingRootFs(
                                            progress = stepProgress.coerceIn(0f, 1f),
                                            statusMessage = "${dl.statusMessage} (${(dl.progress * 100).toInt()}%)"
                                        )
                                    }
                                    delay(80)
                                }
                            }
                            val dlResult = try {
                                withContext(ioDispatcher) {
                                    sandboxManager.installToolchain(tool.id)
                                }
                            } finally {
                                monitorJob.cancel()
                            }

                            if (dlResult.isFailure) {
                                val detail = dlResult.exceptionOrNull()?.message ?: "Download failed"
                                withContext(ioDispatcher) { sandboxManager.refreshInstalledTools() }
                                _installState.value = SandboxInstallState.Error(
                                    "Failed to install ${tool.title}: ${detail.take(300)}"
                                )
                                return@coroutineScope
                            }
                        } else {
                            _installState.value = SandboxInstallState.InstallingPackages(
                                currentPackage = tool.title,
                                progress = baseProgress + 0.05f,
                                statusMessage = "Configuring ${tool.title} ($pkgList)..."
                            )

                            val cmd = tool.verifyCommand
                            val result = withContext(ioDispatcher) {
                                executionProvider.runCommand(cmd)
                            }

                            if (!result.isSuccess || result.exitCode != 0) {
                                val detail = when {
                                    result.stderr.isNotBlank() -> result.stderr.trim()
                                    result.stdout.isNotBlank() -> result.stdout.trim()
                                    else -> "Command exited with code ${result.exitCode}"
                                }
                                withContext(ioDispatcher) { sandboxManager.refreshInstalledTools() }
                                _installState.value = SandboxInstallState.Error(
                                    "Failed to install ${tool.title}: ${detail.take(300)}"
                                )
                                return@coroutineScope
                            }

                            val isVerified = withContext(ioDispatcher) {
                                sandboxManager.isToolInstalled(tool.id)
                            }
                            if (!isVerified) {
                                val stderrNote = result.stderr.trim().takeIf { it.isNotEmpty() }
                                    ?.let { " (${it.take(200)})" }
                                    ?: ""
                                withContext(ioDispatcher) { sandboxManager.refreshInstalledTools() }
                                _installState.value = SandboxInstallState.Error(
                                    "Failed to verify ${tool.title} installation$stderrNote"
                                )
                                return@coroutineScope
                            }
                        }

                        _installState.value = SandboxInstallState.InstallingPackages(
                            currentPackage = tool.title,
                            progress = (index + 1).toFloat() / totalTools,
                            statusMessage = "Configured ${tool.title}"
                        )
                        delay(200)
                    }
                }

                // Refresh sandbox installed tools map so other screens update immediately
                withContext(ioDispatcher) {
                    sandboxManager.refreshInstalledTools()
                }

                _installState.value = SandboxInstallState.Completed
                delay(300)
                onSuccess()
            }
        } catch (e: CancellationException) {
            if (isInProgressInstallState(_installState.value)) {
                _installState.value = SandboxInstallState.Idle
            }
            throw e
        } catch (e: Exception) {
            _installState.value = SandboxInstallState.Error(e.message ?: "Installation error occurred")
        }
    }

    fun retryInstallation(
        context: Context,
        sandboxManager: SandboxManager,
        onSuccess: () -> Unit
    ) {
        if (installInProgress.get() ||
            activeInstallJob?.isActive == true ||
            isInProgressInstallState(_installState.value)
        ) {
            return
        }
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

