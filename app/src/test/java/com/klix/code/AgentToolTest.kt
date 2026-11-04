package com.klix.code

import com.klix.code.data.agent.tools.AgentTools
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentToolTest {

    @Test
    fun testToolDefinitionsAndSchemas() {
        val tools = AgentTools.ALL_TOOLS
        assertEquals(11, tools.size)

        val readFileTool = AgentTools.READ_FILE
        val schema = readFileTool.toJsonSchema()

        assertEquals("object", schema.getString("type"))
        assertTrue(schema.has("properties"))
        val props = schema.getJSONObject("properties")
        assertTrue(props.has("path"))
        assertTrue(props.has("start_line"))
        assertTrue(props.has("end_line"))

        val required = schema.getJSONArray("required")
        assertEquals(1, required.length())
        assertEquals("path", required.getString(0))

        val multiReplaceTool = AgentTools.MULTI_REPLACE_FILE_CONTENT
        val multiSchema = multiReplaceTool.toJsonSchema()
        assertTrue(multiSchema.getJSONObject("properties").has("replacement_chunks"))
        val chunksProp = multiSchema.getJSONObject("properties").getJSONObject("replacement_chunks")
        assertEquals("array", chunksProp.getString("type"))
        assertTrue(chunksProp.has("items"))

        val fetchTool = AgentTools.FETCH_WEB_PAGE
        val fetchSchema = fetchTool.toJsonSchema()
        assertTrue(fetchSchema.getJSONObject("properties").has("url"))

        val searchTool = AgentTools.SEARCH_WEB
        val searchSchema = searchTool.toJsonSchema()
        assertTrue(searchSchema.getJSONObject("properties").has("query"))

        val subagentTool = AgentTools.INVOKE_SUBAGENT
        val subagentSchema = subagentTool.toJsonSchema()
        assertTrue(subagentSchema.getJSONObject("properties").has("task"))
        assertTrue(subagentSchema.getJSONObject("properties").has("max_turns"))

        val askQuestionTool = AgentTools.ASK_QUESTION
        val askSchema = askQuestionTool.toJsonSchema()
        assertTrue(askSchema.getJSONObject("properties").has("question"))
        assertTrue(askSchema.getJSONObject("properties").has("options"))
        assertTrue(askSchema.getJSONObject("properties").has("allow_multiple"))
        val askRequired = askSchema.getJSONArray("required")
        assertTrue(askRequired.length() >= 2)

        // Subagent tools must exclude invoke_subagent to prevent recursive delegation loops
        assertFalse(AgentTools.SUBAGENT_TOOLS.contains(AgentTools.INVOKE_SUBAGENT))
        assertEquals(6, AgentTools.SUBAGENT_TOOLS.size)
    }

    @Test
    fun testIsDestructiveClassification() {
        assertTrue(AgentTools.isDestructive("write_file"))
        assertTrue(AgentTools.isDestructive("edit_file"))
        assertTrue(AgentTools.isDestructive("multi_replace_file_content"))
        assertTrue(AgentTools.isDestructive("multi_replace"))
        assertTrue(AgentTools.isDestructive("run_command"))

        assertFalse(AgentTools.isDestructive("read_file"))
        assertFalse(AgentTools.isDestructive("list_dir"))
        assertFalse(AgentTools.isDestructive("search_files"))
        assertFalse(AgentTools.isDestructive("fetch_web_page"))
        assertFalse(AgentTools.isDestructive("search_web"))
        assertFalse(AgentTools.isDestructive("invoke_subagent"))
        assertFalse(AgentTools.isDestructive("ask_question"))
    }

    @Test
    fun testParseStringList() {
        val jsonArray = """["Option 1", "Option 2", "Option 3"]"""
        val parsedJson = AgentTools.parseStringList(jsonArray)
        assertEquals(3, parsedJson.size)
        assertEquals("Option 1", parsedJson[0])
        assertEquals("Option 2", parsedJson[1])
        assertEquals("Option 3", parsedJson[2])

        val commaSeparated = "Alpha, Beta, Gamma"
        val parsedComma = AgentTools.parseStringList(commaSeparated)
        assertEquals(3, parsedComma.size)
        assertEquals("Alpha", parsedComma[0])
        assertEquals("Beta", parsedComma[1])
        assertEquals("Gamma", parsedComma[2])

        val empty = AgentTools.parseStringList("")
        assertTrue(empty.isEmpty())
    }

    @Test
    fun testParseArguments() {
        val json = """{"path": "src/Main.kt", "content": "println(\"Hello\")"}"""
        val parsed = AgentTools.parseArguments(json)

        assertEquals("src/Main.kt", parsed["path"])
        assertEquals("println(\"Hello\")", parsed["content"])
    }

    @Test
    fun testParseArgumentsWithMarkdownFencesAndPrimitives() {
        val json = """
            ```json
            {
               "path": "test.txt",
               "lines": 42,
               "flag": true
            }
            ```
        """.trimIndent()
        val parsed = AgentTools.parseArguments(json)

        assertEquals("test.txt", parsed["path"])
        assertEquals("42", parsed["lines"])
        assertEquals("true", parsed["flag"])
    }

    @Test
    fun testToolCallRequestPreservesThoughtSignature() {
        val tc = com.klix.code.data.agent.tools.ToolCallRequest(
            id = "tc_1",
            name = "list_dir",
            rawArguments = "{}",
            parsedArguments = emptyMap(),
            thoughtSignature = "sig_abc_123"
        )
        assertEquals("sig_abc_123", tc.thoughtSignature)
    }
}
