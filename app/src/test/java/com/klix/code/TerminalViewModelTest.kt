package com.klix.code

import android.app.Application
import android.content.Context
import android.net.Uri
import com.klix.code.data.sandbox.SandboxExecutionProvider
import com.klix.code.data.workspace.SandboxWorkspace
import com.klix.code.ui.components.terminal.TerminalTabUiHelper
import com.klix.code.ui.terminal.TerminalViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File

class TerminalViewModelTest {

    private lateinit var mockApp: Application
    private lateinit var tempDir: File
    private lateinit var terminalViewModel: TerminalViewModel

    @Before
    fun setup() {
        mockApp = mock(Application::class.java)
        tempDir = File(System.getProperty("java.io.tmpdir"), "klix_test_terminal_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        `when`(mockApp.filesDir).thenReturn(tempDir)
        `when`(mockApp.applicationContext).thenReturn(mockApp)

        val execProvider = SandboxExecutionProvider(mockApp)
        terminalViewModel = TerminalViewModel(mockApp, execProvider)
    }

    @Test
    fun testInitialBannerLines() {
        val lines = terminalViewModel.terminalLines.value
        assertTrue(lines.isNotEmpty())
        assertTrue(lines.any { it.text.contains("KlixCode Linux Terminal") })
    }

    @Test
    fun testCommandHistoryNavigation() {
        terminalViewModel.executeCommand("echo test1")
        terminalViewModel.executeCommand("echo test2")
        terminalViewModel.executeCommand("echo test3")

        val history = terminalViewModel.commandHistory.value
        assertEquals(3, history.size)
        assertEquals("echo test1", history[0])
        assertEquals("echo test2", history[1])
        assertEquals("echo test3", history[2])

        // Previous recall
        val prev1 = terminalViewModel.getPreviousHistory()
        assertEquals("echo test3", prev1)

        val prev2 = terminalViewModel.getPreviousHistory()
        assertEquals("echo test2", prev2)

        val next1 = terminalViewModel.getNextHistory()
        assertEquals("echo test3", next1)
    }

    @Test
    fun testBuiltInClearCommand() {
        terminalViewModel.executeCommand("clear")
        val lines = terminalViewModel.terminalLines.value
        assertEquals(1, lines.size)
        assertTrue(lines.first().isPrompt)
    }

    @Test
    fun testBuiltInHelpCommand() {
        terminalViewModel.executeCommand("help")
        val lines = terminalViewModel.terminalLines.value
        assertTrue(lines.any { it.text.contains("Available Klix Terminal Commands") })
    }

    @Test
    fun testWorkingDirectoryChange() {
        terminalViewModel.setWorkingDirectory("/workspace/my-app")
        assertEquals("/workspace/my-app", terminalViewModel.currentCwd.value)
    }

    @Test
    fun testMultipleTerminalsCreationAndSelection() {
        // Initial state should have 1 terminal named Terminal 1
        val initialTabs = terminalViewModel.tabs.value
        assertEquals(1, initialTabs.size)
        assertEquals("Terminal 1", initialTabs[0].title)
        val initialId = terminalViewModel.activeTerminalId.value
        assertEquals(initialTabs[0].id, initialId)

        // Add a second terminal
        val term2Id = terminalViewModel.addNewTerminal()
        val updatedTabs = terminalViewModel.tabs.value
        assertEquals(2, updatedTabs.size)
        assertEquals("Terminal 2", updatedTabs[1].title)
        assertEquals(term2Id, terminalViewModel.activeTerminalId.value)

        // Add a custom named terminal
        val term3Id = terminalViewModel.addNewTerminal("Dev Server")
        val tabs3 = terminalViewModel.tabs.value
        assertEquals(3, tabs3.size)
        assertEquals("Dev Server", tabs3[2].title)
        assertEquals(term3Id, terminalViewModel.activeTerminalId.value)

        // Switch back to initial terminal
        terminalViewModel.selectTerminal(initialId)
        assertEquals(initialId, terminalViewModel.activeTerminalId.value)
    }

    @Test
    fun testSwitchingTerminalsMaintainsSeparateBuffers() {
        val term1Id = terminalViewModel.activeTerminalId.value
        terminalViewModel.executeCommand("help")
        val term1LinesCount = terminalViewModel.terminalLines.value.size
        assertTrue(term1LinesCount > 0)

        // Spin up second terminal and clear it
        val term2Id = terminalViewModel.addNewTerminal()
        terminalViewModel.executeCommand("clear")
        assertEquals(1, terminalViewModel.terminalLines.value.size)
        assertTrue(terminalViewModel.terminalLines.value.first().isPrompt)

        // Switch back to terminal 1 and verify buffer preserved
        terminalViewModel.selectTerminal(term1Id)
        assertEquals(term1LinesCount, terminalViewModel.terminalLines.value.size)

        // Switch back to terminal 2 and verify it still has its persistent prompt line
        terminalViewModel.selectTerminal(term2Id)
        assertEquals(1, terminalViewModel.terminalLines.value.size)
        assertTrue(terminalViewModel.terminalLines.value.first().isPrompt)
    }

    @Test
    fun testClosingTerminalSwitchesActiveTab() {
        val term1Id = terminalViewModel.activeTerminalId.value
        val term2Id = terminalViewModel.addNewTerminal()
        val term3Id = terminalViewModel.addNewTerminal()

        assertEquals(3, terminalViewModel.tabs.value.size)
        assertEquals(term3Id, terminalViewModel.activeTerminalId.value)

        // Close active terminal 3 -> should switch to terminal 2
        terminalViewModel.closeTerminal(term3Id)
        assertEquals(2, terminalViewModel.tabs.value.size)
        assertEquals(term2Id, terminalViewModel.activeTerminalId.value)

        // Close terminal 2 -> should switch to terminal 1
        terminalViewModel.closeTerminal(term2Id)
        assertEquals(1, terminalViewModel.tabs.value.size)
        assertEquals(term1Id, terminalViewModel.activeTerminalId.value)
    }

    @Test
    fun testTerminalTabUiHelperCloseableRules() {
        // Single tab (primary tab only) cannot be closed
        assertFalse(TerminalTabUiHelper.isCloseable(tabIndex = 0, totalTabs = 1))

        // Two tabs: primary tab (0) cannot be closed, secondary tab (1) can be closed
        assertFalse(TerminalTabUiHelper.isCloseable(tabIndex = 0, totalTabs = 2))
        assertTrue(TerminalTabUiHelper.isCloseable(tabIndex = 1, totalTabs = 2))

        // Three tabs: primary tab cannot be closed, both secondary tabs can be closed
        assertFalse(TerminalTabUiHelper.isCloseable(tabIndex = 0, totalTabs = 3))
        assertTrue(TerminalTabUiHelper.isCloseable(tabIndex = 1, totalTabs = 3))
        assertTrue(TerminalTabUiHelper.isCloseable(tabIndex = 2, totalTabs = 3))

        // Boundary and invalid index handling
        assertFalse(TerminalTabUiHelper.isCloseable(tabIndex = -1, totalTabs = 3))
        assertFalse(TerminalTabUiHelper.isCloseable(tabIndex = 3, totalTabs = 3))
        assertFalse(TerminalTabUiHelper.isCloseable(tabIndex = 1, totalTabs = 0))
    }

    @Test
    fun testTerminalTabUiHelperNewTerminalButtonLabel() {
        assertEquals("Terminal", TerminalTabUiHelper.NEW_TERMINAL_BUTTON_LABEL)
    }

    @Test
    fun testExitCommandClosesSecondaryTerminalTab() {
        val term1Id = terminalViewModel.activeTerminalId.value
        val term2Id = terminalViewModel.addNewTerminal()
        assertEquals(2, terminalViewModel.tabs.value.size)
        assertEquals(term2Id, terminalViewModel.activeTerminalId.value)

        // Type 'exit' in secondary terminal
        terminalViewModel.executeCommand("exit")

        // Should close terminal 2 and cleanly switch active back to terminal 1
        assertEquals(1, terminalViewModel.tabs.value.size)
        assertEquals(term1Id, terminalViewModel.activeTerminalId.value)
        assertEquals("Terminal 1", terminalViewModel.tabs.value[0].title)
    }

    @Test
    fun testClosingLastTerminalSpawnsFreshTerminal() {
        val term1Id = terminalViewModel.activeTerminalId.value
        terminalViewModel.closeTerminal(term1Id)

        // Should still have 1 fresh terminal
        val tabs = terminalViewModel.tabs.value
        assertEquals(1, tabs.size)
        assertEquals("Terminal 1", tabs[0].title)
        assertNotEquals(term1Id, tabs[0].id)
    }

    @Test
    fun testRenameTerminal() {
        val activeId = terminalViewModel.activeTerminalId.value
        terminalViewModel.renameTerminal(activeId, "Build Server")

        val tab = terminalViewModel.tabs.value.find { it.id == activeId }
        assertNotNull(tab)
        assertEquals("Build Server", tab?.title)
    }

    @Test
    fun testInputDraftPreservedAcrossTabs() {
        val term1Id = terminalViewModel.activeTerminalId.value
        terminalViewModel.setInputDraft("npm run build")

        val term2Id = terminalViewModel.addNewTerminal()
        assertEquals("", terminalViewModel.getInputDraft())

        terminalViewModel.setInputDraft("python server.py")

        // Switch back to terminal 1
        terminalViewModel.selectTerminal(term1Id)
        assertEquals("npm run build", terminalViewModel.getInputDraft())

        // Switch back to terminal 2
        terminalViewModel.selectTerminal(term2Id)
        assertEquals("python server.py", terminalViewModel.getInputDraft())
    }

    @Test
    fun testCancelExecutionUpdatesStateAndAppendsInterrupted() {
        terminalViewModel.cancelExecution()
        assertEquals(false, terminalViewModel.isExecuting.value)
        val lines = terminalViewModel.terminalLines.value
        val hasInterrupted = lines.any { it.text.contains("Process interrupted") }
        assertTrue(hasInterrupted)
    }

    @Test
    fun testSendInputCtrlCInterruptsProcess() {
        // Trigger execution state simulation
        terminalViewModel.cancelExecution()
        terminalViewModel.sendInput("^C")
        val lines = terminalViewModel.terminalLines.value
        val count = lines.count { it.text.contains("Process interrupted") }
        assertTrue(count >= 1)
    }

    @Test
    fun testAutoDetectAndSwitchWorkspaceUpdatesCwdAndPrompt() {
        val dummyUri = mock(Uri::class.java)
        `when`(dummyUri.toString()).thenReturn("file:///workspaces/project-beta")

        val ws2 = SandboxWorkspace(
            id = "ws_beta_123",
            name = "Project Beta",
            directoryName = "project-beta",
            relativePath = "/workspaces/project-beta",
            rootUri = dummyUri
        )

        terminalViewModel.onActiveWorkspaceChanged(ws2)

        assertEquals("/workspaces/project-beta", terminalViewModel.currentCwd.value)
        val lines = terminalViewModel.terminalLines.value
        assertTrue(lines.any { it.text.contains("Project Beta") })
        val prompt = lines.lastOrNull { it.isPrompt }
        assertNotNull(prompt)
        assertEquals("klix@mobile:/workspaces/project-beta$", prompt?.text)
    }

    @Test
    fun testWorkspaceSwitchPreservesSeparateTerminalSessions() {
        val dummyUri1 = mock(Uri::class.java)
        `when`(dummyUri1.toString()).thenReturn("file:///workspaces/default")
        val ws1 = SandboxWorkspace(
            id = "default_workspace",
            name = "Default Workspace",
            directoryName = "default",
            relativePath = "/workspaces/default",
            rootUri = dummyUri1
        )

        val dummyUri2 = mock(Uri::class.java)
        `when`(dummyUri2.toString()).thenReturn("file:///workspaces/project-two")
        val ws2 = SandboxWorkspace(
            id = "ws_two",
            name = "Project Two",
            directoryName = "project-two",
            relativePath = "/workspaces/project-two",
            rootUri = dummyUri2
        )

        // 1. Start in ws1, execute a command and create a 2nd terminal
        terminalViewModel.onActiveWorkspaceChanged(ws1)
        terminalViewModel.executeCommand("help")
        val ws1Term2Id = terminalViewModel.addNewTerminal("Server Tab")
        assertEquals(2, terminalViewModel.tabs.value.size)

        // 2. Switch to ws2: it should auto-create its own Terminal 1 with project-two cwd
        terminalViewModel.onActiveWorkspaceChanged(ws2)
        assertEquals(1, terminalViewModel.tabs.value.size)
        assertEquals("/workspaces/project-two", terminalViewModel.currentCwd.value)

        terminalViewModel.executeCommand("clear")
        assertEquals(1, terminalViewModel.terminalLines.value.size)
        assertEquals("klix@mobile:/workspaces/project-two$", terminalViewModel.terminalLines.value.first().text)

        // 3. Switch back to ws1: should restore its 2 tabs, active Server Tab, and help output
        terminalViewModel.onActiveWorkspaceChanged(ws1)
        assertEquals(2, terminalViewModel.tabs.value.size)
        assertEquals(ws1Term2Id, terminalViewModel.activeTerminalId.value)
        assertEquals("Server Tab", terminalViewModel.tabs.value.find { it.id == ws1Term2Id }?.title)

        // 4. Switch back to ws2: still has 1 cleared tab
        terminalViewModel.onActiveWorkspaceChanged(ws2)
        assertEquals(1, terminalViewModel.tabs.value.size)
        assertEquals("Terminal 1", terminalViewModel.tabs.value[0].title)
    }

    @Test
    fun testWorkspaceDeletionCleansUpSessions() {
        val dummyUri = mock(Uri::class.java)
        `when`(dummyUri.toString()).thenReturn("file:///workspaces/to-delete")
        val wsToDelete = SandboxWorkspace(
            id = "ws_to_delete",
            name = "Temporary",
            directoryName = "to-delete",
            relativePath = "/workspaces/to-delete",
            rootUri = dummyUri
        )

        terminalViewModel.onActiveWorkspaceChanged(wsToDelete)
        terminalViewModel.addNewTerminal("Temp 2")
        assertEquals(2, terminalViewModel.tabs.value.size)

        terminalViewModel.onWorkspaceDeleted("ws_to_delete")

        // Switch to fresh workspace
        val dummyUriMain = mock(Uri::class.java)
        `when`(dummyUriMain.toString()).thenReturn("file:///workspaces/default")
        val wsMain = SandboxWorkspace(
            id = "default_workspace",
            name = "Default Workspace",
            directoryName = "default",
            relativePath = "/workspaces/default",
            rootUri = dummyUriMain
        )
        terminalViewModel.onActiveWorkspaceChanged(wsMain)
        assertEquals(1, terminalViewModel.tabs.value.size)
    }

    @Test
    fun testWorkspaceDeletionImmediatelyCleansUpActiveBuffers() {
        val dummyUri = mock(Uri::class.java)
        `when`(dummyUri.toString()).thenReturn("file:///workspaces/ephemeral")
        val ephemeralWs = SandboxWorkspace(
            id = "ws_ephemeral",
            name = "Ephemeral",
            directoryName = "ephemeral",
            relativePath = "/workspaces/ephemeral",
            rootUri = dummyUri
        )

        terminalViewModel.onActiveWorkspaceChanged(ephemeralWs)
        terminalViewModel.executeCommand("echo secret_key_12345")
        assertTrue(terminalViewModel.terminalLines.value.any { it.text.contains("secret_key_12345") })

        terminalViewModel.onWorkspaceDeleted("ws_ephemeral")

        // Ephemeral buffers should not linger in active lines
        assertFalse(terminalViewModel.terminalLines.value.any { it.text.contains("secret_key_12345") })
    }
}
