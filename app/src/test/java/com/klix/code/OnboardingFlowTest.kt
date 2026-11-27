package com.klix.code

import android.content.Context
import android.content.SharedPreferences
import com.klix.code.data.agent.execution.CommandResult
import com.klix.code.data.agent.execution.ExecutionProvider
import com.klix.code.data.agent.model.ModelProvider
import com.klix.code.data.agent.model.ProviderSettings
import com.klix.code.data.sandbox.SandboxManager
import com.klix.code.data.security.KeyManager
import com.klix.code.ui.onboarding.OnboardingViewModel
import com.klix.code.ui.onboarding.SandboxInstallState
import com.klix.code.ui.onboarding.ToolchainOption
import com.klix.code.ui.onboarding.computeProgressRailLayout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

class OnboardingFlowTest {

    private lateinit var viewModel: OnboardingViewModel
    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    private val prefValues = mutableMapOf<String, Any>()
    private val tempDirs = mutableListOf<File>()

    @Before
    fun setUp() {
        viewModel = OnboardingViewModel()
        mockContext = mock(Context::class.java)
        mockPrefs = mock(SharedPreferences::class.java)
        mockEditor = mock(SharedPreferences.Editor::class.java)
        prefValues.clear()

        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        `when`(mockPrefs.edit()).thenReturn(mockEditor)

        `when`(mockEditor.putBoolean(anyString(), org.mockito.ArgumentMatchers.anyBoolean())).thenAnswer { inv ->
            prefValues[inv.getArgument(0)] = inv.getArgument(1)
            mockEditor
        }
        `when`(mockEditor.putString(anyString(), anyString())).thenAnswer { inv ->
            prefValues[inv.getArgument(0)] = inv.getArgument(1)
            mockEditor
        }
    }

    @After
    fun tearDown() {
        tempDirs.forEach { it.deleteRecursively() }
        tempDirs.clear()
    }

    private fun createTempFilesDir(): File {
        val dir = Files.createTempDirectory("klix_onboarding_test_").toFile()
        tempDirs.add(dir)
        return dir
    }

    @Test
    fun testDefaultToolchainIsPython() {
        val selected = viewModel.selectedTools.value
        assertEquals(1, selected.size)
        assertTrue(selected.contains(ToolchainOption.PYTHON))
    }

    @Test
    fun testToggleToolchains() {
        // Toggle Node.js on
        viewModel.toggleTool(ToolchainOption.NODEJS)
        assertTrue(viewModel.selectedTools.value.contains(ToolchainOption.NODEJS))
        assertTrue(viewModel.selectedTools.value.contains(ToolchainOption.PYTHON))
        assertEquals(2, viewModel.selectedTools.value.size)

        // Toggle Python off
        viewModel.toggleTool(ToolchainOption.PYTHON)
        assertFalse(viewModel.selectedTools.value.contains(ToolchainOption.PYTHON))
        assertTrue(viewModel.selectedTools.value.contains(ToolchainOption.NODEJS))
        assertEquals(1, viewModel.selectedTools.value.size)

        // Toggle Git and C++ on
        viewModel.toggleTool(ToolchainOption.GIT)
        viewModel.toggleTool(ToolchainOption.CPP)
        assertEquals(3, viewModel.selectedTools.value.size)
    }

    @Test
    fun testDefaultProviderIsGemini() {
        assertEquals(ModelProvider.GEMINI, viewModel.selectedProvider.value)
    }

    @Test
    fun testSelectProvider() {
        viewModel.selectProvider(ModelProvider.ANTHROPIC)
        assertEquals(ModelProvider.ANTHROPIC, viewModel.selectedProvider.value)

        viewModel.selectProvider(ModelProvider.OPENAI)
        assertEquals(ModelProvider.OPENAI, viewModel.selectedProvider.value)
    }

