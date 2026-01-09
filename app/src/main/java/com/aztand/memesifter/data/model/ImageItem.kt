package com.aztand.memesifter.data.model

import android.net.Uri

data class MemeImage(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateAdded: Long,
    var ocrText: String? = null,    // 存储识别出的文字
    var isSelected: Boolean = false // 用于 UI 层的勾选状态
)

data class ImageItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val dateAdded: Long,
    val ocrText: String = ""
)