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
        assertEquals(6, tools.size)

        val readFileTool = AgentTools.READ_FILE
        val schema = readFileTool.toJsonSchema()

        assertEquals("object", schema.getString("type"))
        assertTrue(schema.has("properties"))
        val props = schema.getJSONObject("properties")
        assertTrue(props.has("path"))

        val required = schema.getJSONArray("required")
        assertEquals(1, required.length())
        assertEquals("path", required.getString(0))
    }

    @Test
    fun testIsDestructiveClassification() {
        assertTrue(AgentTools.isDestructive("write_file"))
        assertTrue(AgentTools.isDestructive("edit_file"))
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
}
