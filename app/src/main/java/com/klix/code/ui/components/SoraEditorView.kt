package com.klix.code.ui.components

import android.content.Context
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.EditorSearcher

class EditorController {
    var editor: CodeEditor? = null
        internal set

    fun search(query: String, matchCase: Boolean = false) {
        val ed = editor ?: return
        if (query.isEmpty()) {
            ed.searcher.stopSearch()
        } else {
            ed.searcher.search(query, EditorSearcher.SearchOptions(false, matchCase))
        }
    }

    fun findNext() {
        try {
            editor?.searcher?.gotoNext()
        } catch (_: Exception) {}
    }

    fun findPrevious() {
        try {
            editor?.searcher?.gotoPrevious()
        } catch (_: Exception) {}
    }

    fun replaceCurrent(replacement: String) {
        try {
            editor?.searcher?.replaceThis(replacement)
        } catch (_: Exception) {}
    }

    fun replaceAll(replacement: String) {
        try {
            editor?.searcher?.replaceAll(replacement)
        } catch (_: Exception) {}
    }

    fun stopSearch() {
        editor?.searcher?.stopSearch()
    }
}

@Composable
fun rememberEditorController(): EditorController = remember { EditorController() }

@Composable
fun SoraEditorView(
    content: String,
    extension: String,
    isWordWrap: Boolean,
    fontSizeSp: Float,
    onContentChange: (String) -> Unit,
    onCursorChange: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    controller: EditorController = rememberEditorController()
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        TextMateHelper.init(context)
    }

    AndroidView(
        factory = { ctx ->
            CodeEditor(ctx).apply {
                controller.editor = this
                isLineNumberEnabled = true
                typefaceText = Typeface.MONOSPACE
                colorScheme = EditorSetup.createColorScheme()
                setTextSize(fontSizeSp)
                this.isWordwrap = isWordWrap

                // Apply syntax highlighting
                val lang = TextMateHelper.getLanguageForExtension(extension)
                if (lang != null) {
                    setEditorLanguage(lang)
                }

                setText(content)

                // Cursor position listener
                subscribeEvent(SelectionChangeEvent::class.java) { _, _ ->
                    val cur = cursor
                    onCursorChange(cur.leftLine + 1, cur.leftColumn + 1)
                }

                // Text modification listener
                subscribeEvent(ContentChangeEvent::class.java) { _, _ ->
                    val currentText = text.toString()
                    onContentChange(currentText)
                }
            }
        },
        update = { editor ->
            controller.editor = editor
            if (editor.text.toString() != content) {
                editor.setText(content)
            }
            if (editor.isWordwrap != isWordWrap) {
                editor.isWordwrap = isWordWrap
            }
            if (editor.textSizePx != fontSizeSp) {
                editor.setTextSize(fontSizeSp)
            }
            val lang = TextMateHelper.getLanguageForExtension(extension)
            editor.setEditorLanguage(lang)
        },
        modifier = modifier
    )

    DisposableEffect(Unit) {
        onDispose {
            controller.editor?.searcher?.stopSearch()
            controller.editor = null
        }
    }
}
