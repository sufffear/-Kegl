package com.example.domain

/** Local story planning only: no clocks, IO, model calls or economic effects. */
object ChantalStoryEngine {
    data class Region(val id: String, val opensAt: Int, val npcIds: List<String>)
    val regions = listOf(
        Region("reg_old_mill",0,listOf("bramble","marta","hugo")),
        Region("reg_amber_forest",0,listOf("corvus","oleander","tilly")),
        Region("reg_misty_hollow",5,listOf("thorvin","elsa","balin")),
        Region("reg_stone_halls",15,listOf("buttercup","sebastian","roderick")),
        Region("reg_mountain_pass",40,listOf("irene","kalkstein","courier")))
    data class Mystery(val id: String, val region: String, val npc: String,
        val opening: String, val hypothesis: String, val question: String, val clue: String, val resolution: String)
    val mysteries = listOf(
        Mystery("mill_noise","reg_old_mill","marta","За мешками слышался тонкий писк.",
            "Мне показалось, что мешки боятся темноты.","Кто пищит за мешками?",
            "Под мешком нашлось совиное перо, писк раздался сверху.",
            "За балкой сидел совёнок и звал маму. Марта открыла верхнее окошко; взрослая сова прилетела к нему."),
        Mystery("laughing_parcel","reg_amber_forest","corvus","Посылка Корвуса свистнула, когда он её перевернул.",
            "Я решила, что внутри кто-то дразнится.","Почему свистит посылка?",
            "Свист повторялся только при переворачивании; посылка была адресована мастеру игрушек.",
            "Корвус получил разрешение отправителя открыть упаковку: там была заводная свистулька. Никто внутри не сидел."),
        Mystery("river_lights","reg_misty_hollow","elsa","У моста мигали маленькие огоньки.",
            "Я подумала, что это потерявшиеся звёзды.","Что мигает у моста?",
            "Огоньки опускались к влажному мху, а не поднимались к небу.",
            "Эльза показала светлячков среди светящегося мха. Мы погасили фонарь и спокойно дали им пролететь."))
    data class Encounter(val key: String, val problem: String, val solution: String,
        val emotion: ChantalStoryEmotion)
    val encounters = listOf(
        Encounter("sneeze_echo","Я чихнула, а камешек повторил звук.","Спутник объяснил: камешек отзывается на резкий звук. Я проверила шёпотом — тишина.",ChantalStoryEmotion.JOY),
        Encounter("rolling_apples","Из корзины спутника раскатились яблоки.","Мы собирали их с разных сторон и один раз столкнулись у последнего яблока.",ChantalStoryEmotion.EMBARRASSMENT),
        Encounter("missing_ribbon","Ветер сорвал ленточку с дорожного указателя.","Я заметила её на низкой ветке; вместе со спутником мы закрепили её обратно.",ChantalStoryEmotion.PRIDE),
        Encounter("bad_whistle","Я пыталась насвистеть спутнику мелодию.","Получился смешной свист. Мы придумали ему название, а мелодию потом просто напели.",ChantalStoryEmotion.JOY),
        Encounter("empty_seat","Спутник ждал друга, который сегодня не пришёл.","Я немного посидела рядом. Друг не появился, но спутник сказал, что ждать вдвоём было легче.",ChantalStoryEmotion.TENDERNESS),
        Encounter("mud_mark","Я поскользнулась и оставила смешной след в грязи.","Спутник помог подняться. Я отмылась у ручья и сама первой посмеялась над следом.",ChantalStoryEmotion.EMBARRASSMENT),
        Encounter("quiet_bell","На тропе не звенел подвешенный колокольчик.","Мы вынули застрявший листик. Звон вернулся, и я вздрогнула от неожиданности.",ChantalStoryEmotion.PRIDE),
        Encounter("leaf_boat","Наш маленький кораблик из листа застрял у берега.","Мы освободили его веточкой и смотрели, как он уплыл. Возвращаться за ним не стали.",ChantalStoryEmotion.CALM),
        Encounter("wrong_turn","Я уверенно показала спутнику неверный поворот.","Мы сверились с указателем. Я призналась, что перепутала дорогу; спутник поблагодарил за честность.",ChantalStoryEmotion.EMBARRASSMENT),
        Encounter("cold_tea","Спутник расстроился: чай в кружке уже остыл.","Мы сделали паузу и поговорили. Чай не стал горячим, зато перестал быть поводом для ссоры.",ChantalStoryEmotion.CALM),
        Encounter("moon_puddle","В луже отражалась луна, и я попыталась поймать свет.","Волна разбила отражение. Я подождала, вода успокоилась и луна вернулась на место.",ChantalStoryEmotion.JOY),
        Encounter("careful_step","Спутнику было трудно перейти мокрое бревно.","Я предложила более длинную сухую дорожку. Мы пришли позже, зато оба добрались спокойно.",ChantalStoryEmotion.TENDERNESS))

