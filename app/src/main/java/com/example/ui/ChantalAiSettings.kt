package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import kotlinx.coroutines.launch

@Composable
internal fun ChantalAiSettings(
    credentialFactory: ((String) -> ChantalGeminiAccess)? = null,
    checkConnection: (suspend (String) -> String)? = null
) {
    val context = LocalContext.current
    val access = remember(context) { ChantalAiAccess(context) }
    var provider by remember { mutableStateOf(access.provider()) }
    key(provider) {
        val credential = remember { credentialFactory?.invoke(provider) ?: ChantalGeminiAccess(context, provider) }
        var hasKey by remember { mutableStateOf(credential.hasKey()) }
        var model by remember { mutableStateOf(credential.model()) }
        var busy by remember { mutableStateOf(false) }
        var message by remember { mutableStateOf("") }
        var keyDialog by remember { mutableStateOf(false) }
        var modelDialog by remember { mutableStateOf(false) }
        var preview by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope() // Disposing this page cancels UI work and its loading state.
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Модель для историй", style = MaterialTheme.typography.titleLarge)
            listOf("openrouter" to "Grok · OpenRouter", "gemini" to "Gemini").forEach { (id, label) ->
                FilterChip(selected = provider == id, onClick = {
                    if (!busy && id != provider) {
                        runCatching { access.select(id) }.onSuccess { provider = id }
                            .onFailure { message = "Не удалось сохранить выбор модели. Повтори позже." }
                    }
                }, enabled = !busy, label = { Text(label) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("ai_provider_$id"))
            }
            Text(model, modifier = Modifier.testTag("ai_current_model"))
            Text(if (hasKey) "Ключ сохранён на телефоне" else "Ключ не добавлен", modifier = Modifier.testTag("ai_key_status"))
            Button(onClick = {
                if (!busy && hasKey) {
                    if (!access.claimRequest()) message = "Между запросами нужна минута."
                    else {
                        busy = true; message = "Проверяем подключение…"
                        scope.launch {
                            try {
                                val response = checkConnection?.invoke(provider)
                                    ?: access.transport(provider).generate("Return only JSON: {\"ok\":true}")
                                message = if (org.json.JSONObject(response).optBoolean("ok")) "Модель отвечает. Можно создавать истории." else "Ответ проверки не распознан."
                            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                              catch (error: Exception) { message = storyFailureMessage(error) }
                            finally { busy = false }
                        }
                    }
                }
            }, enabled = hasKey && !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("ai_check_connection")) { Text(if (busy) "Проверяем…" else "Проверить подключение") }
            if (busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth().testTag("ai_check_loading"))
                Text("После закрытия раздела уже отправленный запрос всё равно может быть оплачен.", style = MaterialTheme.typography.bodySmall)
            }
            Text(if (provider == "openrouter") "Проверка отправит один короткий платный запрос OpenRouter." else "Проверка отправит один короткий запрос Gemini.", style = MaterialTheme.typography.bodyMedium)
            AiSettingsMessage(message)
            OutlinedButton(onClick = { keyDialog = true }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("ai_manage_key")) { Text("Управление ключом") }
            OutlinedButton(onClick = { modelDialog = true }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("ai_manage_model")) { Text("Настройки модели") }
            OutlinedButton(onClick = { preview = true }, enabled = hasKey && !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("ai_open_preview")) { Text("Пробная история · без изменения игры") }
        }
        if (keyDialog) {
            var secret by remember { mutableStateOf("") } // Never saved in instance state or a game backup.
            var deleting by remember { mutableStateOf(false) }
            var keyMessage by remember { mutableStateOf("") }
            AiSettingsDetail("Ключ ${access.label(provider)}", onDismiss = { keyDialog = false }) {
                Text("Ключ хранится только на этом телефоне и не входит в резервную копию игры.")
                OutlinedTextField(secret, { secret = it }, label = { Text("API-ключ") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth().testTag("ai_key_input"))
                Button(onClick = {
                    if (secret.isNotBlank()) keyMessage = if (runCatching { credential.save(secret.trim()) }.isSuccess) {
                        secret = ""; hasKey = true; "Ключ сохранён. Запрос не отправлялся."
                    } else "Не удалось сохранить ключ. Проверь формат или введи заново. Код KEY_STORAGE."
                }, enabled = secret.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("ai_save_key")) { Text(if (hasKey) "Заменить ключ" else "Сохранить ключ") }
                if (hasKey) OutlinedButton(onClick = { deleting = true }, modifier = Modifier.fillMaxWidth().testTag("ai_delete_key")) { Text("Удалить ключ") }
                if (deleting && hasKey) {
                    Text("Удалить ключ с телефона? Для новых запросов потребуется добавить его снова. Игра сохранится.")
                    Button(onClick = {
                        if (hasKey) keyMessage = if (runCatching { credential.remove() }.isSuccess) {
                            hasKey = false; deleting = false; secret = ""; "Ключ удалён с телефона."
                        } else "Не удалось удалить ключ. Код KEY_DELETE."
                    }, modifier = Modifier.fillMaxWidth().testTag("ai_confirm_delete")) { Text("Подтвердить удаление") }
                    OutlinedButton(onClick = { deleting = false }, modifier = Modifier.fillMaxWidth().testTag("ai_cancel_delete")) { Text("Отмена") }
                }
                AiSettingsMessage(keyMessage)
            }
        }
        if (modelDialog) {
            var input by remember { mutableStateOf(model) }
            var modelMessage by remember { mutableStateOf("") }
            AiSettingsDetail("Настройки модели", onDismiss = { modelDialog = false }) {
                if (provider == "gemini") {
                    OutlinedTextField(input, { input = it }, label = { Text("ID модели Gemini") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("ai_model_input"))
                    Button(onClick = {
                        modelMessage = if (runCatching { credential.setModel(input.trim()) }.isSuccess) {
                            model = credential.model(); message = "Модель сохранена. Запрос не отправлялся."; message
                        } else "Проверь ID модели Gemini. Запрос не отправлялся."
                    }, modifier = Modifier.fillMaxWidth().testTag("ai_save_model")) { Text("Сохранить модель") }
                    OutlinedButton(onClick = {
                        modelMessage = if (runCatching { credential.setModel(ChantalGeminiAccess.DEFAULT_MODEL) }.isSuccess) {
                            model = credential.model(); input = model; message = "Стандартная модель сохранена. Запрос не отправлялся."; message
                        } else "Не удалось сохранить модель."
                    }, modifier = Modifier.fillMaxWidth()) { Text("Вернуть стандартную модель Gemini") }
                } else {
                    Text(OpenRouterStoryTransport.DEFAULT_MODEL)
                    Text("В текущем подключении OpenRouter используется эта модель Grok.")
                }
                Text("Запросы отправляются только по кнопке. Передаются сюжетный контекст, игровые предпочтения и оценки рассказов. Повторов и автоматической смены модели нет.")
                AiSettingsMessage(modelMessage)
            }
        }
        if (preview) ChantalStoryPreview(onDismiss = { preview = false }) {
            if (!access.claimRequest()) throw GeminiLocalQuotaException()
            val copy = org.json.JSONObject(ChantalProgressRepository(context).load().toString())
            copy.remove("narrativeBinding")
            copy.put("pendingEpisode", ChantalProgressCodec.encode(ChantalOfflineAdventure.decorate(ChantalOfflineAdventure.previewBase())))
            val binding = ChantalNarrativeSession.binding(copy, java.security.SecureRandom().nextLong())
            val description = ChantalNarrativeSession.describe(copy, binding)
            val raw = access.transport(provider).generate(ChantalNarrativeSession.prompt(copy, description))
            val text = ChantalNarrativeSession.parse(raw, copy, description)
            ChantalGenerationResult.Ready(ChantalGeneratedStory(ChantalNarrativeSession.rendered(description, text), emptyList()))
        }
    }
}

@Composable
private fun AiSettingsMessage(message: String) {
    if (message.isNotBlank()) Text(message, modifier = Modifier.testTag("ai_status_message").semantics { liveRegion = LiveRegionMode.Polite })
}

@Composable
private fun AiSettingsDetail(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(16.dp).widthIn(max = 560.dp).fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * .82f).dp)) {
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                content()
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("ai_detail_close")) { Text("Закрыть") }
            }
        }
    }
}
