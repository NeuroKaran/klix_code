package com.klix.code

import com.klix.code.data.update.UpdateChecker
import com.klix.code.data.update.UpdateInfo
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import kotlinx.coroutines.runBlocking

class UpdateCheckerTest {

    @Test
    fun testUpdateInfoFromJson_UpdateAvailable() {
        val json = """
            {
                "versionCode": 7,
                "versionName": "0.3.7",
                "minSupportedVersionCode": 5,
                "apkUrl": "https://klixcode.vercel.app/klix-code-v0.3.7.apk",
                "releaseNotes": "• New terminal features\n• Bug fixes",
                "releaseDate": "2026-11-02",
                "forceUpdate": false
            }
        """.trimIndent()

        val info = UpdateInfo.fromJson(json, currentVersionCode = 6, currentVersionName = "0.3.6")

        assertEquals(7, info.latestVersionCode)
        assertEquals("0.3.7", info.latestVersionName)
        assertEquals(5, info.minSupportedVersionCode)
        assertEquals("https://klixcode.vercel.app/klix-code-v0.3.7.apk", info.apkUrl)
        assertTrue(info.isUpdateAvailable)
        assertFalse(info.forceUpdate)
        assertEquals("0.3.6", info.currentVersionName)
        assertEquals(6, info.currentVersionCode)
    }

    @Test
    fun testUpdateInfoFromJson_UpToDate() {
        val json = """
            {
                "versionCode": 6,
                "versionName": "0.3.6",
                "apkUrl": "https://klixcode.vercel.app/klix-code-v0.3.6.apk",
                "releaseNotes": "Current version",
                "forceUpdate": false
            }
        """.trimIndent()

        val info = UpdateInfo.fromJson(json, currentVersionCode = 6, currentVersionName = "0.3.6")

        assertEquals(6, info.latestVersionCode)
        assertEquals("0.3.6", info.latestVersionName)
        assertFalse(info.isUpdateAvailable)
    }

    @Test
    fun testUpdateInfoFromJson_ForceUpdateDueToMinSupported() {
        val json = """
            {
                "versionCode": 8,
                "versionName": "0.4.0",
                "minSupportedVersionCode": 7,
                "apkUrl": "https://klixcode.vercel.app/klix-code-v0.4.0.apk",
                "forceUpdate": false
            }
        """.trimIndent()

        // Current version is 6, which is below minSupportedVersionCode 7
        val info = UpdateInfo.fromJson(json, currentVersionCode = 6, currentVersionName = "0.3.6")

        assertTrue(info.isUpdateAvailable)
        assertTrue(info.forceUpdate)
    }

    @Test
    fun testUpdateCheckerService_SuccessResponse() = runBlocking {
        val jsonResponse = """
            {
                "versionCode": 10,
                "versionName": "0.4.5",
                "apkUrl": "https://klixcode.vercel.app/klix-code-v0.4.5.apk",
                "releaseNotes": "Major upgrade"
            }
        """.trimIndent()

        val mockCall = mock(Call::class.java)

        val response = Response.Builder()
            .request(Request.Builder().url("https://klixcode.vercel.app/version.json").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(jsonResponse.toResponseBody("application/json".toMediaType()))
            .build()

        `when`(mockCall.execute()).thenReturn(response)

        val checker = UpdateChecker(callFactory = { mockCall })
        val result = checker.checkForUpdate(currentVersionCode = 6, currentVersionName = "0.3.6")

        assertTrue(result.isSuccess)
        val info = result.getOrNull()
        org.junit.Assert.assertNotNull(info)
        assertEquals(10, info!!.latestVersionCode)
        assertEquals("0.4.5", info.latestVersionName)
        assertTrue(info.isUpdateAvailable)
    }

    @Test
    fun testUpdateCheckerService_HttpErrorResponse() = runBlocking {
        val mockCall = mock(Call::class.java)

        val response = Response.Builder()
            .request(Request.Builder().url("https://klixcode.vercel.app/version.json").build())
            .protocol(Protocol.HTTP_1_1)
            .code(500)
            .message("Internal Server Error")
            .body("".toResponseBody("application/json".toMediaType()))
            .build()

        `when`(mockCall.execute()).thenReturn(response)

        val checker = UpdateChecker(callFactory = { mockCall })
        val result = checker.checkForUpdate(currentVersionCode = 6, currentVersionName = "0.3.6")

        assertTrue(result.isFailure)
    }
}
