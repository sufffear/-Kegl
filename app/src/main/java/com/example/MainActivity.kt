package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppBuildInfoRepository
import com.example.ui.TechCheckScreen
import com.example.ui.TechCheckViewModel
import com.example.ui.theme.VelarisTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = AppBuildInfoRepository(applicationContext)

        setContent {
            VelarisTheme {
                val viewModel: TechCheckViewModel = viewModel(
                    factory = TechCheckViewModel.Factory(repository)
                )
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                TechCheckScreen(uiState = uiState)
            }
        }
    }
}
