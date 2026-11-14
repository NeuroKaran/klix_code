package com.klix.code

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import com.klix.code.data.sandbox.SandboxManager
import com.klix.code.data.sandbox.SandboxStatus
import com.klix.code.ui.components.sandbox.SandboxSetupUiHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File

class SandboxManagerTest {

    private lateinit var mockContext: Context
    private lateinit var tempDir: File
    private lateinit var sandboxManager: SandboxManager

    @Before
    fun setup() {
        mockContext = mock(Context::class.java)
        tempDir = File(System.getProperty("java.io.tmpdir"), "klix_test_sandbox_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        `when`(mockContext.filesDir).thenReturn(tempDir)

        sandboxManager = SandboxManager(mockContext)
    }

    @Test
    fun testArchitectureDetectionAndUrls() {
        val arch = sandboxManager.detectArch()
        assertNotNull(arch)
        assertTrue(arch in listOf("aarch64", "x86_64", "armv7", "x86"))

        val url = sandboxManager.getDownloadUrl("aarch64")
        assertTrue(url.contains("alpinelinux.org"))
        assertTrue(url.contains("alpine-minirootfs-"))
        assertTrue(url.contains("aarch64.tar.gz"))
    }

    @Test
    fun testDirectoryStructures() {
        val sandboxDir = sandboxManager.getSandboxDir()
        val rootfsDir = sandboxManager.getRootfsDir()
        val binDir = sandboxManager.getBinDir()

        assertEquals(File(tempDir, "sandbox").absolutePath, sandboxDir.absolutePath)
        assertEquals(File(sandboxDir, "rootfs").absolutePath, rootfsDir.absolutePath)
        assertEquals(File(sandboxDir, "bin").absolutePath, binDir.absolutePath)
    }

    @Test
    fun testInitialStatusAndReset() {
        assertEquals(SandboxStatus.NOT_INSTALLED, sandboxManager.status.value)

        val resetSuccess = sandboxManager.resetSandbox()
        assertTrue(resetSuccess)
        assertEquals(SandboxStatus.NOT_INSTALLED, sandboxManager.status.value)
    }

    @Test
    fun testSandboxSetupUiHelperActionMapping() {
        // When READY: should trigger reinstall with reload icon
        assertTrue(SandboxSetupUiHelper.isReinstall(SandboxStatus.READY))
        assertEquals("Reinstall RootFS", SandboxSetupUiHelper.getActionLabel(SandboxStatus.READY))
        assertEquals(Icons.Default.Refresh, SandboxSetupUiHelper.getActionIcon(SandboxStatus.READY))

        // When NOT_INSTALLED, DOWNLOADING, EXTRACTING, ERROR: should trigger install with download icon
        val installStatuses = listOf(
            SandboxStatus.NOT_INSTALLED,
            SandboxStatus.DOWNLOADING,
            SandboxStatus.EXTRACTING,
            SandboxStatus.ERROR
        )

        for (status in installStatuses) {
            assertFalse("Expected status $status to not be reinstall", SandboxSetupUiHelper.isReinstall(status))
            assertEquals("Expected status $status to show 'Install RootFS'", "Install RootFS", SandboxSetupUiHelper.getActionLabel(status))
            assertEquals("Expected status $status to show download icon", Icons.Default.Download, SandboxSetupUiHelper.getActionIcon(status))
        }
    }

    @Test
    fun testInstallSandboxRejectsConcurrentExecution() = runBlocking {
        // Mock status to DOWNLOADING
        val statusField = SandboxManager::class.java.getDeclaredField("_status")
        statusField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val statusFlow = statusField.get(sandboxManager) as kotlinx.coroutines.flow.MutableStateFlow<SandboxStatus>
        statusFlow.value = SandboxStatus.DOWNLOADING

        val result = sandboxManager.installSandbox()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals("RootFS installation is already in progress", result.exceptionOrNull()?.message)
    }

    @Test
    fun testToolchainActionMapping() {
        // Not installed: download icon and Tap to Install
        assertEquals("Tap to Install", SandboxSetupUiHelper.getToolActionLabel(isInstalled = false))
        assertEquals(Icons.Default.Download, SandboxSetupUiHelper.getToolActionIcon(isInstalled = false))

        // Installed: refresh/buffer icon and Reinstall
        assertEquals("Reinstall", SandboxSetupUiHelper.getToolActionLabel(isInstalled = true))
        assertEquals(Icons.Default.Refresh, SandboxSetupUiHelper.getToolActionIcon(isInstalled = true))

        // Installing in progress:
        assertEquals("Installing...", SandboxSetupUiHelper.getToolActionLabel(isInstalled = false, isInstalling = true))
        assertEquals("Installing...", SandboxSetupUiHelper.getToolActionLabel(isInstalled = true, isInstalling = true))
    }

    @Test
    fun testToolInstallationDetectionAndStateFlow() {
        // On clean sandbox (no rootfs), none of the tools are installed
        assertEquals(false, sandboxManager.isToolInstalled("nodejs"))
        assertEquals(false, sandboxManager.isToolInstalled("python3"))
        assertEquals(false, sandboxManager.isToolInstalled("git"))
        assertEquals(false, sandboxManager.isToolInstalled("c/c++"))

        val toolsMap = sandboxManager.installedTools.value
        assertEquals(false, toolsMap["nodejs"])
        assertEquals(false, toolsMap["python3"])
        assertEquals(false, toolsMap["git"])
        assertEquals(false, toolsMap["c/c++"])

        // Simulate installing nodejs binary in rootfs
        val rootfs = sandboxManager.getRootfsDir()
        val nodeBin = File(rootfs, "usr/bin/node")
        nodeBin.parentFile?.mkdirs()
        nodeBin.writeText("#!/bin/sh\necho v20.0.0")

        // Simulate installing python3 via apk world file
        val etcApk = File(rootfs, "etc/apk").apply { mkdirs() }
        val worldFile = File(etcApk, "world")
        worldFile.writeText("python3\n")

        sandboxManager.refreshInstalledTools()

        assertTrue(sandboxManager.isToolInstalled("nodejs"))
        assertTrue(sandboxManager.isToolInstalled("python3"))
        assertFalse(sandboxManager.isToolInstalled("git"))
        assertFalse(sandboxManager.isToolInstalled("c/c++"))

        val updatedMap = sandboxManager.installedTools.value
        assertEquals(true, updatedMap["nodejs"])
        assertEquals(true, updatedMap["python3"])
        assertEquals(false, updatedMap["git"])
        assertEquals(false, updatedMap["c/c++"])
    }

    @Test
    fun testToolInstallingTracking() {
        assertFalse(sandboxManager.installingTools.value.contains("nodejs"))

        sandboxManager.setToolInstalling("nodejs", true)
        assertTrue(sandboxManager.installingTools.value.contains("nodejs"))

        sandboxManager.setToolInstalling("nodejs", false)
        assertFalse(sandboxManager.installingTools.value.contains("nodejs"))
    }
}
