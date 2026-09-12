package com.klix.code.data.model

import android.net.Uri

data class ProjectItem(
    val uri: Uri,
    val name: String,
    val lastOpened: Long = System.currentTimeMillis()
)
