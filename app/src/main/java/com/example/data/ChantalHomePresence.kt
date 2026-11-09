package com.example.data

import com.example.domain.*
import org.json.JSONObject

/** Local presentation only: no reward, catalogue event, network, or invented shared history. */
object ChantalHomePresence {
    enum class Reaction { PETTED, LISTENING }
    data class State(val storyEmotion: ChantalStoryEmotion = ChantalStoryEmotion.CALM,
        val wish: ChantalWish = ChantalWish("quiet_company", "Посиди со мной немного?", ChantalWishStatus.ACTIVE))

    fun fromSnapshot(snapshot: JSONObject): State {
        val local = snapshot.optJSONObject("homePresence")?.let(::decode) ?: State()
        val emotion = if (snapshot.has("storyEmotion")) ChantalStoryEmotion.valueOf(snapshot.getString("storyEmotion"))
            else snapshot.optJSONObject("storyEngineState")?.let {
            ChantalStoryEngineCodec.decode(it).canonical.storyEmotion
        }
        return local.copy(storyEmotion = emotion ?: ChantalStoryEmotion.CALM)
    }

    fun encode(state: State): JSONObject = JSONObject().put("version", 1)
        .put("storyEmotion", state.storyEmotion.name)
        .put("wish", JSONObject().put("id", state.wish.id).put("text", state.wish.text).put("status", state.wish.status.name))

    fun decode(raw: JSONObject): State {
        require(raw.keys().asSequence().toSet() == setOf("version", "storyEmotion", "wish"))
        require(raw.get("version") is Int && raw.getInt("version") == 1)
        val wish = raw.getJSONObject("wish")
        require(wish.keys().asSequence().toSet() == setOf("id", "text", "status"))
        require(wish.getString("id") == "quiet_company" && wish.getString("text") == State().wish.text)
        return State(ChantalStoryEmotion.valueOf(raw.getString("storyEmotion")),
            ChantalWish(wish.getString("id"), wish.getString("text"), ChantalWishStatus.valueOf(wish.getString("status"))))
    }

    fun respond(state: State, status: ChantalWishStatus): State {
        if (state.wish.status == status) return state
        require((state.wish.status == ChantalWishStatus.ACTIVE && status != ChantalWishStatus.ACTIVE) ||
            (state.wish.status == ChantalWishStatus.DEFERRED && status == ChantalWishStatus.ACTIVE))
        return state.copy(wish = state.wish.copy(status = status))
    }

    fun expression(state: State, reaction: Reaction?): String = when (reaction) {
        Reaction.PETTED -> "Мне приятно 💛"
        Reaction.LISTENING -> "Я слушаю тебя"
        null -> when (state.storyEmotion) {
            ChantalStoryEmotion.JOY -> "Мне радостно"
            ChantalStoryEmotion.SADNESS -> "Мне немного грустно"
            ChantalStoryEmotion.ANGER -> "Я ещё сержусь"
            ChantalStoryEmotion.FEAR -> "Мне пока тревожно"
            ChantalStoryEmotion.PRIDE -> "Я собой горжусь"
            ChantalStoryEmotion.TENDERNESS -> "Мне сейчас тепло на душе"
            ChantalStoryEmotion.EMBARRASSMENT -> "Я всё ещё смущаюсь"
            ChantalStoryEmotion.CALM -> "Мне спокойно"
        }
    }

    fun rememberedLine(snapshot: JSONObject, character: ChantalCharacterState): String {
        val journal = snapshot.optJSONArray("journalList")
        val accepted = journal?.let { array -> (0 until array.length()).asSequence().map { array.getJSONObject(it) }
            .firstOrNull { !it.isNull("chosenActionSummaryRu") && it.optString("chosenActionSummaryRu").isNotBlank() } }
        val title = accepted?.optString("titleRu")?.replace(Regex("\\s+"), " ")?.trim()?.take(140)
        if (title.isNullOrBlank()) return when (character.age) {
            ChantalAge.INFANT -> "Я пока осваиваюсь. Можно просто посидеть рядом?"
            ChantalAge.CUB -> "Мне хочется немного посидеть рядом с тобой."
            ChantalAge.ADULT_FOX -> "Можно сегодня просто побыть вместе."
            ChantalAge.HUMAN -> "Мне хорошо спокойно посидеть рядом с тобой."
        }
        return when (character.age) {
            ChantalAge.INFANT -> "Я вспомнила нашу историю «$title». Почитаем её?"
            ChantalAge.CUB -> "Помнишь историю «$title»? Мне хочется её перечитать."
            ChantalAge.ADULT_FOX -> "Я сейчас вспомнила «$title». Хорошо, что эта история осталась в летописи."
            ChantalAge.HUMAN -> "Я вспомнила историю «$title». Можно открыть летопись и перечитать её вместе."
        }
    }
}
