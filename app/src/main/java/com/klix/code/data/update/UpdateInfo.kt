package com.klix.code.data.update

import org.json.JSONObject

/**
 * Metadata representing an app version update payload.
 */
data class UpdateInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val minSupportedVersionCode: Int = 1,
    val apkUrl: String,
    val releaseNotes: String = "",
    val releaseDate: String = "",
    val forceUpdate: Boolean = false,
    val isUpdateAvailable: Boolean = false,
    val currentVersionCode: Int = 0,
    val currentVersionName: String = ""
) {
    companion object {
        fun fromJson(jsonStr: String, currentVersionCode: Int, currentVersionName: String): UpdateInfo {
            val json = JSONObject(jsonStr)
            val latestCode = json.optInt("versionCode", json.optInt("latestVersionCode", 0))
            val latestName = json.optString("versionName", json.optString("latestVersionName", ""))
            val minSupported = json.optInt("minSupportedVersionCode", 1)
            val apkUrl = json.optString("apkUrl", "")
            val releaseNotes = json.optString("releaseNotes", "")
            val releaseDate = json.optString("releaseDate", "")
            val force = json.optBoolean("forceUpdate", false) || (currentVersionCode < minSupported && minSupported > 1)

            val isAvailable = latestCode > currentVersionCode

            return UpdateInfo(
                latestVersionCode = latestCode,
                latestVersionName = latestName,
                minSupportedVersionCode = minSupported,
                apkUrl = apkUrl,
                releaseNotes = releaseNotes,
                releaseDate = releaseDate,
                forceUpdate = force,
                isUpdateAvailable = isAvailable,
                currentVersionCode = currentVersionCode,
                currentVersionName = currentVersionName
            )
        }
    }
}
