package com.guoyuan.memoria.ui

enum class AppMode {
    READ,
    PLAY,
    EDIT
}

data class AppUiState(
    val currentMode: AppMode = AppMode.READ,
    val currentTextTitle: String = "",
    val fullTextContent: String = "",
    val sentences: List<String> = emptyList(),
    val currentPlayingIndex: Int = 0,
    val isLoading: Boolean = false
)
