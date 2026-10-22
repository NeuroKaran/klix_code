package com.klix.code.data.repository

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.klix.code.data.model.FileNode
import com.klix.code.data.model.ProjectItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.klix.code.data.stats.StreakManager
import com.klix.code.widget.KlixWidgets
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

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

class WorkspaceRepositoryImpl(
    private val context: Context
) : WorkspaceRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("klix_workspace_prefs", Context.MODE_PRIVATE)

    override suspend fun persistProjectPermission(uri: Uri, name: String) = withContext(Dispatchers.IO) {
        if (uri.scheme == "content") {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (_: Exception) {
                // Might already be persisted or transient
            }
        }

        val projects = getRecentProjects().toMutableList()
        val existingIndex = projects.indexOfFirst { it.uri.toString() == uri.toString() }
        if (existingIndex >= 0) {
            projects[existingIndex] = projects[existingIndex].copy(
                name = name,
                lastOpened = System.currentTimeMillis()
            )
        } else {
            projects.add(0, ProjectItem(uri = uri, name = name, lastOpened = System.currentTimeMillis()))
        }
        StreakManager.recordActivity(context)
        saveRecentProjects(projects)
    }

    override suspend fun getRecentProjects(): List<ProjectItem> = withContext(Dispatchers.IO) {
        val jsonString = prefs.getString("recent_projects", null) ?: return@withContext emptyList()
        val list = mutableListOf<ProjectItem>()
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
        list.sortedByDescending { it.lastOpened }
    }

    override suspend fun removeRecentProject(uri: Uri) = withContext(Dispatchers.IO) {
        val projects = getRecentProjects().filter { it.uri.toString() != uri.toString() }
        saveRecentProjects(projects)
    }

    private fun saveRecentProjects(projects: List<ProjectItem>) {
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
        KlixWidgets.updateAll(context)
    }

    private fun getDocumentFile(uri: Uri): DocumentFile? {
        return if (uri.scheme == "file") {
            val path = uri.path ?: return null
            val file = File(path)
            if (file.exists()) DocumentFile.fromFile(file) else null
        } else {
            DocumentFile.fromTreeUri(context, uri) ?: DocumentFile.fromSingleUri(context, uri)
        }
    }

    override suspend fun loadTree(treeUri: Uri): FileNode? = withContext(Dispatchers.IO) {
        val rootDoc = getDocumentFile(treeUri) ?: return@withContext null
        buildNode(rootDoc, level = 0)
    }

    private fun buildNode(doc: DocumentFile, level: Int): FileNode {
        val isDir = doc.isDirectory
        val children = if (isDir) {
            doc.listFiles()
                .map { buildNode(it, level + 1) }
                .sortedWith(compareBy<FileNode> { !it.isDirectory }.thenBy { it.name.lowercase() })
        } else {
            emptyList()
        }

        return FileNode(
            uri = doc.uri,
            name = doc.name ?: if (isDir) "folder" else "file",
            isDirectory = isDir,
            mimeType = doc.type,
            size = if (isDir) 0L else doc.length(),
            lastModified = doc.lastModified(),
            children = children,
            level = level
        )
    }

    override suspend fun readFile(fileUri: Uri): String = withContext(Dispatchers.IO) {
        if (fileUri.scheme == "file") {
            val path = fileUri.path ?: return@withContext ""
            val file = File(path)
            if (file.exists() && file.isFile) file.readText(Charsets.UTF_8) else ""
        } else {
            context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } ?: ""
        }
    }

    override suspend fun writeFile(fileUri: Uri, content: String) = withContext(Dispatchers.IO) {
        if (fileUri.scheme == "file") {
            val path = fileUri.path ?: throw IllegalStateException("Empty path for $fileUri")
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeText(content, Charsets.UTF_8)
        } else {
            context.contentResolver.openOutputStream(fileUri, "wt")?.use { outputStream ->
                outputStream.write(content.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            } ?: throw IllegalStateException("Unable to open output stream for $fileUri")
        }
        StreakManager.recordActivity(context)
    }

    override suspend fun createFile(parentDirUri: Uri, name: String): Uri? = withContext(Dispatchers.IO) {
        if (parentDirUri.scheme == "file") {
            val parentPath = parentDirUri.path ?: return@withContext null
            val file = File(parentPath, name)
            try {
                if (file.createNewFile() || file.exists()) Uri.fromFile(file) else null
            } catch (_: Exception) {
                null
            }
        } else {
            val parent = getDocumentFile(parentDirUri) ?: return@withContext null
            val mimeType = resolveMimeType(name)
            val file = parent.createFile(mimeType, name)
            file?.uri
        }
    }

    override suspend fun createDirectory(parentDirUri: Uri, name: String): Uri? = withContext(Dispatchers.IO) {
        if (parentDirUri.scheme == "file") {
            val parentPath = parentDirUri.path ?: return@withContext null
            val dir = File(parentPath, name)
            if (dir.mkdirs() || dir.exists()) Uri.fromFile(dir) else null
        } else {
            val parent = getDocumentFile(parentDirUri) ?: return@withContext null
            val dir = parent.createDirectory(name)
            dir?.uri
        }
    }

    override suspend fun delete(targetUri: Uri): Boolean = withContext(Dispatchers.IO) {
        if (targetUri.scheme == "file") {
            val path = targetUri.path ?: return@withContext false
            val file = File(path)
            file.deleteRecursively()
        } else {
            val doc = getDocumentFile(targetUri) ?: return@withContext false
            doc.delete()
        }
    }

    override suspend fun rename(targetUri: Uri, newName: String): Boolean = withContext(Dispatchers.IO) {
        if (targetUri.scheme == "file") {
            val path = targetUri.path ?: return@withContext false
            val file = File(path)
            val dest = File(file.parentFile, newName)
            file.renameTo(dest)
        } else {
            val doc = getDocumentFile(targetUri) ?: return@withContext false
            doc.renameTo(newName)
        }
    }

    override suspend fun findDocumentByPath(treeUri: Uri, relativePath: String): DocumentFile? = withContext(Dispatchers.IO) {
        val rootDoc = getDocumentFile(treeUri) ?: return@withContext null
        val segments = relativePath.split('/', '\\').filter { it.isNotBlank() && it != "." }
        if (segments.isEmpty()) return@withContext rootDoc

        var current: DocumentFile = rootDoc
        for (segment in segments) {
            current = current.findFile(segment) ?: return@withContext null
        }
        current
    }

    override suspend fun findOrCreateDocumentByPath(treeUri: Uri, relativePath: String): DocumentFile? = withContext(Dispatchers.IO) {
        val rootDoc = getDocumentFile(treeUri) ?: return@withContext null
        val segments = relativePath.split('/', '\\').filter { it.isNotBlank() && it != "." }
        if (segments.isEmpty()) return@withContext rootDoc

        var current = rootDoc
        for (i in 0 until segments.size - 1) {
            val dirName = segments[i]
            var nextDir = current.findFile(dirName)
            if (nextDir == null || !nextDir.isDirectory) {
                nextDir = current.createDirectory(dirName) ?: return@withContext null
            }
            current = nextDir
        }

        val fileName = segments.last()
        val existing = current.findFile(fileName)
        if (existing != null) return@withContext existing

        val mimeType = resolveMimeType(fileName)
        current.createFile(mimeType, fileName)
    }

    override suspend fun listDirectory(treeUri: Uri, relativePath: String): List<FileNode> = withContext(Dispatchers.IO) {
        val dirDoc = findDocumentByPath(treeUri, relativePath) ?: return@withContext emptyList()
        if (!dirDoc.isDirectory) return@withContext emptyList()

        dirDoc.listFiles()
            .map { buildNode(it, 0) }
            .sortedWith(compareBy<FileNode> { !it.isDirectory }.thenBy { it.name.lowercase() })
    }

    override suspend fun searchFiles(
        treeUri: Uri,
        query: String,
        relativeScope: String?,
        maxResults: Int
    ): List<SearchMatch> = withContext(Dispatchers.IO) {
        val startDoc = if (!relativeScope.isNullOrBlank() && relativeScope != ".") {
            findDocumentByPath(treeUri, relativeScope) ?: return@withContext emptyList()
        } else {
            getDocumentFile(treeUri) ?: return@withContext emptyList()
        }

        val results = mutableListOf<SearchMatch>()
        fun searchRecursive(dir: DocumentFile, currentPath: String) {
            if (results.size >= maxResults) return
            val children = dir.listFiles()
            for (child in children) {
                val name = child.name ?: continue
                // Skip hidden and build directories
                if (child.isDirectory) {
                    if (name.startsWith(".") || name == "build" || name == "node_modules") continue
                    val nextPath = if (currentPath.isEmpty()) name else "$currentPath/$name"
                    searchRecursive(child, nextPath)
                } else {
                    if (name.startsWith(".")) continue
                    val ext = name.substringAfterLast('.', "").lowercase()
                    // Filter text-based extensions
                    val isText = ext in setOf(
                        "kt", "kts", "java", "py", "js", "ts", "json", "html", "css",
                        "xml", "md", "txt", "yaml", "yml", "gradle", "properties", "sh", "c", "cpp", "h"
                    )
                    if (!isText) continue

                    val filePath = if (currentPath.isEmpty()) name else "$currentPath/$name"
                    try {
                        val stream = if (child.uri.scheme == "file") {
                            val path = child.uri.path
                            if (path != null) java.io.FileInputStream(File(path)) else null
                        } else {
                            context.contentResolver.openInputStream(child.uri)
                        }
                        stream?.use { inputStream ->
                            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).useLines { lines ->
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
                        }
                    } catch (_: Exception) {
                        // Ignore unreadable files
                    }
                }
            }
        }

        searchRecursive(startDoc, "")
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