    /** Fail closed if authored v1 content changes; migrate archives explicitly instead of retconning them. */
    val catalogueSignature: String by lazy {
        val fields = regions.flatMap { listOf(it.id,it.opensAt.toString())+it.npcIds } +
            mysteries.flatMap { listOf(it.id,it.region,it.npc,it.opening,it.hypothesis,it.question,it.clue,it.resolution) } +
            encounters.flatMap { listOf(it.key,it.problem,it.solution,it.emotion.name) }
        java.security.MessageDigest.getInstance("SHA-256").digest(fields.joinToString("\u0000").toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }
    }

    enum class Kind { ORDINARY, OPEN, INVESTIGATE, RESUME, RESOLVE }
    /** A read-only projection of the six existing traits, not new acquired personality. */
    enum class Temperament { CURIOUS, GENTLE, PLAYFUL, ROMANTIC, BRAVE, WITTY }
    data class Request(val journeyId: String, val locationId: String, val resumeSuspended: Boolean = false, val temperament: Temperament = Temperament.CURIOUS, val legacyPlotReserved: Boolean = false) {
        init { require(journeyId.isNotBlank()) }
    }
    enum class MeetingStatus { FIRST, KNOWN, LEGACY_UNKNOWN }
    data class Choice(val label: String, val intention: String, val deferPlot: Boolean = false)
    data class Plan(val request: Request, val age: ChantalCharacterState, val kind: Kind,
        val plotId: String?, val npcId: String, val encounterKey: String?, val outcome: String,
        val emotion: ChantalStoryEmotion, val choices: List<Choice>, val guidance: String = "",
        val meetingStatus: MeetingStatus = MeetingStatus.LEGACY_UNKNOWN,
        val formChange: String? = if(age.acceptedAdventures==99) "После сотого приключения проклятие спало: Шанталь приняла человеческий облик." else null) {
        val characterAfter get() = ChantalCharacterState.fromAdventures(Math.addExact(age.acceptedAdventures,1))
    }
    data class Accepted(val plan: Plan, val choiceIndex: Int)
    data class ExternalAdvancement(val episodeId: String, val afterRuns: Int)
    class State private constructor(val seed: Long, val initialAdventures: Int,
        internal val accepted: List<Accepted>, internal val canonical: ChantalStoryState, private val external: List<ExternalAdvancement>) {
        val completedAdventures get() = Math.addExact(Math.addExact(initialAdventures,accepted.size),external.size)
        val externalHistory get() = external.toList()
        val history get() = accepted.map { it.copy(plan=it.plan.copy(choices=it.plan.choices.map { c -> c.copy() })) }
        val activePlot get() = canonical.plots.entries.firstOrNull { it.value != ChantalPlotStatus.RESOLVED }?.key
        val plotStates get() = canonical.plots.toMap()
        val events get() = canonical.events.map { it.copy(event=it.event.copy(
            spec=it.event.spec.copy(participantIds=it.event.spec.participantIds.toSet(),referencedItemIds=it.event.spec.referencedItemIds.toSet()),
            predecessorIds=it.event.predecessorIds.toSet())) }
        val unresolvedQuestions get() = canonical.events.filter {
            val record=(it.event.spec.effect as? ChantalStoryEffect.Remember)?.record
            record?.kind==ChantalMemoryKind.OPEN_QUESTION && canonical.plots[record.key.substringBefore(':')]!=ChantalPlotStatus.RESOLVED
        }.map { it.event.id }
        val meetings get() = accepted.groupingBy { it.plan.npcId }.eachCount()
        companion object {
            internal fun owned(seed: Long, initial: Int, accepted: List<Accepted>, canonical: ChantalStoryState, external: List<ExternalAdvancement> = emptyList()) = State(seed,initial,accepted,canonical,external)
        }
    }
    fun newGame(seed: Long, initialAdventures: Int = 0): State {
        require(initialAdventures >= 0)
        return State.owned(seed,initialAdventures,emptyList(),ChantalStoryState())
    }
    private fun choose(state: State, request: Request, size: Int, salt: Int) =
        Math.floorMod(state.seed xor request.journeyId.hashCode().toLong() xor (state.completedAdventures.toLong()*31L+salt),size.toLong()).toInt()

