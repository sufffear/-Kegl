package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.resolveChantalStageStatus

/** Age reflects accepted adventures; the preserved XP counter is a separate legacy metric. */
@Composable
internal fun ChantalAgeProgressPanel(journeys: Int, xp: Int, modifier: Modifier = Modifier) {
    val count = journeys.coerceAtLeast(0)
    val stage = resolveChantalStageStatus(count)
    val progress = if (stage.isTransformed) 1f else {
        (count - stage.currentStageStartJourneys).toFloat() /
            (stage.nextStageTargetJourneys - stage.currentStageStartJourneys)
    }.coerceIn(0f, 1f)
    Surface(modifier.fillMaxWidth().testTag("chantal_age_progress"),
        color = HobbitCardBg, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stage.titleRu, color = HobbitGold, style = MaterialTheme.typography.titleMedium)
            Text(if (stage.isTransformed) "Превращение завершено · Приключений: $count"
                else "Приключений: $count / ${stage.nextStageTargetJourneys}",
                color = HobbitCream, style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(progress = { progress }, color = HobbitGold, trackColor = HobbitBg,
                modifier = Modifier.fillMaxWidth().height(8.dp))
            if (!stage.isTransformed) Text(
                "До следующей стадии: ${stage.nextStageTargetJourneys - count} приключений",
                color = HobbitMuted, style = MaterialTheme.typography.bodyMedium)
            Text("Опыт: ${xp.coerceAtLeast(0)} XP", color = HobbitCream, style = MaterialTheme.typography.bodyMedium)
            Text("Шанталь взрослеет после завершённых приключений. Опыт от ласки сохраняется отдельно.",
                color = HobbitMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}
