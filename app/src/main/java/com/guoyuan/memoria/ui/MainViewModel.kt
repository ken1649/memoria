package com.guoyuan.memoria.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.guoyuan.memoria.data.AppDao
import com.guoyuan.memoria.data.TextEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class MainViewModel(private val appDao: AppDao) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()
    
    private val _allTexts = MutableStateFlow<List<TextEntity>>(emptyList())
    val allTexts: StateFlow<List<TextEntity>> = _allTexts.asStateFlow()

    init {
        viewModelScope.launch {
            loadAllTexts()
        }
    }
    
    private suspend fun loadAllTexts() {
        withContext(Dispatchers.IO) {
            _allTexts.value = appDao.getAllTexts()
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
    
    fun togglePlay() {
        _uiState.update { currentState ->
            currentState.copy(isPlaying = !currentState.isPlaying)
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
}

class MainViewModelFactory(private val appDao: AppDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(appDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
