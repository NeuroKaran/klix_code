package com.klix.code

import android.content.Context
import com.klix.code.data.agent.command.BuiltInSlashCommands
import com.klix.code.data.agent.command.SlashCommand
import com.klix.code.data.agent.command.SlashCommandActionType
import com.klix.code.data.agent.command.SlashCommandProcessor
import com.klix.code.data.agent.command.SlashCommandRepository
import com.klix.code.data.agent.command.SlashCommandRepositoryImpl
import com.klix.code.data.agent.provider.ChatMessage
import com.klix.code.data.agent.provider.MessageRole
import com.klix.code.data.agent.session.ChatSession
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.io.File

class SlashCommandTest {

    @Test
    fun testSlashCommandJsonSerializationAndDeserialization() {
        val cmd = SlashCommand(
            id = "cmd_123",
            trigger = "test-cmd",
            description = "Custom test command",
            template = "Analyze {file} with input: {input}",
            actionType = SlashCommandActionType.PROMPT,
            isBuiltIn = false,
            iconName = "code"
        )

        val json = cmd.toJson()
        val restored = SlashCommand.fromJson(json)

        assertEquals("cmd_123", restored.id)
        assertEquals("test-cmd", restored.trigger)
        assertEquals("/test-cmd", restored.displayTrigger)
        assertEquals("Custom test command", restored.description)
        assertEquals("Analyze {file} with input: {input}", restored.template)
        assertEquals(SlashCommandActionType.PROMPT, restored.actionType)
        assertFalse(restored.isBuiltIn)
        assertEquals("code", restored.iconName)
    }

    @Test
    fun testSlashCommandProcessorVariableReplacement() {
        val template = "Review {file}:\n```\n{code}\n```\nFocus: {input}"
        val resolved = SlashCommandProcessor.resolveTemplate(
            template = template,
            input = "Check for memory leaks",
            fileName = "MainActivity.kt",
            fileContent = "class MainActivity : ComponentActivity()"
        )

        assertTrue(resolved.contains("Review MainActivity.kt:"))
        assertTrue(resolved.contains("class MainActivity : ComponentActivity()"))
        assertTrue(resolved.contains("Focus: Check for memory leaks"))
    }

    @Test
    fun testSlashCommandProcessorEmptyInputFallback() {
        val template = "Explain {file}:\n{code}\nTask: {input}"
        val resolved = SlashCommandProcessor.resolveTemplate(
            template = template,
            input = "",
            fileName = "Utils.kt",
            fileContent = "fun add(a: Int, b: Int) = a + b"
        )

        assertTrue(resolved.contains("Explain Utils.kt:"))
        assertTrue(resolved.contains("Task: Perform a thorough analysis."))
    }

    @Test
    fun testSlashCommandProcessorAppendInputWhenNotPresentInTemplate() {
        val template = "Summarize the architecture of {file}:\n{code}"
        val resolved = SlashCommandProcessor.resolveTemplate(
            template = template,
            input = "Keep it under 3 bullets",
            fileName = "Repo.kt",
            fileContent = "class Repo"
        )

        assertTrue(resolved.contains("Summarize the architecture of Repo.kt:"))
        assertTrue(resolved.contains("Additional instructions: Keep it under 3 bullets"))
    }

    @Test
    fun testSlashCommandProcessorNullFileAndCodeFallbacks() {
        val template = "File: {file}, Code: {code}, Input: {input}"
        val resolved = SlashCommandProcessor.resolveTemplate(
            template = template,
            input = "test run",
            fileName = null,
            fileContent = null
        )

        assertTrue(resolved.contains("File: active file"))
        assertTrue(resolved.contains("Code: (no file currently open in editor)"))
        assertTrue(resolved.contains("Input: test run"))
    }

