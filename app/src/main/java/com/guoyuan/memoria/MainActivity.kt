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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guoyuan.memoria.data.AppDatabase
import com.guoyuan.memoria.ui.MainScreen
import com.guoyuan.memoria.ui.MainViewModel
import com.guoyuan.memoria.ui.MainViewModelFactory
import com.guoyuan.memoria.ui.theme.MemoriaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(
                AppDatabase.getDatabase(this).appDao(),
                this.dataStore
            ))
            val uiState by viewModel.uiState.collectAsState()
            
            MemoriaTheme(appTheme = uiState.currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}
