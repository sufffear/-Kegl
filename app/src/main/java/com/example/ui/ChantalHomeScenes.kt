package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal data class ChantalHomeScene(val title: String, val story: String, val answers: List<Pair<String, String>>)
internal val chantalHomeScenes = listOf(
    ChantalHomeScene("Плед победил 🦊", "Я хотела красиво завернуться в плед. Сначала исчезли лапы. Потом дверь. Теперь я вижу только собственный хвост. Кажется, я стала пирожком. Поможешь?", listOf(
        "Найти край и аккуратно развернуть" to "О! Воздух! Я выбралась и тут же села на плед. Чтобы он больше никуда не убежал.",
        "Назвать её главным домашним пирожком" to "Главным? Тогда ладно. Только начинка против того, чтобы её ели. Я высунула нос и потребовала тарелку для почётного лежания.",
        "Предложить выпутаться вместе" to "Ты держишь угол, я пячусь. Раз, два… получилось! Плед лежит смирно. Мы переглянулись. Всё-таки вдвоём мы страшная сила.")),
    ChantalHomeScene("Чашка с характером ☕", "Моя чашка сегодня пискнула. Я пискнула в ответ. Она замолчала. Я уже решила, что победила, но потом услышала ещё один писк. Под чашкой сидел крошечный жук. Ужасно деловой. Что будем делать?", listOf(
        "Подставить листок и отнести жука к окну" to "Жук забрался на листок и улетел. Я проверила чашку. Обычная. Даже обидно: я уже придумала ей имя.",
        "Поприветствовать нового соседа" to "Я сказала: «Добро пожаловать. Крошки справа». Он схватил одну и убежал. Вежливости ноль, зато правила дома усвоил сразу.",
        "Попросить Шанталь самой разобраться" to "Я легла носом к полу. Жук посмотрел на меня, я на жука. Потом он ушёл. Мы договорились. Ну, мне так кажется.")),
    ChantalHomeScene("Последняя крошка 🍪", "Я нашла печенье. Одно. Положила перед собой и стала решать, с какой стороны начать. Пока решала, оно раскрошилось. Теперь печений много, но все подозрительно маленькие. Тебе оставить?", listOf(
        "Попросить самую маленькую крошку" to "Я выбрала тебе самую большую. Потом передумала. Потом снова выбрала большую. Забирай быстрее, пока у меня доброта не закончилась!",
        "Предложить устроить общий пикник" to "Я притащила блюдце и уселась рядом. Печенья хватило на два укуса. Пикника — на весь вечер. Иногда я умею хорошо устраиваться.",
        "Оставить печенье Шанталь" to "Я слопала всё и уже собралась сказать спасибо, но нашла крошку под лапой. Вот. Это тебе. Она чистая. Почти."))
)

@Composable
internal fun ChantalHomeScenes(index: Int, answer: Int?, onAnswer: (Int) -> Unit, onNext: () -> Unit) {
    val scene = chantalHomeScenes[Math.floorMod(index, chantalHomeScenes.size)]
    var opened by remember { mutableStateOf(false) }
    Surface(color = HobbitElevatedBg, shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { opened = !opened }) { Text("Дома · ${scene.title} ${if (opened) "▴" else "▾"}", color = HobbitGold) }
            if (opened) {
                Text("Бесплатная сценка · без энергии и наград", color = HobbitMuted)
                Text(scene.story, color = HobbitCream)
                if (answer == null) scene.answers.forEachIndexed { i, option ->
                    OutlinedButton(onClick = { onAnswer(i) }, modifier = Modifier.fillMaxWidth()) { Text(option.first, color = HobbitCream) }
                } else {
                    Text("Ты: ${scene.answers[answer.coerceIn(0, 2)].first}", color = HobbitMuted)
                    Text(scene.answers[answer.coerceIn(0, 2)].second, color = HobbitCream)
                    TextButton(onClick = onNext) { Text("Ещё домашняя история", color = HobbitGold) }
                }
            }
        }
    }
}
