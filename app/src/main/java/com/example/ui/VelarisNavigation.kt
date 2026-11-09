package com.example.ui

/** Presentation navigation only; never owns a workout, balance or story. */
enum class VelarisNavDestination(val id: String, val titleRu: String) {
    HOME("home", "Главная"), PLAN("plan", "Тренировки"), CHANTAL("chantal", "Шанталь"),
    STATISTICS("statistics", "Статистика"),
    STATISTICS_DAY("statistics_day", "День тренировок"), WORKOUT_JOURNAL("workout_journal", "Журнал тренировок"),
    WORKOUT_SESSION("workout_session", "Занятие"), STATISTICS_DETAILS("statistics_details", "Подробная статистика"), PLAN_DAY("plan_day", "План дня"),
    TRAINING_DETAILS("training_details", "Обзор упражнения"),
    OTHER_WORKOUTS("other_workouts", "Другие тренировки"), WORKOUT("workout", "Текущая тренировка"),
    READER("reader", "Книга"), SETTINGS("settings", "Настройки"),
    SETTINGS_APPEARANCE("settings_appearance", "Оформление"), SETTINGS_SOUND("settings_sound", "Звук и вибрация"),
    SETTINGS_SAMPLES("settings_samples", "Образцы команд"), SETTINGS_BACKUP("settings_backup", "Резервные копии"),
    SETTINGS_ABOUT("settings_about", "О приложении"), DIAGNOSTICS("diagnostics", "Диагностика");

    val isMainSection get() = this in listOf(HOME, PLAN, CHANTAL, STATISTICS)
}

internal data class VelarisNavigation(val routes: List<VelarisNavDestination> = listOf(VelarisNavDestination.HOME)) {
    init {
        require(routes.firstOrNull() == VelarisNavDestination.HOME)
        require(routes.distinct().size == routes.size)
    }
    val current get() = routes.last()
    val canGoBack get() = routes.size > 1
    val mainSection get() = if (current == VelarisNavDestination.WORKOUT) VelarisNavDestination.PLAN
        else routes.last { it.isMainSection }
    fun open(destination: VelarisNavDestination): VelarisNavigation {
        val index = routes.indexOf(destination)
        return if (index >= 0) copy(routes = routes.take(index + 1)) else copy(routes = routes + destination)
    }
    fun back() = if (canGoBack) copy(routes = routes.dropLast(1)) else this
    companion object {
        fun restore(ids: List<String>?): VelarisNavigation = runCatching {
            VelarisNavigation(requireNotNull(ids).map { id -> VelarisNavDestination.entries.first { it.id == id } })
        }.getOrElse { VelarisNavigation() }
    }
}
