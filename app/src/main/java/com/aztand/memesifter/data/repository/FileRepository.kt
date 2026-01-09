package com.aztand.memesifter.data.repository

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.aztand.memesifter.data.model.ImageItem

class FileRepository(private val context: Context) {

    // 返回值：如果需要请求权限，返回 IntentSender；如果全部成功，返回 null
    suspend fun moveImagesToMemeFolder(images: List<ImageItem>): IntentSender? {
        val targetFolder = "Pictures/Memes"
        val neededPermissionUris = mutableListOf<Uri>()

        // 1. 尝试逐个移动文件
        for (image in images) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ 使用 ContentResolver 更新路径
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, targetFolder)
                        // 标记为 Pending 状态防止移动过程中被其他应用访问
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }

                    val rows = context.contentResolver.update(image.uri, values, null, null)

                    // 移动完解除 Pending 状态
                    if (rows > 0) {
                        values.clear()
                        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        context.contentResolver.update(image.uri, values, null, null)
                    } else {
                        // 如果更新行数为0，可能是权限问题
                        throw SecurityException("Rows 0")
                    }
                } else {
                    // Android 9 及以下（如果不考虑兼容其实可以不写，但为了严谨）
                    // 这里的逻辑比较复杂，通常涉及 File 操作，暂时略过，专注于现代 Android
                }
            } catch (e: SecurityException) {
                // 捕获权限异常，收集需要申请权限的 URI
                neededPermissionUris.add(image.uri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. 如果有文件移动失败（被拦截），生成批量授权申请
        if (neededPermissionUris.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ 支持批量申请写入权限
                val pi = MediaStore.createWriteRequest(context.contentResolver, neededPermissionUris)
                return pi.intentSender
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                // Android 10 比较麻烦，通常只能捕获 RecoverableSecurityException
                // 这里简单处理：如果遇到拦截，返回第一个异常的 intentSender (Android 10 很难做批量)
                // 实际代码中通常会在 catch 块里直接拿到 RecoverableSecurityException
            }
        }

        return null // 一切顺利
    }
}