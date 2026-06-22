package com.guoyuan.memoria

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.guoyuan.memoria.data.AppDatabase
import com.guoyuan.memoria.ui.MainScreen
import com.guoyuan.memoria.ui.MainViewModel
import com.guoyuan.memoria.ui.MainViewModelFactory
import com.guoyuan.memoria.ui.theme.MemoriaTheme
import com.guoyuan.memoria.dataStore // 更新导入路径
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(
                AppDatabase.getDatabase(this).appDao(),
                this.dataStore,
                context
            ))
            val uiState by viewModel.uiState.collectAsState()
            
            MemoriaTheme(appTheme = uiState.currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(context = context)
                }
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            // Initialize the Google Mobile Ads SDK on a background thread.
            MobileAds.initialize(this@MainActivity) {
                // 在初始化 MobileAds 時加入
                val configuration = RequestConfiguration.Builder()
                    .setTestDeviceIds(listOf("C1A38187113D4EA428D15C4885B6725E")) // 這裡填入你的手機硬體ID
                    .build()

                MobileAds.setRequestConfiguration(configuration)
            }
        }
    }
}
