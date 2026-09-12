package com.klix.code.ui.components

import android.content.Context
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver

object TextMateHelper {
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        try {
            val resolver = AssetsFileResolver(context.assets)
            FileProviderRegistry.getInstance().addFileProvider(resolver)
            isInitialized = true
        } catch (_: Exception) {}
    }

    fun getLanguageForExtension(extension: String): TextMateLanguage? {
        val scope = when (extension.lowercase()) {
            "kt", "kts" -> "source.kotlin"
            "py" -> "source.python"
            "js", "jsx" -> "source.js"
            "ts", "tsx" -> "source.ts"
            "json" -> "source.json"
            "html" -> "text.html.basic"
            "css" -> "source.css"
            "md", "markdown" -> "text.html.markdown"
            "xml" -> "text.xml"
            "sh", "bash" -> "source.shell"
            else -> null
        } ?: return null

        return try {
            TextMateLanguage.create(scope, true)
        } catch (_: Exception) {
            null
        }
    }
}
