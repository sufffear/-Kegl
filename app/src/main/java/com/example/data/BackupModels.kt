package com.example.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Метаданные резервной копии Velaris (Архитектура v1.2, п. 1–2).
 * Хранится в файле `manifest.json` внутри архива резервной копии.
 */
data class BackupManifest(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val appVersion: String,
    val appVersionCode: Int,
    val schemaVersion: Int,
    val createdAtEpochMs: Long,
    val createdAtRu: String,
    val workoutCount: Int,
    val hasBook: Boolean,
    val bookTitle: String?,
    val bookFileName: String?,
    val chapterCount: Int,
    val actCount: Int,
    val files: Map<String, String> // relative path inside zip -> sha256 hex
) {
    companion object {
        const val CURRENT_FORMAT_VERSION = 1
        const val MANIFEST_FILE_NAME = "manifest.json"
        const val DATABASE_EXPORT_FILE_NAME = "database_export.json"
        const val PREFERENCES_FILE_NAME = "preferences.json"
        const val BOOK_SOURCE_FILE_NAME = "book_source.md"

        fun fromJson(jsonStr: String): BackupManifest {
            val root = JSONObject(jsonStr)
            val filesObj = root.optJSONObject("files") ?: JSONObject()
            val filesMap = mutableMapOf<String, String>()
            val keys = filesObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                filesMap[key] = filesObj.getString(key)
            }
            return BackupManifest(
                formatVersion = root.getInt("formatVersion"),
                appVersion = root.optString("appVersion", "0.5.0-v1.2"),
                appVersionCode = root.optInt("appVersionCode", 5),
                schemaVersion = root.optInt("schemaVersion", 3),
                createdAtEpochMs = root.optLong("createdAtEpochMs", 0L),
                createdAtRu = root.optString("createdAtRu", ""),
                workoutCount = root.optInt("workoutCount", 0),
                hasBook = root.optBoolean("hasBook", false),
                bookTitle = if (root.has("bookTitle") && !root.isNull("bookTitle")) root.getString("bookTitle") else null,
                bookFileName = if (root.has("bookFileName") && !root.isNull("bookFileName")) root.getString("bookFileName") else null,
                chapterCount = root.optInt("chapterCount", 0),
                actCount = root.optInt("actCount", 0),
                files = filesMap
            )
        }
    }

    fun toJson(): String {
        val root = JSONObject()
        root.put("formatVersion", formatVersion)
        root.put("appVersion", appVersion)
        root.put("appVersionCode", appVersionCode)
        root.put("schemaVersion", schemaVersion)
        root.put("createdAtEpochMs", createdAtEpochMs)
        root.put("createdAtRu", createdAtRu)
        root.put("workoutCount", workoutCount)
        root.put("hasBook", hasBook)
        if (bookTitle != null) root.put("bookTitle", bookTitle)
        if (bookFileName != null) root.put("bookFileName", bookFileName)
        root.put("chapterCount", chapterCount)
        root.put("actCount", actCount)

        val filesObj = JSONObject()
        for ((k, v) in files) {
            filesObj.put(k, v)
        }
        root.put("files", filesObj)
        return root.toString(2)
    }
}

/**
 * Информация о содержимом архива перед подтверждением восстановления (v1.2 п. 2).
 */
data class BackupPreviewInfo(
    val formatVersion: Int,
    val appVersion: String,
    val createdAtEpochMs: Long,
    val createdAtRu: String,
    val workoutCount: Int,
    val hasBook: Boolean,
    val bookTitle: String?,
    val bookFileName: String?,
    val chapterCount: Int,
    val archiveSizeBytes: Long
)

/**
 * Снимок реальных тренировочных данных и романа для сериализации в JSON.
 * Демонстрационные данные (`isDemo == true`) строго исключены.
 */
