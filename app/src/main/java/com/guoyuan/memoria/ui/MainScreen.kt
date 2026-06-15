package com.guoyuan.memoria.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Memoria") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // 文字顯示區域
            Text(
                text = if (uiState.isPlaying) {
                    // 播放模式顯示當前段落
                    if (uiState.paragraphs.isNotEmpty() && uiState.currentParagraphIndex < uiState.paragraphs.size) {
                        uiState.paragraphs[uiState.currentParagraphIndex]
                    } else {
                        "無可用內容"
                    }
                } else {
                    // 非播放模式顯示全文
                    uiState.fullTextContent.ifEmpty { "請輸入內容或載入網頁" }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 段落選擇滑桿
            if (uiState.paragraphs.isNotEmpty()) {
                Slider(
                    value = uiState.previewParagraphIndex.toFloat(),
                    onValueChange = { newValue ->
                        viewModel.updatePreviewIndex(newValue.toInt())
                    },
                    onValueChangeFinished = {
                        viewModel.confirmParagraphSelection(uiState.previewParagraphIndex)
                    },
                    valueRange = 0f..(uiState.paragraphs.size - 1).toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
