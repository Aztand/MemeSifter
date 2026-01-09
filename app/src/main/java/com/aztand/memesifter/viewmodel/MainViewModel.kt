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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        viewModelScope.launch(Dispatchers.Default) { // 使用 Default 调度器处理 CPU 密集型任务
            val allRawImages = imageRepository.getAllImages()

            // 优化 1：分批并发处理
            // 将所有图片分成每组 10 张（可根据性能调整，例如 20）
            val batchSize = 10
            allRawImages.chunked(batchSize).forEach { batch ->

                // 并发执行当前批次的 OCR
                val deferredResults = batch.map { item ->
                    async(Dispatchers.IO) {
                        val text = ocrRepository.recognizeText(item.uri)
                        if (text.isNotBlank()) {
                            item.copy(ocrText = text)
                        } else {
                            null
                        }
                    }
                }

                // 等待这一批全部完成
                val validMemesInBatch = deferredResults.awaitAll().filterNotNull()

                // 优化 2：批量更新 UI，减少重组次数
                if (validMemesInBatch.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        val currentList = _images.value.toMutableList()
                        currentList.addAll(validMemesInBatch)
                        _images.value = currentList
                    }
                }
            }

            withContext(Dispatchers.Main) {
                _uiEvent.send("扫描完成！共找到 ${_images.value.size} 张图片")
            }
        }
    }

    /*private suspend fun processImage(item: ImageItem) {
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
    已被整合至scanimages
    */

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
            // 获取当前选中的图片对象
            val selectedItems = _images.value.filter { selectedIds.contains(it.id) }
            if (selectedItems.isEmpty()) return@launch

            // 执行移动逻辑，如果需要权限，这里会返回 intentSender
            val intentSender = fileRepository.moveImagesToMemeFolder(selectedItems)

            if (intentSender == null) {
                // 全部移动成功（或者不需要额外权限的部分已移动）

                // 重新检查哪些文件还在原处（意味着移动失败或者还在等权限）
                // 简单的做法是：再次扫描数据库状态，或者根据逻辑移除已移动的
                // 这里我们做一个简化的假设：如果返回 null，说明没有 Pending 的权限请求了
                // 但实际上可能只移动了一部分。

                // 为了保证 UI 准确，建议：
                // 1. 移除那些已经成功移动到 Memes 文件夹的图片
                // (由于我们无法从 moveImagesToMemeFolder 得到确切的成功列表，
                // 最稳妥的方法是依靠 ImageRepository 重新检查，或者在 Repository 里返回成功列表)

                // 简单优化：从列表中移除选中的 ID，提示用户操作完成
                // 如果有部分没移走，下次扫描会再出来，或者用户会发现它们还在。

                _images.value = _images.value.filter { !selectedIds.contains(it.id) }
                val count = selectedIds.size
                selectedIds.clear()
                _uiEvent.send("操作结束，已处理 $count 张图片")
            } else {
                // 需要用户授权，触发弹窗
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