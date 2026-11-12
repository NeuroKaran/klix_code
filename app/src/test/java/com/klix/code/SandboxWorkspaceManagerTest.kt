package com.klix.code

import android.content.Context
import android.net.Uri
import com.klix.code.data.sandbox.SandboxManager
import com.klix.code.data.workspace.SandboxWorkspaceManagerImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File

class SandboxWorkspaceManagerTest {

    private lateinit var mockContext: Context
    private lateinit var tempDir: File
    private lateinit var sandboxManager: SandboxManager
    private lateinit var workspaceManager: SandboxWorkspaceManagerImpl

    @Before
    fun setup() {
        mockContext = mock(Context::class.java)
        tempDir = File(System.getProperty("java.io.tmpdir"), "klix_test_ws_mgr_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        `when`(mockContext.filesDir).thenReturn(tempDir)

        sandboxManager = SandboxManager(mockContext)
        workspaceManager = SandboxWorkspaceManagerImpl(mockContext, sandboxManager)
    }

    @Test
    fun testInitializationCreatesDefaultWorkspace() = runBlocking {
        workspaceManager.initialize()

        val active = workspaceManager.activeWorkspace.value
        assertNotNull(active)
        assertEquals("Default Workspace", active?.name)
        assertEquals("default", active?.directoryName)
        assertEquals("/workspaces/default", active?.relativePath)

        val workspaces = workspaceManager.workspaces.value
        assertEquals(1, workspaces.size)
        assertEquals(active?.id, workspaces[0].id)

        val wsDir = workspaceManager.getWorkspaceDirectory(active!!)
        assertTrue(wsDir.exists())
        assertTrue(File(wsDir, ".klix").exists())
    }

    @Test
    fun testLegacyWorkspaceMigration() = runBlocking {
        // Prepare legacy workspace directory at sandbox/rootfs/workspace
        val rootfsDir = sandboxManager.getRootfsDir()
        val legacyDir = File(rootfsDir, "workspace").apply { mkdirs() }
        val sampleFile = File(legacyDir, "legacy_file.txt").apply { writeText("legacy code") }

        workspaceManager.initialize()

        val active = workspaceManager.activeWorkspace.value
        assertNotNull(active)
        val defaultDir = workspaceManager.getWorkspaceDirectory(active!!)
        assertTrue(defaultDir.exists())
        assertEquals("default", defaultDir.name)

        // Legacy file should have moved to default workspace
        val migratedFile = File(defaultDir, "legacy_file.txt")
        assertTrue(migratedFile.exists())
        assertEquals("legacy code", migratedFile.readText())
    }

    @Test
    fun testCreateWorkspaceSlugSanitization() = runBlocking {
        workspaceManager.initialize()

        val res = workspaceManager.createWorkspace("Web App 2026")
        assertTrue(res.isSuccess)
        val ws = res.getOrThrow()

        assertEquals("Web App 2026", ws.name)
        assertEquals("web-app-2026", ws.directoryName)
        assertEquals("/workspaces/web-app-2026", ws.relativePath)
        assertEquals(ws.id, workspaceManager.activeWorkspace.value?.id)

        val dir = workspaceManager.getWorkspaceDirectory(ws)
        assertTrue(dir.exists())
        assertTrue(File(dir, ".klix").exists())
    }

    @Test
    fun testCreateWorkspaceWithSpecialCharacters() = runBlocking {
        workspaceManager.initialize()

        val res = workspaceManager.createWorkspace("My @Awesome# Project! 2026")
        assertTrue(res.isSuccess)
        val ws = res.getOrThrow()

        assertEquals("my-awesome-project-2026", ws.directoryName)
    }

    @Test
    fun testCreateWorkspaceEmptyNameFails() = runBlocking {
        workspaceManager.initialize()

        val res = workspaceManager.createWorkspace("   ")
        assertTrue(res.isFailure)
    }

    @Test
    fun testCreateWorkspaceDuplicateSlugAppendsCount() = runBlocking {
        workspaceManager.initialize()

        val ws1 = workspaceManager.createWorkspace("Project Alpha").getOrThrow()
        val ws2 = workspaceManager.createWorkspace("Project Alpha").getOrThrow()
        val ws3 = workspaceManager.createWorkspace("Project Alpha").getOrThrow()

        assertEquals("project-alpha", ws1.directoryName)
        assertEquals("project-alpha-2", ws2.directoryName)
        assertEquals("project-alpha-3", ws3.directoryName)

        assertTrue(workspaceManager.getWorkspaceDirectory(ws1).exists())
        assertTrue(workspaceManager.getWorkspaceDirectory(ws2).exists())
        assertTrue(workspaceManager.getWorkspaceDirectory(ws3).exists())
    }

    @Test
    fun testSwitchWorkspace() = runBlocking {
        workspaceManager.initialize()

        val defaultWs = workspaceManager.activeWorkspace.value!!
        val newWs = workspaceManager.createWorkspace("Beta Project").getOrThrow()
        assertEquals(newWs.id, workspaceManager.activeWorkspace.value?.id)

        // Switch back to default
        val switched = workspaceManager.switchWorkspace(defaultWs.id)
        assertNotNull(switched)
        assertEquals(defaultWs.id, switched?.id)
        assertEquals(defaultWs.id, workspaceManager.activeWorkspace.value?.id)
    }

    @Test
    fun testDeleteWorkspacePreventsDeletingLastWorkspace() = runBlocking {
        workspaceManager.initialize()

        val active = workspaceManager.activeWorkspace.value!!
        val deleted = workspaceManager.deleteWorkspace(active.id)
        assertFalse(deleted)
        assertEquals(1, workspaceManager.workspaces.value.size)
    }

    @Test
    fun testDeleteWorkspaceRemovesDirectoryAndUpdatesActive() = runBlocking {
        workspaceManager.initialize()

        val ws1 = workspaceManager.activeWorkspace.value!!
        val ws2 = workspaceManager.createWorkspace("To Be Deleted").getOrThrow()
        val ws2Dir = workspaceManager.getWorkspaceDirectory(ws2)
        assertTrue(ws2Dir.exists())

        assertEquals(ws2.id, workspaceManager.activeWorkspace.value?.id)

        val deleted = workspaceManager.deleteWorkspace(ws2.id)
        assertTrue(deleted)
        assertFalse(ws2Dir.exists())

        // Active workspace should fallback to remaining workspace
        assertEquals(ws1.id, workspaceManager.activeWorkspace.value?.id)
        assertEquals(1, workspaceManager.workspaces.value.size)
    }

    @Test
    fun testRenameWorkspace() = runBlocking {
        workspaceManager.initialize()

        val active = workspaceManager.activeWorkspace.value!!
        val renamed = workspaceManager.renameWorkspace(active.id, "Renamed Workspace")
        assertTrue(renamed)

        assertEquals("Renamed Workspace", workspaceManager.activeWorkspace.value?.name)
        val found = workspaceManager.workspaces.value.find { it.id == active.id }
        assertEquals("Renamed Workspace", found?.name)
    }

    @Test
    fun testSanitizeDirectoryNameComplex() {
        assertEquals("hello-world", workspaceManager.sanitizeDirectoryName("Hello   World!"))
        assertEquals("project-v2", workspaceManager.sanitizeDirectoryName("Project...v2"))
        assertEquals("path-traversal", workspaceManager.sanitizeDirectoryName("../../../path/traversal"))
        assertEquals("workspace", workspaceManager.sanitizeDirectoryName("!@#$%^&*()"))
        assertEquals("c-project", workspaceManager.sanitizeDirectoryName("C++ Project"))
    }

    @Test
    fun testSlugCollisionAvoidanceWithRegistry() = runBlocking {
        workspaceManager.initialize()

        val ws1 = workspaceManager.createWorkspace("Alpha").getOrThrow()
        assertEquals("alpha", ws1.directoryName)

        // Delete ws1 directory physically, but keep ws1 in manager registry
        val dir1 = workspaceManager.getWorkspaceDirectory(ws1)
        dir1.deleteRecursively()

        // Create another workspace with same name; should not reuse "alpha"
        val ws2 = workspaceManager.createWorkspace("Alpha").getOrThrow()
        assertEquals("alpha-2", ws2.directoryName)
    }

    @Test
    fun testDeleteWorkspaceSafetyGuardDoesNotDeleteParentRootDir() = runBlocking {
        workspaceManager.initialize()

        val ws = workspaceManager.createWorkspace("Guard Test").getOrThrow()
        val workspacesDir = File(sandboxManager.getRootfsDir(), "workspaces")
        assertTrue(workspacesDir.exists())

        val deleted = workspaceManager.deleteWorkspace(ws.id)
        assertTrue(deleted)
        assertTrue("workspaces root directory must remain intact after child workspace deletion", workspacesDir.exists())
    }
}
