package com.klix.code.data.agent.session

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

interface ChatSessionRepository {
    suspend fun getSessions(): List<ChatSession>
    suspend fun saveSessions(sessions: List<ChatSession>)
    suspend fun saveSession(session: ChatSession)
    suspend fun deleteSession(sessionId: String)
    suspend fun getActiveSessionId(): String?
    suspend fun setActiveSessionId(sessionId: String?)
}

class ChatSessionRepositoryImpl(
    private val context: Context
) : ChatSessionRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("klix_chat_sessions_prefs", Context.MODE_PRIVATE)

    private val sessionsFile: File
        get() = File(context.filesDir, "agent_sessions.json")

    override suspend fun getSessions(): List<ChatSession> = withContext(Dispatchers.IO) {
        val file = sessionsFile
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

    override suspend fun saveSessions(sessions: List<ChatSession>) = withContext(Dispatchers.IO) {
        try {
            val jsonArray = JSONArray()
            for (session in sessions) {
                jsonArray.put(session.toJson())
            }
            val tempFile = File(context.filesDir, "agent_sessions.json.tmp")
            tempFile.writeText(jsonArray.toString(), Charsets.UTF_8)
            if (tempFile.exists()) {
                tempFile.renameTo(sessionsFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun saveSession(session: ChatSession) = withContext(Dispatchers.IO) {
        val currentSessions = getSessions().toMutableList()
        val index = currentSessions.indexOfFirst { it.id == session.id }
        if (index >= 0) {
            currentSessions[index] = session
        } else {
            currentSessions.add(0, session)
        }
        saveSessions(currentSessions)
    }

    override suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        val currentSessions = getSessions().filter { it.id != sessionId }
        saveSessions(currentSessions)
        if (getActiveSessionId() == sessionId) {
            setActiveSessionId(currentSessions.firstOrNull()?.id)
        }
    }

    override suspend fun getActiveSessionId(): String? = withContext(Dispatchers.IO) {
        prefs.getString("active_session_id", null)
    }

    override suspend fun setActiveSessionId(sessionId: String?) = withContext(Dispatchers.IO) {
        prefs.edit().apply {
            if (sessionId == null) {
                remove("active_session_id")
            } else {
                putString("active_session_id", sessionId)
            }
        }.apply()
    }
}
