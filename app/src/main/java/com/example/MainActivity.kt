package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.core.view.WindowCompat
import com.example.ui.theme.LocalVelarisPalette
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppBuildInfoRepository
import com.example.ui.TechCheckScreen
import com.example.ui.TechCheckViewModel
import com.example.ui.theme.VelarisTheme

class MainActivity : ComponentActivity() {
    private var returnRequest by androidx.compose.runtime.mutableStateOf(0)
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == com.example.service.ChantalReturnScheduler.OPEN_CHANTAL) returnRequest++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val recovery = runCatching {
            com.example.data.RestoreStartupGate.ensureRecovered(this)
        }
        if (recovery.isFailure) {
            setContent { androidx.compose.material3.Text("Не удалось подготовить данные приложения. Перезапусти приложение; при повторной ошибке сохрани её для диагностики.") }
            return
        }
        if (intent?.action == com.example.service.ChantalReturnScheduler.OPEN_CHANTAL) returnRequest++
        runCatching {
            val saved = com.example.data.ChantalProgressRepository(this).load()
            com.example.service.ChantalReturnScheduler.sync(this, saved.optLong("activeJourneyStartMs", 0))
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        val repository = AppBuildInfoRepository(applicationContext)

        setContent {
            val viewModel: TechCheckViewModel = viewModel(
                factory = TechCheckViewModel.Factory(applicationContext, repository)
            )
            androidx.compose.runtime.LaunchedEffect(returnRequest) {
                if (returnRequest > 0) viewModel.openChantalReturn()
            }
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            VelarisTheme(themeId = settings.themeId) {
                val darkIcons = LocalVelarisPalette.current.surface.luminance() > 0.179f
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = darkIcons
                        isAppearanceLightNavigationBars = darkIcons
                    }
                }
                TechCheckScreen(viewModel = viewModel)
            }
        }
    }
}
