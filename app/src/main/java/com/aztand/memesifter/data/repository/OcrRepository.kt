package com.aztand.memesifter.data.repository

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class OcrRepository(private val context: Context) {

    // 初始化中文识别器 (支持中文 + 英文)
    // 第一次调用时会加载模型，可能会稍微慢一点点
    private val recognizer = TextRecognition.getClient(
        ChineseTextRecognizerOptions.Builder().build()
    )

    // 挂起函数：传入图片 Uri，返回识别到的文字字符串
    suspend fun recognizeText(uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            // .await() 是 kotlinx-coroutines-play-services 提供的扩展，把 Task 转为协程
            val result = recognizer.process(image).await()

            // 返回识别到的所有文本，如果没有字则返回空串
            result.text
        } catch (e: Exception) {
            e.printStackTrace()
            "" // 发生错误（如图片损坏）时返回空，保证程序不崩
        }
    }
}