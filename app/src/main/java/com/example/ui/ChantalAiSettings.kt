package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.*
import kotlinx.coroutines.launch

@Composable
internal fun ChantalAiSettings() {
    val context=LocalContext.current
    val access=remember(context) { ChantalAiAccess(context) }
    var provider by remember { mutableStateOf(access.provider()) }
    var busy by remember { mutableStateOf(false) }
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Модель для историй",style=MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            FilterChip(selected=provider=="openrouter",onClick={access.select("openrouter");provider="openrouter"},enabled=!busy,label={Text("Grok · основной")})
            FilterChip(selected=provider=="gemini",onClick={access.select("gemini");provider="gemini"},enabled=!busy,label={Text("Gemini")})
        }
        key(provider) {
            val credential=remember { access.credential() }
            var secret by remember { mutableStateOf("") }
            var hasKey by remember { mutableStateOf(credential.hasKey()) }
            var editing by remember { mutableStateOf(!hasKey) }
            var message by remember { mutableStateOf("") }
            var preview by remember { mutableStateOf(false) }
            val scope=rememberCoroutineScope()
            Text(if(provider=="openrouter") OpenRouterStoryTransport.DEFAULT_MODEL else credential.model())
            Text("Ключ хранится только на этом телефоне и не входит в резервную копию игры.")
            if(editing) {
                OutlinedTextField(secret,{secret=it},label={Text("API-ключ ${if(provider=="openrouter") "OpenRouter" else "Gemini"}")},
                    visualTransformation=PasswordVisualTransformation(),singleLine=true,enabled=!busy,
                    keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Password),
                    modifier=Modifier.fillMaxWidth().testTag("ai_key_input"))
                Button(onClick={
                    message=if(runCatching { credential.save(secret.trim()) }.isSuccess) {
                        secret="";hasKey=true;editing=false;"Ключ сохранён. Запрос не отправлялся."
                    } else "Не удалось сохранить ключ. Проверь формат или введи заново. Код KEY_STORAGE."
                },enabled=!busy && secret.isNotBlank(),modifier=Modifier.fillMaxWidth().testTag("ai_save_key")) { Text("Сохранить ключ") }
            } else TextButton(onClick={editing=true},enabled=!busy) { Text("Заменить ключ") }
            if(hasKey) {
                Button(onClick={
                    if(!access.claimRequest()) message="Между запросами нужна минута."
                    else {
                        busy=true;message="Проверяем подключение…"
                        scope.launch {
                            try {
                                val response=access.transport(provider).generate("Return only JSON: {\"ok\":true}")
                                message=if(org.json.JSONObject(response).optBoolean("ok")) "Модель отвечает. Можно создавать истории." else "Ответ проверки не распознан."
                            } catch(cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                              catch(error: Exception) { message=storyFailureMessage(error) }
                            finally { busy=false }
                        }
                    }
                },enabled=!busy,modifier=Modifier.fillMaxWidth().testTag("ai_check_connection")) { Text("Проверить подключение") }
                Text(if(provider=="openrouter") "Один короткий платный запрос. Стоимость зависит от тарифа OpenRouter." else "Один короткий запрос к Gemini.",style=MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick={preview=true},enabled=!busy,modifier=Modifier.fillMaxWidth()) { Text("Пробная история · без ожидания") }
                TextButton(onClick={
                    message=if(runCatching { credential.remove() }.isSuccess) {
                        hasKey=false;editing=true;secret="";"Ключ удалён с телефона."
                    } else "Не удалось удалить ключ. Повтори позже. Код KEY_DELETE."
                },enabled=!busy) { Text("Удалить ключ") }
            }
            if(provider=="gemini") TextButton(onClick={credential.setModel(ChantalGeminiAccess.DEFAULT_MODEL);message="Подставлена ${ChantalGeminiAccess.DEFAULT_MODEL}"},enabled=!busy) { Text("Вернуть стандартную модель Gemini") }
            Text("Запросы отправляются только по кнопке. Передаются сюжетный контекст, игровые предпочтения и оценки рассказов. Повторов и автоматической смены модели нет.")
            if(message.isNotBlank()) Text(message)
            if(preview) ChantalStoryPreview(onDismiss={preview=false}) {
                if(!access.claimRequest()) throw GeminiLocalQuotaException()
                val snapshot=ChantalProgressRepository(context).load()
                val copy=org.json.JSONObject(snapshot.toString())
                copy.remove("narrativeBinding")
                copy.put("pendingEpisode",ChantalProgressCodec.encode(ChantalOfflineAdventure.decorate(ChantalOfflineAdventure.previewBase())))
                val binding=ChantalNarrativeSession.binding(copy,java.security.SecureRandom().nextLong())
                val description=ChantalNarrativeSession.describe(copy,binding)
                val raw=access.transport(provider).generate(ChantalNarrativeSession.prompt(copy,description))
                val text=ChantalNarrativeSession.parse(raw,copy,description)
                ChantalGenerationResult.Ready(ChantalGeneratedStory(ChantalNarrativeSession.rendered(description,text),emptyList()))
            }
        }
    }
}