data class BackupDatabaseSnapshot(
    val plans: List<TrainingPlanEntity> = emptyList(),
    val assignments: List<PlanAssignmentEntity> = emptyList(),
    val dailyPlans: List<DailyPlanEntity> = emptyList(),
    val slots: List<DailySlotEntity> = emptyList(),
    val runs: List<WorkoutRunEntity> = emptyList(),
    val completions: List<WorkoutCompletionEntity> = emptyList(),
    val novel: ImportedNovelEntity? = null,
    val novelChapters: List<NovelChapterEntity> = emptyList(),
    val novelBlocks: List<NovelBlockEntity> = emptyList(),
    val novelReadingProgress: NovelReadingProgressEntity? = null,
    val freeTemplates: List<FreeIntervalTemplateEntity> = emptyList()
) {
    fun toJson(): String {
        val root = JSONObject()

        val plansArr = JSONArray()
        plans.forEach { p ->
            plansArr.put(JSONObject().apply {
                put("id", p.id)
                put("version", p.version)
                put("titleRu", p.titleRu)
                put("source", p.source)
                put("effectiveFromDate", p.effectiveFromDate)
                put("isDemo", p.isDemo)
            })
        }
        root.put("plans", plansArr)

        val assignArr = JSONArray()
        assignments.forEach { a ->
            assignArr.put(JSONObject().apply {
                put("id", a.id)
                put("planId", a.planId)
                put("planVersion", a.planVersion)
                put("groupCode", a.groupCode)
                put("sortOrder", a.sortOrder)
                put("titleRu", a.titleRu)
                put("contractionMs", a.contractionMs)
                put("relaxationMs", a.relaxationMs)
                put("repetitions", a.repetitions)
                put("isDemo", a.isDemo)
            })
        }
        root.put("assignments", assignArr)

        val dailyPlansArr = JSONArray()
        dailyPlans.forEach { dp ->
            dailyPlansArr.put(JSONObject().apply {
                put("id", dp.id)
                put("localDate", dp.localDate)
                put("planId", dp.planId)
                put("planVersion", dp.planVersion)
                put("isDemo", dp.isDemo)
            })
        }
        root.put("dailyPlans", dailyPlansArr)

        val slotsArr = JSONArray()
        slots.forEach { s ->
            slotsArr.put(JSONObject().apply {
                put("id", s.id)
                put("dailyPlanId", s.dailyPlanId)
                put("localDate", s.localDate)
                put("assignmentId", s.assignmentId)
                put("groupCode", s.groupCode)
                put("sortOrder", s.sortOrder)
                put("titleRu", s.titleRu)
                put("contractionMs", s.contractionMs)
                put("relaxationMs", s.relaxationMs)
                put("repetitions", s.repetitions)
                put("isLastInGroup", s.isLastInGroup)
                put("isDemo", s.isDemo)
            })
        }
        root.put("slots", slotsArr)

        val runsArr = JSONArray()
        runs.forEach { r ->
            runsArr.put(JSONObject().apply {
                put("id", r.id)
                put("slotId", r.slotId)
                put("status", r.status)
                put("startedAtEpochMs", r.startedAtEpochMs)
                put("startDate", r.startDate)
                put("zoneId", r.zoneId)
                put("planVersion", r.planVersion)
                put("titleRu", r.titleRu)
                put("groupCode", r.groupCode)
                put("contractionMs", r.contractionMs)
                put("relaxationMs", r.relaxationMs)
                put("repetitions", r.repetitions)
                put("isLastInGroup", r.isLastInGroup)
                put("soundPackIdSnapshot", r.soundPackIdSnapshot)
                put("metronomeStyleIdSnapshot", r.metronomeStyleIdSnapshot)
                put("confirmedActiveMs", r.confirmedActiveMs)
                put("currentRepetition", r.currentRepetition)
                put("currentPhase", r.currentPhase)
                put("phaseRemainingMs", r.phaseRemainingMs)
                put("checkpointWallEpochMs", r.checkpointWallEpochMs)
                put("bootSessionId", r.bootSessionId)
                if (r.interruptionReason != null) put("interruptionReason", r.interruptionReason)
                put("isDemo", r.isDemo)
                put("isFreeMode", r.isFreeMode)
            })
        }
        root.put("runs", runsArr)

        val completionsArr = JSONArray()
        completions.forEach { c ->
            completionsArr.put(JSONObject().apply {
                put("runId", c.runId)
                put("slotId", c.slotId)
                put("localDate", c.localDate)
                put("completedAtEpochMs", c.completedAtEpochMs)
                if (c.optionalDifficulty != null) put("optionalDifficulty", c.optionalDifficulty)
                put("isDemo", c.isDemo)
                put("isFreeMode", c.isFreeMode)
            })
        }
        root.put("completions", completionsArr)

        if (novel != null) {
            val novelObj = JSONObject().apply {
                put("slotKey", novel.slotKey)
                put("bookId", novel.bookId)
                put("fileName", novel.fileName)
                if (novel.explicitTitle != null) put("explicitTitle", novel.explicitTitle)
                put("displayTitle", novel.displayTitle)
                put("sha256Hex", novel.sha256Hex)
                put("byteSize", novel.byteSize)
                put("internalFilePath", novel.internalFilePath)
                put("actHeadingLevel", novel.actHeadingLevel)
                put("chapterHeadingLevel", novel.chapterHeadingLevel)
                put("actCount", novel.actCount)
                put("chapterCount", novel.chapterCount)
                put("totalBlockCount", novel.totalBlockCount)
                put("importedAtEpochMs", novel.importedAtEpochMs)
            }
            root.put("novel", novelObj)

            val chaptersArr = JSONArray()
            novelChapters.forEach { ch ->
                chaptersArr.put(JSONObject().apply {
                    put("chapterId", ch.chapterId)
                    put("bookId", ch.bookId)
                    put("chapterNumber", ch.chapterNumber)
                    put("actId", ch.actId)
                    put("actNumber", ch.actNumber)
                    put("actTitle", ch.actTitle)
                    put("title", ch.title)
                    put("blockCount", ch.blockCount)
                    put("stepCount", ch.stepCount)
                })
            }
            root.put("novelChapters", chaptersArr)

            val blocksArr = JSONArray()
            novelBlocks.forEach { b ->
                blocksArr.put(JSONObject().apply {
                    put("blockId", b.blockId)
                    put("bookId", b.bookId)
                    put("chapterId", b.chapterId)
                    put("orderInChapter", b.orderInChapter)
                    put("globalOrder", b.globalOrder)
                    put("stepIndex", b.stepIndex)
                    put("stepId", b.stepId)
                    put("blockType", b.blockType)
                    put("subheadingLevel", b.subheadingLevel)
                    put("plainText", b.plainText)
                    put("inlineSpansEncoded", b.inlineSpansEncoded)
                })
            }
            root.put("novelBlocks", blocksArr)

            if (novelReadingProgress != null) {
                val progressObj = JSONObject().apply {
                    put("bookId", novelReadingProgress.bookId)
                    put("currentActId", novelReadingProgress.currentActId)
                    put("currentChapterId", novelReadingProgress.currentChapterId)
                    put("currentBlockId", novelReadingProgress.currentBlockId)
                    put("currentStepIndex", novelReadingProgress.currentStepIndex)
                    put("readerModeId", novelReadingProgress.readerModeId)
                    put("isCompleted", novelReadingProgress.isCompleted)
                    put("updatedAtEpochMs", novelReadingProgress.updatedAtEpochMs)
                }
                root.put("novelReadingProgress", progressObj)
            }
        }

        val ftArr = JSONArray()
        freeTemplates.forEach { t ->
            ftArr.put(JSONObject().apply {
                put("slotIndex", t.slotIndex)
                put("name", t.name)
                put("targetDurationMinutes", t.targetDurationMinutes)
                put("targetDurationSeconds", t.targetDurationSeconds)
                put("contractionSec", t.contractionSec)
                put("relaxationSec", t.relaxationSec)
                put("isConfigured", t.isConfigured)
                put("updatedAtEpochMs", t.updatedAtEpochMs)
            })
        }
        root.put("freeTemplates", ftArr)

        return root.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): BackupDatabaseSnapshot {
            val root = JSONObject(jsonStr)

            val plans = mutableListOf<TrainingPlanEntity>()
            val plansArr = root.optJSONArray("plans") ?: JSONArray()
            for (i in 0 until plansArr.length()) {
                val obj = plansArr.getJSONObject(i)
                plans.add(
                    TrainingPlanEntity(
                        id = obj.getString("id"),
                        version = obj.getInt("version"),
                        titleRu = obj.getString("titleRu"),
                        source = obj.getString("source"),
                        effectiveFromDate = obj.getString("effectiveFromDate"),
                        isDemo = obj.optBoolean("isDemo", false)
                    )
                )
            }

            val assignments = mutableListOf<PlanAssignmentEntity>()
            val assignArr = root.optJSONArray("assignments") ?: JSONArray()
            for (i in 0 until assignArr.length()) {
                val obj = assignArr.getJSONObject(i)
                assignments.add(
                    PlanAssignmentEntity(
                        id = obj.getString("id"),
                        planId = obj.getString("planId"),
                        planVersion = obj.getInt("planVersion"),
                        groupCode = obj.getString("groupCode"),
                        sortOrder = obj.getInt("sortOrder"),
                        titleRu = obj.getString("titleRu"),
                        contractionMs = obj.getLong("contractionMs"),
                        relaxationMs = obj.getLong("relaxationMs"),
                        repetitions = obj.getInt("repetitions"),
                        isDemo = obj.optBoolean("isDemo", false)
                    )
                )
            }

            val dailyPlans = mutableListOf<DailyPlanEntity>()
            val dpArr = root.optJSONArray("dailyPlans") ?: JSONArray()
            for (i in 0 until dpArr.length()) {
                val obj = dpArr.getJSONObject(i)
                dailyPlans.add(
                    DailyPlanEntity(
                        id = obj.getString("id"),
                        localDate = obj.getString("localDate"),
                        planId = obj.getString("planId"),
                        planVersion = obj.getInt("planVersion"),
                        isDemo = obj.optBoolean("isDemo", false)
                    )
                )
            }

            val slots = mutableListOf<DailySlotEntity>()
            val slotsArr = root.optJSONArray("slots") ?: JSONArray()
            for (i in 0 until slotsArr.length()) {
                val obj = slotsArr.getJSONObject(i)
                slots.add(
                    DailySlotEntity(
                        id = obj.getString("id"),
                        dailyPlanId = obj.getString("dailyPlanId"),
                        localDate = obj.getString("localDate"),
                        assignmentId = obj.getString("assignmentId"),
                        groupCode = obj.getString("groupCode"),
                        sortOrder = obj.getInt("sortOrder"),
                        titleRu = obj.getString("titleRu"),
                        contractionMs = obj.getLong("contractionMs"),
                        relaxationMs = obj.getLong("relaxationMs"),
                        repetitions = obj.getInt("repetitions"),
                        isLastInGroup = obj.optBoolean("isLastInGroup", false),
                        isDemo = obj.optBoolean("isDemo", false)
                    )
                )
            }

            val runs = mutableListOf<WorkoutRunEntity>()
            val runsArr = root.optJSONArray("runs") ?: JSONArray()
            for (i in 0 until runsArr.length()) {
                val obj = runsArr.getJSONObject(i)
                runs.add(
                    WorkoutRunEntity(
                        id = obj.getString("id"),
                        slotId = obj.getString("slotId"),
                        status = obj.getString("status"),
                        startedAtEpochMs = obj.getLong("startedAtEpochMs"),
                        startDate = obj.getString("startDate"),
                        zoneId = obj.optString("zoneId", "Europe/Moscow"),
                        planVersion = obj.getInt("planVersion"),
                        titleRu = obj.getString("titleRu"),
                        groupCode = obj.getString("groupCode"),
                        contractionMs = obj.getLong("contractionMs"),
                        relaxationMs = obj.getLong("relaxationMs"),
                        repetitions = obj.getInt("repetitions"),
                        isLastInGroup = obj.optBoolean("isLastInGroup", false),
                        soundPackIdSnapshot = obj.optString("soundPackIdSnapshot", "SOFT"),
                        metronomeStyleIdSnapshot = obj.optString("metronomeStyleIdSnapshot", "SOFT"),
                        confirmedActiveMs = obj.optLong("confirmedActiveMs", 0L),
                        currentRepetition = obj.optInt("currentRepetition", 1),
                        currentPhase = obj.optString("currentPhase", "DONE"),
                        phaseRemainingMs = obj.optLong("phaseRemainingMs", 0L),
                        checkpointWallEpochMs = obj.optLong("checkpointWallEpochMs", 0L),
                        bootSessionId = obj.optString("bootSessionId", ""),
                        interruptionReason = if (obj.has("interruptionReason") && !obj.isNull("interruptionReason")) obj.getString("interruptionReason") else null,
                        isDemo = obj.optBoolean("isDemo", false),
                        isFreeMode = obj.optBoolean("isFreeMode", false)
                    )
                )
            }

            val completions = mutableListOf<WorkoutCompletionEntity>()
            val compArr = root.optJSONArray("completions") ?: JSONArray()
            for (i in 0 until compArr.length()) {
                val obj = compArr.getJSONObject(i)
                completions.add(
                    WorkoutCompletionEntity(
                        runId = obj.getString("runId"),
                        slotId = obj.getString("slotId"),
                        localDate = obj.getString("localDate"),
                        completedAtEpochMs = obj.getLong("completedAtEpochMs"),
                        optionalDifficulty = if (obj.has("optionalDifficulty") && !obj.isNull("optionalDifficulty")) obj.getString("optionalDifficulty") else null,
                        isDemo = obj.optBoolean("isDemo", false),
                        isFreeMode = obj.optBoolean("isFreeMode", false)
                    )
                )
            }

            val novelObj = root.optJSONObject("novel")
            val novel = if (novelObj != null) {
                ImportedNovelEntity(
                    slotKey = novelObj.optString("slotKey", ImportedNovelEntity.ACTIVE_SLOT_KEY),
                    bookId = novelObj.getString("bookId"),
                    fileName = novelObj.getString("fileName"),
                    explicitTitle = if (novelObj.has("explicitTitle") && !novelObj.isNull("explicitTitle")) novelObj.getString("explicitTitle") else null,
                    displayTitle = novelObj.getString("displayTitle"),
                    sha256Hex = novelObj.getString("sha256Hex"),
                    byteSize = novelObj.getInt("byteSize"),
                    internalFilePath = novelObj.optString("internalFilePath", ""),
                    actHeadingLevel = novelObj.optInt("actHeadingLevel", 0),
                    chapterHeadingLevel = novelObj.optInt("chapterHeadingLevel", 2),
                    actCount = novelObj.optInt("actCount", 0),
                    chapterCount = novelObj.optInt("chapterCount", 0),
                    totalBlockCount = novelObj.optInt("totalBlockCount", 0),
                    importedAtEpochMs = novelObj.optLong("importedAtEpochMs", 0L)
                )
            } else {
                null
            }

            val chapters = mutableListOf<NovelChapterEntity>()
            val chArr = root.optJSONArray("novelChapters") ?: JSONArray()
            for (i in 0 until chArr.length()) {
                val obj = chArr.getJSONObject(i)
                chapters.add(
                    NovelChapterEntity(
                        chapterId = obj.getString("chapterId"),
                        bookId = obj.getString("bookId"),
                        chapterNumber = obj.getInt("chapterNumber"),
                        actId = obj.optString("actId", ""),
                        actNumber = obj.optInt("actNumber", 0),
                        actTitle = obj.optString("actTitle", ""),
                        title = obj.getString("title"),
                        blockCount = obj.optInt("blockCount", 0),
                        stepCount = obj.optInt("stepCount", 0)
                    )
                )
            }

            val blocks = mutableListOf<NovelBlockEntity>()
            val blkArr = root.optJSONArray("novelBlocks") ?: JSONArray()
            for (i in 0 until blkArr.length()) {
                val obj = blkArr.getJSONObject(i)
                blocks.add(
                    NovelBlockEntity(
                        blockId = obj.getString("blockId"),
                        bookId = obj.getString("bookId"),
                        chapterId = obj.getString("chapterId"),
                        orderInChapter = obj.getInt("orderInChapter"),
                        globalOrder = obj.getInt("globalOrder"),
                        stepIndex = obj.getInt("stepIndex"),
                        stepId = obj.getString("stepId"),
                        blockType = obj.getString("blockType"),
                        subheadingLevel = obj.optInt("subheadingLevel", 0),
                        plainText = obj.getString("plainText"),
                        inlineSpansEncoded = obj.optString("inlineSpansEncoded", "")
                    )
                )
            }

            val progressObj = root.optJSONObject("novelReadingProgress")
            val progress = if (progressObj != null) {
                NovelReadingProgressEntity(
                    bookId = progressObj.getString("bookId"),
                    currentActId = progressObj.optString("currentActId", ""),
                    currentChapterId = progressObj.getString("currentChapterId"),
                    currentBlockId = progressObj.getString("currentBlockId"),
                    currentStepIndex = progressObj.optInt("currentStepIndex", 0),
                    readerModeId = progressObj.optString("readerModeId", "BOOK"),
                    isCompleted = progressObj.optBoolean("isCompleted", false),
                    updatedAtEpochMs = progressObj.optLong("updatedAtEpochMs", 0L)
                )
            } else {
                null
            }

            val freeTemplates = mutableListOf<FreeIntervalTemplateEntity>()
            val ftArr = root.optJSONArray("freeTemplates")
            if (ftArr != null) {
                for (i in 0 until ftArr.length()) {
                    val obj = ftArr.getJSONObject(i)
                    freeTemplates.add(
                        FreeIntervalTemplateEntity(
                            slotIndex = obj.getInt("slotIndex"),
                            name = obj.getString("name"),
                            targetDurationMinutes = obj.optInt("targetDurationMinutes", 3),
                            targetDurationSeconds = obj.optInt("targetDurationSeconds", 0),
                            contractionSec = obj.optInt("contractionSec", 0),
                            relaxationSec = obj.optInt("relaxationSec", 0),
                            isConfigured = obj.optBoolean("isConfigured", false),
                            updatedAtEpochMs = obj.optLong("updatedAtEpochMs", 0L)
                        )
                    )
                }
            }

            return BackupDatabaseSnapshot(
                plans = plans,
                assignments = assignments,
                dailyPlans = dailyPlans,
                slots = slots,
                runs = runs,
                completions = completions,
                novel = novel,
                novelChapters = chapters,
                novelBlocks = blocks,
                novelReadingProgress = progress,
                freeTemplates = freeTemplates
            )
        }
    }
}

