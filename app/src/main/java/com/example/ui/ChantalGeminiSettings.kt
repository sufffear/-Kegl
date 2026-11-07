package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.ChantalGeminiAccess

@Composable
internal fun ChantalGeminiSettings() {
    val context = LocalContext.current
    val access = remember(context) { ChantalGeminiAccess(context) }
    var secret by remember { mutableStateOf("") }
    var model by remember { mutableStateOf(access.model()) }
    var hasKey by remember { mutableStateOf(access.hasKey()) }
    var message by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Gemini · личный API-ключ", style = MaterialTheme.typography.titleMedium)
        Text("Запросы отправляются только кнопкой под непрочитанной историей. Для бесплатного использования нужен проект Free Tier без биллинга. Приложение не определяет тариф проекта.")
        Text("В Google передаётся игровая память. На бесплатном уровне Google может использовать запросы и ответы для улучшения продуктов.")
        Text(if (hasKey) "Ключ сохранён на этом телефоне" else "Ключ ещё не сохранён")
        OutlinedTextField(secret, { secret = it }, label = { Text("API-ключ") },
            visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(model, { model = it }, label = { Text("ID модели Gemini") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            message = if (runCatching { access.setModel(model.trim()); if (secret.isNotBlank()) access.save(secret.trim()) }.isSuccess) {
                secret = ""; hasKey = access.hasKey(); "Сохранено. Это действие не отправляет запрос."
            } else "Не удалось сохранить. Проверь поля; при ошибке хранилища введи ключ заново."
        }, modifier = Modifier.fillMaxWidth()) { Text("Сохранить настройки Gemini") }
        if (hasKey) OutlinedButton(onClick = {
            message = if (runCatching { access.remove() }.isSuccess) { hasKey = false; "Ключ удалён" } else "Не удалось удалить ключ"
            secret = ""
        }, modifier = Modifier.fillMaxWidth()) { Text("Удалить ключ с телефона") }
        if (message.isNotBlank()) Text(message)
    }
}
