package com.klix.code

import android.net.Uri
import com.klix.code.data.model.EditorTab
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.mock

class EditorTabTest {

    @Test
    fun testEditorTabExtensionExtraction() {
        val uri = mock(Uri::class.java)
        val pyTab = EditorTab(uri = uri, name = "script.py", content = "print('hi')")
        assertEquals("py", pyTab.extension)
        assertFalse(pyTab.isModified)

        val ktTab = EditorTab(uri = uri, name = "Main.kt", content = "fun main() {}")
        assertEquals("kt", ktTab.extension)

        val jsTab = EditorTab(uri = uri, name = "index.js", content = "console.log(1);")
        assertEquals("js", jsTab.extension)

        val noExtTab = EditorTab(uri = uri, name = "Dockerfile", content = "FROM alpine")
        assertEquals("", noExtTab.extension)
    }

    @Test
    fun testEditorTabModifiedStateTracking() {
        val uri = mock(Uri::class.java)
        val initialContent = "def hello(): pass"
        val tab = EditorTab(
            uri = uri,
            name = "hello.py",
            content = initialContent,
            originalContent = initialContent,
            isModified = false
        )
        assertFalse(tab.isModified)

        // Simulates updateActiveTabContent logic:
        val modifiedContent = "def hello(): print('modified')"
        val isModified = modifiedContent != tab.originalContent
        val modifiedTab = tab.copy(
            content = modifiedContent,
            isModified = isModified
        )
        assertTrue(modifiedTab.isModified)
        assertEquals(modifiedContent, modifiedTab.content)

        // Reverting back to original content clears isModified:
        val restoredTab = modifiedTab.copy(
            content = initialContent,
            isModified = (initialContent != modifiedTab.originalContent)
        )
        assertFalse(restoredTab.isModified)
    }

    @Test
    fun testCloseTabActiveSelectionLogic() {
        val uri1 = mock(Uri::class.java)
        val uri2 = mock(Uri::class.java)
        val uri3 = mock(Uri::class.java)

        val tab1 = EditorTab(uri = uri1, name = "file1.py", content = "")
        val tab2 = EditorTab(uri = uri2, name = "file2.py", content = "")
        val tab3 = EditorTab(uri = uri3, name = "file3.py", content = "")

        val tabs = mutableListOf(tab1, tab2, tab3)
        var activeUri: Uri? = uri3

        // Simulate closing the last active tab (index 2)
        val indexToClose = tabs.indexOfFirst { it.uri == uri3 }
        tabs.removeAt(indexToClose)
        activeUri = when {
            tabs.isEmpty() -> null
            indexToClose < tabs.size -> tabs[indexToClose].uri
            else -> tabs.last().uri
        }

        // Active tab should fallback to tab2 (uri2)
        assertEquals(uri2, activeUri)
        assertEquals(2, tabs.size)

        // Close tab2
        val index2 = tabs.indexOfFirst { it.uri == uri2 }
        tabs.removeAt(index2)
        activeUri = when {
            tabs.isEmpty() -> null
            index2 < tabs.size -> tabs[index2].uri
            else -> tabs.last().uri
        }
        assertEquals(uri1, activeUri)
        assertEquals(1, tabs.size)

        // Close tab1 -> empty, activeUri becomes null (transitions to HomeDashboard)
        val index1 = tabs.indexOfFirst { it.uri == uri1 }
        tabs.removeAt(index1)
        activeUri = when {
            tabs.isEmpty() -> null
            index1 < tabs.size -> tabs[index1].uri
            else -> tabs.last().uri
        }
        assertNull(activeUri)
        assertTrue(tabs.isEmpty())
    }
}