    fun plan(state: State, request: Request): Plan {
        require(state.externalHistory.none { it.episodeId==request.journeyId })
        require(state.accepted.none { it.plan.request.journeyId == request.journeyId }) { "Journey already accepted" }
        require(!request.legacyPlotReserved || state.activePlot == null) { "Legacy and engine plots cannot run together" }
        val region = regions.singleOrNull { it.id == request.locationId } ?: error("Unknown region")
        require(state.completedAdventures >= region.opensAt) { "Locked region" }
        val active = mysteries.firstOrNull { it.id == state.activePlot }
        val recentPlot = state.accepted.takeLast(2).any { it.plan.kind != Kind.ORDINARY }
        val status = active?.let { state.canonical.plots[it.id] }
        val eligible = when {
            request.legacyPlotReserved -> null
            active != null && active.region != region.id -> null
            status == ChantalPlotStatus.SUSPENDED && !request.resumeSuspended -> null
            active != null && request.resumeSuspended -> active
            recentPlot -> null
            active != null -> active
            state.completedAdventures < 3 -> null
            else -> mysteries.firstOrNull { it.region==region.id && it.id !in state.canonical.plots }
        }
        val age = ChantalCharacterState.fromAdventures(state.completedAdventures)
        if (eligible != null) {
            val kind = when(state.canonical.plots[eligible.id]) {
                null -> Kind.OPEN
                ChantalPlotStatus.SUSPENDED -> Kind.RESUME
                ChantalPlotStatus.OPEN -> Kind.INVESTIGATE
                ChantalPlotStatus.INVESTIGATING -> Kind.RESOLVE
                else -> error("Resolved mystery cannot reopen")
            }
            val outcome = when(kind) {
                Kind.OPEN -> eligible.opening
                Kind.INVESTIGATE -> eligible.clue
                Kind.RESUME -> if(state.canonical.events.any { (it.event.spec.effect as? ChantalStoryEffect.Remember)?.record?.key==eligible.id+":clue" })
                    "Я вернулась к вопросу: ${eligible.question} Вспомнила прежнюю зацепку: ${eligible.clue}"
                    else "Я вернулась к вопросу: ${eligible.question} ${eligible.clue}"
                Kind.RESOLVE -> eligible.resolution
                else -> error("Unknown chapter")
            }
            return Plan(request,age,kind,eligible.id,eligible.npc,null,outcome,
                if(kind==Kind.RESOLVE) ChantalStoryEmotion.JOY else ChantalStoryEmotion.CALM,
                if(kind==Kind.RESOLVE) ordinaryChoices else listOf(
                    Choice("Предложить разобраться дальше","Игрок предложил продолжить расследование в будущем"),
                    Choice("Поддержать и напомнить об осторожности","Игрок предложил продолжать без спешки и риска"),
                    Choice("Предложить пока отвлечься","Игрок предложил пока отложить расследование",true))).withMeeting(state)
        }
        val last = state.accepted.takeLast(3).mapNotNull { it.plan.encounterKey }.toSet()
        val available = encounters.filter { it.key !in last }
        val careful = state.accepted.lastOrNull()?.let { it.plan.plotId!=null && it.choiceIndex==1 } == true
        val soft = request.temperament in setOf(Temperament.GENTLE,Temperament.ROMANTIC) || careful
        val playful = request.temperament in setOf(Temperament.PLAYFUL,Temperament.WITTY)
        val pool = available.filter { when {
            soft -> it.emotion in setOf(ChantalStoryEmotion.CALM,ChantalStoryEmotion.TENDERNESS)
            playful -> it.emotion in setOf(ChantalStoryEmotion.JOY,ChantalStoryEmotion.EMBARRASSMENT)
            else -> true
        } }.ifEmpty { available }
        val encounter = pool[choose(state,request,pool.size,17)]
        val recentNpcs = state.accepted.takeLast(1).map { it.plan.npcId }.toSet()
        val npcs = region.npcIds.filter { it !in recentNpcs }.ifEmpty { region.npcIds }
        return Plan(request,age,Kind.ORDINARY,null,npcs[choose(state,request,npcs.size,29)],encounter.key,
            encounter.problem+" "+encounter.solution,encounter.emotion,ordinaryChoices,
            if(careful) "Учти прошлую просьбу не спешить и выбирать безопасные действия." else "").withMeeting(state)
    }
    private fun Plan.withMeeting(state: State) = copy(meetingStatus=when {
        state.accepted.any { it.plan.npcId==npcId } -> MeetingStatus.KNOWN
        state.initialAdventures>0 -> MeetingStatus.LEGACY_UNKNOWN
        else -> MeetingStatus.FIRST
    })
    private val ordinaryChoices get() = listOf(
        Choice("Поддержать Шанталь","Игрок решил поддержать Шанталь"),
        Choice("Вместе посмеяться над случившимся","Игрок предложил вместе посмеяться над рассказом"),
        Choice("Расспросить о самом важном моменте","Игрок решил расспросить о важном моменте"))

