package com.klix.code

import android.content.Context
import com.klix.code.data.sandbox.SandboxManager
import com.klix.code.data.sandbox.SandboxStatus
import org.junit.Assert.assertEquals
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
}
