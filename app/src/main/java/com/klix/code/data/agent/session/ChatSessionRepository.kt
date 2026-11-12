package com.klix.code.data.agent.session

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

interface ChatSessionRepository {
    suspend fun getSessions(workspaceId: String = "default_workspace"): List<ChatSession>
    suspend fun saveSessions(workspaceId: String = "default_workspace", sessions: List<ChatSession>)
    suspend fun saveSession(workspaceId: String = "default_workspace", session: ChatSession)
    suspend fun deleteSession(workspaceId: String = "default_workspace", sessionId: String)
    suspend fun getActiveSessionId(workspaceId: String = "default_workspace"): String?
    suspend fun setActiveSessionId(workspaceId: String = "default_workspace", sessionId: String?)
    suspend fun onWorkspaceDeleted(workspaceId: String)
    fun setWorkspaceDirectoryProvider(provider: ((String) -> File?)?)
}

class ChatSessionRepositoryImpl(
    private val context: Context,
    private var workspaceDirectoryProvider: ((String) -> File?)? = null
) : ChatSessionRepository {

    private fun getPrefs(): SharedPreferences? {
        return try {
            context.getSharedPreferences("klix_chat_sessions_prefs", Context.MODE_PRIVATE)
        } catch (_: Exception) {
            null
        }
    }

    override fun setWorkspaceDirectoryProvider(provider: ((String) -> File?)?) {
        workspaceDirectoryProvider = provider
    }

    private fun getSessionsFile(workspaceId: String): File {
        val dir = workspaceDirectoryProvider?.invoke(workspaceId)
        if (dir != null) {
            val klixDir = File(dir, ".klix").apply { if (!exists()) mkdirs() }
            return File(klixDir, "chat_sessions.json")
        }

        // Direct fallback: check sandbox rootfs workspaces by slug or registry lookup
        val sandboxWorkspacesDir = File(context.filesDir, "sandbox/rootfs/workspaces")
        if (sandboxWorkspacesDir.exists()) {
            var dirName: String? = if (workspaceId == "default_workspace") "default" else null
            if (dirName == null) {
                val registryFile = File(context.filesDir, "sandbox_workspaces_registry.json")
                if (registryFile.exists()) {
                    try {
                        val arr = JSONArray(registryFile.readText(Charsets.UTF_8))
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i)
                            if (obj?.optString("id") == workspaceId) {
                                dirName = obj.optString("directoryName")
                                break
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
            if (dirName == null) {
                val candidateDir = File(sandboxWorkspacesDir, workspaceId)
                if (candidateDir.exists() && candidateDir.isDirectory) {
                    dirName = workspaceId
                }
            }
            if (dirName != null) {
                val candidate = File(sandboxWorkspacesDir, dirName)
                if (candidate.exists() || candidate.mkdirs()) {
                    val klixDir = File(candidate, ".klix").apply { if (!exists()) mkdirs() }
                    return File(klixDir, "chat_sessions.json")
                }
            }
        }

        val fallbackDir = File(context.filesDir, "sandbox_chat/$workspaceId").apply { if (!exists()) mkdirs() }
        return File(fallbackDir, "chat_sessions.json")
    }

    private fun migrateLegacySessionsIfNeeded(targetFile: File, workspaceId: String) {
        if (workspaceId == "default_workspace" && !targetFile.exists()) {
            val legacyFile = File(context.filesDir, "agent_sessions.json")
            if (legacyFile.exists()) {
                try {
                    legacyFile.copyTo(targetFile, overwrite = false)
                } catch (_: Exception) {}
            }
        }
    }

    override suspend fun getSessions(workspaceId: String): List<ChatSession> = withContext(Dispatchers.IO) {
        val file = getSessionsFile(workspaceId)
        migrateLegacySessionsIfNeeded(file, workspaceId)
        if (!file.exists()) return@withContext emptyList()

        return@withContext try {
            val jsonText = file.readText(Charsets.UTF_8)
            val jsonArray = JSONArray(jsonText)
            val list = mutableListOf<ChatSession>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                list.add(ChatSession.fromJson(obj))
            }
            // Sort by most recently updated
            list.sortedByDescending { it.updatedAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun saveSessions(workspaceId: String, sessions: List<ChatSession>) = withContext(Dispatchers.IO) {
        try {
            val file = getSessionsFile(workspaceId)
            val jsonArray = JSONArray()
            for (session in sessions) {
                jsonArray.put(session.toJson())
            }
            val tempFile = File(file.parentFile, "${file.name}.tmp")
            tempFile.writeText(jsonArray.toString(2), Charsets.UTF_8)
            if (tempFile.exists()) {
                tempFile.renameTo(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun saveSession(workspaceId: String, session: ChatSession) = withContext(Dispatchers.IO) {
        val currentSessions = getSessions(workspaceId).toMutableList()
        val index = currentSessions.indexOfFirst { it.id == session.id }
        if (index >= 0) {
            currentSessions[index] = session
        } else {
            currentSessions.add(0, session)
        }
        saveSessions(workspaceId, currentSessions)
    }

    override suspend fun deleteSession(workspaceId: String, sessionId: String) = withContext(Dispatchers.IO) {
        val currentSessions = getSessions(workspaceId).filter { it.id != sessionId }
        saveSessions(workspaceId, currentSessions)
        if (getActiveSessionId(workspaceId) == sessionId) {
            setActiveSessionId(workspaceId, currentSessions.firstOrNull()?.id)
        }
    }

    private val inMemoryActiveSessions = mutableMapOf<String, String>()

    override suspend fun getActiveSessionId(workspaceId: String): String? = withContext(Dispatchers.IO) {
        val prefs = getPrefs()
        val active = prefs?.getString("active_session_id_${workspaceId}", null) ?: inMemoryActiveSessions[workspaceId]
        if (active == null && workspaceId == "default_workspace") {
            prefs?.getString("active_session_id", null) ?: inMemoryActiveSessions["default_workspace"]
        } else {
            active
        }
    }

    override suspend fun setActiveSessionId(workspaceId: String, sessionId: String?) = withContext(Dispatchers.IO) {
        if (sessionId == null) {
            inMemoryActiveSessions.remove(workspaceId)
        } else {
            inMemoryActiveSessions[workspaceId] = sessionId
        }
        getPrefs()?.edit()?.apply {
            if (sessionId == null) {
                remove("active_session_id_${workspaceId}")
            } else {
                putString("active_session_id_${workspaceId}", sessionId)
            }
        }?.apply()
        Unit
    }

    override suspend fun onWorkspaceDeleted(workspaceId: String) {
        withContext(Dispatchers.IO) {
            inMemoryActiveSessions.remove(workspaceId)
            getPrefs()?.edit()?.apply {
                remove("active_session_id_${workspaceId}")
            }?.apply()
            val fallbackDir = File(context.filesDir, "sandbox_chat/$workspaceId")
            if (fallbackDir.exists()) {
                fallbackDir.deleteRecursively()
            }
        }
    }
}
