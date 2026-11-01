package com.klix.code.data.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Lightweight service for checking available application updates against remote manifest.
 */
class UpdateChecker(
    private val callFactory: Call.Factory = defaultClient
) {
    companion object {
        const val DEFAULT_UPDATE_URL = "https://klixcode.vercel.app/version.json"
        private const val PREFS_NAME = "klix_update_prefs"
        private const val KEY_LAST_CHECK_TIMESTAMP = "last_check_timestamp"
        private const val KEY_POSTPONED_VERSION = "postponed_version_code"

        private val defaultClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()
        }

        fun shouldCheckAutomatically(context: Context, intervalHours: Long = 6): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastCheck = prefs.getLong(KEY_LAST_CHECK_TIMESTAMP, 0L)
            val intervalMillis = TimeUnit.HOURS.toMillis(intervalHours)
            return (System.currentTimeMillis() - lastCheck) >= intervalMillis
        }

        fun recordCheckTimestamp(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putLong(KEY_LAST_CHECK_TIMESTAMP, System.currentTimeMillis()).apply()
        }

        fun postponeVersion(context: Context, versionCode: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_POSTPONED_VERSION, versionCode).apply()
        }

        fun isVersionPostponed(context: Context, versionCode: Int): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(KEY_POSTPONED_VERSION, -1) == versionCode
        }

        fun clearPostponed(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(KEY_POSTPONED_VERSION).apply()
        }
    }

    suspend fun checkForUpdate(
        currentVersionCode: Int,
        currentVersionName: String,
        endpointUrl: String = DEFAULT_UPDATE_URL
    ): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(endpointUrl)
                .header("User-Agent", "KlixCode-AppUpdater/$currentVersionName ($currentVersionCode)")
                .header("Cache-Control", "no-cache")
                .build()

            val response = callFactory.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("Update check failed with HTTP ${response.code}: ${response.message}")
                )
            }

            val bodyString = response.body?.string()
                ?: return@withContext Result.failure(IOException("Empty update manifest received"))

            val updateInfo = UpdateInfo.fromJson(
                jsonStr = bodyString,
                currentVersionCode = currentVersionCode,
                currentVersionName = currentVersionName
            )
            Result.success(updateInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
