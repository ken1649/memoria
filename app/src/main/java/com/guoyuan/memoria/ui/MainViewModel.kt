package com.guoyuan.memoria.ui

import android.content.Context
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

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class PunctuationItem(val symbol: String, val label: String, var isChecked: Boolean, val isCustom: Boolean = false)

class MainViewModel(private val appDao: AppDao, private val dataStore: DataStore<Preferences>) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()
    
    private val _allTexts = MutableStateFlow<List<TextEntity>>(emptyList())
    val allTexts: StateFlow<List<TextEntity>> = _allTexts.asStateFlow()

    val punctuationList = MutableStateFlow<List<PunctuationItem>>(emptyList())
    private val gson = Gson()
    private val PUNCTUATION_LIST_KEY = stringPreferencesKey("punctuation_list_json")

    init {
        viewModelScope.launch {
            loadAllTexts()
            loadPunctuationListFromStore()
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
                    PunctuationItem(" ", "空白", true)
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
            currentState.copy(currentMode = newMode)
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
                val textEntity = TextEntity(
                    title = title,
                    fullContent = content,
                    sourceUrl = ""
                )
                appDao.insertText(textEntity)
                loadAllTexts() // 重新加载文章列表

                withContext(Dispatchers.Main) {
                    _uiState.update { currentState ->
                        currentState.copy(
                            currentMode = AppMode.READ,
                            isLoading = false
                        )
                    }
                    // 保存後重新解析內容以更新顯示
                    splitContentToParagraphs(content)
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
                val text = withContext(Dispatchers.IO) {
                    Jsoup.connect(url).get().text()
                }
                updateEditTitle("網頁內容")
                updateEditContent(text)
            } catch (e: Exception) {
                e.printStackTrace()
                setLoading(false)
            }
        }
    }

    fun updatePreviewIndex(index: Int) {
        _uiState.update { currentState ->
            currentState.copy(previewParagraphIndex = index)
        }
    }

    fun confirmParagraphSelection(index: Int) {
        _uiState.update { currentState ->
            currentState.copy(
                currentParagraphIndex = index,
                previewParagraphIndex = index,  // 同步預覽索引
                isPlaying = false
            )
        }
    }
    
    fun startPlay() {
        _uiState.update { currentState ->
            currentState.copy(isPlaying = true)
        }
    }

    fun moveToPrevious() {
        _uiState.update { currentState ->
            val newIndex = if (currentState.previewParagraphIndex > 0) {
                currentState.previewParagraphIndex - 1
            } else {
                0
            }
            currentState.copy(previewParagraphIndex = newIndex)
        }
    }

    fun resetPlayback() {
        _uiState.update { currentState ->
            currentState.copy(
                previewParagraphIndex = 0,
                isPlaying = false
            )
        }
    }

    fun openSettings() {
        _uiState.update { currentState ->
            currentState.copy(showSettingsDialog = true)
        }
    }

    fun closeSettings() {
        _uiState.update { currentState ->
            currentState.copy(showSettingsDialog = false)
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
    
    fun selectText(text: TextEntity) {
        _uiState.update { currentState ->
            currentState.copy(
                currentMode = AppMode.READ,
                currentTextTitle = text.title,
                fullTextContent = text.fullContent,
                currentParagraphIndex = 0,
                previewParagraphIndex = 0,
                isPlaying = false
            )
        }
        splitContentToParagraphs(text.fullContent)
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

    // 切換符號的選中狀態
    fun togglePunctuation(symbol: String) {
        val currentList = punctuationList.value.toMutableList()
        val index = currentList.indexOfFirst { it.symbol == symbol }
        if (index != -1) {
            val item = currentList[index]
            currentList[index] = item.copy(isChecked = !item.isChecked)
            punctuationList.value = currentList
            savePunctuationListToStore(currentList)
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
        }
    }
}

class MainViewModelFactory(private val appDao: AppDao, private val dataStore: DataStore<Preferences>) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(appDao, dataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