    fun accept(state: State, offered: Plan, choiceIndex: Int): State {
        require(choiceIndex in 0..2)
        state.accepted.firstOrNull { it.plan.request.journeyId == offered.request.journeyId }?.let {
            require(it.plan == offered && it.choiceIndex == choiceIndex) { "Conflicting confirmation" }
            return state
        }
        require(state.completedAdventures < Int.MAX_VALUE)
        require(plan(state,offered.request)==offered) { "Stale or altered plan" }
        val result = eventsFor(state,offered,choiceIndex)
        val specs = (state.canonical.events.map { it.event.spec }+result.map { it.spec }).distinctBy { it.key }
        val keys = specs.mapNotNull { (it.effect as? ChantalStoryEffect.Remember)?.record?.key }.toSet()
        val rules = ChantalStoryRules(regions.map { it.id }.toSet(),regions.flatMap { it.npcIds }.toSet()+"chantal",
            keys,mysteries.map { it.id }.toSet(),specs)
        val next = rules.acceptOwnedBatch(state.canonical,result).copy(storyEmotion=offered.emotion)
        return State.owned(state.seed,state.initialAdventures,state.accepted+Accepted(offered.copy(choices=offered.choices.map { it.copy() }),choiceIndex),next,state.externalHistory)
    }

    data class MemoryReference(val eventId: ChantalEventId, val record: ChantalMemoryRecord)
    data class Context(val character: ChantalCharacterState, val activePlot: ChantalPlot?,
        val memories: List<MemoryReference>, val lastIntention: String?, val emotion: ChantalStoryEmotion?)
    /** A bounded, source-linked context. Intention remains separate from external facts. */
    fun context(state: State, locationId: String): Context {
        require(regions.any { it.id==locationId })
        val judged = state.canonical.events.mapNotNull { (it.event.spec.effect as? ChantalStoryEffect.JudgeHypothesis)?.hypothesisEvent }.toSet()
        fun relevant(event: ChantalConfirmedEvent): Boolean {
            val r=(event.event.spec.effect as? ChantalStoryEffect.Remember)?.record ?: return false
            return when(r.kind) {
                ChantalMemoryKind.FACT -> true
                ChantalMemoryKind.HYPOTHESIS -> event.event.id !in judged
                ChantalMemoryKind.OPEN_QUESTION -> r.key.substringBefore(':')==state.activePlot
                else -> false
            }
        }
        val active=state.activePlot
        val plotMemories=if(active==null) emptyList() else state.canonical.events.filter {
            relevant(it) && (it.event.spec.effect as ChantalStoryEffect.Remember).record.key.startsWith("$active:")
        }.takeLast(6)
        val local=state.canonical.events.filter { it.event.spec.locationId==locationId && relevant(it) }.takeLast(6)
        val memories=(plotMemories+local).distinctBy { it.event.id }.map {
            MemoryReference(it.event.id,(it.event.spec.effect as ChantalStoryEffect.Remember).record)
        }
        val intention=state.canonical.events.asReversed().firstNotNullOfOrNull {
            (it.event.spec.effect as? ChantalStoryEffect.Remember)?.record?.takeIf { r -> r.kind==ChantalMemoryKind.PLAYER_DECISION }?.text
        }
        return Context(ChantalCharacterState.fromAdventures(state.completedAdventures),
            active?.let { ChantalPlot(it,state.canonical.plots.getValue(it)) },memories,intention,state.canonical.storyEmotion)
    }