    @Test
    fun testSaveProviderAndKey() {
        val mockKeyManager = object : KeyManager {
            val keys = mutableMapOf<ModelProvider, String>()
            var currentSettings = ProviderSettings()

            override fun getApiKey(provider: ModelProvider): String? = keys[provider]
            override fun setApiKey(provider: ModelProvider, key: String?) {
                if (key != null) keys[provider] = key else keys.remove(provider)
            }
            override fun hasApiKey(provider: ModelProvider): Boolean = keys.containsKey(provider)
            override fun getSettings(): ProviderSettings = currentSettings
            override fun updateSettings(s: ProviderSettings) { currentSettings = s }
            override fun clearAllKeys() { keys.clear() }
        }

        viewModel.selectProvider(ModelProvider.ANTHROPIC)
        viewModel.setApiKeyInput("sk-ant-test-key-12345")

        var callbackCalled = false
        viewModel.saveProviderAndKey(mockContext, mockKeyManager) {
            callbackCalled = true
        }

        assertTrue(callbackCalled)
        assertEquals("sk-ant-test-key-12345", mockKeyManager.getApiKey(ModelProvider.ANTHROPIC))
        assertEquals(ModelProvider.ANTHROPIC, mockKeyManager.getSettings().activeProvider)
        assertEquals(false, prefValues["offline_mode"])
        assertEquals("ANTHROPIC", prefValues["active_ai_provider"])
    }

    @Test
    fun testOfflineModePersisted() {
        var callbackCalled = false
        viewModel.chooseOfflineMode(mockContext) {
            callbackCalled = true
        }

        assertTrue(callbackCalled)
        assertEquals(true, prefValues["offline_mode"])
    }

    @Test
    fun testCompleteOnboardingPersisted() {
        var callbackCalled = false
        viewModel.completeOnboarding(mockContext) {
            callbackCalled = true
        }

        assertTrue(callbackCalled)
        assertEquals(true, prefValues["onboarding_completed"])
    }

    @Test
    fun testProgressRailRestingLayoutsPreserveSpacingAndWidth() {
        for (page in 0 until 4) {
            val layout = computeProgressRailLayout(
                leftIndex = page.toFloat(),
                rightIndex = page.toFloat()
            )
            assertEquals(69f, layout.totalWidthDp, 0.001f)
            assertEquals(30f, layout.pillRightDp - layout.pillLeftDp, 0.001f)
            assertEquals(page * 13f, layout.pillLeftDp, 0.001f)
            assertEquals(3, layout.inactiveDots.size)
            for (dot in layout.inactiveDots) {
                assertEquals(7f, dot.rightDp - dot.leftDp, 0.001f)
                assertTrue(dot.leftDp >= 0f && dot.rightDp <= layout.totalWidthDp)
            }
        }
    }

    @Test
    fun testProgressRailForwardAndBackwardSlideExpandsPillWithinBounds() {
        // Forward step (Next: 0 -> 1): both dot 0 (left) and dot 1 (right) partially peek outside the sliding pill
        val forwardMid = computeProgressRailLayout(leftIndex = 0.25f, rightIndex = 0.85f)
        assertTrue(forwardMid.pillRightDp - forwardMid.pillLeftDp > 30f)
        assertTrue(forwardMid.pillLeftDp >= 0f && forwardMid.pillRightDp <= forwardMid.totalWidthDp)
        assertEquals(4, forwardMid.inactiveDots.size)
        assertEquals(0f, forwardMid.inactiveDots[0].leftDp, 0.001f)
        assertEquals(36f, forwardMid.inactiveDots[1].leftDp, 0.001f)

        // Backward step (Back: 1 -> 0): reverse symmetry keeps pill expanded and dots in bounds
        val backwardMid = computeProgressRailLayout(leftIndex = 0.15f, rightIndex = 0.75f)
        assertTrue(backwardMid.pillRightDp - backwardMid.pillLeftDp > 30f)
        assertTrue(backwardMid.pillLeftDp >= 0f && backwardMid.pillRightDp <= backwardMid.totalWidthDp)
        assertEquals(4, backwardMid.inactiveDots.size)

        // Multi-page skip sweep (0 -> 3): intermediate dots 1 & 2 are covered by the sweeping capsule
        val skipMid = computeProgressRailLayout(leftIndex = 0.4f, rightIndex = 2.6f)
        assertTrue(skipMid.pillRightDp - skipMid.pillLeftDp > 50f)
        assertTrue(skipMid.pillLeftDp >= 0f && skipMid.pillRightDp <= skipMid.totalWidthDp)
        assertEquals(2, skipMid.inactiveDots.size) // dot 0 on left, dot 3 on right
    }

