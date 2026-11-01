package com.klix.code.data.agent.tools

import com.klix.code.data.model.FileDiff
import org.json.JSONArray
import org.json.JSONObject

data class ToolParameter(
    val name: String,
    val type: String,
    val description: String,
    val required: Boolean = true,
    val items: JSONObject? = null
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
            if (param.items != null) {
                paramObj.put("items", param.items)
            }
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
    val parsedArguments: Map<String, String> = emptyMap(),
    val thoughtSignature: String? = null
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
        description = "Read the content of a file in the workspace by its relative path. Supports line-range slicing with 1-indexed line numbers.",
        parameters = listOf(
            ToolParameter(
                name = "path",
                type = "string",
                description = "The relative path to the file from the workspace root (e.g., 'src/Main.kt' or 'package.json').",
                required = true
            ),
            ToolParameter(
                name = "start_line",
                type = "integer",
                description = "Optional 1-indexed line number to start reading from.",
                required = false
            ),
            ToolParameter(
                name = "end_line",
                type = "integer",
                description = "Optional 1-indexed line number to stop reading at (inclusive).",
                required = false
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
        description = "Perform a targeted replacement in an existing file by replacing old_string with new_string. Optional start_line and end_line restrict replacement to a specific line range.",
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
            ),
            ToolParameter(
                name = "start_line",
                type = "integer",
                description = "Optional starting line number (1-indexed) containing the target content.",
                required = false
            ),
            ToolParameter(
                name = "end_line",
                type = "integer",
                description = "Optional ending line number (1-indexed) containing the target content.",
                required = false
            )
        )
    )

    val MULTI_REPLACE_FILE_CONTENT = ToolDefinition(
        name = "multi_replace_file_content",
        description = "Perform multiple non-contiguous or contiguous replacements in an existing file in a single atomic operation.",
        parameters = listOf(
            ToolParameter(
                name = "path",
                type = "string",
                description = "The relative path to the file to edit.",
                required = true
            ),
            ToolParameter(
                name = "replacement_chunks",
                type = "array",
                description = "List of replacement chunks, each specifying old_string and new_string, with optional start_line and end_line.",
                required = true,
                items = JSONObject().apply {
                    put("type", "object")
                    val props = JSONObject()
                    props.put("old_string", JSONObject().put("type", "string").put("description", "The exact content to replace in this chunk."))
                    props.put("new_string", JSONObject().put("type", "string").put("description", "The replacement content."))
                    props.put("start_line", JSONObject().put("type", "integer").put("description", "Optional 1-indexed starting line number."))
                    props.put("end_line", JSONObject().put("type", "integer").put("description", "Optional 1-indexed ending line number."))
                    put("properties", props)
                    put("required", JSONArray().put("old_string").put("new_string"))
                }
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
        MULTI_REPLACE_FILE_CONTENT,
        LIST_DIR,
        SEARCH_FILES,
        RUN_COMMAND
    )

    fun isDestructive(toolName: String): Boolean {
        return toolName in listOf("write_file", "edit_file", "multi_replace_file_content", "multi_replace", "run_command")
    }

    fun parseArguments(rawJson: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val cleaned = rawJson.trim().removeSurrounding("```json", "```").removeSurrounding("```", "```").trim()
        if (cleaned.isBlank() || cleaned == "{}") return map
        try {
            val json = JSONObject(cleaned)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = json.opt(key)
                map[key] = when (value) {
                    null, JSONObject.NULL -> ""
                    is JSONObject, is JSONArray -> value.toString()
                    else -> value.toString()
                }
            }
        } catch (_: Exception) {
            // Raw string fallback
        }
        return map
    }
}
