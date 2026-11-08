package com.example.domain

enum class ChantalVisualState(val title: String, val thought: String) {
    REST("Отдыхает дома", "Можно немного поваляться?"),
    PACKING("Собирается в дорогу", "Так, хвост со мной. Что ещё взять?"),
    TRAVELLING("На лесной тропе", "Я потом всё расскажу!"),
    RETURNED("Вернулась с историей", "У меня столько всего случилось!"),
    HAPPY("Радуется ласке", "Ещё чуть-чуть за ушком…"),
    TIRED("Отдыхает после дороги", "Я сначала посижу. Лапы устали.");

    companion object {
        fun resolve(travelling: Boolean, pendingStory: Boolean, petting: Boolean, tired: Boolean, packing: Boolean) = when {
            travelling -> TRAVELLING
            petting -> HAPPY
            pendingStory -> RETURNED
            tired -> TIRED
            packing -> PACKING
            else -> REST
        }
    }
}
