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

@Composable
fun TutorialDialog(onDismiss: () -> Unit) {
    // 總共有 3 張教學圖
    val pagerState = rememberPagerState(pageCount = { 3 })

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("關閉") }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 滑動區域
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().height(300.dp)
                ) { page ->
                    val imageRes = when (page) {
                        //0 -> R.drawable.tutorial_1
                        //1 -> R.drawable.tutorial_2
                        else -> 0
                    }
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = "教學步驟",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("向左滑動查看更多")
            }
        }
    )
}