    @Test
    fun testInstallSelectedPackagesFailsFastAndSurfacesStderrOnCommandFailure() = runBlocking {
        val executedCommands = mutableListOf<String>()
        val fakeProvider = object : ExecutionProvider {
            override fun getTierName(): String = "Fake"
            override fun isAvailable(): Boolean = true
            override suspend fun runCommand(command: String, workingDir: String?): CommandResult {
                executedCommands.add(command)
                return CommandResult(
                    exitCode = 1,
                    stdout = "",
                    stderr = "ERROR: unsatisfiable constraints: python3"
                )
            }
        }

        val mockSandboxManager = mock(SandboxManager::class.java)
        `when`(mockSandboxManager.isInstalled()).thenReturn(true)

        val vm = OnboardingViewModel(
            executionProviderFactory = { _, _ -> fakeProvider },
            ioDispatcher = Dispatchers.Unconfined,
            coroutineScopeOverride = this
        )
        vm.toggleTool(ToolchainOption.NODEJS) // PYTHON + NODEJS

        val observedStates = mutableListOf<SandboxInstallState>()
        val collector = launch(Dispatchers.Unconfined) {
            vm.installState.collect { observedStates.add(it) }
        }

        var onSuccessCalled = false
        vm.installSelectedPackages(mockContext, mockSandboxManager) {
            onSuccessCalled = true
        }
        vm.activeInstallJob?.join()
        collector.cancel()

        assertFalse(onSuccessCalled)
        assertEquals(1, executedCommands.size) // fail-fast: NODEJS never ran
        val finalState = vm.installState.value
        assertTrue("Expected Error state but got $finalState", finalState is SandboxInstallState.Error)
        assertTrue((finalState as SandboxInstallState.Error).message.contains("ERROR: unsatisfiable constraints: python3"))
        assertFalse(observedStates.any { it is SandboxInstallState.Completed })
        assertFalse(
            observedStates.any {
                it is SandboxInstallState.InstallingPackages && it.statusMessage.startsWith("Configured ")
            }
        )
    }

    @Test
    fun testInstallSelectedPackagesFallsBackWhenStderrBlankOnFailure() = runBlocking {
        val fakeProvider = object : ExecutionProvider {
            override fun getTierName(): String = "Fake"
            override fun isAvailable(): Boolean = true
            override suspend fun runCommand(command: String, workingDir: String?): CommandResult {
                return CommandResult(exitCode = 139, stdout = "", stderr = "")
            }
        }

        val mockSandboxManager = mock(SandboxManager::class.java)
        `when`(mockSandboxManager.isInstalled()).thenReturn(true)

        val vm = OnboardingViewModel(
            executionProviderFactory = { _, _ -> fakeProvider },
            ioDispatcher = Dispatchers.Unconfined,
            coroutineScopeOverride = this
        )

        var onSuccessCalled = false
        vm.installSelectedPackages(mockContext, mockSandboxManager) {
            onSuccessCalled = true
        }
        vm.activeInstallJob?.join()

        assertFalse(onSuccessCalled)
        val finalState = vm.installState.value as SandboxInstallState.Error
        assertTrue(finalState.message.contains("139"))
    }

    @Test
    fun testInstallSelectedPackagesFailsFastWhenToolVerificationReturnsFalse() = runBlocking {
        val executedCommands = mutableListOf<String>()
        val fakeProvider = object : ExecutionProvider {
            override fun getTierName(): String = "Fake"
            override fun isAvailable(): Boolean = true
            override suspend fun runCommand(command: String, workingDir: String?): CommandResult {
                executedCommands.add(command)
                return CommandResult(exitCode = 0, stdout = "ok", stderr = "")
            }
        }

        val mockSandboxManager = mock(SandboxManager::class.java)
        `when`(mockSandboxManager.isInstalled()).thenReturn(true)
        `when`(mockSandboxManager.isToolInstalled(anyString())).thenReturn(false)

        val vm = OnboardingViewModel(
            executionProviderFactory = { _, _ -> fakeProvider },
            ioDispatcher = Dispatchers.Unconfined,
            coroutineScopeOverride = this
        )
        vm.toggleTool(ToolchainOption.GIT) // PYTHON + GIT

        var onSuccessCalled = false
        vm.installSelectedPackages(mockContext, mockSandboxManager) {
            onSuccessCalled = true
        }
        vm.activeInstallJob?.join()

        assertFalse(onSuccessCalled)
        assertEquals(1, executedCommands.size) // fail-fast: GIT never ran
        val finalState = vm.installState.value
        assertTrue("Expected Error state but got $finalState", finalState is SandboxInstallState.Error)
        assertTrue((finalState as SandboxInstallState.Error).message.contains("verify", ignoreCase = true))
    }

