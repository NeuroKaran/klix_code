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
        var lastCommand: String? = null
        var lastWorkingDir: String? = null
        override fun getTierName(): String = "Fake Execution"
        override fun isAvailable(): Boolean = true
        override suspend fun runCommand(command: String, workingDir: String?): CommandResult {
            lastCommand = command
            lastWorkingDir = workingDir
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

    @Test
    fun testHtmlToMarkdownConversion() {
        val fakeRepo = FakeWorkspaceRepository()
        val executor = WorkspaceToolExecutor(fakeRepo, FakeExecutionProvider())

        val sampleHtml = """
            <html>
            <head><script>alert('evil');</script><style>.cls{color:red}</style></head>
            <body>
                <h1>Android Documentation</h1>
                <p>Welcome to <code>KlixCode</code> mobile IDE.</p>
                <pre><code>val greeting = "Hello"</code></pre>
                <ul>
                    <li>Feature A &amp; B</li>
                    <li>Feature &lt;C&gt;</li>
                </ul>
            </body>
            </html>
        """.trimIndent()

        val markdown = executor.convertHtmlToMarkdown(sampleHtml)
        assertTrue(markdown.contains("# Android Documentation"))
        assertTrue(markdown.contains("`KlixCode`"))
        assertTrue(markdown.contains("```\nval greeting = \"Hello\"\n```"))
        assertTrue(markdown.contains("* Feature A & B"))
        assertTrue(markdown.contains("* Feature <C>"))
        org.junit.Assert.assertFalse(markdown.contains("<script>"))
        org.junit.Assert.assertFalse(markdown.contains("alert('evil')"))
        org.junit.Assert.assertFalse(markdown.contains(".cls{color:red}"))
    }

    @Test
    fun testDuckDuckGoHtmlParsing() {
        val fakeRepo = FakeWorkspaceRepository()
        val executor = WorkspaceToolExecutor(fakeRepo, FakeExecutionProvider())

        val sampleDdgHtml = """
            <div class="result results_links">
                <a class="result__a" href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fkotlinlang.org%2Fdocs%2Fhome.html&amp;rut=123">Kotlin Docs &amp; Guides</a>
                <a class="result__snippet" href="#">Comprehensive documentation for Kotlin programming language.</a>
            </div>
            <div class="result results_links">
                <a class="result__a" href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fdeveloper.android.com%2Fguide&amp;rut=456">Android Guides</a>
                <a class="result__snippet" href="#">Official Android developer guide and architecture tutorials.</a>
            </div>
        """.trimIndent()

        val results = executor.parseDuckDuckGoHtml(sampleDdgHtml, maxResults = 2)
        assertEquals(2, results.size)
        assertEquals("Kotlin Docs & Guides", results[0].title)
        assertEquals("https://kotlinlang.org/docs/home.html", results[0].url)
        assertTrue(results[0].snippet.contains("Comprehensive documentation"))

        assertEquals("Android Guides", results[1].title)
        assertEquals("https://developer.android.com/guide", results[1].url)
    }

    @Test
    fun testVerifyWorkspaceCatchesJsonSyntaxError() = runBlocking {
        val fakeRepo = FakeWorkspaceRepository()
        fakeRepo.addFile("broken.json", "{\"name\": \"Klix\", unclosed")
        val executor = WorkspaceToolExecutor(fakeRepo, FakeExecutionProvider())
        val dummyUri = mock(Uri::class.java)

        val errors = executor.verifyWorkspace(dummyUri, setOf("broken.json"))
        org.junit.Assert.assertNotNull(errors)
        assertTrue(errors!!.contains("JSON Syntax Error in 'broken.json'"))
    }

    @Test
    fun testVerifyWorkspaceCatchesCompilerErrorViaSandbox() = runBlocking {
        val fakeRepo = FakeWorkspaceRepository()
        fakeRepo.addFile("script.py", "def broken(:")
        val fakeExec = FakeExecutionProvider(
            outputToReturn = "SyntaxError: invalid syntax (script.py:1)",
            exitCodeToReturn = 1
        )
        val executor = WorkspaceToolExecutor(fakeRepo, fakeExec)
        val dummyUri = mock(Uri::class.java)

        val errors = executor.verifyWorkspace(dummyUri, setOf("script.py"))
        org.junit.Assert.assertNotNull(errors)
        assertTrue(errors!!.contains("Verification error in 'script.py'"))
        assertTrue(errors.contains("SyntaxError: invalid syntax"))
    }

    @Test
    fun testInvokeSubagentToolExecution() = runBlocking {
        val fakeRepo = FakeWorkspaceRepository()
        val executor = WorkspaceToolExecutor(fakeRepo, FakeExecutionProvider())

        executor.subagentRunner = { task, maxTurns ->
            "Subagent Report for '$task': Found 3 matching auth routes in src/routes/auth.kt (lines 12-45)."
        }

        val toolCall = ToolCallRequest(
            id = "call_subagent_1",
            name = "invoke_subagent",
            rawArguments = """{"task": "Find auth validation endpoints", "max_turns": 5}""",
            parsedArguments = mapOf(
                "task" to "Find auth validation endpoints",
                "max_turns" to "5"
            )
        )

        val result = executor.execute(null, toolCall)
        assertTrue(result.isSuccess)
        assertTrue(result.output.contains("Subagent Report for 'Find auth validation endpoints'"))
        assertTrue(result.output.contains("Found 3 matching auth routes"))
    }

    @Test
    fun testAskQuestionToolExecution() = runBlocking {
        val fakeRepo = FakeWorkspaceRepository()
        val executor = WorkspaceToolExecutor(fakeRepo, FakeExecutionProvider())

        val toolCall = ToolCallRequest(
            id = "call_ask_1",
            name = "ask_question",
            rawArguments = """{"question": "Which architecture pattern should be used?", "options": ["MVI", "MVVM"], "allow_multiple": false}""",
            parsedArguments = mapOf(
                "question" to "Which architecture pattern should be used?",
                "options" to "[\"MVI\", \"MVVM\"]",
                "allow_multiple" to "false"
            )
        )

        val result = executor.execute(null, toolCall)
        assertTrue(result.isSuccess)
        assertTrue(result.output.contains("Question: Which architecture pattern should be used?"))
        assertTrue(result.output.contains("1. MVI"))
        assertTrue(result.output.contains("2. MVVM"))
        assertTrue(result.output.contains("Allow Multiple: false"))
    }

    @Test
    fun testAskQuestionToolMissingQuestion() = runBlocking {
        val fakeRepo = FakeWorkspaceRepository()
        val executor = WorkspaceToolExecutor(fakeRepo, FakeExecutionProvider())

        val toolCall = ToolCallRequest(
            id = "call_ask_err",
            name = "ask_question",
            rawArguments = """{}""",
            parsedArguments = emptyMap()
        )

        val result = executor.execute(null, toolCall)
        org.junit.Assert.assertFalse(result.isSuccess)
        assertTrue(result.error!!.contains("Missing required parameter 'question'"))
    }

    @Test
    fun testRunCommandScopesToActiveWorkspaceRoot() = runBlocking {
        val fakeRepo = FakeWorkspaceRepository()
        val fakeExec = FakeExecutionProvider()
        val executor = WorkspaceToolExecutor(fakeRepo, fakeExec)

        val wsUri = mock(Uri::class.java)
        org.mockito.Mockito.`when`(wsUri.path).thenReturn("/data/user/0/com.klix.code/files/sandbox/rootfs/workspaces/project-charlie")

        val toolCall = ToolCallRequest(
            id = "cmd_1",
            name = "run_command",
            rawArguments = """{"command": "ls -la"}""",
            parsedArguments = mapOf("command" to "ls -la")
        )

        val result = executor.execute(wsUri, toolCall)
        assertTrue(result.isSuccess)
        assertEquals("ls -la", fakeExec.lastCommand)
        assertEquals("/workspaces/project-charlie", fakeExec.lastWorkingDir)
    }

    @Test
    fun testRunCommandResolvesRelativeWorkingDir() = runBlocking {
        val fakeRepo = FakeWorkspaceRepository()
        val fakeExec = FakeExecutionProvider()
        val executor = WorkspaceToolExecutor(fakeRepo, fakeExec)

        val wsUri = mock(Uri::class.java)
        org.mockito.Mockito.`when`(wsUri.path).thenReturn("/workspaces/project-delta")

        val toolCall = ToolCallRequest(
            id = "cmd_2",
            name = "run_command",
            rawArguments = """{"command": "pytest", "working_dir": "./tests/unit"}""",
            parsedArguments = mapOf("command" to "pytest", "working_dir" to "./tests/unit")
        )

        val result = executor.execute(wsUri, toolCall)
        assertTrue(result.isSuccess)
        assertEquals("/workspaces/project-delta/tests/unit", fakeExec.lastWorkingDir)
    }

    @Test
    fun testRunCommandPreventsWorkingDirTraversalAboveWorkspaceRoot() = runBlocking {
        val fakeRepo = FakeWorkspaceRepository()
        val fakeExec = FakeExecutionProvider()
        val executor = WorkspaceToolExecutor(fakeRepo, fakeExec)

        val wsUri = mock(Uri::class.java)
        org.mockito.Mockito.`when`(wsUri.path).thenReturn("/workspaces/project-delta")

        val toolCall = ToolCallRequest(
            id = "cmd_3",
            name = "run_command",
            rawArguments = """{"command": "cat flag", "working_dir": "../../../../etc"}""",
            parsedArguments = mapOf("command" to "cat flag", "working_dir" to "../../../../etc")
        )

        val result = executor.execute(wsUri, toolCall)
        assertTrue(result.isSuccess)
        // Traversal above /workspaces/project-delta is clamped to workspace root
        assertEquals("/workspaces/project-delta/etc", fakeExec.lastWorkingDir)
    }
}
