package com.klix.code.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Handles downloading APK binaries with stream progress and initiating Android package installs.
 */
class UpdateDownloader(
    private val callFactory: Call.Factory = defaultClient
) {
    companion object {
        private val defaultClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()
        }
    }

    suspend fun downloadApk(
        context: Context,
        updateInfo: UpdateInfo,
        onProgress: (progressPercent: Int, bytesRead: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (updateInfo.apkUrl.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("APK download URL is blank"))
            }

            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val sanitizedName = updateInfo.latestVersionName.replace(Regex("[^a-zA-Z0-9.-]"), "_")
            val targetFile = File(updatesDir, "klix-code-$sanitizedName.apk")

            // If file already exists and is valid size, we can check or delete to ensure clean copy
            if (targetFile.exists()) {
                targetFile.delete()
            }

            val request = Request.Builder()
                .url(updateInfo.apkUrl)
                .header("User-Agent", "KlixCode-AppUpdater")
                .build()

            val response = callFactory.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("Download failed with HTTP ${response.code}: ${response.message}")
                )
            }

            val body = response.body
                ?: return@withContext Result.failure(IOException("Response body is null"))

            val totalBytes = body.contentLength()
            var bytesReadTotal = 0L

            body.byteStream().use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    val buffer = ByteArray(8 * 1024)
                    var read: Int
                    var lastPercentReported = -1

                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                        bytesReadTotal += read

                        val percent = if (totalBytes > 0) {
                            ((bytesReadTotal * 100) / totalBytes).toInt().coerceIn(0, 100)
                        } else {
                            -1
                        }

                        if (percent != lastPercentReported) {
                            lastPercentReported = percent
                            onProgress(percent, bytesReadTotal, totalBytes)
                        }
                    }
                    outputStream.flush()
                }
            }

            if (!targetFile.exists() || targetFile.length() == 0L) {
                return@withContext Result.failure(IOException("Downloaded APK file is empty or missing"))
            }

            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun installApk(context: Context, apkFile: File): Result<Unit> {
        return try {
            if (!apkFile.exists()) {
                return Result.failure(IOException("APK file does not exist: ${apkFile.absolutePath}"))
            }

            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun openInBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Safe fallback
        }
    }
}
