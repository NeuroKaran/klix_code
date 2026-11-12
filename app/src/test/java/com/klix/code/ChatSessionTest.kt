package com.klix.code

import android.content.Context
import android.net.Uri
import com.klix.code.data.agent.provider.ChatMessage
import com.klix.code.data.agent.provider.MessageRole
import com.klix.code.data.agent.session.ChatSession
import com.klix.code.data.agent.session.ChatSessionRepositoryImpl
import com.klix.code.data.agent.session.TaggedFileItem
import com.klix.code.data.agent.session.flattenFileTree
import com.klix.code.data.agent.tools.ToolCallRequest
import com.klix.code.data.agent.tools.ToolCallResult
import com.klix.code.data.model.FileNode
import com.klix.code.ui.components.agent.MarkdownBlock
import com.klix.code.ui.components.agent.parseMarkdown
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File

class ChatSessionTest {

    @Test
    fun testChatSessionSerializationAndDeserialization() {
        val toolCall = ToolCallRequest(
            id = "tc_1",
            name = "read_file",
            rawArguments = "{\"path\": \"app/build.gradle.kts\"}",
            parsedArguments = mapOf("path" to "app/build.gradle.kts"),
            thoughtSignature = "thought_sig"
        )
        val toolResult = ToolCallResult(
            toolCallId = "tc_1",
            name = "read_file",
            isSuccess = true,
            output = "plugins { alias(libs.plugins.android.application) }",
            error = null
        )
        val userMsg = ChatMessage(
            id = "m1",
            role = MessageRole.USER,
            content = "Inspect @app/build.gradle.kts"
        )
        val assistantMsg = ChatMessage(
            id = "m2",
            role = MessageRole.ASSISTANT,
            content = "I read the file.",
            thought = "Let me inspect the dependencies",
            toolCalls = listOf(toolCall),
            toolResults = listOf(toolResult)
        )

        val session = ChatSession(
            id = "session_123",
            title = "Inspect Gradle",
            createdAt = 1000L,
            updatedAt = 2000L,
            messages = listOf(userMsg, assistantMsg),
            promptTokens = 150,
            completionTokens = 85
        )

        val json = session.toJson()
        val restored = ChatSession.fromJson(json)

        assertEquals("session_123", restored.id)
        assertEquals("Inspect Gradle", restored.title)
        assertEquals(1000L, restored.createdAt)
        assertEquals(2000L, restored.updatedAt)
        assertEquals(150, restored.promptTokens)
        assertEquals(85, restored.completionTokens)
        assertEquals(235, restored.totalTokens)
        assertEquals(2, restored.messages.size)

        val rUser = restored.messages[0]
        assertEquals("m1", rUser.id)
        assertEquals(MessageRole.USER, rUser.role)
        assertEquals("Inspect @app/build.gradle.kts", rUser.content)

        val rAssistant = restored.messages[1]
        assertEquals("m2", rAssistant.id)
        assertEquals(MessageRole.ASSISTANT, rAssistant.role)
        assertEquals("I read the file.", rAssistant.content)
        assertEquals("Let me inspect the dependencies", rAssistant.thought)
        assertEquals(1, rAssistant.toolCalls.size)
        assertEquals("tc_1", rAssistant.toolCalls[0].id)
        assertEquals("read_file", rAssistant.toolCalls[0].name)
        assertEquals("app/build.gradle.kts", rAssistant.toolCalls[0].parsedArguments["path"])
        assertEquals(1, rAssistant.toolResults.size)
        assertEquals(true, rAssistant.toolResults[0].isSuccess)
    }

    @Test
    fun testFlattenFileTree() {
        val dummyUri = mock(Uri::class.java)

        val file1 = FileNode(uri = dummyUri, name = "MainActivity.kt", isDirectory = false)
        val file2 = FileNode(uri = dummyUri, name = "AgentChatPanel.kt", isDirectory = false)
        val subDir = FileNode(
            uri = dummyUri,
            name = "components",
            isDirectory = true,
            children = listOf(file2)
        )
        val root = FileNode(
            uri = dummyUri,
            name = "src",
            isDirectory = true,
            children = listOf(file1, subDir)
        )

        val flat = flattenFileTree(root)
        assertEquals(3, flat.size)

        assertEquals("MainActivity.kt", flat[0].path)
        assertEquals(false, flat[0].isDirectory)

        assertEquals("components", flat[1].path)
        assertEquals(true, flat[1].isDirectory)

        assertEquals("components/AgentChatPanel.kt", flat[2].path)
        assertEquals(false, flat[2].isDirectory)
        assertEquals("AgentChatPanel.kt", flat[2].fileName)
        assertEquals("components", flat[2].parentPath)
    }