    /** A real accepted legacy chapter/chest advances age, but invents no engine facts. */
    fun advanceExternal(state: State, episodeId: String): State {
        require(episodeId.isNotBlank())
        if(state.externalHistory.any { it.episodeId==episodeId }) return state
        require(state.accepted.none { it.plan.request.journeyId==episodeId })
        require(state.completedAdventures<Int.MAX_VALUE)
        return State.owned(state.seed,state.initialAdventures,state.accepted,state.canonical,
            state.externalHistory+ExternalAdvancement(episodeId,state.accepted.size))
    }

    private fun eventsFor(state: State, p: Plan, choice: Int): List<ChantalStoryEvent> {
        val output = mutableListOf<ChantalStoryEvent>()
        val prefix = "journey:${p.request.journeyId}:"
        fun append(suffix: String, effect: ChantalStoryEffect, predecessors: Set<ChantalEventId> = emptySet(), decision: Boolean=false) {
            val id = ChantalEventId(prefix+suffix)
            output += ChantalStoryEvent(id,ChantalEventSpec(prefix+suffix,ChantalSceneOrigin.JOURNEY,p.request.locationId,
                setOf("chantal",p.npcId),effect),ChantalJourneyId(p.request.journeyId),
                if(decision) ChantalDecisionId(prefix+"choice") else null,predecessors)
        }
        fun remember(suffix: String, kind: ChantalMemoryKind, text: String) =
            append(suffix,ChantalStoryEffect.Remember(ChantalMemoryRecord((p.plotId ?: p.encounterKey)+":"+suffix,kind,text)))
        val m = mysteries.firstOrNull { it.id==p.plotId }
        when(p.kind) {
            Kind.ORDINARY -> remember("outcome",ChantalMemoryKind.FACT,p.outcome)
            Kind.OPEN -> {
                remember("opening",ChantalMemoryKind.FACT,requireNotNull(m).opening)
                remember("hypothesis",ChantalMemoryKind.HYPOTHESIS,m.hypothesis)
                remember("question",ChantalMemoryKind.OPEN_QUESTION,m.question)
                append("plot",ChantalStoryEffect.MovePlot(m.id,ChantalPlotStatus.OPEN))
            }
            Kind.INVESTIGATE -> {
                remember("clue",ChantalMemoryKind.FACT,requireNotNull(m).clue)
                append("plot",ChantalStoryEffect.MovePlot(m.id,ChantalPlotStatus.INVESTIGATING))
            }
            Kind.RESUME -> {
                val mystery=requireNotNull(m)
                if(state.canonical.events.none { (it.event.spec.effect as? ChantalStoryEffect.Remember)?.record?.key==mystery.id+":clue" })
                    remember("clue",ChantalMemoryKind.FACT,mystery.clue)
                append("plot",ChantalStoryEffect.MovePlot(mystery.id,ChantalPlotStatus.INVESTIGATING))
            }
            Kind.RESOLVE -> {
                val mystery = requireNotNull(m)
                remember("resolution",ChantalMemoryKind.FACT,mystery.resolution)
                val hypothesis = state.canonical.events.first { (it.event.spec.effect as? ChantalStoryEffect.Remember)?.record?.key==mystery.id+":hypothesis" }.event.id
                val evidence = output.last().id
                append("verdict",ChantalStoryEffect.JudgeHypothesis(hypothesis,evidence,ChantalHypothesisVerdict.REFUTED),setOf(hypothesis,evidence))
                append("plot",ChantalStoryEffect.MovePlot(mystery.id,ChantalPlotStatus.RESOLVED))
            }
        }
        p.formChange?.let { remember("transformation",ChantalMemoryKind.FACT,it) }
        remember("impression",ChantalMemoryKind.IMPRESSION,"Эмоция после встречи: ${p.emotion}")
        append("decision",ChantalStoryEffect.Remember(ChantalMemoryRecord("decision",ChantalMemoryKind.PLAYER_DECISION,p.choices[choice].intention)),decision=true)
        if(m!=null && p.kind!=Kind.RESOLVE && p.choices[choice].deferPlot) append("defer",ChantalStoryEffect.MovePlot(m.id,ChantalPlotStatus.SUSPENDED))
        return output
    }
}
