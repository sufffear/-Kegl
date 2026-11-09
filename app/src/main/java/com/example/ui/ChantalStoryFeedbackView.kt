package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.*

/** Editorial preference only: caller persists it independently of story choices and rewards. */
@Composable
internal fun ChantalStoryFeedbackView(
    episodeId: String,
    existingFeedback: ChantalStoryFeedback?,
    onSave: (ChantalStoryFeedback) -> Unit
) {
    val current = existingFeedback?.takeIf { it.episodeId == episodeId }
    var expanded by rememberSaveable(episodeId) { mutableStateOf(false) }
    var rating by rememberSaveable(episodeId, current) { mutableStateOf(current?.rating?.name.orEmpty()) }
    var scope by rememberSaveable(episodeId, current) { mutableStateOf((current?.scope ?: ChantalFeedbackScope.THIS_STORY).name) }
    var reasons by rememberSaveable(episodeId, current) { mutableStateOf(current?.reasons?.map { it.name } ?: emptyList<String>()) }
    var comment by rememberSaveable(episodeId, current) { mutableStateOf(current?.comment.orEmpty()) }
    Column(Modifier.fillMaxWidth().testTag("story_feedback"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.testTag("feedback_expand")) {
            Text(if (expanded) "Скрыть оценку" else if (current != null) "Оценка сохранена · изменить" else "Как тебе рассказ? · необязательно", color = HobbitGold)
        }
        if (expanded) {
            Text("Это пожелание к рассказам. На награды и отношение Шанталь оно не влияет.", color = HobbitMuted)
            ChantalStoryRating.entries.forEach { value ->
                FilterChip(selected = rating == value.name, onClick = { rating = value.name },
                    label = { Text(when (value) {
                        ChantalStoryRating.LIKED -> "Понравилось"
                        ChantalStoryRating.OKAY -> "Нормально"
                        ChantalStoryRating.DISLIKED -> "Не понравилось"
                    }) }, modifier = Modifier.testTag("feedback_rating_${value.name}"))
            }
            Text("Что запомнилось?", color = HobbitCream)
            ChantalFeedbackReason.entries.forEach { value ->
                FilterChip(selected = value.name in reasons, onClick = {
                    reasons = if (value.name in reasons) reasons - value.name else reasons + value.name
                }, label = { Text(when (value) {
                    ChantalFeedbackReason.FUNNY -> "Смешно"
                    ChantalFeedbackReason.TOUCHING -> "Тронуло"
                    ChantalFeedbackReason.WANT_CONTINUATION -> "Хочу продолжение"
                    ChantalFeedbackReason.TOO_LONG -> "Слишком длинно"
                    ChantalFeedbackReason.TOO_VAGUE -> "Много воды"
                    ChantalFeedbackReason.UNCLEAR -> "Непонятно"
                    ChantalFeedbackReason.REPETITIVE -> "Повторяется"
                }) }, modifier = Modifier.testTag("feedback_reason_${value.name}"))
            }
            Text("К чему относится пожелание", color = HobbitCream)
            ChantalFeedbackScope.entries.forEach { value ->
                FilterChip(selected = scope == value.name, onClick = { scope = value.name }, label = {
                    Text(if (value == ChantalFeedbackScope.THIS_STORY) "Только к этому рассказу" else "К будущим рассказам в целом")
                }, modifier = Modifier.testTag("feedback_scope_${value.name}"))
            }
            OutlinedTextField(value = comment, onValueChange = { comment = it.take(1000) },
                label = { Text("Комментарий · необязательно") }, supportingText = { Text("${comment.length}/1000") },
                modifier = Modifier.fillMaxWidth().testTag("feedback_comment"), minLines = 2, maxLines = 5)
            Button(enabled = rating.isNotEmpty(), onClick = {
                onSave(ChantalStoryFeedback(episodeId, ChantalStoryRating.valueOf(rating),
                    ChantalFeedbackScope.valueOf(scope), reasons.map { ChantalFeedbackReason.valueOf(it) }.toSet(), comment))
                expanded = false
            }, modifier = Modifier.fillMaxWidth().testTag("feedback_save")) { Text("Сохранить оценку") }
        }
    }
}
