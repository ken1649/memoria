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
    val currentSentences: List<String> = emptyList(), // 新增：當前段落的句子列表
    val currentSentenceIndex: Int = 0, // 新增：當前播放的句子索引
    val fullTextContent: String = "",
    val currentTextTitle: String = "",
    val isLoading: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showPunctuationDialog: Boolean = false,
    val isEditingReadingMode: Boolean = false,
    val isEditingTitleDialogVisible: Boolean = false
)
