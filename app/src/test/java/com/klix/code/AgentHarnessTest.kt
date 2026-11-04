package com.klix.code

import android.net.Uri
import com.klix.code.data.agent.execution.ExecutionProvider
import com.klix.code.data.agent.execution.CommandResult
import com.klix.code.data.agent.harness.AgentHarness
import com.klix.code.data.agent.provider.ChatMessage
import com.klix.code.data.agent.provider.MessageRole
import com.klix.code.data.agent.tools.ToolCallRequest
import com.klix.code.data.agent.tools.ToolCallResult
import com.klix.code.data.agent.tools.WorkspaceToolExecutor
import com.klix.code.data.model.FileNode
import com.klix.code.data.model.ProjectItem
import com.klix.code.data.repository.SearchMatch
import com.klix.code.data.repository.WorkspaceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentHarnessTest {

    private class FakeRepo : WorkspaceRepository {
        override suspend fun persistProjectPermission(uri: Uri, name: String) {}
        override suspend fun getRecentProjects(): List<ProjectItem> = emptyList()
        override suspend fun removeRecentProject(uri: Uri) {}
        override suspend fun loadTree(treeUri: Uri): FileNode? = null
        override suspend fun readFile(fileUri: Uri): String = ""
        override suspend fun writeFile(fileUri: Uri, content: String) {}
        override suspend fun createFile(parentDirUri: Uri, name: String): Uri? = null
        override suspend fun createDirectory(parentDirUri: Uri, name: String): Uri? = null
        override suspend fun delete(targetUri: Uri): Boolean = true
        override suspend fun rename(targetUri: Uri, newName: String): Boolean = true
        override suspend fun findDocumentByPath(treeUri: Uri, relativePath: String) = null
        override suspend fun findOrCreateDocumentByPath(treeUri: Uri, relativePath: String) = null
        override suspend fun searchFiles(treeUri: Uri, query: String, relativeScope: String?, maxResults: Int): List<SearchMatch> = emptyList()
        override suspend fun listDirectory(treeUri: Uri, relativePath: String): List<FileNode> = emptyList()
    }

    private class FakeExec : ExecutionProvider {
        override fun getTierName(): String = "Fake"
        override fun isAvailable(): Boolean = true
        override suspend fun runCommand(command: String, workingDir: String?) = CommandResult(0, "", "", 0)
    }

    @Test
    fun testCompactHistoryPreservesRootAndLatestMessages() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val toolExecutor = WorkspaceToolExecutor(FakeRepo(), FakeExec())
        val harness = AgentHarness(toolExecutor, scope)

        // Generate 14 messages
        val messages = mutableListOf<ChatMessage>()
        val rootInstruction = ChatMessage(
            role = MessageRole.USER,
            content = "Root task: Refactor the workspace modules and fix syntax."
        )
        messages.add(rootInstruction)

        for (i in 1..9) {
            if (i % 2 == 1) {
                messages.add(
                    ChatMessage(
                        role = MessageRole.ASSISTANT,
                        content = "Inspecting module step $i",
                        toolCalls = listOf(
                            ToolCallRequest("tc_$i", "read_file", "{\"path\": \"src/Module$i.kt\"}", mapOf("path" to "src/Module$i.kt"))
                        )
                    )
                )
            } else {
                messages.add(
                    ChatMessage(
                        role = MessageRole.USER,
                        content = "",
                        toolResults = listOf(
                            ToolCallResult("tc_${i - 1}", "read_file", true, "class Module${i - 1} content")
                        )
                    )
                )
            }
        }

        // Add 4 recent messages
        val recent1 = ChatMessage(role = MessageRole.ASSISTANT, content = "Recent 1")
        val recent2 = ChatMessage(role = MessageRole.USER, content = "Recent 2")
        val recent3 = ChatMessage(role = MessageRole.ASSISTANT, content = "Recent 3")
        val recent4 = ChatMessage(role = MessageRole.USER, content = "Recent 4")
        messages.addAll(listOf(recent1, recent2, recent3, recent4))

        assertEquals(14, messages.size)

        harness.restoreSession(messages, 100, 200)
        harness.compactHistory()

        val compacted = harness.messages.value
        // Root (1) + Summary (1) + Recent (4) = 6
        assertEquals(6, compacted.size)

        // Root must match original user instruction exactly
        assertEquals(rootInstruction.id, compacted[0].id)
        assertEquals(rootInstruction.content, compacted[0].content)

        // Summary message must contain compaction info and ASSISTANT role for strict alternation
        val summaryMsg = compacted[1]
        assertEquals(MessageRole.ASSISTANT, summaryMsg.role)
        assertTrue(summaryMsg.content.contains("[Context Compaction Summary of Prior Steps]"))
        assertTrue(summaryMsg.content.contains("read_file(src/Module"))

        // Latest 4 messages must match exactly
        assertEquals(recent1.id, compacted[2].id)
        assertEquals(recent2.id, compacted[3].id)
        assertEquals(recent3.id, compacted[4].id)
        assertEquals(recent4.id, compacted[5].id)
    }

    @Test
    fun testCompactHistoryNoOpWhenUnderThreshold() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val toolExecutor = WorkspaceToolExecutor(FakeRepo(), FakeExec())
        val harness = AgentHarness(toolExecutor, scope)

        val smallList = listOf(
            ChatMessage(role = MessageRole.USER, content = "Hello"),
            ChatMessage(role = MessageRole.ASSISTANT, content = "Hi there")
        )
        harness.restoreSession(smallList, 10, 10)
        harness.compactHistory()

        assertEquals(2, harness.messages.value.size)
    }

    @Test
    fun testInteractiveAskQuestionApprovalWithCustomSelection() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val toolExecutor = WorkspaceToolExecutor(FakeRepo(), FakeExec())
        val harness = AgentHarness(toolExecutor, scope)

        val questionCall = ToolCallRequest(
            id = "q_1",
            name = "ask_question",
            rawArguments = """{"question": "Pick database", "options": ["Room", "SQLDelight"]}""",
            parsedArguments = mapOf("question" to "Pick database", "options" to "[\"Room\", \"SQLDelight\"]")
        )

        // Directly trigger pending question
        harness.restoreSession(emptyList(), 0, 0)
        harness.approvePendingTool(listOf("SQLDelight")) // When nothing pending, no-op
        assertEquals(0, harness.messages.value.size)

        // Answer question directly
        val pendingQ = com.klix.code.data.agent.harness.PendingQuestion(
            toolCall = questionCall,
            question = "Pick database",
            options = listOf("Room", "SQLDelight")
        )
        val pendingField = AgentHarness::class.java.getDeclaredField("_pendingQuestion")
        pendingField.isAccessible = true
        (pendingField.get(harness) as kotlinx.coroutines.flow.MutableStateFlow<com.klix.code.data.agent.harness.PendingQuestion?>).value = pendingQ

        harness.answerQuestion(listOf("SQLDelight"))

        val messages = harness.messages.value
        assertEquals(1, messages.size)
        val resultMsg = messages[0]
        assertEquals(MessageRole.USER, resultMsg.role)
        assertEquals(1, resultMsg.toolResults.size)
        val result = resultMsg.toolResults[0]
        assertTrue(result.isSuccess)
        assertEquals("User selected: SQLDelight", result.output)
    }
}
