package com.guoyuan.memoria.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.guoyuan.memoria.R
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale

@Composable
fun TutorialDialog(onDismiss: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 10 })

    // 改用基礎的 Dialog，這讓你擁有完全的控制權
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false // 關閉預設寬度限制
        )
    ) {
        // 使用 Surface 包覆，設定為幾乎全螢幕
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.98f) // 寬度佔螢幕 95%
                .fillMaxHeight(0.95f), // 高度佔螢幕 95%
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 讓滑動區域盡量撐開空間
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) { page ->
                    val imageRes = getTutorialImage(page) // 建議封裝邏輯
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = "教學步驟",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit // 確保圖片比例正確
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "${pagerState.currentPage + 1} / ${pagerState.pageCount}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("向左滑動查看更多", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 5.dp)) {
                    Text("關閉")
                }
            }
        }
    }
}

fun getTutorialImage(page: Int): Int {
    return when (page) {
        0 -> R.drawable.tutorial_1
        1 -> R.drawable.tutorial_2
        2 -> R.drawable.tutorial_3
        3 -> R.drawable.tutorial_4
        4 -> R.drawable.tutorial_5
        5 -> R.drawable.tutorial_6
        6 -> R.drawable.tutorial_7
        7 -> R.drawable.tutorial_8
        8 -> R.drawable.tutorial_9
        else -> R.drawable.tutorial_10
    }
}
