package com.klix.code.data.agent.tools

import com.klix.code.data.model.FileDiff
import org.json.JSONArray
import org.json.JSONObject

data class ToolParameter(
    val name: String,
    val type: String,
    val description: String,
    val required: Boolean = true
)

data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: List<ToolParameter>
) {
    /**
     * Converts parameter definition to a standard JSON Schema object (type: object, properties, required).
     */
    fun toJsonSchema(): JSONObject {
        val schema = JSONObject()
        schema.put("type", "object")

        val properties = JSONObject()
        val requiredList = JSONArray()

        for (param in parameters) {
            val paramObj = JSONObject()
            paramObj.put("type", param.type)
            paramObj.put("description", param.description)
            properties.put(param.name, paramObj)

            if (param.required) {
                requiredList.put(param.name)
            }
        }

        schema.put("properties", properties)
        schema.put("required", requiredList)
        return schema
    }
}

data class ToolCallRequest(
    val id: String,
    val name: String,
    val rawArguments: String,
    val parsedArguments: Map<String, String> = emptyMap()
)

data class ToolCallResult(
    val toolCallId: String,
    val name: String,
    val isSuccess: Boolean,
    val output: String,
    val error: String? = null,
    val proposedDiff: FileDiff? = null
)

object AgentTools {
    val READ_FILE = ToolDefinition(
        name = "read_file",
        description = "Read the content of a file in the workspace by its relative path.",
        parameters = listOf(
            ToolParameter(
                name = "path",
                type = "string",
                description = "The relative path to the file from the workspace root (e.g., 'src/Main.kt' or 'package.json').",
                required = true
            )
        )
    )

    val WRITE_FILE = ToolDefinition(
        name = "write_file",
        description = "Create a new file or completely overwrite an existing file with the provided content.",
        parameters = listOf(
            ToolParameter(
                name = "path",
                type = "string",
                description = "The relative path to the file from the workspace root.",
                required = true
            ),
            ToolParameter(
                name = "content",
                type = "string",
                description = "The complete text content to write into the file.",
                required = true
            )
        )
    )

    val EDIT_FILE = ToolDefinition(
        name = "edit_file",
        description = "Perform a targeted replacement in an existing file by replacing old_string with new_string.",
        parameters = listOf(
            ToolParameter(
                name = "path",
                type = "string",
                description = "The relative path to the file to edit.",
                required = true
            ),
            ToolParameter(
                name = "old_string",
                type = "string",
                description = "The exact sequence of characters/lines to be replaced.",
                required = true
            ),
            ToolParameter(
                name = "new_string",
                type = "string",
                description = "The replacement content.",
                required = true
            )
        )
    )

    val LIST_DIR = ToolDefinition(
        name = "list_dir",
        description = "List files and subdirectories in the specified workspace directory.",
        parameters = listOf(
            ToolParameter(
                name = "path",
                type = "string",
                description = "The relative directory path to list (use '.' or empty for workspace root).",
                required = false
            )
        )
    )

    val SEARCH_FILES = ToolDefinition(
        name = "search_files",
        description = "Search for a query string or pattern across text files in the workspace.",
        parameters = listOf(
            ToolParameter(
                name = "query",
                type = "string",
                description = "The search text or pattern to find.",
                required = true
            ),
            ToolParameter(
                name = "path",
                type = "string",
                description = "Optional subfolder path to limit search scope.",
                required = false
            )
        )
    )

    val RUN_COMMAND = ToolDefinition(
        name = "run_command",
        description = "Execute a shell command inside the local execution environment.",
        parameters = listOf(
            ToolParameter(
                name = "command",
                type = "string",
                description = "The shell command to execute.",
                required = true
            )
        )
    )

    val ALL_TOOLS = listOf(
        READ_FILE,
        WRITE_FILE,
        EDIT_FILE,
        LIST_DIR,
        SEARCH_FILES,
        RUN_COMMAND
    )

    fun isDestructive(toolName: String): Boolean {
        return toolName in listOf("write_file", "edit_file", "run_command")
    }

    fun parseArguments(rawJson: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val json = JSONObject(rawJson)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = json.optString(key, "")
            }
        } catch (_: Exception) {
            // Raw string fallback
        }
        return map
    }
}
