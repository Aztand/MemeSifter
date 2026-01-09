package com.aztand.memesifter.data.repository

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.aztand.memesifter.data.model.ImageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageRepository(private val context: Context) {

    suspend fun getAllImages(): List<ImageItem> = withContext(Dispatchers.IO) {
        val imageList = mutableListOf<ImageItem>()

        // 1. 动态构建查询字段，Android 10 (Q) 以上才查路径
        val projectionList = mutableListOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projectionList.add(MediaStore.Images.Media.RELATIVE_PATH)
        }

        val projection = projectionList.toTypedArray()
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            // 尝试获取路径列的索引 (如果不是 Android 10+，或者是旧设备，这个可能是 -1)
            val pathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
            } else -1

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val dateAdded = cursor.getLong(dateColumn)

                // 2. 核心过滤逻辑：如果在 Memes 文件夹，直接跳过
                if (pathColumn != -1) {
                    val path = cursor.getString(pathColumn)
                    // 只要路径包含我们设定的目标文件夹，就视为已处理
                    if (path != null && path.contains("Pictures/Memes")) {
                        continue
                    }
                }

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                imageList.add(ImageItem(id, contentUri, name, dateAdded))
            }
        }
        return@withContext imageList
    }
}