    @Test
    fun testBuiltInSlashCommandsCoverage() {
        val triggers = BuiltInSlashCommands.ALL.map { it.trigger }

        assertTrue(triggers.contains("explain"))
        assertTrue(triggers.contains("fix"))
        assertTrue(triggers.contains("refactor"))
        assertTrue(triggers.contains("test"))
        assertTrue(triggers.contains("doc"))
        assertTrue(triggers.contains("review"))
        assertTrue(triggers.contains("boost"))
        assertTrue(triggers.contains("commit"))
        assertTrue(triggers.contains("clear"))
        assertTrue(triggers.contains("new"))
        assertTrue(triggers.contains("commands"))
        assertTrue(triggers.contains("settings"))
        assertTrue(triggers.contains("help"))

        BuiltInSlashCommands.ALL.forEach { cmd ->
            assertTrue(cmd.isBuiltIn)
            assertFalse(cmd.trigger.startsWith("/"))
            assertTrue(cmd.description.isNotBlank())
        }
    }

    @Test
    fun testChatMessageCommandSerialization() {
        val msg = ChatMessage(
            id = "msg_cmd_1",
            role = MessageRole.USER,
            content = "Explain this code",
            command = "/explain"
        )

        val session = ChatSession(
            id = "sess_1",
            messages = listOf(msg)
        )

        val json = session.toJson()
        val restored = ChatSession.fromJson(json)

        assertEquals(1, restored.messages.size)
        val restoredMsg = restored.messages[0]
        assertEquals("/explain", restoredMsg.command)
        assertEquals("Explain this code", restoredMsg.content)
    }

    @Test
    fun testSlashCommandRepositoryMock() {
        runBlocking {
            val mockRepo = object : SlashCommandRepository {
                private val list = mutableListOf<SlashCommand>()

                override suspend fun getCustomCommands(): List<SlashCommand> = list

                override suspend fun saveCustomCommand(command: SlashCommand) {
                    val index = list.indexOfFirst { it.id == command.id }
                    if (index >= 0) list[index] = command else list.add(command)
                }

                override suspend fun deleteCustomCommand(commandId: String) {
                    list.removeAll { it.id == commandId }
                }

                override suspend fun getAllCommands(): List<SlashCommand> {
                    return BuiltInSlashCommands.ALL + list
                }
            }

            val customCmd = SlashCommand(
                id = "custom_1",
                trigger = "audit",
                description = "Security audit",
                template = "Audit {file} for OWASP issues: {input}",
                actionType = SlashCommandActionType.PROMPT,
                isBuiltIn = false
            )

            mockRepo.saveCustomCommand(customCmd)

            val all = mockRepo.getAllCommands()
            assertTrue(all.any { it.trigger == "audit" })
            assertEquals(BuiltInSlashCommands.ALL.size + 1, all.size)

            mockRepo.deleteCustomCommand("custom_1")
            assertEquals(BuiltInSlashCommands.ALL.size, mockRepo.getAllCommands().size)
        }
    }

    @Test
    fun testSlashCommandRepositoryImplPersistence() {
        runBlocking {
            val tempDir = File(System.getProperty("java.io.tmpdir"), "klix_slash_test_" + System.currentTimeMillis())
            tempDir.mkdirs()

            val mockContext = mock(Context::class.java)
            `when`(mockContext.filesDir).thenReturn(tempDir)

            val repo = SlashCommandRepositoryImpl(mockContext)

            // Initial custom commands loaded
            val initialCustom = repo.getCustomCommands()
            assertTrue(initialCustom.any { it.trigger == "summarize" })
            assertTrue(initialCustom.any { it.trigger == "optimize" })

            // Save a new custom command
            val newCmd = SlashCommand(
                id = "custom_new",
                trigger = "/translate-docs", // Leading slash should be sanitized
                description = "Translate documentation",
                template = "Translate {file} comments to French: {input}",
                actionType = SlashCommandActionType.PROMPT
            )
            repo.saveCustomCommand(newCmd)

            // Create new instance of repository pointing to same directory to verify persistence
            val repo2 = SlashCommandRepositoryImpl(mockContext)
            val loaded = repo2.getCustomCommands()
            val found = loaded.find { it.id == "custom_new" }

            assertTrue(found != null)
            assertEquals("translate-docs", found!!.trigger)
            assertEquals("/translate-docs", found.displayTrigger)
            assertEquals("Translate documentation", found.description)

            // Delete command
            repo2.deleteCustomCommand("custom_new")
            val afterDelete = repo2.getCustomCommands()
            assertFalse(afterDelete.any { it.id == "custom_new" })

            tempDir.deleteRecursively()
        }
    }
}
