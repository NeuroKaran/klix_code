package com.klix.code

import android.app.Application
import android.content.Context
import com.klix.code.data.sandbox.SandboxExecutionProvider
import com.klix.code.ui.terminal.TerminalViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
