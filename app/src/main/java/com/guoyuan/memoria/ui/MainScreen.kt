package com.guoyuan.memoria.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.compose.material3.DrawerValue
import androidx.compose.foundation.clickable
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Row
import com.guoyuan.memoria.data.AppDatabase
import com.guoyuan.memoria.data.AppDao
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import android.util.Log
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.guoyuan.memoria.data.TextEntity
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.toMutableStateList
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.graphics.graphicsLayer
import java.util.Collections
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val dataStore = context.dataStore
    val appDao: AppDao = remember { AppDatabase.getDatabase(context).appDao() }
    val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(appDao, dataStore))
    val uiState by viewModel.uiState.collectAsState()
    val allTexts by viewModel.allTexts.collectAsState()
    val punctuationList by viewModel.punctuationList.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                // 新增文本按鈕
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    label = { Text("新增文本") },
                    selected = false,
                    onClick = {
                        // 新增時強制清空輸入欄位
                        viewModel.updateEditTitle("")
                        viewModel.updateEditContent("")
                        viewModel.updateMode(AppMode.EDIT)
                        scope.launch { drawerState.close() }
                    }
                )
                
                Divider()
                
                // 模式切換按鈕
                NavigationDrawerItem(
                    icon = { 
                        Icon(
                            if (uiState.isSidebarManagementMode) Icons.Filled.Close else Icons.Filled.Settings,
                            contentDescription = null
                        )
                    },
                    label = { 
                        Text(if (uiState.isSidebarManagementMode) "結束管理" else "編輯列表") 
                    },
                    selected = false,
                    onClick = {
                        viewModel.toggleSidebarManagementMode()
                    }
                )

                Divider()
                
                // 文章列表
                if (allTexts.isEmpty()) {
                    Text(
                        text = "目前沒有文本，請點擊上方新增",
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    val favoriteItem = allTexts.firstOrNull { it.isFavorite }
                    val regularItems = allTexts.filterNot { it.isFavorite }
                        .sortedBy { it.displayOrder }
                    val reorderableRegularItems = remember(regularItems) { regularItems.toMutableStateList() }
                    // 將拖曳狀態提升至 LazyColumn 外層
                    var draggedIndex by remember { mutableStateOf<Int?>(null) }
                    var dragOffset by remember { mutableStateOf(0f) }
                    var itemHeightPx by remember { mutableStateOf(0f) } // 動態量測項目高度
                    
                    LazyColumn {
                        // 最愛項目區
                        favoriteItem?.let {
                            item {
                                ManagementListItem(
                                    item = it,
                                    isManagementMode = uiState.isSidebarManagementMode,
                                    isFavorite = true,
                                    onToggleFavorite = { viewModel.toggleFavorite(it.id) },
                                    onDelete = { viewModel.deleteText(it.id) },
                                    onClick = {
                                        if (!uiState.isSidebarManagementMode) {
                                            viewModel.selectText(it)
                                            scope.launch { drawerState.close() }
                                        }
                                    },
                                    onEditConfirm = { updatedTitle, updatedContent ->
                                        // TODO: 待實作資料庫更新邏輯
                                        // 目前只更新本地列表以確保UI刷新
                                    }
                                )
                            }
                        }

                        // 一般項目區
                        items(reorderableRegularItems, key = { it.id }) { textEntity ->
                            val index = reorderableRegularItems.indexOf(textEntity)
                            val isDragging = draggedIndex == index
                            
                            ManagementListItem(
                                item = textEntity,
                                isManagementMode = uiState.isSidebarManagementMode,
                                isFavorite = false,
                                onToggleFavorite = { viewModel.toggleFavorite(textEntity.id) },
                                onDelete = { viewModel.deleteText(textEntity.id) },
                                onClick = {
                                    if (!uiState.isSidebarManagementMode) {
                                        viewModel.selectText(textEntity)
                                        scope.launch { drawerState.close() }
                                    }
                                },
                                onEditConfirm = { updatedTitle, updatedContent ->
                                    // 1. 更新本地快照清單
                                    val targetIndex = reorderableRegularItems.indexOfFirst { it.id == textEntity.id }
                                    if (targetIndex != -1) {
                                        val updatedEntity = reorderableRegularItems[targetIndex].copy(
                                            title = updatedTitle,
                                            fullContent = updatedContent
                                        )
                                        reorderableRegularItems[targetIndex] = updatedEntity
                                    }
                                    
                                    // TODO: 待實作資料庫更新邏輯
                                    // 需要添加 ViewModel 中的資料庫更新函式
                                },
                                dragHandle = {
                                    if (uiState.isSidebarManagementMode && !textEntity.isFavorite) {
                                        Icon(
                                            imageVector = Icons.Filled.DragHandle,
                                            contentDescription = "拖曳排序",
                                            modifier = Modifier
                                                .size(24.dp)
                                                .pointerInput(textEntity.id) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = {
                                                            Log.d("SidebarDrag", "【開始拖曳】項目標題: ${textEntity.title}")
                                                            // 初始時直接使用外層索引
                                                            draggedIndex = reorderableRegularItems.indexOfFirst { it.id == textEntity.id }
                                                            dragOffset = 0f
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            // 強制消費事件，防止被上層攔截
                                                            change.consume()
                                                            dragOffset += dragAmount.y
                                                            Log.d("SidebarDrag", "【拖曳中】目前累積位移 dragOffset: $dragOffset")
                                                            
                                                            // 每次都動態查找最新索引
                                                            val currentActiveIndex = reorderableRegularItems.indexOfFirst { it.id == textEntity.id }
                                                            if (currentActiveIndex == -1) return@detectDragGesturesAfterLongPress
                                                            
                                                            draggedIndex = currentActiveIndex
                                                            
                                                            // 使用動態量測高度或預設值
                                                            val currentThreshold = if (itemHeightPx > 0f) itemHeightPx else 180f
                                                            
                                                            // 階梯式距離換位演算法
                                                            when {
                                                                // 往下拖曳超過閾值
                                                                dragOffset > currentThreshold -> {
                                                                    val nextIndex = currentActiveIndex + 1
                                                                    if (nextIndex in reorderableRegularItems.indices) {
                                                                        Collections.swap(reorderableRegularItems, currentActiveIndex, nextIndex)
                                                                        draggedIndex = nextIndex
                                                                        dragOffset -= currentThreshold // 精確扣除真實高度
                                                                    }
                                                                }
                                                                // 往上拖曳超過閾值
                                                                dragOffset < -currentThreshold -> {
                                                                    val prevIndex = currentActiveIndex - 1
                                                                    if (prevIndex >= 0 && prevIndex in reorderableRegularItems.indices) {
                                                                        Collections.swap(reorderableRegularItems, currentActiveIndex, prevIndex)
                                                                        draggedIndex = prevIndex
                                                                        dragOffset += currentThreshold // 精確加回真實高度
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        onDragEnd = {
                                                            Log.d("SidebarDrag", "【拖曳結束】已放開手指，準備呼叫 ViewModel 存檔")
                                                            viewModel.updateItemsOrder(reorderableRegularItems)
                                                            draggedIndex = null
                                                            dragOffset = 0f
                                                        },
                                                        onDragCancel = {
                                                            Log.d("SidebarDrag", "【拖曳取消】手勢中斷")
                                                            draggedIndex = null
                                                            dragOffset = 0f
                                                        }
                                                    )
                                                }
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .onSizeChanged { size ->
                                        // 只在尚未設置高度且量測到有效高度時更新
                                        if (size.height > 0 && itemHeightPx == 0f) {
                                            itemHeightPx = size.height.toFloat()
                                        }
                                    }
                                    .graphicsLayer {
                                        // 只有當前行是被拖曳的項目才應用位移
                                        translationY = if (isDragging) dragOffset else 0f
                                    }
                                    .alpha(if (isDragging) 0.5f else 1f)
                                    .animateItem() // 平滑排序動畫
                            )
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(uiState.currentTextTitle.ifEmpty { "Memoria" })
                            if (uiState.isEditingReadingMode) {
                                IconButton(
                                    onClick = { viewModel.openEditTitleDialog() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = "編輯標題",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }) {
                            Icon(Icons.Filled.Menu, contentDescription = "開啟側邊欄")
                        }
                    },
                    actions = {
                        if (uiState.currentMode == AppMode.READ && !uiState.isEditingReadingMode) {
                            IconButton(onClick = { viewModel.toggleReadingEditMode() }) {
                                Icon(Icons.Filled.Edit, contentDescription = "編輯內容")
                            }
                        } else if (uiState.currentMode == AppMode.READ && uiState.isEditingReadingMode) {
                            IconButton(onClick = {
                                // 保存後自動退出編輯模式
                                viewModel.toggleReadingEditMode()
                            }) {
                                Icon(Icons.Filled.Done, contentDescription = "儲存編輯")
                            }
                        }
                        
                        IconButton(onClick = { viewModel.openSettings() }) {
                            Icon(Icons.Filled.Settings, contentDescription = "設定")
                        }
                    }
                )
            },
            floatingActionButton = {
                if (!uiState.isEditingReadingMode && uiState.currentMode != AppMode.EDIT) {
                    FloatingActionButton(
                        onClick = {
                            viewModel.updateMode(
                                if (uiState.currentMode == AppMode.READ) AppMode.PLAY
                                else AppMode.READ
                            )
                        }
                    ) {
                        Text(
                            text = if (uiState.currentMode == AppMode.READ) 
                                "開始" 
                            else 
                                "返回"
                        )
                    }
                }
            },
            floatingActionButtonPosition = FabPosition.End
        ) { innerPadding ->
            if (uiState.currentMode == AppMode.EDIT) {
                // 編輯模式：顯示標題和內容輸入框，以及保存按鈕
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                ) {
                    // 標題輸入框
                    OutlinedTextField(
                        value = uiState.currentTextTitle,
                        onValueChange = { viewModel.updateEditTitle(it) },
                        label = { Text("標題") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    // 內容輸入框
                    OutlinedTextField(
                        value = uiState.fullTextContent,
                        onValueChange = { viewModel.updateEditContent(it) },
                        label = { Text("內容") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        textStyle = LocalTextStyle.current,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                        maxLines = Int.MAX_VALUE
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (uiState.currentTextTitle.isBlank() || uiState.fullTextContent.isBlank()) {
                                    android.util.Log.w("Memoria", "標題與內文不能為空")
                                    return@Button
                                }
                                viewModel.saveTextToDatabase()
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isLoading
                        ) {
                            Text("儲存文章")
                        }
                        
                        Button(
                            onClick = {
                                viewModel.updateMode(AppMode.READ)
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer,
                                contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text("取消新增")
                        }
                    }
                }
            } else {
                // 非編輯模式：原來的顯示
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                ) {
                    // 可滾動的文字顯示區域
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (uiState.currentMode == AppMode.READ) {
                            var editableText by remember(uiState.isEditingReadingMode) { 
                                mutableStateOf(uiState.fullTextContent) 
                            }
                            
                            if (uiState.isEditingReadingMode) {
                                // 編輯狀態：原地變身為輸入框
                                OutlinedTextField(
                                    value = editableText,
                                    onValueChange = {
                                        editableText = it
                                        viewModel.updateEditContent(it) // 即時更新ViewModel
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                    textStyle = TextStyle(fontSize = androidx.compose.material3.MaterialTheme.typography.bodyLarge.fontSize),
                                    maxLines = Int.MAX_VALUE
                                )
                            } else {
                                // 唯讀狀態：維持原本優雅的 Text 渲染
                                val displayText = if (uiState.paragraphs.isNotEmpty()) {
                                    uiState.paragraphs.joinToString("\n")
                                } else {
                                    uiState.fullTextContent.ifEmpty { "請輸入內容或載入網頁" }
                                }
                                
                                Text(
                                    text = displayText,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                    textAlign = TextAlign.Start
                                )
                            }
                        } else {
                            // 播放模式顯示
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                // 加入偵錯日誌
                                val takeCount = uiState.currentSentenceIndex + 1
                                val sentenceText = if (uiState.currentSentences.isNotEmpty()) {
                                    uiState.currentSentences.take(takeCount).joinToString("")
                                } else {
                                    ""
                                }
                                        
                                val displayText = if (uiState.currentMode == AppMode.PLAY) {
                                    if (uiState.currentParagraphIndex >= uiState.paragraphs.size) {
                                        "全文背誦完畢！"
                                    } else if (uiState.isPlaying) {
                                        sentenceText
                                    } else {
                                        "${uiState.currentParagraphIndex + 1}."
                                    }
                                } else {
                                    uiState.fullTextContent.ifEmpty { "請輸入內容或載入網頁" }
                                }
                                        
                                // 輸出詳細偵錯日誌
                                Log.d("MemoriaUI", "【UI 刷新】isPlaying = ${uiState.isPlaying}, 索引 = ${uiState.currentSentenceIndex}, " +
                                    "預計裁切數量 = $takeCount, 裁切出文字 = '$sentenceText', " +
                                    "最終畫面顯示 = '$displayText'")
                                        
                                Text(
                                    text = displayText,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }
                            
                    // 只在播放模式顯示控制元件
                    if (uiState.currentMode == AppMode.PLAY) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Slider(
                            value = uiState.previewParagraphIndex.toFloat(),
                            onValueChange = { newValue ->
                                val index = newValue.toInt()
                                viewModel.updateParagraphIndexDuringDrag(index)
                                viewModel.updatePreviewIndex(index)
                            },
                            onValueChangeFinished = {
                                viewModel.confirmParagraphSelection(uiState.previewParagraphIndex)
                            },
                            valueRange = 0f..(uiState.paragraphs.size - 1).toFloat(),
                            modifier = Modifier.fillMaxWidth()
                        )
                                
                        // 播放控制按鈕組
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                        ) {
                            // 清空按鈕
                            IconButton(
                                onClick = { viewModel.resetPlayback() },
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                                    contentDescription = "清空"
                                )
                            }
                                    
                            // 上一步按鈕
                            IconButton(
                                onClick = { viewModel.moveToPrevious() },
                                modifier = Modifier.padding(horizontal = 8.dp),
                                enabled = uiState.previewParagraphIndex > 0
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.KeyboardArrowLeft,
                                    contentDescription = "上一步"
                                )
                            }
                                    
                            // 播放按鈕
                            IconButton(
                                onClick = {
                                    viewModel.handlePlayButtonClick()
                                },
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .size(48.dp), // 放大按鈕
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.PlayArrow,
                                    contentDescription = "播放",
                                    modifier = Modifier.size(36.dp) // 放大圖標
                                )
                            }
                        }
                                
                        Spacer(modifier = Modifier.height(80.dp)) // 避免FAB遮擋
                    }
                }
            }
        }
    }
    
    // 標題編輯對話框
    if (uiState.isEditingTitleDialogVisible) {
        var tempTitle by remember { mutableStateOf(uiState.currentTextTitle) }
        
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.closeEditTitleDialog() },
            title = { Text("修改文本標題") },
            text = {
                OutlinedTextField(
                    value = tempTitle,
                    onValueChange = { tempTitle = it },
                    label = { Text("新標題") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateTextTitle(tempTitle)
                    viewModel.closeEditTitleDialog()
                }) {
                    Text("儲存")
                }
            },
            dismissButton = {
                Button(onClick = { viewModel.closeEditTitleDialog() }) {
                    Text("取消")
                }
            }
        )
    }

    // 系統設定總選單
    if (uiState.showSettingsDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.closeSettings() },
            title = { Text("系統設定") },
            text = {
                Column {
                    // 設定斷句符號選項
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openPunctuationDialog() }
                            .padding(16.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                    ) {
                        androidx.compose.foundation.layout.Row {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("設定斷句符號")
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "前往"
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.closeSettings() }) {
                    Text("關閉")
                }
            }
        )
    }

    // 斷句符號設定彈窗
    if (uiState.showPunctuationDialog) {
        val customInput = remember { mutableStateOf("") }
        
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.closePunctuationDialog() },
            title = { Text("自定義斷句符號") },
            text = {
                Column {
                    // 上半部：Checkbox 列表
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .height(250.dp)
                    ) {
                        LazyColumn {
                            items(punctuationList) { item ->
                                androidx.compose.foundation.layout.Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.togglePunctuation(item.symbol)
                                        }
                                        .padding(vertical = 8.dp, horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    androidx.compose.material3.Checkbox(
                                        checked = item.isChecked,
                                        onCheckedChange = { checked ->
                                            viewModel.togglePunctuation(item.symbol)
                                        }
                                    )
                                    Text(
                                        text = item.label,
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // 分隔線
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // 下半部：新增自定義區
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customInput.value,
                            onValueChange = {
                                if (it.length <= 2) {
                                    customInput.value = it
                                }
                            },
                            label = { Text("新符號") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        
                        Button(
                            onClick = {
                                if (customInput.value.isNotBlank()) {
                                    viewModel.addCustomPunctuation(customInput.value)
                                    customInput.value = ""
                                }
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text("新增")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.closePunctuationDialog() }) {
                    Text("確定")
                }
            }
        )
    }
}

@Composable
private fun ManagementListItem(
    item: TextEntity,
    isManagementMode: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onEditConfirm: (String, String) -> Unit, // 新增：編輯確認回呼
    dragHandle: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showFavoriteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) } // 新增：編輯對話框狀態
    var editedTitle by remember { mutableStateOf(item.title) } // 新增：編輯中的標題
    var editedContent by remember { mutableStateOf(item.fullContent) } // 新增：編輯中的內容
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .clickable(enabled = !isManagementMode) { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isManagementMode) {
            // 最愛按鈕
            IconButton(
                onClick = { showFavoriteDialog = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = "設為最愛",
                    tint = if (isFavorite) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                )
            }
            
            // 新增：編輯按鈕
            IconButton(
                onClick = { showEditDialog = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "編輯文本",
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                )
            }
        }

        Text(
            text = item.title,
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically),
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
        )
        
        if (isManagementMode) {
            // 刪除按鈕
            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "刪除",
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.error
                )
            }
            
            // 為最愛項目添加空白佔位符，與非最愛項目的拖拉把手寬度一致
            if (isFavorite) {
                Spacer(modifier = Modifier.size(24.dp))
            } else {
                // 拖曳把手 (僅限非最愛項目)
                dragHandle()
            }
        }
    }
    
    // 刪除確認對話框
    if (showDeleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("確認刪除文本") },
            text = { Text("確定要刪除「${item.title}」嗎？此動作將無法還原。") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer,
                        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("確定")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 最愛置頂確認對話框
    if (showFavoriteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showFavoriteDialog = false },
            title = { Text("確認設為最愛置頂") },
            text = {
                if (isFavorite) {
                    Text("確定要取消「${item.title}」的最愛置頂狀態嗎？")
                } else {
                    Text("確定要將「${item.title}」設為最愛置頂嗎？（注意：原本的最愛項目將會被自動取代）")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onToggleFavorite()
                        showFavoriteDialog = false
                    }
                ) {
                    Text("確定")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showFavoriteDialog = false },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer,
                        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("取消")
                }
            }
        )
    }
    
    // 新增：編輯文本對話框
    if (showEditDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("編輯文本") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editedTitle,
                        onValueChange = { editedTitle = it },
                        label = { Text("標題") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editedContent,
                        onValueChange = { editedContent = it },
                        label = { Text("內容") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        maxLines = Int.MAX_VALUE
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onEditConfirm(editedTitle, editedContent)
                        showEditDialog = false
                    }
                ) {
                    Text("儲存")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showEditDialog = false }
                ) {
                    Text("取消")
                }
            }
        )
    }
}
