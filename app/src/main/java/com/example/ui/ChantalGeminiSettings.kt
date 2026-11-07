package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.data.ChantalGeminiAccess

@Composable
internal fun ChantalGeminiSettings() {
    val context = LocalContext.current
    val access = remember(context) { ChantalGeminiAccess(context) }
    var secret by remember { mutableStateOf("") }
    var model by remember { mutableStateOf(access.model()) }
    var hasKey by remember { mutableStateOf(access.hasKey()) }
    var editKey by remember { mutableStateOf(!hasKey) }
    var editModel by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf(false) }
    var help by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var keyMessage by remember { mutableStateOf("") }
    var modelMessage by remember { mutableStateOf("") }
    var connectionMessage by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Подключение Gemini", style = MaterialTheme.typography.titleLarge)
                Text(if (hasKey) "Ключ сохранён на телефоне" else "Добавь API-ключ из AI Studio")
                if (editKey) {
                    OutlinedTextField(secret, { secret = it; keyMessage = "" }, label = { Text("API-ключ") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                        singleLine = true, enabled = !checking, modifier = Modifier.fillMaxWidth().testTag("gemini_key_input"))
                    Button(onClick = {
                        val value = secret.trim()
                        if (runCatching { ChantalGeminiAccess.validateSecret(value) }.isFailure) {
                            keyMessage = "Проверь ключ: вставь целиком, без кавычек, пробелов и переноса строки. Код KEY_FORMAT."
                        } else {
                            val saved = runCatching { access.save(value) }
                            if (saved.isSuccess) {
                                secret = ""; hasKey = true; editKey = false
                                keyMessage = "Ключ сохранён. Запрос не отправлялся."
                            } else keyMessage = "Телефон не сохранил ключ в защищённом хранилище. Попробуй ещё раз и сообщи код KEY_STORAGE, если повторится."
                        }
                    }, enabled = !checking && secret.isNotBlank(), modifier = Modifier.fillMaxWidth().testTag("gemini_save_key")) { Text("Сохранить ключ") }
                } else TextButton(onClick = { editKey = true }, enabled = !checking) { Text("Заменить ключ") }
                if (keyMessage.isNotBlank()) Text(keyMessage)
                if (hasKey) {
                    Button(onClick = {
                        if (!access.claimRequest()) connectionMessage = "Между запросами нужна минута. Подожди немного."
                        else {
                            checking = true; connectionMessage = "Проверяем…"
                            scope.launch {
                                try {
                                    val secretValue = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { access.read() }
                                    val reply = com.example.data.GeminiStoryTransport(secretValue, access.model())
                                        .generate("Return only this JSON: {\"ok\":true}")
                                    connectionMessage = if (org.json.JSONObject(reply).optBoolean("ok")) "Gemini отвечает. Можно создавать истории." else "Ответ не прошёл проверку."
                                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                                  catch (error: Exception) { connectionMessage = com.example.data.geminiFailureMessage(error) }
                                finally { checking = false }
                            }
                        }
                    }, enabled = !checking, modifier = Modifier.fillMaxWidth()) { Text(if (checking) "Проверяем…" else "Проверить подключение") }
                    Text("Проверка отправит один короткий запрос.", style = MaterialTheme.typography.bodySmall)
                }
                if (hasKey) OutlinedButton(onClick = { preview = true }, enabled = !checking,
                    modifier = Modifier.fillMaxWidth().testTag("open_story_preview")) { Text("Пробная история · без ожидания") }
                if (connectionMessage.isNotBlank()) Text(connectionMessage)
            }
        }
        OutlinedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Модель", style = MaterialTheme.typography.titleMedium)
                Text(access.model())
                TextButton(onClick = { editModel = !editModel }, enabled = !checking) { Text(if (editModel) "Свернуть" else "Изменить модель") }
                if (editModel) {
                    OutlinedTextField(model, { model = it }, label = { Text("Полный ID из AI Studio") },
                        singleLine = true, modifier = Modifier.fillMaxWidth().testTag("gemini_model_input"))
                    Text("Например: ${ChantalGeminiAccess.DEFAULT_MODEL}. Короткое название вроде «3.8» может не совпадать с API ID.")
                    OutlinedButton(onClick = {
                        model = ChantalGeminiAccess.DEFAULT_MODEL
                    }, modifier = Modifier.fillMaxWidth()) { Text("Подставить предложенную модель") }
                    Button(onClick = {
                        modelMessage = if (runCatching { access.setModel(model.trim()) }.isSuccess) "Модель сохранена. Ключ не менялся." else "Нужен ID вида gemini-…; проверь название или сообщи код MODEL_SAVE."
                    }, modifier = Modifier.fillMaxWidth()) { Text("Сохранить модель") }
                }
                if (modelMessage.isNotBlank()) Text(modelMessage)
            }
        }
        TextButton(onClick = { help = !help }, modifier = Modifier.fillMaxWidth()) { Text(if (help) "Скрыть сведения" else "О бесплатной квоте и данных") }
        if (help) {
            Text("Бесплатная квота зависит от модели и проекта. Выбери в AI Studio Free Tier без биллинга: приложение не определяет тариф по ключу.")
            Text("Запросы отправляются только по кнопке проверки или создания истории. В историю передаётся игровая память; Google может использовать данные Free Tier для улучшения продуктов.")
            Text("Ключ хранится зашифрованным на этом телефоне и не входит в игровую резервную копию.")
            if (hasKey) OutlinedButton(onClick = {
                keyMessage = if (runCatching { access.remove() }.isSuccess) { hasKey = false; editKey = true; "Ключ удалён" } else "Не удалось удалить ключ. Код KEY_DELETE."
                secret = ""
            }, enabled = !checking, modifier = Modifier.fillMaxWidth()) { Text("Удалить ключ с телефона") }
        }
    }
    if (preview) ChantalStoryPreview(onDismiss = { preview = false }) {
        if (!access.claimRequest()) {
            throw com.example.data.GeminiLocalQuotaException()
        }
        val saved = com.example.data.ChantalProgressRepository(context).load()
        val memory = saved.optJSONObject("worldMemory") ?: com.example.data.ChantalWorldMemory.empty()
        val character = com.example.domain.ChantalTraits(
            saved.optInt("curiosity", 5), saved.optInt("tenderness", 6), saved.optInt("playfulness", 4),
            saved.optInt("romanticism", 3), saved.optInt("courage", 4), saved.optInt("wit", 5))
        val secretValue = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { access.read() }
        com.example.data.ChantalStoryGeneration.generate(
            com.example.data.GeminiStoryTransport(secretValue, access.model()),
            com.example.data.ChantalOfflineAdventure.previewBase(), memory, character)
    }

}
