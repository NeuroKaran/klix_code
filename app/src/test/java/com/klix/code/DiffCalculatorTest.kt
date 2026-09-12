package com.klix.code

import com.klix.code.data.model.DiffCalculator
import com.klix.code.data.model.DiffLineType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiffCalculatorTest {

    @Test
    fun testEmptyDiff() {
        val diff = DiffCalculator.computeDiff(
            filePath = "test.txt",
            originalText = "",
            modifiedText = ""
        )
        assertEquals(0, diff.additionsCount)
        assertEquals(0, diff.deletionsCount)
    }

    @Test
    fun testIdenticalContent() {
        val content = "fun main() {\n    println(\"Hello\")\n}"
        val diff = DiffCalculator.computeDiff(
            filePath = "Main.kt",
            originalText = content,
            modifiedText = content
        )
        assertEquals(0, diff.additionsCount)
        assertEquals(0, diff.deletionsCount)
        assertTrue(diff.lines.all { it.type == DiffLineType.UNCHANGED })
    }

    @Test
    fun testAdditionsAndDeletions() {
        val original = "line1\nline2\nline3"
        val modified = "line1\nline2_modified\nline3\nline4_added"

        val diff = DiffCalculator.computeDiff(
            filePath = "file.txt",
            originalText = original,
            modifiedText = modified
        )

        // line2 is removed, line2_modified and line4_added are added
        assertEquals(2, diff.additionsCount)
        assertEquals(1, diff.deletionsCount)

        val removed = diff.lines.filter { it.type == DiffLineType.REMOVED }
        assertEquals(1, removed.size)
        assertEquals("line2", removed[0].content)

        val added = diff.lines.filter { it.type == DiffLineType.ADDED }
        assertEquals(2, added.size)
        assertEquals("line2_modified", added[0].content)
        assertEquals("line4_added", added[1].content)
    }
}
