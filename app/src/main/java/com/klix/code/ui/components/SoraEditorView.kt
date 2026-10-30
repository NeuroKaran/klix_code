package com.klix.code.ui.components

import android.content.Context
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    fun insertText(text: String) {
        val ed = editor ?: return
        try {
            ed.insertText(text, text.length)
        } catch (_: Exception) {}
    }

    fun undo() {
        try {
            editor?.undo()
        } catch (_: Exception) {}
    }

    fun redo() {
        try {
            editor?.redo()
        } catch (_: Exception) {}
    }

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
    var lastKnownText by remember { mutableStateOf(content) }
    var lastExtension by remember { mutableStateOf(extension) }

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

                // Configure inputType to prevent soft keyboards (e.g. Gboard) from treating
                // code as English prose with auto-corrections while ensuring IME key events
                // (backspace deletion) and gestures (spacebar cursor sliding) function properly.
                inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                        android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

                // Apply syntax highlighting
                val lang = TextMateHelper.getLanguageForExtension(extension)
                if (lang != null) {
                    setEditorLanguage(lang)
                }

                setText(content)
                lastKnownText = content

                // Cursor position listener
                subscribeEvent(SelectionChangeEvent::class.java) { _, _ ->
                    val cur = cursor
                    onCursorChange(cur.leftLine + 1, cur.leftColumn + 1)
                }

                // Text modification listener
                subscribeEvent(ContentChangeEvent::class.java) { _, _ ->
                    val currentText = text.toString()
                    lastKnownText = currentText
                    onContentChange(currentText)
                }
            }
        },
        update = { editor ->
            controller.editor = editor
            // Only update text externally if it changed outside of user keystrokes (e.g. diff or reload)
            // to avoid restarting IME input connection and dropping keyboard mode
            if (content != lastKnownText && editor.text.toString() != content) {
                lastKnownText = content
                editor.setText(content)
            }
            if (editor.isWordwrap != isWordWrap) {
                editor.isWordwrap = isWordWrap
            }
            if (editor.textSizePx != fontSizeSp) {
                editor.setTextSize(fontSizeSp)
            }
            // Only update editor language when file extension changes, not on every keystroke
            if (lastExtension != extension) {
                lastExtension = extension
                val lang = TextMateHelper.getLanguageForExtension(extension)
                editor.setEditorLanguage(lang)
            }
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
