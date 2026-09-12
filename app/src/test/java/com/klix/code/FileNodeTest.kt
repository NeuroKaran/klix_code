package com.klix.code

import android.net.Uri
import com.klix.code.data.model.FileNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito

class FileNodeTest {

    @Test
    fun testFileNodeExtension() {
        val dummyUri = Mockito.mock(Uri::class.java)
        val ktNode = FileNode(uri = dummyUri, name = "MainActivity.kt", isDirectory = false)
        assertEquals("kt", ktNode.extension)
        assertTrue(ktNode.isCodeFile)

        val pyNode = FileNode(uri = dummyUri, name = "script.py", isDirectory = false)
        assertEquals("py", pyNode.extension)
        assertTrue(pyNode.isCodeFile)

        val dirNode = FileNode(uri = dummyUri, name = "src", isDirectory = true)
        assertEquals("", dirNode.extension)
        assertFalse(dirNode.isCodeFile)

        val imageNode = FileNode(uri = dummyUri, name = "photo.png", isDirectory = false)
        assertEquals("png", imageNode.extension)
        assertFalse(imageNode.isCodeFile)
    }
}
