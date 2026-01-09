package com.aztand.memesifter.viewmodel

import android.app.Application
import android.content.IntentSender
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aztand.memesifter.data.model.ImageItem
import com.aztand.memesifter.data.repository.FileRepository
import com.aztand.memesifter.data.repository.ImageRepository
import com.aztand.memesifter.data.repository.OcrRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val imageRepository = ImageRepository(application)
    private val ocrRepository = OcrRepository(application)
    // 界面上显示的图片列表（只包含有字的）
    private val _images = MutableStateFlow<List<ImageItem>>(emptyList())
    val images: StateFlow<List<ImageItem>> = _images

    // 被用户选中的图片 ID
    val selectedIds = mutableStateListOf<Long>()

    private val fileRepository = FileRepository(application)
    private val _permissionRequest = MutableStateFlow<IntentSender?>(null)
    val permissionRequest: StateFlow<IntentSender?> = _permissionRequest
    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()
    fun scanImages() {
        viewModelScope.launch {
            val allRawImages = imageRepository.getAllImages()

            // ✅ 移除 .take(50)，对所有图片进行遍历
            allRawImages.forEach { item ->
                // 为了避免瞬间把 UI 线程卡死或内存爆掉，
                // 可以在这里加一点点延时，或者依靠协程的调度自动处理
                // 对于几千张图，目前的逐个 processImage 是安全的
                processImage(item)
            }
        }
    }

    private suspend fun processImage(item: ImageItem) {
        // 调用 OCR 识别
        val text = ocrRepository.recognizeText(item.uri)

        // 核心逻辑修改：只有识别出文字的图片，才有资格进入“前台”
        if (text.isNotBlank()) {
            val validMeme = item.copy(ocrText = text)

            // 将合格的图片追加到界面列表
            val currentList = _images.value.toMutableList()
            currentList.add(validMeme)
            _images.value = currentList

            // ⚠️ 注意：这里删除了 selectedIds.add(...) 代码
            // 我们不再自动勾选，把选择权完全交给用户
        }
    }

    // 手动切换选中状态
    fun toggleSelection(id: Long) {
        if (selectedIds.contains(id)) {
            selectedIds.remove(id)
        } else {
            selectedIds.add(id)
        }
    }



    // 全选功能（可选，方便用户）
    fun selectAll() {
        _images.value.forEach {
            if (!selectedIds.contains(it.id)) selectedIds.add(it.id)
        }
    }

    fun moveSelectedImages() {
        viewModelScope.launch {
            val selectedItems = _images.value.filter { selectedIds.contains(it.id) }
            if (selectedItems.isEmpty()) return@launch

            val intentSender = fileRepository.moveImagesToMemeFolder(selectedItems)

            if (intentSender == null) {
                // 成功！
                val count = selectedIds.size

                // 2. 优化：直接从当前列表中移除已移动的图片，而不是重新扫描 (更流畅)
                _images.value = _images.value.filter { !selectedIds.contains(it.id) }

                // 3. 清空选中状态
                selectedIds.clear()

                // 4. 发送提示消息
                _uiEvent.send("成功归档 $count 张Meme图！")
            } else {
                _permissionRequest.value = intentSender
            }
        }
    }

    fun onPermissionResult(resultOk: Boolean) {
        _permissionRequest.value = null
        if (resultOk) {
            moveSelectedImages()
        }
    }
}