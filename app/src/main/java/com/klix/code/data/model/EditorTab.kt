package com.klix.code.data.model

import android.net.Uri

data class EditorTab(
    val uri: Uri,
    val name: String,
    val content: String,
    val originalContent: String = content,
    val isModified: Boolean = false,
    val cursorLine: Int = 1,
    val cursorColumn: Int = 1
) {
    val extension: String
        get() = name.substringAfterLast('.', "")
}
