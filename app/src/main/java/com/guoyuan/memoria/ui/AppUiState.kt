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
    val fontSize: Float = 18f, // 新增：字體大小狀態
    val backupFontSize: Float = 18f, // 新增：備份字體大小（用於取消時還原）
    val isLoading: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showPunctuationDialog: Boolean = false,
    val isEditingReadingMode: Boolean = false,
    val isEditingTitleDialogVisible: Boolean = false,
    val isSidebarManagementMode: Boolean = false,
    val favoriteTextId: Int? = null,
    val currentTextId: Int? = null,
    val isAddingNewText: Boolean = false, // 新增：是否正在新增文本
    val showFontSizeDialog: Boolean = false // 新增：字体大小对话框状态
)
