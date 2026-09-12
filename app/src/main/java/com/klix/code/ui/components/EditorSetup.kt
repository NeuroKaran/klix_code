package com.klix.code.ui.components

import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula

object EditorSetup {
    fun createColorScheme(): EditorColorScheme {
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