    @Test
    fun testInstallSelectedPackagesSucceedsAcrossAllToolchainsWithVerification() = runBlocking {
        val filesDir = createTempFilesDir()
        `when`(mockContext.filesDir).thenReturn(filesDir)
        val sandboxManager = SandboxManager(mockContext)
        val rootfs = sandboxManager.getRootfsDir()
        File(rootfs, "bin").mkdirs()
        File(rootfs, "bin/sh").writeText("#!/bin/sh\n")
        File(rootfs, "usr/bin").mkdirs()

        val executedCommands = mutableListOf<String>()
        val fakeProvider = object : ExecutionProvider {
            override fun getTierName(): String = "Fake"
            override fun isAvailable(): Boolean = true
            override suspend fun runCommand(command: String, workingDir: String?): CommandResult {
                executedCommands.add(command)
                when {
                    command.contains("python3") -> File(rootfs, "usr/bin/python3").writeText("")
                    command.contains("node") -> File(rootfs, "usr/bin/node").writeText("")
                    command.contains("git") -> File(rootfs, "usr/bin/git").writeText("")
                    command.contains("clang") || command.contains("build-base") -> File(rootfs, "usr/bin/clang").writeText("")
                }
                return CommandResult(exitCode = 0, stdout = "OK", stderr = "")
            }
        }

        val vm = OnboardingViewModel(
            executionProviderFactory = { _, _ -> fakeProvider },
            ioDispatcher = Dispatchers.Unconfined,
            coroutineScopeOverride = this
        )
        vm.toggleTool(ToolchainOption.NODEJS)
        vm.toggleTool(ToolchainOption.GIT)
        vm.toggleTool(ToolchainOption.CPP)

        var onSuccessCalled = false
        vm.installSelectedPackages(mockContext, sandboxManager) {
            onSuccessCalled = true
        }
        vm.activeInstallJob?.join()

        assertTrue(onSuccessCalled)
        assertEquals(4, executedCommands.size)
        assertEquals(SandboxInstallState.Completed, vm.installState.value)
        assertEquals(true, sandboxManager.installedTools.value["python3"])
        assertEquals(true, sandboxManager.installedTools.value["nodejs"])
        assertEquals(true, sandboxManager.installedTools.value["git"])
        assertEquals(true, sandboxManager.installedTools.value["c/c++"])
    }

    @Test
    fun testRetryInstallationRecoversFromErrorToCompleted() = runBlocking {
        val filesDir = createTempFilesDir()
        `when`(mockContext.filesDir).thenReturn(filesDir)
        val sandboxManager = SandboxManager(mockContext)
        val rootfs = sandboxManager.getRootfsDir()
        File(rootfs, "bin").mkdirs()
        File(rootfs, "bin/sh").writeText("#!/bin/sh\n")
        File(rootfs, "usr/bin").mkdirs()

        var attempt = 0
        val fakeProvider = object : ExecutionProvider {
            override fun getTierName(): String = "Fake"
            override fun isAvailable(): Boolean = true
            override suspend fun runCommand(command: String, workingDir: String?): CommandResult {
                attempt++
                return if (attempt == 1) {
                    CommandResult(exitCode = 1, stdout = "", stderr = "temporary mirror failure")
                } else {
                    File(rootfs, "usr/bin/python3").writeText("")
                    CommandResult(exitCode = 0, stdout = "OK", stderr = "")
                }
            }
        }

        val vm = OnboardingViewModel(
            executionProviderFactory = { _, _ -> fakeProvider },
            ioDispatcher = Dispatchers.Unconfined,
            coroutineScopeOverride = this
        )

        var successCount = 0
        vm.installSelectedPackages(mockContext, sandboxManager) {
            successCount++
        }
        vm.activeInstallJob?.join()
        assertTrue(vm.installState.value is SandboxInstallState.Error)
        assertEquals(0, successCount)

        vm.retryInstallation(mockContext, sandboxManager) {
            successCount++
        }
        vm.activeInstallJob?.join()
        assertEquals(SandboxInstallState.Completed, vm.installState.value)
        assertEquals(1, successCount)
    }

