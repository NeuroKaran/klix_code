package com.klix.code.data.sandbox

import android.net.Uri
import com.klix.code.data.model.EditorTab
import com.klix.code.data.workspace.SandboxWorkspace
import java.io.File

data class RunResolution(
    val command: String?,
    val errorMessage: String? = null,
    val missingToolId: String? = null,
    val requiresSetup: Boolean = false
)

object LanguageRunner {

    fun posixQuote(value: String): String {
        return "'" + value.replace("'", "'\\''") + "'"
    }

    fun resolveRunCommand(
        tab: EditorTab,
        workspace: SandboxWorkspace?,
        sandboxManager: SandboxManager
    ): RunResolution {
        val ext = tab.extension.lowercase()
        val requiredTool = requiredToolForExtension(ext)
            ?: return RunResolution(
                command = null,
                errorMessage = if (ext.isBlank()) {
                    "Cannot run '${tab.name}': file has no recognized language extension."
                } else {
                    "Unsupported file type '.$ext'. Supported extensions: .js, .py, .c, .cpp, .cc, .cxx, .sh."
                },
                missingToolId = null,
                requiresSetup = false
            )

        val relPathResult = resolveWorkspaceRelativePath(tab, workspace, sandboxManager)
        if (relPathResult.isFailure) {
            return RunResolution(
                command = null,
                errorMessage = relPathResult.exceptionOrNull()?.message ?: "Invalid workspace file path.",
                missingToolId = null,
                requiresSetup = false
            )
        }
        val relPath = relPathResult.getOrThrow()

        if (!sandboxManager.isInstalled()) {
            return RunResolution(
                command = null,
                errorMessage = "Local sandbox is not installed. Please complete Sandbox Setup before running '${tab.name}'.",
                missingToolId = requiredTool,
                requiresSetup = true
            )
        }

        if (!sandboxManager.isToolInstalled(requiredTool)) {
            return RunResolution(
                command = null,
                errorMessage = "Required toolchain '$requiredTool' is not installed. Please install it in Sandbox Setup.",
                missingToolId = requiredTool,
                requiresSetup = true
            )
        }

        val quotedPath = posixQuote(relPath)
        val cmd = when (ext) {
            "js" -> "node $quotedPath"
            "py" -> "python3 -u $quotedPath"
            "sh" -> "sh $quotedPath"
            "c" -> buildCompileAndRunCommand(compiler = "clang", flags = "-Wall -Wextra", sourceRelPath = relPath, extraLibs = "-lm", sandboxManager = sandboxManager)
            "cpp", "cc", "cxx" -> buildCompileAndRunCommand(compiler = "clang++", flags = "-std=c++17 -Wall -Wextra", sourceRelPath = relPath, extraLibs = "", sandboxManager = sandboxManager)
            else -> null
        }

        return RunResolution(command = cmd)
    }

    private fun requiredToolForExtension(ext: String): String? = when (ext) {
        "js" -> "nodejs"
        "py" -> "python3"
        "c" -> "c"
        "cpp", "cc", "cxx" -> "cpp"
        "sh" -> "dash"
        else -> null
    }

    private fun buildCompileAndRunCommand(
        compiler: String,
        flags: String,
        sourceRelPath: String,
        extraLibs: String,
        sandboxManager: SandboxManager
    ): String {
        val buildDir = sandboxManager.getBuildDir()
        val safeStem = sourceRelPath
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim('_')
            .ifBlank { "a_out" }
        val outputBin = File(buildDir, "${safeStem}_bin").absolutePath
        val quotedSrc = posixQuote(sourceRelPath)
        val quotedOut = posixQuote(outputBin)
        val libsPart = if (extraLibs.isNotBlank()) " $extraLibs" else ""
        return "mkdir -p ${posixQuote(buildDir.absolutePath)} && $compiler $flags $quotedSrc -o $quotedOut$libsPart && /system/bin/linker64 $quotedOut"
    }

    private fun resolveWorkspaceRelativePath(
        tab: EditorTab,
        workspace: SandboxWorkspace?,
        sandboxManager: SandboxManager
    ): Result<String> {
        val uri: Uri? = tab.uri
        if (workspace == null) {
            val fallbackName = tab.name.replace('\\', '/')
            if (containsTraversal(fallbackName) || fallbackName.startsWith("/")) {
                return Result.failure(IllegalArgumentException("Invalid file path outside workspace: '$fallbackName'"))
            }
            return Result.success(fallbackName)
        }

        val workspaceDir = resolveWorkspaceDirectory(workspace, sandboxManager)
        val rawPath = uri?.path
        if (uri != null && (uri.scheme == "file" || uri.scheme.isNullOrBlank()) && !rawPath.isNullOrBlank()) {
            val wsCanonical = workspaceDir.canonicalFile
            val targetCanonical = File(rawPath).canonicalFile
            val wsPrefix = wsCanonical.path.trimEnd(File.separatorChar) + File.separator
            if (targetCanonical.path.startsWith(wsPrefix)) {
                val rel = targetCanonical.path.removePrefix(wsPrefix).replace('\\', '/')
                if (rel.isNotBlank() && !containsTraversal(rel)) {
                    return Result.success(rel)
                }
            }
            return Result.failure(
                IllegalArgumentException("Security error: file '${tab.name}' ($rawPath) is outside active workspace '${workspaceDir.absolutePath}'.")
            )
        }

        // Handle SAF content:// URIs safely
        if (!rawPath.isNullOrBlank()) {
            val decoded = rawPath.replace('\\', '/')
            val afterColon = if (decoded.contains(':')) decoded.substringAfterLast(':') else decoded
            val candidate = afterColon.trimStart('/')
            if (candidate.isNotBlank() && !containsTraversal(candidate)) {
                return Result.success(candidate)
            }
        }

        val cleanName = tab.name.replace('\\', '/')
        if (containsTraversal(cleanName) || cleanName.startsWith("/")) {
            return Result.failure(IllegalArgumentException("Invalid file name '${tab.name}'."))
        }
        return Result.success(cleanName)
    }

    private fun resolveWorkspaceDirectory(workspace: SandboxWorkspace, sandboxManager: SandboxManager): File {
        val rootUri = workspace.rootUri
        if ((rootUri.scheme == "file" || rootUri.scheme.isNullOrBlank()) && !rootUri.path.isNullOrBlank()) {
            return File(rootUri.path!!)
        }
        return File(sandboxManager.getRootfsDir(), workspace.relativePath.removePrefix("/"))
    }

    private fun containsTraversal(path: String): Boolean {
        return path.split('/', '\\').any { it == ".." }
    }
}
