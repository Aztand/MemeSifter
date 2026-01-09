package com.aztand.memesifter

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity // 1. 必须导入这个类
import androidx.activity.compose.setContent // 2. setContent 是扩展函数，必须导入
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text // 或者 androidx.compose.material.Text (取决于你用 Material2 还是 3)
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue // 3. 使用 'by' 委托属性必须导入
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue // 3. 使用 'by' 委托属性必须导入
import com.aztand.memesifter.ui.theme.MemeSifterTheme // 4. 导入你的主题
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import com.aztand.memesifter.viewmodel.MainViewModel
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.activity.result.IntentSenderRequest
import android.app.Activity
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MemeSifterTheme {
                var hasPermission by remember { mutableStateOf(false) }
                if (!hasPermission) {
                    RequestStoragePermission(
                        onPermissionGranted = { hasPermission = true },
                        onPermissionDenied = { }
                    )
                } else {
                    MainGalleryScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainGalleryScreen(viewModel: MainViewModel = viewModel()) {
    val images by viewModel.images.collectAsState()
    val permissionRequest by viewModel.permissionRequest.collectAsState()
    val selectedCount = viewModel.selectedIds.size

    // 1. 获取 Context 用于弹 Toast
    val context = LocalContext.current

    // 2. 监听“移动完成”事件
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // 2. 处理 Android 10+ 的 MediaStore 修改权限弹窗
    val intentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onPermissionResult(result.resultCode == Activity.RESULT_OK)
    }
    LaunchedEffect(Unit) {
        viewModel.scanImages()
    }
    LaunchedEffect(permissionRequest) {
        permissionRequest?.let { sender ->
            intentSenderLauncher.launch(
                IntentSenderRequest.Builder(sender).build()
            )
        }
    }

    // 3. UI 布局
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meme Sifter") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (selectedCount > 0) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.moveSelectedImages() },
                    icon = { Icon(Icons.Default.Done, contentDescription = "Move") },
                    text = { Text("移动 $selectedCount 张Meme图") }
                )
            }
        }
    ) { paddingValues ->
        if (images.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("正在挖掘Meme图...")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = paddingValues,
                modifier = Modifier.fillMaxSize()
            ) {
                items(images) { image ->
                    val isSelected = viewModel.selectedIds.contains(image.id)

                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .aspectRatio(1f)
                            .clickable { viewModel.toggleSelection(image.id) }
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(image.uri),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                                    .border(4.dp, MaterialTheme.colorScheme.primary)
                            )
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .padding(2.dp)
                            )
                        }

                        if (image.ocrText.isNotEmpty()) {
                            Text(
                                text = "TEXT",
                                fontSize = 10.sp,
                                color = Color.White,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun RequestStoragePermission(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
) {
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onPermissionGranted()
        } else {
            onPermissionDenied()
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch(permissionToRequest)
    }
}