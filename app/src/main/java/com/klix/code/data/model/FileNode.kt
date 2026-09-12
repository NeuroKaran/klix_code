package com.klix.code.data.model

import android.net.Uri

data class FileNode(
    val uri: Uri,
    val name: String,
    val isDirectory: Boolean,
    val mimeType: String? = null,
    val size: Long = 0,
    val lastModified: Long = 0,
    val children: List<FileNode> = emptyList(),
    val isExpanded: Boolean = false,
    val level: Int = 0
) {
    val extension: String
        get() = if (isDirectory) "" else name.substringAfterLast('.', "")

    val isCodeFile: Boolean
        get() = extension.lowercase() in setOf(
            "kt", "kts", "java", "py", "js", "ts", "jsx", "tsx",
            "html", "css", "json", "xml", "md", "sh", "c", "cpp", "h", "rs", "go", "toml", "yaml", "yml"
        )
}
