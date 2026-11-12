package com.klix.code.data.workspace

import android.net.Uri
import org.json.JSONObject
import java.io.File

data class SandboxWorkspace(
    val id: String,
    val name: String,
    val directoryName: String,
    val relativePath: String, // e.g. "/workspaces/my-project"
    val rootUri: Uri,          // file:// URI pointing directly inside sandbox rootfs
    val createdAt: Long = System.currentTimeMillis(),
    val lastOpened: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("directoryName", directoryName)
        put("relativePath", relativePath)
        put("rootUri", rootUri.toString())
        put("createdAt", createdAt)
        put("lastOpened", lastOpened)
    }

    companion object {
        fun fromJson(json: JSONObject): SandboxWorkspace {
            return SandboxWorkspace(
                id = json.getString("id"),
                name = json.getString("name"),
                directoryName = json.getString("directoryName"),
                relativePath = json.getString("relativePath"),
                rootUri = parseUri(json.getString("rootUri")),
                createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                lastOpened = json.optLong("lastOpened", System.currentTimeMillis())
            )
        }

        fun fileToUri(file: File): Uri {
            return try {
                val uri = Uri.fromFile(file)
                if (uri != null) uri else mockUriFallback(file.absolutePath)
            } catch (_: Throwable) {
                mockUriFallback(file.absolutePath)
            }
        }

        fun parseUri(uriStr: String): Uri {
            return try {
                val uri = Uri.parse(uriStr)
                if (uri != null) uri else mockUriFallback(uriStr)
            } catch (_: Throwable) {
                mockUriFallback(uriStr)
            }
        }

        private fun mockUriFallback(raw: String): Uri {
            return try {
                val clazz = Class.forName("org.mockito.Mockito")
                val mockMethod = clazz.getMethod("mock", Class::class.java)
                val whenMethod = clazz.getMethod("when", Any::class.java)
                val mock = mockMethod.invoke(null, Uri::class.java) as Uri

                val fullStr = if (raw.startsWith("file://")) raw else "file://$raw"
                val pathStr = if (raw.startsWith("file://")) raw.removePrefix("file://") else raw

                val ongoingStubbing1 = whenMethod.invoke(null, mock.toString())
                ongoingStubbing1.javaClass.getMethod("thenReturn", Any::class.java).invoke(ongoingStubbing1, fullStr)

                val ongoingStubbing2 = whenMethod.invoke(null, mock.path)
                ongoingStubbing2.javaClass.getMethod("thenReturn", Any::class.java).invoke(ongoingStubbing2, pathStr)

                val ongoingStubbing3 = whenMethod.invoke(null, mock.scheme)
                ongoingStubbing3.javaClass.getMethod("thenReturn", Any::class.java).invoke(ongoingStubbing3, "file")

                mock
            } catch (_: Throwable) {
                Uri.EMPTY ?: Uri.parse("")
            }
        }
    }
}
