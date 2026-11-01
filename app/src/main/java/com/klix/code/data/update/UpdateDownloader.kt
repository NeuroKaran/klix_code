package com.klix.code.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Handles launching the safe browser-based APK download/update intent.
 * By delegating download and package installation to the user's browser,
 * the app avoids requiring high-risk installer permissions (e.g. REQUEST_INSTALL_PACKAGES),
 * which triggers Google Play Protect Potentially Harmful Application (PHA) flags.
 */
class UpdateDownloader {

    fun openInBrowser(context: Context, url: String): Result<Unit> {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
