package com.klix.code.data.agent.command

import org.json.JSONObject
import java.util.UUID

enum class SlashCommandActionType {
    PROMPT,          // Expands template into an agent prompt
    CLEAR_CHAT,      // Clears the chat history
    NEW_CHAT,        // Starts a new chat session
    SETTINGS,        // Opens agent settings dialog
    COMMANDS,        // Opens custom slash command manager
    HELP             // Shows help message in chat
}

data class SlashCommand(
    val id: String = UUID.randomUUID().toString(),
    val trigger: String, // Command name without leading slash (e.g. "explain", "fix", "summarize")
    val description: String,
    val template: String = "", // Prompt template used when actionType == PROMPT
    val actionType: SlashCommandActionType = SlashCommandActionType.PROMPT,
    val isBuiltIn: Boolean = false,
    val iconName: String = "code"
) {
    val displayTrigger: String
        get() = "/$trigger"

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("trigger", trigger)
        obj.put("description", description)
        obj.put("template", template)
        obj.put("actionType", actionType.name)
        obj.put("isBuiltIn", isBuiltIn)
        obj.put("iconName", iconName)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): SlashCommand {
            return SlashCommand(
                id = obj.optString("id", UUID.randomUUID().toString()),
                trigger = obj.optString("trigger", "").removePrefix("/").trim(),
                description = obj.optString("description", ""),
                template = obj.optString("template", ""),
                actionType = try {
                    SlashCommandActionType.valueOf(
                        obj.optString("actionType", SlashCommandActionType.PROMPT.name)
                    )
                } catch (_: Exception) {
                    SlashCommandActionType.PROMPT
                },
                isBuiltIn = obj.optBoolean("isBuiltIn", false),
                iconName = obj.optString("iconName", "code")
            )
        }
    }
}

object BuiltInSlashCommands {
    val ALL = listOf(
        SlashCommand(
            id = "builtin_explain",
            trigger = "explain",
            description = "Explain how the current code works step-by-step",
            template = "Explain the code in {file}:\n\n```\n{code}\n```\n\nSpecific questions or focus: {input}",
            actionType = SlashCommandActionType.PROMPT,
            isBuiltIn = true,
            iconName = "sparkles"
        ),
        SlashCommand(
            id = "builtin_fix",
            trigger = "fix",
            description = "Find and fix bugs, errors, or syntax issues in active file",
            template = "Analyze and fix bugs, compilation errors, or runtime issues in {file}:\n\n```\n{code}\n```\n\nIssue description: {input}",
            actionType = SlashCommandActionType.PROMPT,
            isBuiltIn = true,
            iconName = "bug"
        ),
        SlashCommand(
            id = "builtin_refactor",
            trigger = "refactor",
            description = "Refactor code for cleanliness, structure, and best practices",
            template = "Refactor the code in {file} to improve readability, efficiency, and modularity without changing its behavior:\n\n```\n{code}\n```\n\nRefactoring guidance: {input}",
            actionType = SlashCommandActionType.PROMPT,
            isBuiltIn = true,
            iconName = "code"
        ),
        SlashCommand(
            id = "builtin_test",
            trigger = "test",
            description = "Generate comprehensive unit tests for active file",
            template = "Write comprehensive unit tests with edge cases for {file}:\n\n```\n{code}\n```\n\nSpecific test cases to include: {input}",
            actionType = SlashCommandActionType.PROMPT,
            isBuiltIn = true,
            iconName = "test"
        ),
        SlashCommand(
            id = "builtin_doc",
            trigger = "doc",
            description = "Add thorough docstrings, KDoc comments, and explanations",
            template = "Add complete documentation comments (KDoc/Javadoc/docstrings) to all public functions, classes, and complex logic in {file}:\n\n```\n{code}\n```\n\nAdditional notes: {input}",
            actionType = SlashCommandActionType.PROMPT,
            isBuiltIn = true,
            iconName = "doc"
        ),
        SlashCommand(
            id = "builtin_boost",
            trigger = "boost",
            description = "Boost and supercharge code performance, algorithmic efficiency, and memory footprint",
            template = "Analyze the code in {file} to maximize runtime execution performance, eliminate bottlenecks, and boost efficiency:\n\n```\n{code}\n```\n\nPerformance goals: {input}",
            actionType = SlashCommandActionType.PROMPT,
            isBuiltIn = true,
            iconName = "bolt"
        ),
        SlashCommand(
            id = "builtin_review",
            trigger = "review",
            description = "Perform a thorough code review for quality and edge cases",
            template = "Perform an in-depth code review on {file}. Identify potential bugs, security issues, null pointer risks, performance bottlenecks, and architectural improvements:\n\n```\n{code}\n```\n\nFocus areas: {input}",
            actionType = SlashCommandActionType.PROMPT,
            isBuiltIn = true,
            iconName = "sparkles"
        ),
        SlashCommand(
            id = "builtin_commit",
            trigger = "commit",
            description = "Generate a conventional git commit message for recent changes",
            template = "Generate a conventional commit message (e.g. feat:, fix:, refactor:, chore:) with a concise 50-character title and bullet points summarizing the work done:\n{input}",
            actionType = SlashCommandActionType.PROMPT,
            isBuiltIn = true,
            iconName = "code"
        ),
        SlashCommand(
            id = "builtin_clear",
            trigger = "clear",
            description = "Clear conversation history",
            actionType = SlashCommandActionType.CLEAR_CHAT,
            isBuiltIn = true,
            iconName = "clear"
        ),
        SlashCommand(
            id = "builtin_new",
            trigger = "new",
            description = "Start a fresh chat session",
            actionType = SlashCommandActionType.NEW_CHAT,
            isBuiltIn = true,
            iconName = "add"
        ),
        SlashCommand(
            id = "builtin_commands",
            trigger = "commands",
            description = "Manage and create custom slash commands",
            actionType = SlashCommandActionType.COMMANDS,
            isBuiltIn = true,
            iconName = "settings"
        ),
        SlashCommand(
            id = "builtin_settings",
            trigger = "settings",
            description = "Open app settings, theme, and AI providers",
            actionType = SlashCommandActionType.SETTINGS,
            isBuiltIn = true,
            iconName = "settings"
        ),
        SlashCommand(
            id = "builtin_help",
            trigger = "help",
            description = "Show available slash commands and usage guide",
            actionType = SlashCommandActionType.HELP,
            isBuiltIn = true,
            iconName = "help"
        )
    )
}

object SlashCommandProcessor {
    /**
     * Resolves variables in a prompt template:
     * - {file}: Name of active file or default text
     * - {code}: Content of active file or default placeholder
     * - {input}: Arguments passed after command, or default prompt text
     */
    fun resolveTemplate(
        template: String,
        input: String,
        fileName: String?,
        fileContent: String?
    ): String {
        val fileStr = fileName?.trim()?.ifBlank { null } ?: "active file"
        val codeStr = fileContent?.trim()?.ifBlank { null } ?: "(no file currently open in editor)"
        val cleanInput = input.trim()

        if (template.isBlank()) {
            return if (cleanInput.isNotBlank()) cleanInput else "Perform this task thoroughly."
        }

        var result = template
            .replace("{file}", fileStr)
            .replace("{code}", codeStr)

        if (result.contains("{input}")) {
            val inputReplacement = if (cleanInput.isNotBlank()) cleanInput else "Perform a thorough analysis."
            result = result.replace("{input}", inputReplacement)
        } else if (cleanInput.isNotBlank()) {
            result = "$result\n\nAdditional instructions: $cleanInput"
        }

        return result
    }
}

