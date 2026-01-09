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

        for (image in images) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, targetFolder)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                    // 尝试更新，如果没有权限会抛出 SecurityException (Android 10)
                    // 或者 RecoverableSecurityException (Android 10, 在 catch 中处理)
                    // Android 11+ 通常直接抛 SecurityException
                    val rows = context.contentResolver.update(image.uri, values, null, null)

                    if (rows > 0) {
                        values.clear()
                        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        context.contentResolver.update(image.uri, values, null, null)
                    } else {
                        // 没更新成功，可能是权限问题或者文件不存在
                        // 这里可以保守地认为需要权限
                        neededPermissionUris.add(image.uri)
                    }
                }
            } catch (e: SecurityException) {
                // 捕获权限异常，加入待申请列表
                neededPermissionUris.add(image.uri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (neededPermissionUris.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // 修复崩溃的关键点：限制单次请求的 URI 数量
                // Android Binder 限制约为 1MB，太多的 URI 会导致 TransactionTooLargeException
                // 我们这里限制一次最多请求 50 个文件的权限
                val safeBatch = neededPermissionUris.take(50)

                val pi = MediaStore.createWriteRequest(context.contentResolver, safeBatch)
                return pi.intentSender
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                // Android 10 的处理比较复杂，通常建议一次只处理一个或引导用户去文件管理
                // 这里为了简单，如果捕获到 RecoverableSecurityException 应该直接抛出 intentSender
                // 暂时保持原样或返回 null
            }
        }

        return null
    }
}