    @Test
    fun testInstallSelectedPackagesFailsWhenRootFsInstallFails() = runBlocking {
        val mockSandboxManager = mock(SandboxManager::class.java)
        `when`(mockSandboxManager.isInstalled()).thenReturn(false)
        `when`(mockSandboxManager.installSandbox()).thenReturn(Result.failure(IOException("network down")))

        var providerInvoked = false
        val vm = OnboardingViewModel(
            executionProviderFactory = { _, _ ->
                providerInvoked = true
                mock(ExecutionProvider::class.java)
            },
            ioDispatcher = Dispatchers.Unconfined,
            coroutineScopeOverride = this
        )

        var onSuccessCalled = false
        vm.installSelectedPackages(mockContext, mockSandboxManager) {
            onSuccessCalled = true
        }
        vm.activeInstallJob?.join()

        assertFalse(providerInvoked)
        assertFalse(onSuccessCalled)
        val finalState = vm.installState.value as SandboxInstallState.Error
        assertTrue(finalState.message.contains("network down"))
    }

    @Test
    fun testConcurrentInstallSelectedPackagesDoubleTapRunsOnlyOnce() = runBlocking {
        val runCount = AtomicInteger(0)
        val fakeProvider = object : ExecutionProvider {
            override fun getTierName(): String = "Fake"
            override fun isAvailable(): Boolean = true
            override suspend fun runCommand(command: String, workingDir: String?): CommandResult {
                runCount.incrementAndGet()
                return CommandResult(exitCode = 0, stdout = "OK", stderr = "")
            }
        }

        val mockSandboxManager = mock(SandboxManager::class.java)
        `when`(mockSandboxManager.isInstalled()).thenAnswer {
            Thread.sleep(80)
            true
        }
        `when`(mockSandboxManager.isToolInstalled(anyString())).thenReturn(true)

        val vm = OnboardingViewModel(
            executionProviderFactory = { _, _ -> fakeProvider },
            ioDispatcher = Dispatchers.IO,
            coroutineScopeOverride = this
        )

        val successCount = AtomicInteger(0)
        vm.installSelectedPackages(mockContext, mockSandboxManager) {
            successCount.incrementAndGet()
        }
        val firstJob = vm.activeInstallJob

        // Rapid double-tap while isInstalled() is still running on Dispatchers.IO
        vm.installSelectedPackages(mockContext, mockSandboxManager) {
            successCount.incrementAndGet()
        }
        val secondJob = vm.activeInstallJob

        firstJob?.join()

        assertTrue(firstJob != null)
        assertTrue(firstJob === secondJob)
        assertEquals(1, runCount.get())
        assertEquals(1, successCount.get())
        assertEquals(SandboxInstallState.Completed, vm.installState.value)
    }

    @Test
    fun testRetryAndInstallRecoverCleanlyAfterCancellationException() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val attempt = AtomicInteger(0)

        val fakeProvider = object : ExecutionProvider {
            override fun getTierName(): String = "Fake"
            override fun isAvailable(): Boolean = true
            override suspend fun runCommand(command: String, workingDir: String?): CommandResult {
                val current = attempt.incrementAndGet()
                if (current == 1) {
                    started.complete(Unit)
                    throw CancellationException("cancelled during install")
                }
                return CommandResult(exitCode = 0, stdout = "OK", stderr = "")
            }
        }

        val mockSandboxManager = mock(SandboxManager::class.java)
        `when`(mockSandboxManager.isInstalled()).thenReturn(true)
        `when`(mockSandboxManager.isToolInstalled(anyString())).thenReturn(true)

        val vm = OnboardingViewModel(
            executionProviderFactory = { _, _ -> fakeProvider },
            ioDispatcher = Dispatchers.Unconfined,
            coroutineScopeOverride = this
        )

        val successCount = AtomicInteger(0)
        vm.installSelectedPackages(mockContext, mockSandboxManager) {
            successCount.incrementAndGet()
        }
        started.await()
        vm.activeInstallJob?.join()

        // Cancellation must reset state back to Idle (neither Error nor wedged in InstallingPackages)
        assertEquals(SandboxInstallState.Idle, vm.installState.value)
        assertEquals(0, successCount.get())

        // Retry immediately after cancellation must execute and complete
        vm.retryInstallation(mockContext, mockSandboxManager) {
            successCount.incrementAndGet()
        }
        vm.activeInstallJob?.join()

        assertEquals(2, attempt.get())
        assertEquals(1, successCount.get())
        assertEquals(SandboxInstallState.Completed, vm.installState.value)

