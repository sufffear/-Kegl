package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.ChantalLivingWorld
import com.example.domain.ChantalStoryEpisode
import org.json.JSONObject

@Composable
internal fun ChantalFriendScenes(journal: List<ChantalStoryEpisode>, answers: JSONObject, onAnswer: (String, Int) -> Unit) {
    var opened by remember { mutableStateOf(false) }
    val accepted = journal.filter { !it.chosenActionSummaryRu.isNullOrBlank() }.distinctBy { it.id }
    val unlocked = ChantalLivingWorld.profiles.filter { p -> accepted.count { ChantalLivingWorld.profile(it.npcNameRu)?.key == p.key } >= 3 }
    if (unlocked.isEmpty()) return
    Surface(color = HobbitElevatedBg, shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { opened = !opened }) { Text("В гости к знакомым · ${unlocked.size} ${if(opened) "▴" else "▾"}", color = HobbitGold) }
            if (opened) unlocked.forEach { p ->
                Text(p.name, color = HobbitGold, style = MaterialTheme.typography.titleMedium)
                Text(p.invitation, color = HobbitCream)
                if (!answers.has(p.key)) p.replies.forEachIndexed { index, reply ->
                    OutlinedButton(onClick = { onAnswer(p.key, index) }, modifier = Modifier.fillMaxWidth()) { Text(reply.first, color = HobbitCream) }
                } else {
                    val choice = answers.getInt(p.key).coerceIn(0, 2)
                    Text("Ты: ${p.replies[choice].first}", color = HobbitMuted)
                    Text(p.replies[choice].second, color = HobbitCream)
                }
                HorizontalDivider()
            }
        }
    }
}
