package com.klix.code.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.klix.code.data.model.FileNode
import com.klix.code.data.model.ProjectItem
import com.klix.code.data.stats.StreakManager
import com.klix.code.widget.KlixWidgets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URLDecoder

data class SearchMatch(
    val filePath: String,
    val lineNumber: Int,
    val lineContent: String
)

interface WorkspaceRepository {
    suspend fun persistProjectPermission(uri: Uri, name: String)
    suspend fun getRecentProjects(): List<ProjectItem>
    suspend fun removeRecentProject(uri: Uri)
    suspend fun loadTree(treeUri: Uri): FileNode?
    suspend fun readFile(fileUri: Uri): String
    suspend fun writeFile(fileUri: Uri, content: String)
    suspend fun createFile(parentDirUri: Uri, name: String): Uri?
    suspend fun createDirectory(parentDirUri: Uri, name: String): Uri?
    suspend fun delete(targetUri: Uri): Boolean
    suspend fun rename(targetUri: Uri, newName: String): Boolean
    suspend fun findDocumentByPath(treeUri: Uri, relativePath: String): DocumentFile?
    suspend fun findOrCreateDocumentByPath(treeUri: Uri, relativePath: String): DocumentFile?
    suspend fun searchFiles(treeUri: Uri, query: String, relativeScope: String? = null, maxResults: Int = 50): List<SearchMatch>
    suspend fun listDirectory(treeUri: Uri, relativePath: String): List<FileNode>
}

