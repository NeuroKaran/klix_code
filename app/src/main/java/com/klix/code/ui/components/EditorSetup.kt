package com.klix.code.ui.components

import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula

object EditorSetup {
    fun createColorScheme(isDark: Boolean = false): EditorColorScheme {
        if (!isDark) {
            val scheme = EditorColorScheme()
            scheme.setColor(EditorColorScheme.WHOLE_BACKGROUND, android.graphics.Color.parseColor("#FFFFFF"))
            scheme.setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, android.graphics.Color.parseColor("#F8FAFC"))
            scheme.setColor(EditorColorScheme.LINE_NUMBER, android.graphics.Color.parseColor("#94A3B8"))
            scheme.setColor(EditorColorScheme.LINE_NUMBER_CURRENT, android.graphics.Color.parseColor("#0969DA"))
            scheme.setColor(EditorColorScheme.CURRENT_LINE, android.graphics.Color.parseColor("#F1F5F9"))
            scheme.setColor(EditorColorScheme.SELECTION_INSERT, android.graphics.Color.parseColor("#0969DA"))
            scheme.setColor(EditorColorScheme.SELECTION_HANDLE, android.graphics.Color.parseColor("#0969DA"))
            scheme.setColor(EditorColorScheme.SELECTED_TEXT_BACKGROUND, android.graphics.Color.parseColor("#BFDBFE"))
            scheme.setColor(EditorColorScheme.TEXT_NORMAL, android.graphics.Color.parseColor("#1F2328"))
            return scheme
        }
        val scheme = SchemeDarcula()
        scheme.setColor(EditorColorScheme.WHOLE_BACKGROUND, android.graphics.Color.parseColor("#14161B"))
        scheme.setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, android.graphics.Color.parseColor("#14161B"))
        scheme.setColor(EditorColorScheme.LINE_NUMBER, android.graphics.Color.parseColor("#4B5563"))
        scheme.setColor(EditorColorScheme.LINE_NUMBER_CURRENT, android.graphics.Color.parseColor("#93C5FD"))
        scheme.setColor(EditorColorScheme.CURRENT_LINE, android.graphics.Color.parseColor("#1E232B"))
        scheme.setColor(EditorColorScheme.SELECTION_INSERT, android.graphics.Color.parseColor("#3B82F6"))
        scheme.setColor(EditorColorScheme.SELECTION_HANDLE, android.graphics.Color.parseColor("#3B82F6"))
        scheme.setColor(EditorColorScheme.SELECTED_TEXT_BACKGROUND, android.graphics.Color.parseColor("#264F78"))
        return scheme
    }
}
