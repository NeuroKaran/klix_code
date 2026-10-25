package com.klix.code

import android.app.Application
import android.content.Context
import com.klix.code.data.sandbox.SandboxExecutionProvider
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
        assertTrue(lines.isEmpty())
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
        assertEquals(0, terminalViewModel.terminalLines.value.size)

        // Switch back to terminal 1 and verify buffer preserved
        terminalViewModel.selectTerminal(term1Id)
        assertEquals(term1LinesCount, terminalViewModel.terminalLines.value.size)

        // Switch back to terminal 2 and verify it's still cleared
        terminalViewModel.selectTerminal(term2Id)
        assertEquals(0, terminalViewModel.terminalLines.value.size)
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
}
