package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

internal data class ChantalItemAction(
    val id: String,
    val label: String,
    val enabled: Boolean = true,
    val confirmation: String? = null,
    val success: String,
    val perform: () -> Unit
)

/** One invocation per open details window; no network, reward or storage work here. */
@Composable
internal fun ChantalItemDetailsDialog(
    title: String,
    description: String,
    availability: String = "",
    actions: List<ChantalItemAction> = emptyList(),
    closeTag: String = "item_close",
    onDismiss: () -> Unit
) {
    var confirming by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }
    val latestActions by rememberUpdatedState(actions)
    val scope = rememberCoroutineScope()
    fun execute(id: String) {
        if (busy || completed) return
        val action = latestActions.firstOrNull { it.id == id && it.enabled } ?: run {
            confirming = null; message = "Действие больше недоступно. Закрой окно и проверь предмет."; failed = true
            return
        }
        busy = true // Immediate latch also protects two clicks before recomposition.
        confirming = null
        scope.launch {
            yield()
            runCatching { action.perform() }
                .onSuccess { completed = true; message = action.success; failed = false }
                .onFailure { message = "Не удалось выполнить действие. Закрой окно и проверь состояние предмета."; failed = true }
            busy = false
        }
    }
    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(shape = RoundedCornerShape(20.dp), color = HobbitCardBg,
            modifier = Modifier.fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * .82f).dp)
                .testTag("chantal_item_dialog")) {
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = HobbitGold)
                Text(description, style = MaterialTheme.typography.bodyLarge, color = HobbitCream)
                if (availability.isNotBlank()) Text(availability, color = HobbitMuted)
                if (busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Выполняется…", color = HobbitCream)
                }
                message?.let {
                    Text(it, color = if (failed) MaterialTheme.colorScheme.error else HobbitCream,
                        modifier = Modifier.testTag("item_action_result").semantics { liveRegion = LiveRegionMode.Polite })
                }
                val pending = latestActions.firstOrNull { it.id == confirming }
                if (pending != null && !busy && !completed) {
                    Text(pending.confirmation.orEmpty(), color = HobbitCream, modifier = Modifier.testTag("item_confirmation"))
                    Button(onClick = { execute(pending.id) }, enabled = pending.enabled,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("item_confirm")) { Text("Подтвердить") }
                    OutlinedButton(onClick = { confirming = null }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("item_cancel")) { Text("Отмена") }
                } else if (!completed) {
                    actions.forEach { action ->
                        Button(onClick = {
                            if (!busy && !completed) {
                                if (action.confirmation == null) execute(action.id) else confirming = action.id
                            }
                        }, enabled = action.enabled && !busy && !failed,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("item_action_${action.id}")) { Text(action.label) }
                    }
                }
                OutlinedButton(onClick = onDismiss, enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(closeTag)) { Text("Закрыть") }
            }
        }
    }
}
