package com.klix.code.data.workspace

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.klix.code.data.sandbox.SandboxManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.util.UUID

interface SandboxWorkspaceManager {
    val activeWorkspace: StateFlow<SandboxWorkspace?>
    val workspaces: StateFlow<List<SandboxWorkspace>>

    suspend fun initialize()
    suspend fun getWorkspaces(): List<SandboxWorkspace>
    suspend fun createWorkspace(name: String): Result<SandboxWorkspace>
    suspend fun switchWorkspace(workspaceId: String): SandboxWorkspace?
    suspend fun deleteWorkspace(workspaceId: String): Boolean
    suspend fun renameWorkspace(workspaceId: String, newName: String): Boolean
    fun getWorkspaceDirectory(workspace: SandboxWorkspace): File
    fun getWorkspaceDirectoryById(workspaceId: String): File?
}

class SandboxWorkspaceManagerImpl(
    private val context: Context,
    val sandboxManager: SandboxManager = SandboxManager(context)
) : SandboxWorkspaceManager {

    private val _workspaces = MutableStateFlow<List<SandboxWorkspace>>(emptyList())
    override val workspaces: StateFlow<List<SandboxWorkspace>> = _workspaces.asStateFlow()

    private val _activeWorkspace = MutableStateFlow<SandboxWorkspace?>(null)
    override val activeWorkspace: StateFlow<SandboxWorkspace?> = _activeWorkspace.asStateFlow()

    private val registryFile: File
        get() = File(context.filesDir, "sandbox_workspaces_registry.json")

    private val workspacesRootDir: File
        get() = File(sandboxManager.getRootfsDir(), "workspaces").apply { if (!exists()) mkdirs() }

    private fun getPrefs(): SharedPreferences? {
        return try {
            context.getSharedPreferences("klix_workspace_state", Context.MODE_PRIVATE)
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun initialize() = withContext(Dispatchers.IO) {
        migrateLegacyWorkspaceIfNeeded()
        val list = loadRegistry().toMutableList()
        if (list.isEmpty()) {
            val defaultWs = createDefaultWorkspaceInternal()
            list.add(defaultWs)
            saveRegistry(list)
        }
        // Reconcile and ensure physical workspace directories and .klix directories exist on disk
        list.forEach { ws ->
            val dir = File(workspacesRootDir, ws.directoryName)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val klixDir = File(dir, ".klix")
            if (!klixDir.exists()) {
                klixDir.mkdirs()
            }
        }
        _workspaces.value = list.sortedByDescending { it.lastOpened }

        val lastActiveId = getPrefs()?.getString("last_active_workspace_id", null) ?: inMemoryLastActiveId
        val selected = list.find { it.id == lastActiveId } ?: list.first()
        _activeWorkspace.value = selected
    }

    override suspend fun getWorkspaces(): List<SandboxWorkspace> = withContext(Dispatchers.IO) {
        if (_workspaces.value.isEmpty()) {
            val list = loadRegistry()
            if (list.isNotEmpty()) {
                _workspaces.value = list.sortedByDescending { it.lastOpened }
            }
        }
        _workspaces.value
    }

    override suspend fun createWorkspace(name: String): Result<SandboxWorkspace> = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Workspace name cannot be empty"))
        }

        val existing = if (_workspaces.value.isEmpty()) loadRegistry() else _workspaces.value
        val existingSlugs = existing.map { it.directoryName }.toSet()

        val sanitized = sanitizeDirectoryName(trimmed)
        var candidateSlug = sanitized
        var targetDir = File(workspacesRootDir, candidateSlug)
        var count = 2
        while (targetDir.exists() || existingSlugs.contains(candidateSlug)) {
            candidateSlug = "$sanitized-$count"
            targetDir = File(workspacesRootDir, candidateSlug)
            count++
        }
        targetDir.mkdirs()

        // Create metadata directory
        File(targetDir, ".klix").apply { if (!exists()) mkdirs() }

        val newWs = SandboxWorkspace(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            directoryName = targetDir.name,
            relativePath = "/workspaces/${targetDir.name}",
            rootUri = SandboxWorkspace.fileToUri(targetDir),
            createdAt = System.currentTimeMillis(),
            lastOpened = System.currentTimeMillis()
        )

        val updated = (existing + newWs).sortedByDescending { it.lastOpened }
        _workspaces.value = updated
        _activeWorkspace.value = newWs
        saveRegistry(updated)
        persistActiveWorkspaceId(newWs.id)

        Result.success(newWs)
    }

    override suspend fun switchWorkspace(workspaceId: String): SandboxWorkspace? = withContext(Dispatchers.IO) {
        val list = (if (_workspaces.value.isEmpty()) loadRegistry() else _workspaces.value).toMutableList()
        val index = list.indexOfFirst { it.id == workspaceId }
        if (index == -1) return@withContext null

        val current = list[index]
        val updatedWs = current.copy(lastOpened = System.currentTimeMillis())
        list[index] = updatedWs
        val sorted = list.sortedByDescending { it.lastOpened }

        _workspaces.value = sorted
        _activeWorkspace.value = updatedWs
        saveRegistry(sorted)
        persistActiveWorkspaceId(updatedWs.id)

        updatedWs
    }

    override suspend fun deleteWorkspace(workspaceId: String): Boolean = withContext(Dispatchers.IO) {
        val list = (if (_workspaces.value.isEmpty()) loadRegistry() else _workspaces.value).toMutableList()
        val target = list.find { it.id == workspaceId } ?: return@withContext false

        // Cannot delete if it is the only workspace
        if (list.size <= 1) return@withContext false

        // Delete physical directory inside rootfs safely without deleting root workspaces dir
        val dir = File(workspacesRootDir, target.directoryName).canonicalFile
        val rootDirCanonical = workspacesRootDir.canonicalFile
        if (dir != rootDirCanonical && dir.path.startsWith(rootDirCanonical.path + File.separator)) {
            if (dir.exists()) {
                dir.deleteRecursively()
            }
        }

        list.remove(target)
        _workspaces.value = list
        saveRegistry(list)

        if (_activeWorkspace.value?.id == workspaceId) {
            val next = list.first()
            _activeWorkspace.value = next
            persistActiveWorkspaceId(next.id)
        }

        true
    }

    override suspend fun renameWorkspace(workspaceId: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return@withContext false

        val list = (if (_workspaces.value.isEmpty()) loadRegistry() else _workspaces.value).toMutableList()
        val index = list.indexOfFirst { it.id == workspaceId }
        if (index == -1) return@withContext false

        val ws = list[index]
        val updated = ws.copy(name = trimmed)
        list[index] = updated

        _workspaces.value = list
        if (_activeWorkspace.value?.id == workspaceId) {
            _activeWorkspace.value = updated
        }
        saveRegistry(list)
        true
    }

    override fun getWorkspaceDirectory(workspace: SandboxWorkspace): File {
        return File(workspacesRootDir, workspace.directoryName)
    }

    override fun getWorkspaceDirectoryById(workspaceId: String): File? {
        val list = if (_workspaces.value.isEmpty()) loadRegistry() else _workspaces.value
        val ws = list.find { it.id == workspaceId }
        if (ws != null) {
            return getWorkspaceDirectory(ws)
        }
        if (workspaceId == "default_workspace") {
            return File(workspacesRootDir, "default")
        }
        return null
    }

    fun sanitizeDirectoryName(name: String): String {
        return name.lowercase()
            .replace(Regex("[^a-z0-9_-]"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
            .ifEmpty { "workspace" }
    }

    private var inMemoryLastActiveId: String? = null

    private fun persistActiveWorkspaceId(id: String) {
        inMemoryLastActiveId = id
        try {
            getPrefs()?.edit()?.putString("last_active_workspace_id", id)?.apply()
        } catch (_: Exception) {}
    }

    private fun migrateLegacyWorkspaceIfNeeded() {
        val legacyDir = File(sandboxManager.getRootfsDir(), "workspace")
        val defaultDir = File(workspacesRootDir, "default")
        if (legacyDir.exists() && legacyDir.isDirectory && !defaultDir.exists()) {
            legacyDir.renameTo(defaultDir)
        }
    }

    private fun createDefaultWorkspaceInternal(): SandboxWorkspace {
        val defaultDir = File(workspacesRootDir, "default").apply { if (!exists()) mkdirs() }
        File(defaultDir, ".klix").apply { if (!exists()) mkdirs() }
        return SandboxWorkspace(
            id = "default_workspace",
            name = "Default Workspace",
            directoryName = "default",
            relativePath = "/workspaces/default",
            rootUri = SandboxWorkspace.fileToUri(defaultDir),
            createdAt = System.currentTimeMillis(),
            lastOpened = System.currentTimeMillis()
        )
    }

    private fun loadRegistry(): List<SandboxWorkspace> {
        if (!registryFile.exists()) return emptyList()
        return try {
            val content = registryFile.readText(Charsets.UTF_8)
            val jsonArray = JSONArray(content)
            val list = mutableListOf<SandboxWorkspace>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(SandboxWorkspace.fromJson(obj))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveRegistry(list: List<SandboxWorkspace>) {
        try {
            val jsonArray = JSONArray()
            list.forEach { jsonArray.put(it.toJson()) }
            registryFile.writeText(jsonArray.toString(2), Charsets.UTF_8)
        } catch (_: Exception) {}
    }
}
