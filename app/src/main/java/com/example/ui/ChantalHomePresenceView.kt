package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.ChantalHomePresence as Presence
import com.example.domain.ChantalWishStatus

@Composable
internal fun ChantalHomePresenceView(state: Presence.State, petting: Boolean, memory: String,
    onWish: (ChantalWishStatus)->Unit, onJournal: ()->Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    Surface(color=HobbitCardBg,shape=androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(Presence.expression(state,if(petting) Presence.Reaction.PETTED else null),color=HobbitCream)
            TextButton(onClick={open=!open}) { Text(if(open) "Свернуть разговор" else "Побыть с Шанталь",color=HobbitGold) }
            if(open) {
                Text(memory,color=HobbitCream)
                TextButton(onClick=onJournal) { Text("Открыть летопись") }
                when(state.wish.status) {
                    ChantalWishStatus.ACTIVE -> {
                        Text(state.wish.text,color=HobbitCream)
                        OutlinedButton(onClick={onWish(ChantalWishStatus.FULFILLED)}) { Text("Посидеть рядом") }
                        TextButton(onClick={onWish(ChantalWishStatus.DEFERRED)}) { Text("Не сейчас") }
                        TextButton(onClick={onWish(ChantalWishStatus.DECLINED)}) { Text("Сегодня не хочется") }
                    }
                    ChantalWishStatus.DEFERRED -> TextButton(onClick={onWish(ChantalWishStatus.ACTIVE)}) { Text("Вернуться к нашему разговору") }
                    ChantalWishStatus.FULFILLED -> Text("Спасибо за компанию 💛",color=HobbitCream)
                    ChantalWishStatus.DECLINED -> Unit
                }
            }
        }
    }
}
