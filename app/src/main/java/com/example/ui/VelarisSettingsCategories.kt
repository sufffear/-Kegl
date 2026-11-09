package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
internal fun VelarisSettingsCategories(onOpen: (String) -> Unit) {
    val categories = listOf(
        Triple("appearance", "Оформление", "Тема и цвета приложения"),
        Triple("sound", "Звук и вибрация", "Профиль, громкость, метроном и фоновая музыка"),
        Triple("backup", "Резервные копии", "Сохранить игру и занятия или восстановить ZIP"),
        Triple("samples", "Образцы команд", "Прослушать сигналы тренировки"),
        Triple("about", "О приложении", "Сведения и диагностика")
    )
    categories.forEach { (id, title, summary) ->
        OutlinedButton(onClick = { onOpen(id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("settings_category_$id")) {
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(summary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