    @Test
    fun testMarkdownParsing() {
        val markdown = """
            # Architecture Overview
            
            This is an introduction paragraph.
            
            - Feature A
            - Feature B
            
            ```kotlin
            fun hello(): String = "world"
            ```
            
            1. First step
            2. Second step
        """.trimIndent()

        val blocks = parseMarkdown(markdown)
        assertTrue(blocks.isNotEmpty())

        val header = blocks[0] as MarkdownBlock.HeaderBlock
        assertEquals(1, header.level)
        assertEquals("Architecture Overview", header.text)

        val para = blocks[1] as MarkdownBlock.ParagraphBlock
        assertEquals("This is an introduction paragraph.", para.text)

        val item1 = blocks[2] as MarkdownBlock.ListItemBlock
        assertEquals("Feature A", item1.text)

        val item2 = blocks[3] as MarkdownBlock.ListItemBlock
        assertEquals("Feature B", item2.text)

        val code = blocks[4] as MarkdownBlock.CodeBlock
        assertEquals("kotlin", code.language)
        assertEquals("fun hello(): String = \"world\"", code.code)

        val num1 = blocks[5] as MarkdownBlock.ListItemBlock
        assertEquals("1.", num1.prefix)
        assertEquals("First step", num1.text)

        val num2 = blocks[6] as MarkdownBlock.ListItemBlock
        assertEquals("2.", num2.prefix)
        assertEquals("Second step", num2.text)
    }

    @Test
    fun testTagDetectionInPrompt() {
        val prompt = "Please inspect @app/src/main/java/MainActivity.kt and also @app/build.gradle.kts"
        val regex = Regex("""@([a-zA-Z0-9_.\-/]+)""")
        val tags = regex.findAll(prompt).map { it.groupValues[1] }.toList()

        assertEquals(2, tags.size)
        assertEquals("app/src/main/java/MainActivity.kt", tags[0])
        assertEquals("app/build.gradle.kts", tags[1])
    }

    @Test
    fun testWorkspacePartitionedChatRepository() = runBlocking {
        val mockContext = mock(Context::class.java)
        val tempDir = File(System.getProperty("java.io.tmpdir"), "klix_test_chat_repo_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        `when`(mockContext.filesDir).thenReturn(tempDir)

        val ws1Dir = File(tempDir, "workspaces/project-alpha").apply { mkdirs() }
        val ws2Dir = File(tempDir, "workspaces/project-beta").apply { mkdirs() }

        val repo = ChatSessionRepositoryImpl(mockContext) { wsId ->
            when (wsId) {
                "ws_alpha" -> ws1Dir
                "ws_beta" -> ws2Dir
                else -> null
            }
        }

        // Initially both should be empty
        assertTrue(repo.getSessions("ws_alpha").isEmpty())
        assertTrue(repo.getSessions("ws_beta").isEmpty())

        // Save session in alpha
        val sessionAlpha = ChatSession(id = "s_alpha_1", title = "Alpha Architecture")
        repo.saveSession("ws_alpha", sessionAlpha)
        repo.setActiveSessionId("ws_alpha", "s_alpha_1")

        // Save session in beta
        val sessionBeta = ChatSession(id = "s_beta_1", title = "Beta Tests")
        repo.saveSession("ws_beta", sessionBeta)
        repo.setActiveSessionId("ws_beta", "s_beta_1")

        // Verify alpha only sees its session
        val alphaSessions = repo.getSessions("ws_alpha")
        assertEquals(1, alphaSessions.size)
        assertEquals("s_alpha_1", alphaSessions[0].id)
        assertEquals("s_alpha_1", repo.getActiveSessionId("ws_alpha"))

        // Verify beta only sees its session
        val betaSessions = repo.getSessions("ws_beta")
        assertEquals(1, betaSessions.size)
        assertEquals("s_beta_1", betaSessions[0].id)
        assertEquals("s_beta_1", repo.getActiveSessionId("ws_beta"))

        // Verify files exist inside respective workspace .klix/ folders
        assertTrue(File(ws1Dir, ".klix/chat_sessions.json").exists())
        assertTrue(File(ws2Dir, ".klix/chat_sessions.json").exists())

        // Delete session in alpha
        repo.deleteSession("ws_alpha", "s_alpha_1")
        assertTrue(repo.getSessions("ws_alpha").isEmpty())
        // Beta should still have its session intact
        assertEquals(1, repo.getSessions("ws_beta").size)
    }

    @Test
    fun testChatRepositoryResolvesDirectoryViaRegistryWhenProviderNull() = runBlocking {
        val mockContext = mock(Context::class.java)
        val tempDir = File(System.getProperty("java.io.tmpdir"), "klix_test_chat_fallback_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        `when`(mockContext.filesDir).thenReturn(tempDir)

        val wsDir = File(tempDir, "sandbox/rootfs/workspaces/custom-slug").apply { mkdirs() }
        val registryFile = File(tempDir, "sandbox_workspaces_registry.json")
        registryFile.writeText("""
            [
                {
                    "id": "ws_12345",
                    "name": "Custom Workspace",
                    "directoryName": "custom-slug",
                    "relativePath": "/workspaces/custom-slug",
                    "rootUri": "file://${wsDir.absolutePath.replace('\\', '/')}",
                    "createdAt": 1000,
                    "lastOpened": 1000
                }
            ]
        """.trimIndent())

        val repo = ChatSessionRepositoryImpl(mockContext, null)
        val session = ChatSession(id = "s_fall_1", title = "Fallback Chat")
        repo.saveSession("ws_12345", session)

        val retrieved = repo.getSessions("ws_12345")
        assertEquals(1, retrieved.size)
        assertEquals("s_fall_1", retrieved[0].id)
        assertTrue(File(wsDir, ".klix/chat_sessions.json").exists())
    }
}
