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
        val docs = mutableMapOf<String, DocumentFile>()

        fun addFile(relativePath: String, content: String): DocumentFile {
            val fileUri = mock(Uri::class.java)
            org.mockito.Mockito.`when`(fileUri.toString()).thenReturn("fake://$relativePath")
            val doc = mock(DocumentFile::class.java)
            org.mockito.Mockito.`when`(doc.uri).thenReturn(fileUri)
            org.mockito.Mockito.`when`(doc.isDirectory).thenReturn(false)
            org.mockito.Mockito.`when`(doc.name).thenReturn(relativePath.substringAfterLast("/"))
            files[fileUri.toString()] = content
            docs[relativePath] = doc
            return doc
        }

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

        override suspend fun findDocumentByPath(treeUri: Uri, relativePath: String): DocumentFile? = docs[relativePath]
        override suspend fun findOrCreateDocumentByPath(treeUri: Uri, relativePath: String): DocumentFile? {
            return docs[relativePath] ?: addFile(relativePath, "")
        }

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

    @Test
    fun testReadFileWithLineSlicingAndNumbering() = runBlocking {
        val fakeRepo = FakeWorkspaceRepository()
        fakeRepo.addFile(
            "src/Example.kt",
            "line one\nline two\nline three\nline four\nline five"
        )
        val executor = WorkspaceToolExecutor(fakeRepo, FakeExecutionProvider())
        val dummyUri = mock(Uri::class.java)

        val toolCall = ToolCallRequest(
            id = "call_read_slice",
            name = "read_file",
            rawArguments = """{"path": "src/Example.kt", "start_line": 2, "end_line": 4}""",
            parsedArguments = mapOf(
                "path" to "src/Example.kt",
                "start_line" to "2",
                "end_line" to "4"
            )
        )

        val result = executor.execute(dummyUri, toolCall)
        assertTrue(result.isSuccess)
        val expected = "2: line two\n3: line three\n4: line four"
        assertEquals(expected, result.output)
    }

    @Test
    fun testEditFileWithLineBoundsAndDisambiguation() = runBlocking {
        val fakeRepo = FakeWorkspaceRepository()
        val original = """
            fun a() {
                return false
            }
            fun b() {
                return false
            }
        """.trimIndent()
        fakeRepo.addFile("src/Logic.kt", original)
        val executor = WorkspaceToolExecutor(fakeRepo, FakeExecutionProvider())
        val dummyUri = mock(Uri::class.java)

        // 1. Without line bounds: duplicate target returns ambiguous error
        val ambiguousCall = ToolCallRequest(
            id = "call_ambiguous",
            name = "edit_file",
            rawArguments = """{"path": "src/Logic.kt", "old_string": "    return false", "new_string": "    return true"}""",
            parsedArguments = mapOf(
                "path" to "src/Logic.kt",
                "old_string" to "    return false",
                "new_string" to "    return true"
            )
        )
        val ambiguousResult = executor.execute(dummyUri, ambiguousCall)
        org.junit.Assert.assertFalse(ambiguousResult.isSuccess)
        assertTrue(ambiguousResult.error!!.contains("found 2 times"))

        // 2. With line bounds targeting lines 4..7: replaces only second occurrence
        val boundedCall = ToolCallRequest(
            id = "call_bounded",
            name = "edit_file",
            rawArguments = """{"path": "src/Logic.kt", "old_string": "    return false", "new_string": "    return true", "start_line": 4, "end_line": 6}""",
            parsedArguments = mapOf(
                "path" to "src/Logic.kt",
                "old_string" to "    return false",
                "new_string" to "    return true",
                "start_line" to "4",
                "end_line" to "6"
            )
        )
        val boundedResult = executor.execute(dummyUri, boundedCall, dryRunWrite = false)
        assertTrue(boundedResult.isSuccess)

        val doc = fakeRepo.findDocumentByPath(dummyUri, "src/Logic.kt")!!
        val updatedText = fakeRepo.readFile(doc.uri)
        assertTrue(updatedText.contains("fun a() {\n    return false"))
        assertTrue(updatedText.contains("fun b() {\n    return true"))
    }

    @Test
    fun testMultiReplaceFileContent() = runBlocking {
        val fakeRepo = FakeWorkspaceRepository()
        val original = """
            import alpha
            
            fun calculate() {
                val x = 10
                val y = 20
                return x + y
            }
        """.trimIndent()
        fakeRepo.addFile("src/Calc.kt", original)
        val executor = WorkspaceToolExecutor(fakeRepo, FakeExecutionProvider())
        val dummyUri = mock(Uri::class.java)

        val chunksJson = """
            [
              {"old_string": "import alpha", "new_string": "import alpha\nimport beta"},
              {"old_string": "val x = 10", "new_string": "val x = 100"},
              {"old_string": "return x + y", "new_string": "return (x + y) * 2"}
            ]
        """.trimIndent()

        val toolCall = ToolCallRequest(
            id = "call_multi",
            name = "multi_replace_file_content",
            rawArguments = """{"path": "src/Calc.kt", "replacement_chunks": $chunksJson}""",
            parsedArguments = mapOf(
                "path" to "src/Calc.kt",
                "replacement_chunks" to chunksJson
            )
        )

        val result = executor.execute(dummyUri, toolCall, dryRunWrite = false)
        assertTrue(result.isSuccess)
        assertTrue(result.output.contains("Successfully applied 3 replacement chunks"))

        val doc = fakeRepo.findDocumentByPath(dummyUri, "src/Calc.kt")!!
        val updatedText = fakeRepo.readFile(doc.uri)
        assertTrue(updatedText.contains("import alpha\nimport beta"))
        assertTrue(updatedText.contains("val x = 100"))
        assertTrue(updatedText.contains("return (x + y) * 2"))
    }
}
