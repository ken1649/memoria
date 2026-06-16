package com.guoyuan.memoria.ui

import androidx.compose.foundation.layout.Column
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
                        viewModel.updateMode(AppMode.EDIT)
                        scope.launch { drawerState.close() }
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
                    LazyColumn {
                        items(allTexts) { textEntity ->
                            Text(
                                text = textEntity.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .clickable {
                                        viewModel.selectText(textEntity)
                                        scope.launch { drawerState.close() }
                                    }
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
                    title = { Text(uiState.currentTextTitle.ifEmpty { "Memoria" }) },
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
                        IconButton(onClick = { viewModel.openSettings() }) {
                            Icon(Icons.Filled.Settings, contentDescription = "設定")
                        }
                    }
                )
            },
            floatingActionButton = {
                if (uiState.currentMode != AppMode.EDIT) {
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
                            .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    ) {
                        val displayText = if (uiState.currentMode == AppMode.PLAY) {
                            if (uiState.currentParagraphIndex >= uiState.paragraphs.size) {
                                "全文背誦完畢！"
                            } else if (uiState.isPlaying && uiState.currentSentences.isNotEmpty()) {
                                // 正在播放中：累加顯示目前已解鎖的句子
                                uiState.currentSentences.take(uiState.currentSentenceIndex + 1).joinToString("")
                            } else {
                                // 未播放/滑動中/等待中：顯示極簡序號
                                "${uiState.currentParagraphIndex + 1}."
                            }
                        } else {
                            // 閱讀模式顯示完整內容
                            if (uiState.paragraphs.isNotEmpty()) {
                                uiState.paragraphs.joinToString("\n")
                            } else {
                                uiState.fullTextContent.ifEmpty { "請輸入內容或載入網頁" }
                            }
                        }
                        
                        Text(
                            text = displayText,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
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
                            
                            // 播放/下一句按鈕
                            IconButton(
                                onClick = {
                                    if (uiState.isPlaying) {
                                        viewModel.moveToNextSentence()
                                    } else {
                                        viewModel.startPlay()
                                    }
                                },
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .size(48.dp), // 放大按鈕
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.PlayArrow,
                                    contentDescription = if (uiState.isPlaying) "下一句" else "播放",
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