/**
 * Снимок настроек темы, звуков, метронома и чтения для резервной копии (DataStore).
 */
data class BackupPreferencesSnapshot(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val metronomeEnabled: Boolean = true,
    val signalVolume: Float = 0.80f,
    val metronomeVolume: Float = 0.35f,
    val soundPackId: String = "SOFT",
    val metronomeTickStyleId: String = "SOFT",
    val themeId: String = "DAWN",
    val readerModeId: String = "STEP_BY_STEP",
    val readerFontSizeId: String = "NORMAL",
    val readerLineSpacingId: String = "COMFORTABLE",
    val isRealProgramEnabled: Boolean = false,
    val realProgramSource: String = BenchSignalSettings.DEFAULT_REAL_PROGRAM_SOURCE
) {
    fun toJson(): String {
        val root = JSONObject().apply {
            put("soundEnabled", soundEnabled)
            put("vibrationEnabled", vibrationEnabled)
            put("metronomeEnabled", metronomeEnabled)
            put("signalVolume", signalVolume.toDouble())
            put("metronomeVolume", metronomeVolume.toDouble())
            put("soundPackId", soundPackId)
            put("metronomeTickStyleId", metronomeTickStyleId)
            put("themeId", themeId)
            put("readerModeId", readerModeId)
            put("readerFontSizeId", readerFontSizeId)
            put("readerLineSpacingId", readerLineSpacingId)
            put("isRealProgramEnabled", isRealProgramEnabled)
            put("realProgramSource", realProgramSource)
        }
        return root.toString(2)
    }

    companion object {
        fun fromJson(jsonStr: String): BackupPreferencesSnapshot {
            val root = JSONObject(jsonStr)
            return BackupPreferencesSnapshot(
                soundEnabled = root.optBoolean("soundEnabled", true),
                vibrationEnabled = root.optBoolean("vibrationEnabled", true),
                metronomeEnabled = root.optBoolean("metronomeEnabled", true),
                signalVolume = root.optDouble("signalVolume", 0.80).toFloat().coerceIn(0f, 1f),
                metronomeVolume = root.optDouble("metronomeVolume", 0.35).toFloat().coerceIn(0f, 1f),
                soundPackId = root.optString("soundPackId", "SOFT"),
                metronomeTickStyleId = root.optString("metronomeTickStyleId", "SOFT"),
                themeId = root.optString("themeId", "DAWN"),
                readerModeId = root.optString("readerModeId", "STEP_BY_STEP"),
                readerFontSizeId = root.optString("readerFontSizeId", "NORMAL"),
                readerLineSpacingId = root.optString("readerLineSpacingId", "COMFORTABLE"),
                isRealProgramEnabled = root.optBoolean("isRealProgramEnabled", false),
                realProgramSource = root.optString("realProgramSource", BenchSignalSettings.DEFAULT_REAL_PROGRAM_SOURCE)
            )
        }
    }
}
