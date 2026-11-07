package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import com.example.domain.ChantalStoryEpisode
import kotlinx.coroutines.launch

/** Isolated reader: choosing a preview response never invokes gameplay or persistence callbacks. */
@Composable
internal fun ChantalStoryPreview(onDismiss: () -> Unit, generate: suspend () -> ChantalGenerationResult) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var episode by remember { mutableStateOf<ChantalStoryEpisode?>(ChantalNarrativeSample.episode()) }
    var isAuthoredSample by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().safeDrawingPadding().padding(12.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Пробная история Gemini", style = MaterialTheme.typography.titleLarge)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Без ожидания похода. Энергия, награды, журнал и характер не меняются. Запрос использует игровую память и квоту Gemini.")
                    Text(if (isAuthoredSample) "Авторский образец стиля 3.0 · вне игрового журнала" else "Новый рассказ Gemini · вне игрового журнала")
                    episode?.let { ep ->
                        Text(ep.titleRu, style = MaterialTheme.typography.titleMedium)
                        Text(ep.storyTextRu, modifier = Modifier.testTag("preview_story_text"))
                        Text("Попробуй выбрать ответ:")
                        ep.choices.forEachIndexed { index, choice ->
                            OutlinedButton(onClick = { selected = index }, enabled = !busy,
                                modifier = Modifier.fillMaxWidth().testTag("preview_choice_$index")) { Text(choice.labelRu) }
                        }
                        selected?.let { index ->
                            Text("Реакция Шанталь: ${ep.choices[index].reactionRu}", modifier = Modifier.testTag("preview_reaction"))
                            Text("В игре этот ответ изменил бы черты: ${ep.choices[index].delta.formatArrowIndicators().joinToString(" · ")}. Сейчас прогресс не меняется.")
                        }
                    }
                    if (message.isNotBlank()) Text(message)
                }
                Button(onClick = {
                    busy = true; message = "Шанталь вспоминает приключение…"
                    scope.launch {
                        try {
                            when (val result = generate()) {
                                is ChantalGenerationResult.Ready -> { episode = result.story.episode; isAuthoredSample = false; selected = null; message = "Пробный рассказ. В летопись не записан." }
                                else -> message = "Рассказ не прошёл проверку или Gemini недоступен. Прогресс не менялся."
                            }
                        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                          catch (error: Exception) { message = geminiFailureMessage(error) }
                        finally { busy = false }
                    }
                }, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("preview_generate")) {
                    Text(if (busy) "Создаём рассказ…" else if (isAuthoredSample) "Получить историю Gemini · один запрос" else "Другой рассказ · один запрос")
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().testTag("preview_close")) { Text(if (busy) "Отменить и закрыть" else "Закрыть") }
            }
        }
    }
}
