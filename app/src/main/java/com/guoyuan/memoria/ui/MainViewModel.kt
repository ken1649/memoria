package com.guoyuan.memoria.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    fun updateMode(newMode: AppMode) {
        _uiState.update { currentState ->
            currentState.copy(currentMode = newMode)
        }
    }

    fun updateText(title: String, content: String) {
        _uiState.update { currentState ->
            currentState.copy(
                currentTextTitle = title,
                fullTextContent = content,
                isLoading = false
            )
        }
    }

    fun updateSentences(newSentences: List<String>) {
        _uiState.update { currentState ->
            currentState.copy(sentences = newSentences)
        }
    }

    fun updatePlayingIndex(index: Int) {
        _uiState.update { currentState ->
            currentState.copy(currentPlayingIndex = index)
        }
    }

    fun setLoading(isLoading: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(isLoading = isLoading)
        }
    }
}
