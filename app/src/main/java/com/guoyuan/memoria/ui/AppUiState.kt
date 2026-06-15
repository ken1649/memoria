package com.guoyuan.memoria.ui

enum class AppMode {
    READ,
    PLAY,
    EDIT
}

data class AppUiState(
    val currentMode: AppMode = AppMode.READ,
    val currentParagraphIndex: Int = 0,
    val previewParagraphIndex: Int = 0,
    val isPlaying: Boolean = false,
    val paragraphs: List<String> = emptyList(),
    val fullTextContent: String = "",
    val currentTextTitle: String = "",
    val isLoading: Boolean = false
)
