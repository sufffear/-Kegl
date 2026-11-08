package com.example.ui
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.ChantalTrailLife

@Composable
internal fun ChantalTrailNotesView(notes: List<String>, visibleCount: Int) {
    var expanded by remember { mutableStateOf(false) }
    Surface(color=HobbitElevatedBg, shape=androidx.compose.foundation.shape.RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
            TextButton(onClick={ expanded=!expanded }) { Text("Весточки с тропы · ${visibleCount.coerceAtMost(notes.size)}/2", color=HobbitGold) }
            if (expanded) {
                if (visibleCount==0) Text("Шанталь ещё в начале пути. Весточки можно прочитать и после возвращения.", color=HobbitCream)
                notes.take(visibleCount).forEachIndexed { i, note -> Text("${if(i==0) "15 мин" else "40 мин"} · $note", color=HobbitCream) }
            }
        }
    }
}
@Composable
internal fun ChantalSharedHabits(snack: Int, greeting: Int, onSnack: (Int)->Unit, onGreeting: (Int)->Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement=Arrangement.spacedBy(6.dp)) {
        TextButton(onClick={expanded=!expanded}) { Text("Наши маленькие привычки ${if(expanded) "▴" else "▾"}", color=HobbitGold) }
        if(expanded) {
            Text("Выбери угощение и приветствие. Их можно поменять в любой момент.", color=HobbitCream)
            ChantalTrailLife.snacks.forEachIndexed { i, text -> TextButton(onClick={onSnack(i)}) { Text("${if(snack==i) "✓ " else ""}$text", color=HobbitCream) } }
            HorizontalDivider()
            ChantalTrailLife.greetings.forEachIndexed { i, text -> TextButton(onClick={onGreeting(i)}) { Text("${if(greeting==i) "✓ " else ""}$text", color=HobbitCream) } }
            if(snack>0) Text("Шанталь: «${ChantalTrailLife.snacks[snack]}? Запомнила. Особенно часть, где это можно съесть». 🦊", color=HobbitGold)
        }
    }
}