class WorkspaceRepositoryImpl @JvmOverloads constructor(
    private val context: Context,
    private val allowedSandboxDir: File? = null
) : WorkspaceRepository {

    private fun getPrefs(): SharedPreferences? {
        return try {
            context.getSharedPreferences("klix_workspace_prefs", Context.MODE_PRIVATE)
        } catch (_: Exception) {
            null
        }
    }

    private val inMemoryRecentProjects = mutableListOf<ProjectItem>()

    override suspend fun persistProjectPermission(uri: Uri, name: String) = withContext(Dispatchers.IO) {
        val projects = getRecentProjects().toMutableList()
        val canonicalTarget = canonicalizeUri(uri)
        val existingIndex = projects.indexOfFirst { canonicalizeUri(it.uri) == canonicalTarget }
        if (existingIndex >= 0) {
            projects.removeAt(existingIndex)
        }
        projects.add(0, ProjectItem(uri = uri, name = name, lastOpened = System.currentTimeMillis()))
        try { StreakManager.recordActivity(context) } catch (_: Exception) {}
        saveRecentProjects(projects)
    }

    override suspend fun getRecentProjects(): List<ProjectItem> = withContext(Dispatchers.IO) {
        val prefs = getPrefs()
        val list = mutableListOf<ProjectItem>()
        if (prefs != null) {
            val jsonString = prefs.getString("recent_projects", null)
            if (jsonString != null) {
                try {
                    val jsonArray = JSONArray(jsonString)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val uriStr = obj.getString("uri")
                        val name = obj.getString("name")
                        val lastOpened = obj.optLong("lastOpened", System.currentTimeMillis())
                        list.add(ProjectItem(uri = Uri.parse(uriStr), name = name, lastOpened = lastOpened))
                    }
                } catch (_: Exception) {
                    // Ignore malformed preferences
                }
            }
        } else {
            list.addAll(inMemoryRecentProjects)
        }
        val sorted = list.sortedByDescending { it.lastOpened }
        val seen = mutableSetOf<String>()
        val deduplicated = mutableListOf<ProjectItem>()
        for (item in sorted) {
            if (uriToFile(item.uri) != null) {
                val canon = canonicalizeUri(item.uri)
                if (seen.add(canon)) {
                    deduplicated.add(item)
                }
            }
        }
        deduplicated
    }

    override suspend fun removeRecentProject(uri: Uri) = withContext(Dispatchers.IO) {
        val canonicalTarget = canonicalizeUri(uri)
        val projects = getRecentProjects().filter { canonicalizeUri(it.uri) != canonicalTarget }
        saveRecentProjects(projects)
    }

    companion object {
        fun canonicalizeUri(uri: Uri): String {
            val raw = uri.toString()
            return try {
                val safeRaw = raw.replace("+", "%2B")
                URLDecoder.decode(safeRaw, "UTF-8").trimEnd('/')
            } catch (_: Exception) {
                raw.trimEnd('/')
            }
        }
    }

    private fun saveRecentProjects(projects: List<ProjectItem>) {
        inMemoryRecentProjects.clear()
        inMemoryRecentProjects.addAll(projects)
        val prefs = getPrefs() ?: return
        val jsonArray = JSONArray()
        projects.take(20).forEach {
            val obj = JSONObject().apply {
                put("uri", it.uri.toString())
                put("name", it.name)
                put("lastOpened", it.lastOpened)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("recent_projects", jsonArray.toString()).apply()
        try { KlixWidgets.updateAll(context) } catch (_: Exception) {}
    }

    private fun uriToFile(uri: Uri): File? {
        val scheme = try { uri.scheme } catch (_: Throwable) { null }
        if (scheme != null && scheme.isNotEmpty() && scheme != "file") {
            return null
        }
        val path = try { uri.path } catch (_: Throwable) { null } ?: return null
        val normalized = path.replace('\\', '/')
        if (normalized.startsWith("/sdcard", ignoreCase = true) ||
            normalized.startsWith("/storage", ignoreCase = true) ||
            normalized.startsWith("/mnt", ignoreCase = true) ||
            normalized.startsWith("/data/media", ignoreCase = true)
        ) {
            return null
        }

        val file = File(path)
        val fileCanonical = try { file.canonicalFile } catch (_: Throwable) { file.absoluteFile }
        val baseSandbox = allowedSandboxDir ?: File(context.filesDir, "sandbox")
        val sandboxCanonical = try { baseSandbox.canonicalFile } catch (_: Throwable) { baseSandbox.absoluteFile }

        if (!isSubPath(fileCanonical, sandboxCanonical)) {
            return null
        }
        return file
    }

    private fun getDocumentFile(uri: Uri): DocumentFile? {
        val file = uriToFile(uri) ?: return null
        return if (file.exists()) DocumentFile.fromFile(file) else null
    }

    override suspend fun loadTree(treeUri: Uri): FileNode? = withContext(Dispatchers.IO) {
        val rootFile = uriToFile(treeUri) ?: return@withContext null
        if (!rootFile.exists()) return@withContext null
        buildNodeFromFile(rootFile, level = 0)
    }

    private fun buildNodeFromFile(file: File, level: Int): FileNode {
        val isDir = file.isDirectory
        val children = if (isDir) {
            file.listFiles()?.filter { !it.name.startsWith(".klix") && it.name != ".git" }
                ?.map { buildNodeFromFile(it, level + 1) }
                ?.sortedWith(compareBy<FileNode> { !it.isDirectory }.thenBy { it.name.lowercase() })
                ?: emptyList()
        } else {
            emptyList()
        }

        return FileNode(
            uri = Uri.fromFile(file),
            name = file.name.ifEmpty { if (isDir) "folder" else "file" },
            isDirectory = isDir,
            mimeType = if (isDir) null else resolveMimeType(file.name),
            size = if (isDir) 0L else file.length(),
            lastModified = file.lastModified(),
            children = children,
            level = level
        )
    }

    override suspend fun readFile(fileUri: Uri): String = withContext(Dispatchers.IO) {
        val file = uriToFile(fileUri) ?: return@withContext ""
        if (file.exists() && file.isFile) file.readText(Charsets.UTF_8) else ""
    }

    override suspend fun writeFile(fileUri: Uri, content: String) = withContext(Dispatchers.IO) {
        val file = uriToFile(fileUri) ?: throw IllegalStateException("Invalid file URI: $fileUri")
        file.parentFile?.mkdirs()
        file.writeText(content, Charsets.UTF_8)
        try { StreakManager.recordActivity(context) } catch (_: Exception) {}
    }

    private fun isSubPath(child: File, parent: File): Boolean {
        val childPath = child.canonicalPath
        val parentPath = parent.canonicalPath
        return childPath == parentPath || childPath.startsWith(parentPath + File.separator)
    }

    override suspend fun createFile(parentDirUri: Uri, name: String): Uri? = withContext(Dispatchers.IO) {
        val parentFile = uriToFile(parentDirUri)?.canonicalFile ?: return@withContext null
        val target = File(parentFile, name).canonicalFile
        if (!isSubPath(target, parentFile)) return@withContext null
        try {
            if (target.createNewFile() || target.exists()) Uri.fromFile(target) else null
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun createDirectory(parentDirUri: Uri, name: String): Uri? = withContext(Dispatchers.IO) {
        val parentFile = uriToFile(parentDirUri)?.canonicalFile ?: return@withContext null
        val target = File(parentFile, name).canonicalFile
        if (!isSubPath(target, parentFile)) return@withContext null
        if (target.mkdirs() || target.exists()) Uri.fromFile(target) else null
    }

    override suspend fun delete(targetUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val file = uriToFile(targetUri) ?: return@withContext false
        file.deleteRecursively()
    }

    override suspend fun rename(targetUri: Uri, newName: String): Boolean = withContext(Dispatchers.IO) {
        val file = uriToFile(targetUri) ?: return@withContext false
        val parent = file.parentFile ?: return@withContext false
        val dest = File(parent, newName).canonicalFile
        if (!isSubPath(dest, parent.canonicalFile)) return@withContext false
        file.renameTo(dest)
    }

    override suspend fun findDocumentByPath(treeUri: Uri, relativePath: String): DocumentFile? = withContext(Dispatchers.IO) {
        val rootFile = uriToFile(treeUri)?.canonicalFile ?: return@withContext null
        val segments = relativePath.split('/', '\\').filter { it.isNotBlank() && it != "." }
        if (segments.isEmpty()) return@withContext DocumentFile.fromFile(rootFile)

        var current = rootFile
        for (segment in segments) {
            if (segment == "..") {
                current = current.parentFile ?: return@withContext null
            } else {
                current = File(current, segment)
            }
        }
        val canonicalCurrent = current.canonicalFile
        if (!isSubPath(canonicalCurrent, rootFile)) {
            // Path traversal prevented
            return@withContext null
        }
        if (canonicalCurrent.exists()) DocumentFile.fromFile(canonicalCurrent) else null
    }

    override suspend fun findOrCreateDocumentByPath(treeUri: Uri, relativePath: String): DocumentFile? = withContext(Dispatchers.IO) {
        val rootFile = uriToFile(treeUri)?.canonicalFile ?: return@withContext null
        val segments = relativePath.split('/', '\\').filter { it.isNotBlank() && it != "." }
        if (segments.isEmpty()) return@withContext DocumentFile.fromFile(rootFile)

        var current = rootFile
        for (i in 0 until segments.size - 1) {
            val dirName = segments[i]
            current = if (dirName == "..") {
                current.parentFile ?: return@withContext null
            } else {
                File(current, dirName)
            }
            if (!isSubPath(current.canonicalFile, rootFile)) {
                return@withContext null
            }
            if (!current.exists()) {
                current.mkdirs()
            }
        }

        val fileName = segments.last()
        val targetFile = File(current, fileName).canonicalFile
        if (!isSubPath(targetFile, rootFile)) {
            return@withContext null
        }

        if (!targetFile.exists()) {
            targetFile.parentFile?.mkdirs()
            targetFile.createNewFile()
        }
        DocumentFile.fromFile(targetFile)
    }

    override suspend fun listDirectory(treeUri: Uri, relativePath: String): List<FileNode> = withContext(Dispatchers.IO) {
        val rootFile = uriToFile(treeUri)?.canonicalFile ?: return@withContext emptyList()
        val dirFile = if (relativePath.isBlank() || relativePath == ".") rootFile else File(rootFile, relativePath).canonicalFile
        if (!isSubPath(dirFile, rootFile) || !dirFile.isDirectory) return@withContext emptyList()

        dirFile.listFiles()?.filter { !it.name.startsWith(".klix") && it.name != ".git" }
            ?.map { buildNodeFromFile(it, 0) }
            ?.sortedWith(compareBy<FileNode> { !it.isDirectory }.thenBy { it.name.lowercase() })
            ?: emptyList()
    }

    override suspend fun searchFiles(
        treeUri: Uri,
        query: String,
        relativeScope: String?,
        maxResults: Int
    ): List<SearchMatch> = withContext(Dispatchers.IO) {
        val rootFile = uriToFile(treeUri)?.canonicalFile ?: return@withContext emptyList()
        val startDir = if (!relativeScope.isNullOrBlank() && relativeScope != ".") {
            val scoped = File(rootFile, relativeScope).canonicalFile
            if (!isSubPath(scoped, rootFile)) return@withContext emptyList()
            scoped
        } else {
            rootFile
        }

        if (!startDir.exists() || !startDir.isDirectory) return@withContext emptyList()

        val results = mutableListOf<SearchMatch>()
        fun searchRecursive(dir: File, currentRelPath: String) {
            if (results.size >= maxResults) return
            val children = dir.listFiles() ?: return
            for (child in children) {
                val name = child.name
                if (child.isDirectory) {
                    if (name.startsWith(".") || name == "build" || name == "node_modules") continue
                    val nextPath = if (currentRelPath.isEmpty()) name else "$currentRelPath/$name"
                    searchRecursive(child, nextPath)
                } else {
                    if (name.startsWith(".")) continue
                    val ext = name.substringAfterLast('.', "").lowercase()
                    val isText = ext in setOf(
                        "kt", "kts", "java", "py", "js", "ts", "json", "html", "css",
                        "xml", "md", "txt", "yaml", "yml", "gradle", "properties", "sh", "c", "cpp", "h"
                    )
                    if (!isText) continue

                    val filePath = if (currentRelPath.isEmpty()) name else "$currentRelPath/$name"
                    try {
                        child.bufferedReader(Charsets.UTF_8).useLines { lines ->
                            var lineNum = 1
                            for (line in lines) {
                                if (results.size >= maxResults) break
                                if (line.contains(query, ignoreCase = true)) {
                                    results.add(
                                        SearchMatch(
                                            filePath = filePath,
                                            lineNumber = lineNum,
                                            lineContent = line.trim()
                                        )
                                    )
                                }
                                lineNum++
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        searchRecursive(startDir, "")
        results
    }

    private fun resolveMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "kt", "kts", "java" -> "text/x-kotlin"
            "py" -> "text/x-python"
            "js" -> "application/javascript"
            "ts" -> "application/typescript"
            "json" -> "application/json"
            "html" -> "text/html"
            "xml" -> "text/xml"
            "md" -> "text/markdown"
            "css" -> "text/css"
            else -> "text/plain"
        }
    }
}
