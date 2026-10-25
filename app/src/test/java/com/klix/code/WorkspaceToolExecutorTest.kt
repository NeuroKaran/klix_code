package com.klix.code

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.klix.code.data.agent.execution.CommandResult
import com.klix.code.data.agent.execution.ExecutionProvider
import com.klix.code.data.agent.tools.AgentTools
import com.klix.code.data.agent.tools.ToolCallRequest
import com.klix.code.data.agent.tools.WorkspaceToolExecutor
import com.klix.code.data.model.FileNode
import com.klix.code.data.model.ProjectItem
import com.klix.code.data.repository.SearchMatch
import com.klix.code.data.repository.WorkspaceRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class WorkspaceToolExecutorTest {

    private class FakeExecutionProvider(
        var outputToReturn: String = "All tests passed (2/2)",
        var exitCodeToReturn: Int = 0
    ) : ExecutionProvider {
        override fun getTierName(): String = "Fake Execution"
        override fun isAvailable(): Boolean = true
        override suspend fun runCommand(command: String, workingDir: String?): CommandResult {
            return CommandResult(
                exitCode = exitCodeToReturn,
                stdout = outputToReturn,
                stderr = "",
                executionTimeMs = 42L
            )
        }
    }

    private class FakeWorkspaceRepository : WorkspaceRepository {
        val files = mutableMapOf<String, String>()

        override suspend fun persistProjectPermission(uri: Uri, name: String) {}
        override suspend fun getRecentProjects(): List<ProjectItem> = emptyList()
        override suspend fun removeRecentProject(uri: Uri) {}
        override suspend fun loadTree(treeUri: Uri): FileNode? = null
        override suspend fun readFile(fileUri: Uri): String = files[fileUri.toString()] ?: ""
        override suspend fun writeFile(fileUri: Uri, content: String) {
            files[fileUri.toString()] = content
        }
        override suspend fun createFile(parentDirUri: Uri, name: String): Uri? = null
        override suspend fun createDirectory(parentDirUri: Uri, name: String): Uri? = null
        override suspend fun delete(targetUri: Uri): Boolean = true
        override suspend fun rename(targetUri: Uri, newName: String): Boolean = true

        override suspend fun findDocumentByPath(treeUri: Uri, relativePath: String): DocumentFile? = null
        override suspend fun findOrCreateDocumentByPath(treeUri: Uri, relativePath: String): DocumentFile? = null

        override suspend fun searchFiles(
            treeUri: Uri,
            query: String,
            relativeScope: String?,
            maxResults: Int
        ): List<SearchMatch> {
            return listOf(
                SearchMatch("src/Main.kt", 10, "val query = \"$query\"")
            )
        }

        override suspend fun listDirectory(treeUri: Uri, relativePath: String): List<FileNode> = emptyList()
    }

    @Test
    fun testRunCommandToolExecution() = runBlocking {
        val fakeRepo = FakeWorkspaceRepository()
        val fakeExec = FakeExecutionProvider(outputToReturn = "Compiled successfully", exitCodeToReturn = 0)
        val executor = WorkspaceToolExecutor(fakeRepo, fakeExec)

        val toolCall = ToolCallRequest(
            id = "call_123",
            name = "run_command",
            rawArguments = """{"command": "echo hello"}""",
            parsedArguments = mapOf("command" to "echo hello")
        )

        val result = executor.execute(
            rootTreeUri = null,
            toolCall = toolCall,
            dryRunWrite = false
        )

        assertTrue(result.isSuccess)
        assertEquals("Compiled successfully", result.output)
    }

    @Test
    fun testDefaultApiPrefixResolution() = runBlocking {
        val fakeRepo = FakeWorkspaceRepository()
        val fakeExec = FakeExecutionProvider(outputToReturn = "Command resolved via default_api prefix")
        val executor = WorkspaceToolExecutor(fakeRepo, fakeExec)

        val toolCall = ToolCallRequest(
            id = "call_prefix",
            name = "default_api:run_command",
            rawArguments = """{"command": "echo test"}""",
            parsedArguments = mapOf("command" to "echo test")
        )

        val result = executor.execute(
            rootTreeUri = null,
            toolCall = toolCall,
            dryRunWrite = false
        )

        assertTrue(result.isSuccess)
        assertEquals("Command resolved via default_api prefix", result.output)
    }

    @Test
    fun testSearchFilesToolExecution() = runBlocking {
        val fakeRepo = FakeWorkspaceRepository()
        val fakeExec = FakeExecutionProvider()
        val executor = WorkspaceToolExecutor(fakeRepo, fakeExec)

        val dummyUri = mock(Uri::class.java)

        val toolCall = ToolCallRequest(
            id = "call_456",
            name = "search_files",
            rawArguments = """{"query": "test"}""",
            parsedArguments = mapOf("query" to "test")
        )

        val result = executor.execute(
            rootTreeUri = dummyUri,
            toolCall = toolCall,
            dryRunWrite = false
        )

        assertTrue(result.isSuccess)
        assertTrue(result.output.contains("Found 1 match(es)"))
        assertTrue(result.output.contains("src/Main.kt:10"))
    }

    @Test
    fun testWriteFileGeneratesDiffOnDryRun() = runBlocking {
        val fakeRepo = FakeWorkspaceRepository()
        val fakeExec = FakeExecutionProvider()
        val executor = WorkspaceToolExecutor(fakeRepo, fakeExec)

        val dummyUri = mock(Uri::class.java)

        val toolCall = ToolCallRequest(
            id = "call_789",
            name = "write_file",
            rawArguments = """{"path": "hello.py", "content": "print('hello world')"}""",
            parsedArguments = mapOf("path" to "hello.py", "content" to "print('hello world')")
        )

        val result = executor.execute(
            rootTreeUri = dummyUri,
            toolCall = toolCall,
            dryRunWrite = true
        )

        assertTrue(result.isSuccess)
        assertNotNull(result.proposedDiff)
        assertEquals("hello.py", result.proposedDiff?.filePath)
        assertEquals(1, result.proposedDiff?.additionsCount)
    }
}
