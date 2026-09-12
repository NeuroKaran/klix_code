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
import java.io.InputStreamReader

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
}

class WorkspaceRepositoryImpl(
    private val context: Context
) : WorkspaceRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("klix_workspace_prefs", Context.MODE_PRIVATE)

    override suspend fun persistProjectPermission(uri: Uri, name: String) = withContext(Dispatchers.IO) {
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (_: SecurityException) {
            // Might already be persisted or transient
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

    override suspend fun loadTree(treeUri: Uri): FileNode? = withContext(Dispatchers.IO) {
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext null
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
        context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                reader.readText()
            }
        } ?: ""
    }

    override suspend fun writeFile(fileUri: Uri, content: String) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(fileUri, "wt")?.use { outputStream ->
            outputStream.write(content.toByteArray(Charsets.UTF_8))
            outputStream.flush()
        } ?: throw IllegalStateException("Unable to open output stream for $fileUri")
        StreakManager.recordActivity(context)
    }

    override suspend fun createFile(parentDirUri: Uri, name: String): Uri? = withContext(Dispatchers.IO) {
        val parent = DocumentFile.fromTreeUri(context, parentDirUri)
            ?: DocumentFile.fromSingleUri(context, parentDirUri)
            ?: return@withContext null
        val ext = name.substringAfterLast('.', "")
        val mimeType = when (ext.lowercase()) {
            "kt", "kts", "java" -> "text/x-kotlin"
            "py" -> "text/x-python"
            "js" -> "application/javascript"
            "ts" -> "application/typescript"
            "json" -> "application/json"
            "html" -> "text/html"
            "xml" -> "text/xml"
            "md" -> "text/markdown"
            else -> "text/plain"
        }
        val file = parent.createFile(mimeType, name)
        file?.uri
    }

    override suspend fun createDirectory(parentDirUri: Uri, name: String): Uri? = withContext(Dispatchers.IO) {
        val parent = DocumentFile.fromTreeUri(context, parentDirUri)
            ?: DocumentFile.fromSingleUri(context, parentDirUri)
            ?: return@withContext null
        val dir = parent.createDirectory(name)
        dir?.uri
    }

    override suspend fun delete(targetUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val doc = DocumentFile.fromSingleUri(context, targetUri)
            ?: DocumentFile.fromTreeUri(context, targetUri)
            ?: return@withContext false
        doc.delete()
    }

    override suspend fun rename(targetUri: Uri, newName: String): Boolean = withContext(Dispatchers.IO) {
        val doc = DocumentFile.fromSingleUri(context, targetUri)
            ?: DocumentFile.fromTreeUri(context, targetUri)
            ?: return@withContext false
        doc.renameTo(newName)
    }
}
