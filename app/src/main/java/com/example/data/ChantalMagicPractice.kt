package com.example.data
import org.json.*
object ChantalMagicPractice {
    val choices = listOf("Напеть три простые ноты", "Попросить Корвуса напеть", "Послушать уже знакомый напев")
    val reactions = listOf(
        "Я напела три ноты и поднесла нитку к миске воды. Она вернула мелодию. Вместе с моей фальшивой последней нотой. Честная штука!",
        "Я попросила Корвуса повторить мелодию и поднесла нитку к воде. Она повторила даже хриплый конец. Корвус утверждает, что так и задумано.",
        "У воды нитка вернула знакомый напев переправы. Я слушала тихо. Хотелось проверить ещё раз, но я решила сначала запомнить сама.")
    fun apply(state: JSONObject, memory: JSONObject, choice: Int): JSONObject {
        require(choice in choices.indices)
        val items=memory.getJSONArray("magicItems")
        val item=(0 until items.length()).map { items.getJSONObject(it) }.first { it.getString("id")=="silver_thread" }
        val copy=JSONObject(state.toString())
        if(!copy.has("silverThreadPractice")) copy.put("silverThreadPractice",JSONObject().put("choice",choice)
            .put("sourceEpisodeId",item.getString("sourceEpisodeId")).put("textRu",reactions[choice]))
        return copy
    }
}
