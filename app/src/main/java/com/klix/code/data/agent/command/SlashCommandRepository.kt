package com.klix.code.data.agent.command

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

interface SlashCommandRepository {
    suspend fun getCustomCommands(): List<SlashCommand>
    suspend fun saveCustomCommand(command: SlashCommand)
    suspend fun deleteCustomCommand(commandId: String)
    suspend fun getAllCommands(): List<SlashCommand>
}

class SlashCommandRepositoryImpl(
    private val context: Context
) : SlashCommandRepository {

    private val customCommandsFile: File
        get() = File(context.filesDir, "custom_slash_commands.json")

    override suspend fun getCustomCommands(): List<SlashCommand> = withContext(Dispatchers.IO) {
        val file = customCommandsFile
        if (!file.exists()) {
            val defaults = getInitialCustomCommands()
            saveCustomCommandsInternal(defaults)
            return@withContext defaults
        }

        try {
            val jsonText = file.readText(Charsets.UTF_8)
            val jsonArray = JSONArray(jsonText)
            val list = mutableListOf<SlashCommand>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                list.add(SlashCommand.fromJson(obj))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun saveCustomCommand(command: SlashCommand) = withContext(Dispatchers.IO) {
        val current = getCustomCommands().toMutableList()
        val index = current.indexOfFirst {
            it.id == command.id || it.trigger.equals(command.trigger, ignoreCase = true)
        }
        val sanitizedTrigger = command.trigger.removePrefix("/").trim()
        val updated = command.copy(
            trigger = sanitizedTrigger,
            isBuiltIn = false
        )
        if (index >= 0) {
            current[index] = updated
        } else {
            current.add(updated)
        }
        saveCustomCommandsInternal(current)
    }

    override suspend fun deleteCustomCommand(commandId: String) = withContext(Dispatchers.IO) {
        val current = getCustomCommands().filter { it.id != commandId }
        saveCustomCommandsInternal(current)
    }

    override suspend fun getAllCommands(): List<SlashCommand> {
        val custom = getCustomCommands()
        return BuiltInSlashCommands.ALL + custom
    }

    private fun saveCustomCommandsInternal(commands: List<SlashCommand>) {
        try {
            val jsonArray = JSONArray()
            for (cmd in commands) {
                jsonArray.put(cmd.toJson())
            }
            val tempFile = File(context.filesDir, "custom_slash_commands.json.tmp")
            tempFile.writeText(jsonArray.toString(), Charsets.UTF_8)
            if (tempFile.exists()) {
                tempFile.renameTo(customCommandsFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getInitialCustomCommands(): List<SlashCommand> {
        return listOf(
            SlashCommand(
                id = "custom_summarize",
                trigger = "summarize",
                description = "Summarize file or concept in 3 key bullet points",
                template = "Provide a concise 3-bullet-point summary of {file}:\n\n```\n{code}\n```\n\nSpecific focus: {input}",
                actionType = SlashCommandActionType.PROMPT,
                isBuiltIn = false,
                iconName = "sparkles"
            ),
            SlashCommand(
                id = "custom_optimize",
                trigger = "optimize",
                description = "Analyze Big-O complexity and propose optimizations",
                template = "Analyze the algorithmic runtime and memory complexity of the code in {file} and provide high-performance alternatives:\n\n```\n{code}\n```\n\nKey areas: {input}",
                actionType = SlashCommandActionType.PROMPT,
                isBuiltIn = false,
                iconName = "code"
            )
        )
    }
}

