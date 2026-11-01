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
        assertEquals(7, tools.size)

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
