package com.guoyuan.memoria.ui

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.guoyuan.memoria.data.AppDao
import com.guoyuan.memoria.data.TextEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import androidx.datastore.preferences.core.floatPreferencesKey
import java.io.InputStreamReader

data class PunctuationItem(val symbol: String, val label: String, var isChecked: Boolean, val isCustom: Boolean = false)

class MainViewModel(private val appDao: AppDao, private val dataStore: DataStore<Preferences>, private val context: Context) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()
    
    private val premiumManager = PremiumManager(dataStore)
    val isPremium: StateFlow<Boolean> = premiumManager.isPremium.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        false
    )
    
    private val _allTexts = MutableStateFlow<List<TextEntity>>(emptyList())
    val allTexts: StateFlow<List<TextEntity>> = _allTexts.asStateFlow()

    val punctuationList = MutableStateFlow<List<PunctuationItem>>(emptyList())
    private val gson = Gson()
    private val PUNCTUATION_LIST_KEY = stringPreferencesKey("punctuation_list_json")
    private val FONT_SIZE_KEY = floatPreferencesKey("font_size") // 新增：字體大小儲存鍵
    private val THEME_KEY = stringPreferencesKey("theme_mode") // 修復：使用一致的鍵名

    init {
        Log.d("ThemeDebug", "MainViewModel 初始化開始")
        // 1. 先處理主題 (獨立的)
        // 獨立啟動一個協程，專門處理主題載入，不與其他任務混在一起
        viewModelScope.launch {
            loadTheme()
        }

        // 其他任務保持原樣
        viewModelScope.launch {
            // 2. 將所有資料初始化任務串聯起來，按順序執行
            Log.d("InitDebug", "開始初始化資料...")

            // 檢查資料庫是否為空 (使用一次性查詢)
            val dbData = appDao.getAllTexts() // 確保這裡是 suspend 方法或一次性查詢
            if (dbData.isEmpty()) {
                Log.d("InitDebug", "資料庫為空，開始從 Assets 載入範例資料...")
                insertDefaultData()
            } else {
                _allTexts.value = dbData
                Log.d("InitDebug", "從資料庫載入 ${dbData.size} 筆資料")
            }
            // 3. 其他依賴於資料載入後的任務
            loadPunctuationListFromStore()
            loadFontSize()
            Log.d("InitDebug", "所有初始化任務完成")
        }

        viewModelScope.launch {

        }

        Log.d("ThemeDebug", "MainViewModel 初始化結束")
    }

    private suspend fun insertDefaultData() {
        withContext(Dispatchers.IO) {
            try {
                // 從 assets 讀取道德經.json
                Log.d("InitDebug", "正在開啟 Assets 檔案...")
                val inputStream = context.assets.open("道德經.json")
                Log.d("InitDebug", "正在讀取並解析 JSON...")
                val reader = InputStreamReader(inputStream, "UTF-8")
                val jsonString = reader.readText()
                reader.close()
                inputStream.close()
                Log.d("InitDebug", "JSON 解析中...")
                // 解析 JSON
                val type = object : TypeToken<List<TextEntity>>() {}.type
                val defaultTexts: List<TextEntity> = gson.fromJson(jsonString, type)
                Log.d("InitDebug", "準備插入 ${defaultTexts.size} 筆資料到資料庫...")


                // 插入資料庫
                defaultTexts.forEach { text ->
                    appDao.insertText(text)
                }

                // 重新加載文章列表
                loadAllTexts()
                Log.d("InitDebug", "成功插入預設資料")
            } catch (e: Exception) {
                Log.e("InitDebug", "插入預設資料失敗: ", e)
            }
        }
    }

    private fun loadTheme() {
        viewModelScope.launch {
            try {
                Log.d("ThemeDebug", "嘗試從DataStore讀取主題")
                val preferences = dataStore.data.first()
                val themeName = preferences[THEME_KEY] ?: "SYSTEM"
                Log.d("ThemeDebug", "成功讀取到持久化主題: $themeName")

                val theme = try {
                    AppTheme.valueOf(themeName)
                } catch (e: Exception) {
                    AppTheme.SYSTEM
                }
                _uiState.update { it.copy(currentTheme = theme) }
            } catch (e: Exception) {
                Log.e("ThemeDebug", "讀取失敗: ${e.message}")
            }
        }
    }
    
    private suspend fun loadAllTexts() {
        withContext(Dispatchers.IO) {
            _allTexts.value = appDao.getAllTexts()
        }
    }

    private suspend fun loadPunctuationListFromStore() {
        try {
            val json = dataStore.data.map { preferences ->
                preferences[PUNCTUATION_LIST_KEY]
            }.first()

            if (!json.isNullOrBlank()) {
                val type = object : TypeToken<List<PunctuationItem>>() {}.type
                val storedList: List<PunctuationItem> = gson.fromJson(json, type)
                punctuationList.value = storedList
            } else {
                // 首次啟動使用預設值
                val defaultList = listOf(
                    PunctuationItem("，", "逗號", true),
                    PunctuationItem("。", "句號", true),
                    PunctuationItem("；", "分號", true),
                    PunctuationItem("？", "問號", true),
                    PunctuationItem("！", "驚嘆號", true),
                    PunctuationItem("：", "冒號", true),
                    PunctuationItem(" ", "空白", true),
                    PunctuationItem("／", "正斜線", false),  // 新增正斜線選項（預設不勾選）
                    PunctuationItem("＼", "反斜線", false)   // 新增反斜線選項（預設不勾選）
                )
                punctuationList.value = defaultList
                savePunctuationListToStore(defaultList)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 解析失敗時使用預設值
            val defaultList = listOf(
                PunctuationItem("，", "逗號", true),
                PunctuationItem("。", "句號", true),
                PunctuationItem("；", "分號", true),
                PunctuationItem("？", "問號", true),
                PunctuationItem("！", "驚嘆號", true),
                PunctuationItem("：", "冒號", true),
                PunctuationItem(" ", "空白", true)
            )
            punctuationList.value = defaultList
            savePunctuationListToStore(defaultList)
        }
    }

    private fun savePunctuationListToStore(list: List<PunctuationItem>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = gson.toJson(list)
                dataStore.edit { preferences ->
                    preferences[PUNCTUATION_LIST_KEY] = json
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateMode(newMode: AppMode) {
        _uiState.update { currentState ->
            if (newMode == AppMode.PLAY) {
                // 進入播放模式時自動開始播放
                updateCurrentSentences()
                currentState.copy(
                    currentMode = newMode,
                    currentSentenceIndex = 0,
                    isPlaying = true
                )
            } else if (newMode == AppMode.EDIT) {
                // 進入編輯模式時標記為新增文本狀態
                currentState.copy(
                    currentMode = newMode,
                    isAddingNewText = true
                )
            } else {
                // 退出編輯模式時清除新增狀態
                currentState.copy(
                    currentMode = newMode,
                    isAddingNewText = false
                )
            }
        }
    }

    fun updateEditTitle(title: String) {
        _uiState.update { currentState ->
            currentState.copy(currentTextTitle = title)
        }
    }

    fun updateEditContent(content: String) {
        _uiState.update { currentState ->
            currentState.copy(fullTextContent = content)
        }
    }

    fun saveTextToDatabase() {
        val title = _uiState.value.currentTextTitle
        val content = _uiState.value.fullTextContent
        if (title.isBlank() || content.isBlank()) {
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 計算當前最大的 displayOrder
                val maxDisplayOrder = _allTexts.value.maxOfOrNull { it.displayOrder } ?: 0
                val newDisplayOrder = maxDisplayOrder + 1

                val textEntity = TextEntity(
                    title = title,
                    content = content,
                    sourceUrl = "",
                    displayOrder = newDisplayOrder
                )
                val newId = appDao.insertText(textEntity).toInt()
                loadAllTexts() // 重新加载文章列表

                // 获取新插入的文章
                val newText = _allTexts.value.firstOrNull { it.id == newId }
                
                withContext(Dispatchers.Main) {
                    _uiState.update { currentState ->
                        currentState.copy(
                            currentMode = AppMode.READ,
                            isLoading = false,
                            currentTextId = newId,
                            currentTextTitle = newText?.title ?: title,
                            fullTextContent = newText?.content ?: content
                        )
                    }
                    // 保存後重新解析內容以更新顯示
                    splitContentToParagraphs(newText?.content ?: content)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun setLoading(isLoading: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(isLoading = isLoading)
        }
    }

    fun fetchTextFromUrl(url: String) {
        viewModelScope.launch {
            setLoading(true)
            try {
                val isGoogleDoc = url.contains("docs.google.com/document/d/")
                val (processedUrl, title) = if (isGoogleDoc) {
                    val docId = url.substringAfter("document/d/").substringBefore("/")
                    val exportUrl = "https://docs.google.com/document/d/$docId/export?format=txt"
                    
                    // 1. 嘗試從標準檢視網址抓取 Google 文件的網頁標題
                    val rawTitle = withContext(Dispatchers.IO) {
                        try {
                            Jsoup.connect("https://docs.google.com/document/d/$docId/view").get().title()
                        } catch (e: Exception) {
                            "匯入的 Google 文件"
                        }
                    }
                    
                    // 2. 清理掉 Google 附加的後綴字串，還原真實標題
                    val cleanTitle = rawTitle
                        .replace(" - Google 文件", "")
                        .replace(" - Google Docs", "")
                        .trim()
                        
                    val finalTitle = if (cleanTitle.isNotEmpty()) cleanTitle else "匯入的 Google 文件"
                    Pair(exportUrl, finalTitle)
                } else {
                    val webTitle = withContext(Dispatchers.IO) {
                        try { Jsoup.connect(url).get().title() } catch (e: Exception) { "網頁內容" }
                    }
                    Pair(url, webTitle)
                }

                // 3. 抓取文章內文
                val text = withContext(Dispatchers.IO) {
                    if (isGoogleDoc) {
                        Jsoup.connect(processedUrl).ignoreContentType(true).execute().body()
                    } else {
                        Jsoup.connect(processedUrl).get().text()
                    }
                }
                
                updateEditTitle(title)
                updateEditContent(text)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                setLoading(false)
            }
        }
    }

    fun updatePreviewIndex(index: Int) {
        _uiState.update { currentState ->
            currentState.copy(previewParagraphIndex = index)
        }
    }
    
    fun updateParagraphIndexDuringDrag(index: Int) {
        _uiState.update { currentState ->
            currentState.copy(
                currentParagraphIndex = index,
                isPlaying = false
            )
        }
    }

    fun confirmParagraphSelection(index: Int) {
        _uiState.update { currentState ->
            currentState.copy(
                currentParagraphIndex = index,
                previewParagraphIndex = index,  // 同步預覽索引
                currentSentenceIndex = 0,
                isPlaying = false // 重置播放狀態
            )
        }
        // 更新當前段落的句子列表
        updateCurrentSentences()
        Log.d("MemoriaDebug", "確認段落選擇: 段落索引=$index, 句子數量=${_uiState.value.currentSentences.size}")
    }
    
    fun moveToPreviousParagraph() {
        val paragraphs = _uiState.value.paragraphs
        if (paragraphs.isEmpty()) return
        
        val newIndex = if (_uiState.value.currentParagraphIndex == 0) {
            paragraphs.lastIndex
        } else {
            _uiState.value.currentParagraphIndex - 1
        }
        Log.d("NavDebug", "準備跳轉至索引: $newIndex, 目前長度: ${paragraphs.size}")
        confirmParagraphSelection(newIndex)
    }
    
    fun moveToNextParagraph() {
        val paragraphs = _uiState.value.paragraphs
        if (paragraphs.isEmpty()) return
        
        val newIndex = if (_uiState.value.currentParagraphIndex >= paragraphs.lastIndex) {
            0
        } else {
            _uiState.value.currentParagraphIndex + 1
        }
        Log.d("NavDebug", "準備跳轉至索引: $newIndex, 目前長度: ${paragraphs.size}")
        confirmParagraphSelection(newIndex)
    }
    
    fun handlePlayButtonClick() {
        Log.d("MemoriaDebug", "點擊播放鍵 -> 當前 isPlaying = ${_uiState.value.isPlaying}, 句子總數 = ${_uiState.value.currentSentences.size}, 當前索引 = ${_uiState.value.currentSentenceIndex}")
        
        _uiState.update { currentState ->
            if (!currentState.isPlaying) {
                // 如果已經在某個句子上，就從那裡繼續；如果是在索引 0 (段落開頭)，才跳到 1
                val nextIndex = if (currentState.currentSentenceIndex == 0) 1 else currentState.currentSentenceIndex

                currentState.copy(
                    currentSentenceIndex = nextIndex,
                    isPlaying = true
                )
            } else {
                // 已經在播放中，前進下一句
                val nextIndex = currentState.currentSentenceIndex + 1
                if (nextIndex < currentState.currentSentences.size) {
                    currentState.copy(currentSentenceIndex = nextIndex)
                } else {
                    // 已到段落结尾，检查是否有下一段落
                    if (currentState.currentParagraphIndex + 1 < currentState.paragraphs.size) {
                        val newParagraphIndex = currentState.currentParagraphIndex + 1
                        val newParagraph = currentState.paragraphs[newParagraphIndex]
                        val newSentences = splitParagraphIntoSentences(newParagraph)
                        currentState.copy(
                            currentParagraphIndex = newParagraphIndex,
                            previewParagraphIndex = newParagraphIndex,
                            currentSentences = newSentences,
                            currentSentenceIndex = 0
                        )
                    } else {
                        // 已經是最後一段，循環回第一段
                        val newParagraphIndex = 0
                        val newParagraph = currentState.paragraphs[newParagraphIndex]
                        val newSentences = splitParagraphIntoSentences(newParagraph)
                        currentState.copy(
                            currentParagraphIndex = newParagraphIndex,
                            previewParagraphIndex = newParagraphIndex,
                            currentSentences = newSentences,
                            currentSentenceIndex = 0
                        )
                    }
                }
            }
        }
    }


    fun previousSentence() {
        val currentState = _uiState.value
        val paragraphs = currentState.paragraphs
        
        // 如果沒有內容，直接返回
        if (paragraphs.isEmpty()) return

        // 新增：記錄按鈕點擊時的狀態
        android.util.Log.d("NavDebug", "Previous clicked: Para=${currentState.currentParagraphIndex}, Sent=${currentState.currentSentenceIndex}")

        val currentParaIdx = currentState.currentParagraphIndex
        val currentSentIdx = currentState.currentSentenceIndex

        // 計算邏輯
        var nextParaIdx = currentParaIdx
        var nextSentIdx = currentSentIdx - 1

        if (nextSentIdx < 0) {
            // 需要往上一個段落跳
            nextParaIdx = currentParaIdx - 1
            if (nextParaIdx < 0) {
                // 循環：跳到最後一個段落
                nextParaIdx = paragraphs.size - 1
            }
            // 取得目標段落的句子總數
            val targetSentences = splitParagraphIntoSentences(paragraphs[nextParaIdx])
            // 如果目標段落有句子，則跳到最後一句；否則，跳到該段落的開頭（索引0）
            nextSentIdx = if (targetSentences.isNotEmpty()) targetSentences.size - 1 else 0
        }

        // 更新狀態
        _uiState.update { 
            it.copy(
                currentParagraphIndex = nextParaIdx,
                currentSentenceIndex = nextSentIdx.coerceAtLeast(0)
            )
        }
        
        // 強制刷新句子列表以確保 UI 更新
        updateCurrentSentences()
        
        // 除錯日誌
        android.util.Log.d("NavDebug", "Jumped to Para: $nextParaIdx, Sent: $nextSentIdx")
    }

    fun resetPlayback() {
        _uiState.update { currentState ->
            currentState.copy(
                currentSentenceIndex = 0,
                isPlaying = false
            )
        }
    }

    // 新增：更新字體大小（即時預覽）
    fun updateFontSize(size: Float) {
        _uiState.update { currentState ->
            currentState.copy(fontSize = size)
        }
    }
    
    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[THEME_KEY] = theme.name
                Log.d("ThemeDebug", "成功寫入主題: ${theme.name}")
            }
            _uiState.update { currentState ->
                currentState.copy(currentTheme = theme)
            }
        }
    }
    
    // 新增：保存字體大小到持久化儲存
    fun saveFontSize(size: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dataStore.edit { preferences ->
                    preferences[FONT_SIZE_KEY] = size
                }
                // 保存成功後更新狀態
                _uiState.update { currentState ->
                    currentState.copy(fontSize = size)
                }
                Log.d("FontSize", "字體大小已保存: $size")
            } catch (e: Exception) {
                Log.e("FontSize", "保存字體大小失敗", e)
            }
        }
    }
    
    // 新增：從持久化儲存載入字體大小
    fun setPremium(value: Boolean) {
        viewModelScope.launch {
            premiumManager.setPremium(value)
        }
    }

    private suspend fun loadFontSize() {
        try {
            dataStore.data.map { preferences ->
                preferences[FONT_SIZE_KEY] ?: 18f // 只在完全沒有儲存值時使用預設值
            }.collect { size ->
                _uiState.update { currentState ->
                    currentState.copy(
                        fontSize = size,
                        backupFontSize = size
                    )
                }
                Log.d("FontSize", "字體大小已載入: $size")
            }
        } catch (e: Exception) {
            Log.e("FontSize", "載入字體大小失敗", e)
        }
    }

    fun openSettings() {
        // 備份當前字體大小
        _uiState.update { currentState ->
            currentState.copy(
                showSettingsDialog = true,
                backupFontSize = currentState.fontSize
            )
        }
    }

    fun closeSettings() {
        // 取消時還原字體大小
        _uiState.update { currentState ->
            currentState.copy(
                showSettingsDialog = false,
                fontSize = currentState.backupFontSize
            )
        }
    }

    fun openPunctuationDialog() {
        _uiState.update { currentState ->
            currentState.copy(showSettingsDialog = false, showPunctuationDialog = true)
        }
    }

    fun closePunctuationDialog() {
        _uiState.update { currentState ->
            currentState.copy(showPunctuationDialog = false)
        }
    }
    
    // 新增：打开字体大小对话框
    fun openFontSizeDialog() {
        _uiState.update { currentState ->
            currentState.copy(showFontSizeDialog = true)
        }
    }
    
    // 新增：关闭字体大小对话框
    fun closeFontSizeDialog() {
        _uiState.update { currentState ->
            currentState.copy(showFontSizeDialog = false)
        }
    }
    
    fun toggleReadingEditMode() {
        _uiState.update { currentState ->
            currentState.copy(isEditingReadingMode = !currentState.isEditingReadingMode)
        }
    }

    fun toggleSidebarManagementMode() {
        _uiState.update { currentState ->
            currentState.copy(isSidebarManagementMode = !currentState.isSidebarManagementMode)
        }
    }

    fun toggleFavorite(itemId: Int) {
        viewModelScope.launch {
            val currentFavorite = _uiState.value.favoriteTextId
            val newFavorite = if (currentFavorite == itemId) null else itemId
            
            withContext(Dispatchers.IO) {
                appDao.clearAllFavorites()
                if (newFavorite != null) {
                    appDao.setFavorite(newFavorite, true)
                }
            }
            
            _uiState.update { currentState ->
                currentState.copy(favoriteTextId = newFavorite)
            }
            loadAllTexts()
        }
    }

    fun deleteText(itemId: Int) {
        viewModelScope.launch {
            if (_uiState.value.currentTextId == itemId) {
                val firstText = appDao.getAllTexts().firstOrNull()
                firstText?.let { selectText(it) }
            }
            
            withContext(Dispatchers.IO) {
                appDao.deleteText(itemId)
            }
            loadAllTexts()
        }
    }

    fun updateItemsOrder(reorderedList: List<TextEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            reorderedList.forEachIndexed { index, item ->
                appDao.updateDisplayOrder(item.id, index)
            }
            loadAllTexts()
        }
    }
    
    fun openEditTitleDialog() {
        _uiState.update { currentState ->
            currentState.copy(isEditingTitleDialogVisible = true)
        }
    }
    
    fun closeEditTitleDialog() {
        _uiState.update { currentState ->
            currentState.copy(isEditingTitleDialogVisible = false)
        }
    }
    
    fun updateTextTitle(newTitle: String) {
        Log.d("MemoriaFlow", "【ViewModel】準備寫入標題更新, ID: ${_uiState.value.currentTextId}")
        viewModelScope.launch(Dispatchers.IO) {
            _allTexts.value.firstOrNull { it.id == _uiState.value.currentTextId }?.let { text ->
                val updatedText = text.copy(title = newTitle)
                appDao.updateText(updatedText)
                withContext(Dispatchers.Main) {
                    _uiState.update { currentState ->
                        currentState.copy(currentTextTitle = newTitle)
                    }
                    _allTexts.value = _allTexts.value.map { 
                        if (it.id == _uiState.value.currentTextId) updatedText else it 
                    }
                }
            }
        }
    }

    fun updateTextContent(newTitle: String, newContent: String) {
        Log.d("MemoriaFlow", "【3. ViewModel 進入】準備完整更新, ID: ${_uiState.value.currentTextId}")
        viewModelScope.launch(Dispatchers.IO) {
            _allTexts.value.firstOrNull { it.id == _uiState.value.currentTextId }?.let { text ->
                Log.d("MemoriaFlow", "【3.1】找到要更新的文本: $text")
                val updatedText = text.copy(
                    title = newTitle,
                    content = newContent
                )
                Log.d("MemoriaFlow", "【4. Room 寫入前】準備更新資料庫: $updatedText")
                appDao.updateText(updatedText)
                Log.d("MemoriaFlow", "【4.1 Room 寫入完成】已完整更新")
                
                withContext(Dispatchers.Main) {
                    _uiState.update { currentState ->
                        currentState.copy(
                            currentTextTitle = newTitle,
                            fullTextContent = newContent
                        )
                    }
                    _allTexts.value = _allTexts.value.map { 
                        if (it.id == _uiState.value.currentTextId) updatedText else it 
                    }
                    splitContentToParagraphs(newContent)
                }
            }
        }
    }
    
    fun updateReadingContent(newContent: String) {
        Log.d("MemoriaFlow", "【3. ViewModel 進入】準備寫入內容更新, ID: ${_uiState.value.currentTextId}")
        viewModelScope.launch(Dispatchers.IO) {
            _allTexts.value.firstOrNull { it.id == _uiState.value.currentTextId }?.let { text ->
                Log.d("MemoriaFlow", "【3.1】找到要更新的文本: $text")
                val updatedText = text.copy(content = newContent)
                Log.d("MemoriaFlow", "【4. Room 寫入前】準備更新資料庫: $updatedText")
                appDao.updateText(updatedText)
                Log.d("MemoriaFlow", "【4.1 Room 寫入完成】已更新內容")
                
                withContext(Dispatchers.Main) {
                    _uiState.update { currentState ->
                        currentState.copy(fullTextContent = newContent)
                    }
                    _allTexts.value = _allTexts.value.map { 
                        if (it.id == _uiState.value.currentTextId) updatedText else it 
                    }
                    splitContentToParagraphs(newContent)
                }
            }
        }
    }
    
    // 進度保存
    fun updateProgress(itemId: Int, index: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                appDao.saveLastPlayedIndex(itemId, index)
                Log.d("ProgressDebug", "已保存進度: ID=$itemId, Index=$index")
            } catch (e: Exception) {
                Log.e("ProgressDebug", "儲存進度失敗", e)
            }
        }
    }
    
    // 進度讀取
    suspend fun getLastIndex(itemId: Int): Int {
        return withContext(Dispatchers.IO) {
            try {
                appDao.getLastPlayedIndex(itemId) ?: 0
            } catch (e: Exception) {
                Log.e("ProgressDebug", "讀取進度失敗", e)
                0
            }
        }
    }
    
    fun selectText(text: TextEntity) {
        _uiState.update { currentState ->
            currentState.copy(
                currentMode = AppMode.READ,
                currentTextTitle = text.title,
                fullTextContent = text.content,
                currentTextId = text.id, // 新增 ID 追蹤
                currentParagraphIndex = 0,
                previewParagraphIndex = 0,
                currentSentences = emptyList(),
                currentSentenceIndex = 0,
                isPlaying = false
            )
        }
        splitContentToParagraphs(text.content)
    }

    fun splitContentToParagraphs(content: String) {
        val newParagraphs = content.split("\n")
            .mapIndexed { index, paragraph ->
                val trimmed = paragraph.trim()
                if (trimmed.isNotBlank()) "${index + 1}. $trimmed\n" else "" // 在段落結尾加入換行
            }
            .filter { it.isNotBlank() }
        
        _uiState.update { currentState ->
            currentState.copy(paragraphs = newParagraphs)
        }
    }

    // 將段落切分為句子
    private fun splitParagraphIntoSentences(paragraph: String): List<String> {
        // 獲取啟用的標點符號
        val activeSymbols = punctuationList.value
            .filter { it.isChecked }
            .map { it.symbol }
        
        if (activeSymbols.isEmpty()) {
            // 如果沒有啟用的符號，返回整個段落作為單一句子
            return listOf(paragraph)
        }
        
        // 建立全形半形對應表
        val fullWidthToHalfWidth = mapOf(
            '，' to ',',
            '。' to '.',
            '；' to ';',
            '？' to '?',
            '！' to '!',
            '：' to ':',
            '／' to '/',   // 正斜線全形到半形
            '＼' to '\\'   // 反斜線全形到半形（注意：使用雙反斜線表示單個反斜線）
        )
        
        // 擴展啟用符號列表（加入對應的全形/半形符號）
        val extendedSymbols = activeSymbols.flatMap { symbol ->
            if (symbol.length == 1) {
                val char = symbol[0]
                val mappedChar = fullWidthToHalfWidth[char] ?: fullWidthToHalfWidth.entries
                    .firstOrNull { it.value == char }?.key
                if (mappedChar != null) {
                    listOf(symbol, mappedChar.toString())
                } else {
                    listOf(symbol)
                }
            } else {
                listOf(symbol)
            }
        }.distinct()
        
        // 轉義符號並構建正則表達式
        val escapedSymbols = extendedSymbols.joinToString("") { Regex.escape(it) }
        val regex = "(?<=[$escapedSymbols])".toRegex()
        
        // 使用正則切分句子
        return paragraph.split(regex)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    // 切換符號的選中狀態
    fun togglePunctuation(symbol: String) {
        val currentList = punctuationList.value.toMutableList()
        val index = currentList.indexOfFirst { it.symbol == symbol }
        if (index != -1) {
            val item = currentList[index]
            currentList[index] = item.copy(isChecked = !item.isChecked)
            punctuationList.value = currentList
            savePunctuationListToStore(currentList)
            // 更新當前播放的句子
            updateCurrentSentences()
        }
    }

    // 新增自定義符號
    fun addCustomPunctuation(symbol: String) {
        val trimmedSymbol = symbol.trim()
        if (trimmedSymbol.isEmpty()) return

        // 檢查是否已存在相同符號
        val exists = punctuationList.value.any { it.symbol == trimmedSymbol }
        if (!exists) {
            val newItem = PunctuationItem(trimmedSymbol, trimmedSymbol, true, true)
            val updatedList = punctuationList.value + newItem
            punctuationList.value = updatedList
            savePunctuationListToStore(updatedList)
            // 更新當前播放的句子
            updateCurrentSentences()
        }
    }

    // 更新當前段落的句子列表
    private fun updateCurrentSentences() {
        val paragraph = if (_uiState.value.currentParagraphIndex < _uiState.value.paragraphs.size) {
            _uiState.value.paragraphs[_uiState.value.currentParagraphIndex]
        } else ""
        val sentences = splitParagraphIntoSentences(paragraph)
        // 確保每次都創建新的列表實例
        val newSentences = sentences.toList()
        val newIndex = _uiState.value.currentSentenceIndex.coerceAtMost(newSentences.size - 1)
        _uiState.update { currentState ->
            currentState.copy(
                currentSentences = newSentences,
                currentSentenceIndex = newIndex
            )
        }
    }
}

class MainViewModelFactory(private val appDao: AppDao, private val dataStore: DataStore<Preferences>, private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(appDao, dataStore, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