        // Also verify external Job.cancel() while suspended does not wedge installSelectedPackages()
        val gate = CompletableDeferred<Unit>()
        val cancelAttempt = AtomicInteger(0)
        val started2 = CompletableDeferred<Unit>()
        val provider2 = object : ExecutionProvider {
            override fun getTierName(): String = "Fake"
            override fun isAvailable(): Boolean = true
            override suspend fun runCommand(command: String, workingDir: String?): CommandResult {
                if (cancelAttempt.incrementAndGet() == 1) {
                    started2.complete(Unit)
                    gate.await()
                }
                return CommandResult(exitCode = 0, stdout = "OK", stderr = "")
            }
        }
        val vm2 = OnboardingViewModel(
            executionProviderFactory = { _, _ -> provider2 },
            ioDispatcher = Dispatchers.IO,
            coroutineScopeOverride = this
        )
        val success2 = AtomicInteger(0)
        vm2.installSelectedPackages(mockContext, mockSandboxManager) { success2.incrementAndGet() }
        started2.await()
        val jobToCancel = vm2.activeInstallJob
        jobToCancel?.cancel()
        jobToCancel?.join()

        assertEquals(SandboxInstallState.Idle, vm2.installState.value)
        vm2.installSelectedPackages(mockContext, mockSandboxManager) { success2.incrementAndGet() }
        vm2.activeInstallJob?.join()
        assertEquals(1, success2.get())
        assertEquals(SandboxInstallState.Completed, vm2.installState.value)
    }

    @Test
    fun testLateCancellationDuringPostCompleteDelayPreservesCompletedState() = runBlocking {
        val fakeProvider = object : ExecutionProvider {
            override fun getTierName(): String = "Fake"
            override fun isAvailable(): Boolean = true
            override suspend fun runCommand(command: String, workingDir: String?): CommandResult {
                return CommandResult(exitCode = 0, stdout = "OK", stderr = "")
            }
        }

        val mockSandboxManager = mock(SandboxManager::class.java)
        `when`(mockSandboxManager.isInstalled()).thenReturn(true)
        `when`(mockSandboxManager.isToolInstalled(anyString())).thenReturn(true)

        val vm = OnboardingViewModel(
            executionProviderFactory = { _, _ -> fakeProvider },
            ioDispatcher = Dispatchers.Unconfined,
            coroutineScopeOverride = this
        )

        var sawCompleted = false
        val collector = launch {
            vm.installState.collect { state ->
                if (state is SandboxInstallState.Completed) {
                    sawCompleted = true
                    vm.activeInstallJob?.cancel()
                }
            }
        }

        var onSuccessCalled = false
        vm.installSelectedPackages(mockContext, mockSandboxManager) {
            onSuccessCalled = true
        }
        vm.activeInstallJob?.join()
        collector.cancel()

        assertTrue("Expected Completed state before cancellation", sawCompleted)
        assertFalse("onSuccess must not run when cancelled during delay(300)", onSuccessCalled)
        assertEquals(SandboxInstallState.Completed, vm.installState.value)
        assertNull(vm.activeInstallJob)
    }

    @Test
    fun testCancellationExceptionInsideOnSuccessPreservesCompletedState() = runBlocking {
        val fakeProvider = object : ExecutionProvider {
            override fun getTierName(): String = "Fake"
            override fun isAvailable(): Boolean = true
            override suspend fun runCommand(command: String, workingDir: String?): CommandResult {
                return CommandResult(exitCode = 0, stdout = "OK", stderr = "")
            }
        }

        val mockSandboxManager = mock(SandboxManager::class.java)
        `when`(mockSandboxManager.isInstalled()).thenReturn(true)
        `when`(mockSandboxManager.isToolInstalled(anyString())).thenReturn(true)

        val vm = OnboardingViewModel(
            executionProviderFactory = { _, _ -> fakeProvider },
            ioDispatcher = Dispatchers.Unconfined,
            coroutineScopeOverride = this
        )

        var onSuccessCalled = false
        vm.installSelectedPackages(mockContext, mockSandboxManager) {
            onSuccessCalled = true
            throw CancellationException("caller scope cancelled during navigation")
        }
        vm.activeInstallJob?.join()

        assertTrue(onSuccessCalled)
        assertEquals(SandboxInstallState.Completed, vm.installState.value)
        assertNull(vm.activeInstallJob)
    }
}
