package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
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
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                scrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                scrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT
            )
        )

        val repository = AppBuildInfoRepository(applicationContext)

        setContent {
            val viewModel: TechCheckViewModel = viewModel(
                factory = TechCheckViewModel.Factory(applicationContext, repository)
            )
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            VelarisTheme(themeId = settings.themeId) {
                TechCheckScreen(viewModel = viewModel)
            }
        }
    }
}
