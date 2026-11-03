package com.example.domain

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest
import java.util.Locale

/**
 * Режим отображения в универсальном офлайн-читателе (ТЗ п. 5, Update v1.1 п. 7).
 */
enum class ReaderMode(val id: String, val titleRu: String) {
    BOOK("book", "Книга"),
    STEP_BY_STEP("step", "Пошаговый");

    companion object {
        fun fromId(id: String?): ReaderMode =
            entries.firstOrNull { it.id == id } ?: BOOK
    }
}

/**
 * Размер шрифта в читателе.
 */
enum class ReaderFontSize(val id: String, val titleRu: String, val bodySp: Int, val titleSp: Int) {
    SMALL("small", "Компактный", 15, 18),
    NORMAL("normal", "Стандартный", 17, 20),
    LARGE("large", "Крупный", 20, 23),
    EXTRA_LARGE("xlarge", "Очень крупный", 23, 26);

    companion object {
        fun fromId(id: String?): ReaderFontSize =
            entries.firstOrNull { it.id == id } ?: NORMAL
    }
}

/**
 * Межстрочный интервал в читателе.
 */
enum class ReaderLineSpacing(val id: String, val titleRu: String, val multiplier: Float) {
    COMPACT("compact", "Плотный (1.35)", 1.35f),
    COMFORTABLE("comfortable", "Комфортный (1.55)", 1.55f),
    SPACIOUS("spacious", "Свободный (1.78)", 1.78f);

    companion object {
        fun fromId(id: String?): ReaderLineSpacing =
            entries.firstOrNull { it.id == id } ?: COMFORTABLE
    }
}

/**
 * Правило выбора уровней заголовков Markdown для актов и глав (Update v1.1 п. 6).
 * - `0` = Автоматическое распознавание
 * - `-1` = Без деления на акты (для `actHeadingLevel`)
 * - `1..6` = Уровень заголовка Markdown (`#` .. `######`)
 */
data class HeadingLevelRule(
    val actHeadingLevel: Int = AUTO,
    val chapterHeadingLevel: Int = AUTO
) {
    companion object {
        const val AUTO = 0
        const val NO_ACTS = -1
    }
}

/**
 * Тип структурного блока внутри главы линейного романа.
 */
enum class NovelBlockType {
    SUBHEADING,
    PARAGRAPH,
    BLOCKQUOTE,
    DIVIDER,
    SAFE_RAW;

    companion object {
        fun fromName(name: String?): NovelBlockType =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: PARAGRAPH
    }
}

/**
 * Тип встроенного оформления внутри абзаца/цитаты/подзаголовка.
 */
enum class NovelInlineStyle(val code: String) {
    BOLD("B"),
    ITALIC("I"),
    BOLD_ITALIC("BI"),
    INLINE_CODE("C");

    companion object {
        fun fromCode(code: String?): NovelInlineStyle? =
            entries.firstOrNull { it.code == code }
    }
}

/**
 * Диапазон форматирования в очищенном от разметки авторском тексте `plainText`.
 */
data class NovelInlineSpan(
    val start: Int,
    val end: Int,
    val style: NovelInlineStyle
) {
    companion object {
        fun encodeList(spans: List<NovelInlineSpan>): String {
            if (spans.isEmpty()) return ""
            return spans.joinToString(";") { "${it.start},${it.end},${it.style.code}" }
        }

        fun decodeList(encoded: String): List<NovelInlineSpan> {
            if (encoded.isBlank()) return emptyList()
            return encoded.split(";").mapNotNull { token ->
                val parts = token.split(",")
                if (parts.size != 3) return@mapNotNull null
                val start = parts[0].toIntOrNull() ?: return@mapNotNull null
                val end = parts[1].toIntOrNull() ?: return@mapNotNull null
                val style = NovelInlineStyle.fromCode(parts[2]) ?: return@mapNotNull null
                if (start < 0 || end <= start) return@mapNotNull null
                NovelInlineSpan(start = start, end = end, style = style)
            }
        }
    }
}

/**
 * Единичный структурный блок текста главы с постоянным стабильным ID (`blockId`).
 */
data class NovelBlock(
    val blockId: String,
    val chapterId: String,
    val orderInChapter: Int,
    val globalOrder: Int,
    val stepIndex: Int,
    val stepId: String,
    val type: NovelBlockType,
    val subheadingLevel: Int = 0,
    val plainText: String,
    val inlineSpans: List<NovelInlineSpan> = emptyList()
)

/**
 * Порционный шаг («Пошаговый» режим): объединяет несколько целых исходных абзацев
 * по границам существующих абзацев и разделителей без разрыва предложений и без потери текста.
 */
data class NovelStepChunk(
    val stepIndex: Int,
    val stepId: String,
    val chapterId: String,
    val blocks: List<NovelBlock>
)

/**
 * Глава линейного романа.
 */
data class NovelChapter(
    val chapterId: String,
    val chapterNumber: Int,
    val actId: String,
    val actNumber: Int,
    val actTitle: String,
    val title: String,
    val blocks: List<NovelBlock>,
    val steps: List<NovelStepChunk>
)

/**
 * Акт линейного романа (содержит упорядоченный список глав).
 */
data class NovelAct(
    val actId: String,
    val actNumber: Int,
    val title: String,
    val chapters: List<NovelChapter>
)

/**
 * Результат программного разбора Markdown-файла линейного романа.
 */
data class ParsedLinearNovel(
    val bookId: String,
    val fileName: String,
    val explicitBookTitle: String?,
    val displayTitle: String,
    val sha256Hex: String,
    val byteSize: Int,
    val acts: List<NovelAct>,
    val chapters: List<NovelChapter>,
    val allBlocks: List<NovelBlock>,
    val isStructureAmbiguous: Boolean,
    val ambiguityReasonRu: String?,
    val detectedActHeadingLevel: Int,
    val detectedChapterHeadingLevel: Int,
    val headingLevelsSummaryRu: String
)

/**
 * Предварительная сводка перед окончательным импортом Markdown-книги (Update v1.1 п. 6).
 */
data class MarkdownImportPreview(
    val fileName: String,
    val explicitTitle: String?,
    val displayTitle: String,
    val actCount: Int,
    val chapterCount: Int,
    val totalBlockCount: Int,
    val sha256Hex: String,
    val byteSize: Int,
    val isIdenticalToCurrentBook: Boolean,
    val isReplacingExistingBook: Boolean,
    val isStructureAmbiguous: Boolean,
    val ambiguityReasonRu: String?,
    val selectedActHeadingLevel: Int,
    val selectedChapterHeadingLevel: Int,
    val detectedActHeadingLevel: Int,
    val detectedChapterHeadingLevel: Int,
    val headingLevelsSummaryRu: String
)

/**
 * Универсальный детерминированный парсер линейного романа в формате Markdown (Update v1.1 п. 5–8).
 *
 * Гарантирует:
 * - строгую проверку кодировки UTF-8 и расчёт контрольной суммы SHA-256 оригинальных байтов;
 * - программное распознавание названия книги (если оно явно задано), актов и глав без зашитых констант 3/12;
 * - возможность явно указать уровни заголовков для актов и глав при неоднозначной структуре;
 * - полное сохранение авторского порядка, слов, пунктуации, абзацев и безопасное преобразование Markdown-разметки;
 * - математическое тождество авторского текста между режимами «Книга» и «Пошаговый»;
 * - отказ от загрузки внешних изображений и от исполнения HTML.
 */
object MarkdownNovelParser {

    private val ACT_KEYWORD_REGEX = Regex(
        pattern = """^\s*(?:акт|act|часть|part|книга|book)\b""",
        option = RegexOption.IGNORE_CASE
    )
    private val CHAPTER_KEYWORD_REGEX = Regex(
        pattern = """^\s*(?:глава|chapter|сцена|эпизод|пролог|эпилог)\b""",
        option = RegexOption.IGNORE_CASE
    )
    private val HEADING_LINE_REGEX = Regex("""^(#{1,6})\s+(.+?)\s*#*\s*$""")
    private val DIVIDER_LINE_REGEX = Regex("""^\s*(?:(-\s*){3,}|(\*\s*){3,}|(_\s*){3,})\s*$""")
    private val FENCE_LINE_REGEX = Regex("""^\s*(`{3,}|~{3,}).*$""")

    fun computeSha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        val sb = StringBuilder(digest.size * 2)
        for (b in digest) {
            sb.append(String.format(Locale.US, "%02x", b))
        }
        return sb.toString()
    }

    /**
     * Строгая проверка корректности UTF-8 и отсутствия бинарного/повреждённого содержимого.
     */
    fun decodeUtf8Strict(bytes: ByteArray): Result<String> {
        if (bytes.isEmpty()) {
            return Result.failure(IllegalArgumentException("Файл пуст (0 байт)."))
        }
        // Проверка на нулевые байты (признак бинарного или повреждённого файла)
        for (b in bytes) {
            if (b == 0.toByte()) {
                return Result.failure(IllegalArgumentException("Файл содержит бинарные нулевые байты и не является текстовым Markdown."))
            }
        }
        return try {
            val decoder = Charsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            val charBuffer = decoder.decode(ByteBuffer.wrap(bytes))
            var text = charBuffer.toString()
            if (text.startsWith("\uFEFF")) {
                text = text.removePrefix("\uFEFF")
            }
            if (text.isBlank()) {
                Result.failure(IllegalArgumentException("Файл содержит только пробельные символы."))
            } else {
                Result.success(text)
            }
        } catch (e: CharacterCodingException) {
            Result.failure(IllegalArgumentException("Повреждённая кодировка файла (ожидается корректный UTF-8).", e))
        }
    }

    private data class RawHeading(
        val lineIndex: Int,
        val level: Int,
        val rawTitle: String,
        val cleanTitle: String
    )

    private data class RawBlockDraft(
        val type: NovelBlockType,
        val subheadingLevel: Int = 0,
        val rawText: String
    )

    fun parseMarkdownNovel(
        rawBytes: ByteArray,
        fileName: String,
        headingRule: HeadingLevelRule = HeadingLevelRule()
    ): Result<ParsedLinearNovel> {
        val decodedText = decodeUtf8Strict(rawBytes).getOrElse { return Result.failure(it) }
        val sha256 = computeSha256Hex(rawBytes)
        val normalizedLines = decodedText
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .split('\n')

        // 1. Собираем все заголовки вне ограждённых блоков кода
        val headings = mutableListOf<RawHeading>()
        var inFence = false
        for ((idx, line) in normalizedLines.withIndex()) {
            if (FENCE_LINE_REGEX.matches(line)) {
                inFence = !inFence
                continue
            }
            if (inFence) continue
            val match = HEADING_LINE_REGEX.matchEntire(line)
            if (match != null) {
                val level = match.groupValues[1].length
                val titleRaw = match.groupValues[2].trim()
                val (cleanTitle, _) = parseInlineFormatting(titleRaw)
                if (cleanTitle.isNotBlank()) {
                    headings.add(
                        RawHeading(
                            lineIndex = idx,
                            level = level,
                            rawTitle = titleRaw,
                            cleanTitle = cleanTitle
                        )
                    )
                }
            }
        }

        if (headings.isEmpty()) {
            return Result.failure(
                IllegalArgumentException("В файле не найдено ни одного заголовка Markdown (# .. ####) для выделения глав.")
            )
        }

        val countByLevel: Map<Int, Int> = headings.groupingBy { it.level }.eachCount()
        val levelsSummary = countByLevel.entries
            .sortedBy { it.key }
            .joinToString(", ") { "H${it.key}: ${it.value}" }

        // 2. Определяем уровни заголовков для названия книги, актов и глав
        val detection = detectStructureLevels(headings, headingRule)
        val actLevel = detection.actLevel
        val chapterLevel = detection.chapterLevel
        val titleHeadingLineIndex = detection.titleHeadingLineIndex
        val explicitBookTitle = detection.explicitBookTitle

        // 3. Проходим по строкам и распределяем блоки по актам и главам
        data class MutableChapterBuilder(
            val chapterNumber: Int,
            val actNumber: Int,
            val actTitle: String,
            val title: String,
            val drafts: MutableList<RawBlockDraft> = mutableListOf()
        )

        val chapterBuilders = mutableListOf<MutableChapterBuilder>()
        val preChapterDrafts = mutableListOf<RawBlockDraft>()

        var currentActNumber = 0
        var currentActTitle = ""
        var currentChapter: MutableChapterBuilder? = null

        val paragraphLines = mutableListOf<String>()
        val quoteLines = mutableListOf<String>()
        val fenceLines = mutableListOf<String>()
        var insideFence = false

        fun targetDraftList(): MutableList<RawBlockDraft> {
            return currentChapter?.drafts ?: preChapterDrafts
        }

        fun flushParagraph() {
            if (paragraphLines.isNotEmpty()) {
                val joined = paragraphLines.joinToString("\n").trim()
                if (joined.isNotEmpty()) {
                    targetDraftList().add(RawBlockDraft(type = NovelBlockType.PARAGRAPH, rawText = joined))
                }
                paragraphLines.clear()
            }
        }

        fun flushQuote() {
            if (quoteLines.isNotEmpty()) {
                val joined = quoteLines.joinToString("\n").trim()
                if (joined.isNotEmpty()) {
                    targetDraftList().add(RawBlockDraft(type = NovelBlockType.BLOCKQUOTE, rawText = joined))
                }
                quoteLines.clear()
            }
        }

        fun flushFence() {
            if (fenceLines.isNotEmpty()) {
                val joined = fenceLines.joinToString("\n").trim()
                if (joined.isNotEmpty()) {
                    targetDraftList().add(RawBlockDraft(type = NovelBlockType.SAFE_RAW, rawText = joined))
                }
                fenceLines.clear()
            }
        }

        fun flushAllOpenBlocks() {
            flushParagraph()
            flushQuote()
            flushFence()
        }

        for ((lineIdx, line) in normalizedLines.withIndex()) {
            if (FENCE_LINE_REGEX.matches(line)) {
                if (insideFence) {
                    insideFence = false
                    flushFence()
                } else {
                    flushParagraph()
                    flushQuote()
                    insideFence = true
                }
                continue
            }

            if (insideFence) {
                fenceLines.add(line)
                continue
            }

            if (lineIdx == titleHeadingLineIndex) {
                flushAllOpenBlocks()
                continue
            }

            val headingMatch = HEADING_LINE_REGEX.matchEntire(line)
            if (headingMatch != null) {
                flushAllOpenBlocks()
                val level = headingMatch.groupValues[1].length
                val rawHeadingText = headingMatch.groupValues[2].trim()
                val (cleanHeadingText, _) = parseInlineFormatting(rawHeadingText)

                val isActHeading = when {
                    actLevel == HeadingLevelRule.NO_ACTS -> false
                    headingRule.actHeadingLevel == HeadingLevelRule.AUTO &&
                        detection.useKeywordMatching &&
                        ACT_KEYWORD_REGEX.containsMatchIn(cleanHeadingText) -> true
                    level == actLevel && level != chapterLevel -> true
                    else -> false
                }

                val isChapterHeading = when {
                    isActHeading -> false
                    headingRule.chapterHeadingLevel == HeadingLevelRule.AUTO &&
                        detection.useKeywordMatching &&
                        CHAPTER_KEYWORD_REGEX.containsMatchIn(cleanHeadingText) -> true
                    level == chapterLevel -> true
                    else -> false
                }

                when {
                    isActHeading -> {
                        currentActNumber += 1
                        currentActTitle = cleanHeadingText
                    }
                    isChapterHeading -> {
                        val nextChapterNum = chapterBuilders.size + 1
                        val assignedActNum = if (currentActNumber > 0) currentActNumber else 1
                        val newBuilder = MutableChapterBuilder(
                            chapterNumber = nextChapterNum,
                            actNumber = assignedActNum,
                            actTitle = currentActTitle,
                            title = cleanHeadingText
                        )
                        // Если перед первой главой были вводные абзацы (эпиграф/аннотация), сохраняем их в начале первой главы без потери текста
                        if (chapterBuilders.isEmpty() && preChapterDrafts.isNotEmpty()) {
                            newBuilder.drafts.addAll(preChapterDrafts)
                            preChapterDrafts.clear()
                        }
                        chapterBuilders.add(newBuilder)
                        currentChapter = newBuilder
                    }
                    else -> {
                        targetDraftList().add(
                            RawBlockDraft(
                                type = NovelBlockType.SUBHEADING,
                                subheadingLevel = level,
                                rawText = rawHeadingText
                            )
                        )
                    }
                }
                continue
            }

            if (DIVIDER_LINE_REGEX.matches(line)) {
                flushAllOpenBlocks()
                targetDraftList().add(RawBlockDraft(type = NovelBlockType.DIVIDER, rawText = "* * *"))
                continue
            }

            if (line.isBlank()) {
                flushParagraph()
                flushQuote()
                continue
            }

            val trimmedStart = line.trimStart()
            if (trimmedStart.startsWith(">")) {
                flushParagraph()
                val quoteContent = trimmedStart.removePrefix(">").let {
                    if (it.startsWith(" ")) it.substring(1) else it
                }
                quoteLines.add(quoteContent)
            } else {
                flushQuote()
                paragraphLines.add(line.trim())
            }
        }
        flushAllOpenBlocks()

        if (chapterBuilders.isEmpty()) {
            return Result.failure(
                IllegalArgumentException(
                    "Не удалось выделить главы на выбранном уровне заголовков ($levelsSummary). Выберите подходящий уровень заголовков для глав."
                )
            )
        }

        // 4. Формируем финальные модели NovelBlock, NovelStepChunk, NovelChapter и NovelAct
        var globalBlockCounter = 0
        val builtChapters = mutableListOf<NovelChapter>()

        for (cb in chapterBuilders) {
            val chapterId = String.format(Locale.US, "ch_%02d", cb.chapterNumber)
            val actId = String.format(Locale.US, "act_%02d", cb.actNumber)

            // Сначала группируем черновые блоки в пошаговые фрагменты (steps) по существующим абзацам и разделителям
            val stepDraftGroups = groupDraftsIntoStepChunks(cb.drafts)
            val chapterBlocks = mutableListOf<NovelBlock>()
            val chapterSteps = mutableListOf<NovelStepChunk>()

            var orderInChapter = 0
            for ((stepIdx, groupDrafts) in stepDraftGroups.withIndex()) {
                val firstBlockId = String.format(Locale.US, "%s_blk_%04d", chapterId, orderInChapter + 1)
                val stepBlocks = mutableListOf<NovelBlock>()

                for (draft in groupDrafts) {
                    orderInChapter += 1
                    globalBlockCounter += 1
                    val blockId = String.format(Locale.US, "%s_blk_%04d", chapterId, orderInChapter)
                    val (plainText, spans) = when (draft.type) {
                        NovelBlockType.DIVIDER -> "* * *" to emptyList()
                        NovelBlockType.SAFE_RAW -> draft.rawText to emptyList()
                        else -> parseInlineFormatting(draft.rawText)
                    }
                    val block = NovelBlock(
                        blockId = blockId,
                        chapterId = chapterId,
                        orderInChapter = orderInChapter - 1,
                        globalOrder = globalBlockCounter - 1,
                        stepIndex = stepIdx,
                        stepId = firstBlockId,
                        type = draft.type,
                        subheadingLevel = draft.subheadingLevel,
                        plainText = plainText,
                        inlineSpans = spans
                    )
                    chapterBlocks.add(block)
                    stepBlocks.add(block)
                }

                if (stepBlocks.isNotEmpty()) {
                    chapterSteps.add(
                        NovelStepChunk(
                            stepIndex = stepIdx,
                            stepId = firstBlockId,
                            chapterId = chapterId,
                            blocks = stepBlocks
                        )
                    )
                }
            }

            builtChapters.add(
                NovelChapter(
                    chapterId = chapterId,
                    chapterNumber = cb.chapterNumber,
                    actId = actId,
                    actNumber = cb.actNumber,
                    actTitle = cb.actTitle,
                    title = cb.title,
                    blocks = chapterBlocks,
                    steps = chapterSteps
                )
            )
        }

        val allBlocks = builtChapters.flatMap { it.blocks }
        if (allBlocks.isEmpty()) {
            return Result.failure(
                IllegalArgumentException("В выбранном файле главы не содержат текстовых абзацев.")
            )
        }

        // Формируем список актов (если акты были выделены явно)
        val hasExplicitActs = builtChapters.any { it.actTitle.isNotBlank() }
        val builtActs: List<NovelAct> = if (hasExplicitActs) {
            builtChapters
                .groupBy { it.actNumber }
                .entries
                .sortedBy { it.key }
                .map { (actNum, chList) ->
                    val actId = String.format(Locale.US, "act_%02d", actNum)
                    val actTitle = chList.firstOrNull { it.actTitle.isNotBlank() }?.actTitle
                        ?: "Акт $actNum"
                    NovelAct(
                        actId = actId,
                        actNumber = actNum,
                        title = actTitle,
                        chapters = chList
                    )
                }
        } else {
            emptyList()
        }

        val fallbackFileTitle = fileName
            .substringAfterLast('/')
            .removeSuffix(".markdown")
            .removeSuffix(".md")
            .ifBlank { "Импортированная книга" }

        val displayTitle = explicitBookTitle?.takeIf { it.isNotBlank() } ?: fallbackFileTitle
        val bookId = "novel_${sha256.take(16)}"

        return Result.success(
            ParsedLinearNovel(
                bookId = bookId,
                fileName = fileName.substringAfterLast('/'),
                explicitBookTitle = explicitBookTitle,
                displayTitle = displayTitle,
                sha256Hex = sha256,
                byteSize = rawBytes.size,
                acts = builtActs,
                chapters = builtChapters,
                allBlocks = allBlocks,
                isStructureAmbiguous = detection.isAmbiguous,
                ambiguityReasonRu = detection.ambiguityReasonRu,
                detectedActHeadingLevel = actLevel,
                detectedChapterHeadingLevel = chapterLevel,
                headingLevelsSummaryRu = levelsSummary
            )
        )
    }

    private data class StructureLevelDetection(
        val actLevel: Int,
        val chapterLevel: Int,
        val titleHeadingLineIndex: Int?,
        val explicitBookTitle: String?,
        val useKeywordMatching: Boolean,
        val isAmbiguous: Boolean,
        val ambiguityReasonRu: String?
    )

    private fun detectStructureLevels(
        headings: List<RawHeading>,
        rule: HeadingLevelRule
    ): StructureLevelDetection {
        val actKeywordHeadings = headings.filter { ACT_KEYWORD_REGEX.containsMatchIn(it.cleanTitle) }
        val chapterKeywordHeadings = headings.filter { CHAPTER_KEYWORD_REGEX.containsMatchIn(it.cleanTitle) }

        val firstStructuralLine = (actKeywordHeadings + chapterKeywordHeadings)
            .minOfOrNull { it.lineIndex }

        // Проверяем, есть ли явный заголовок книги (H1 в самом начале до первого акта/главы)
        val firstHeading = headings.first()
        val hasExplicitTopBookTitle = when {
            firstHeading.level == 1 &&
                !ACT_KEYWORD_REGEX.containsMatchIn(firstHeading.cleanTitle) &&
                !CHAPTER_KEYWORD_REGEX.containsMatchIn(firstHeading.cleanTitle) &&
                (firstStructuralLine == null || firstHeading.lineIndex < firstStructuralLine) &&
                headings.count { it.level == 1 } == 1 -> true
            else -> false
        }

        val titleLineIndex = if (hasExplicitTopBookTitle) firstHeading.lineIndex else null
        val explicitTitle = if (hasExplicitTopBookTitle) firstHeading.cleanTitle else null
        val remainingHeadings = if (hasExplicitTopBookTitle) headings.drop(1) else headings

        // Если пользователь явно задал уровни заголовков
        if (rule.actHeadingLevel != HeadingLevelRule.AUTO || rule.chapterHeadingLevel != HeadingLevelRule.AUTO) {
            val chosenAct = if (rule.actHeadingLevel == HeadingLevelRule.AUTO) {
                actKeywordHeadings.firstOrNull()?.level ?: HeadingLevelRule.NO_ACTS
            } else {
                rule.actHeadingLevel
            }
            val chosenChapter = if (rule.chapterHeadingLevel == HeadingLevelRule.AUTO) {
                chapterKeywordHeadings.firstOrNull()?.level
                    ?: remainingHeadings.map { it.level }.firstOrNull { it != chosenAct }
                    ?: remainingHeadings.firstOrNull()?.level
                    ?: 2
            } else {
                rule.chapterHeadingLevel
            }
            return StructureLevelDetection(
                actLevel = chosenAct,
                chapterLevel = chosenChapter,
                titleHeadingLineIndex = titleLineIndex,
                explicitBookTitle = explicitTitle,
                useKeywordMatching = false,
                isAmbiguous = false,
                ambiguityReasonRu = null
            )
        }

        // Автоматический режим:
        // Случай 1: в тексте есть явные заголовки "Акт ..." и "Глава ..."
        if (actKeywordHeadings.isNotEmpty() && chapterKeywordHeadings.isNotEmpty()) {
            val actLvl = actKeywordHeadings.groupingBy { it.level }.eachCount().maxBy { it.value }.key
            val chLvl = chapterKeywordHeadings.groupingBy { it.level }.eachCount().maxBy { it.value }.key
            return StructureLevelDetection(
                actLevel = actLvl,
                chapterLevel = chLvl,
                titleHeadingLineIndex = titleLineIndex,
                explicitBookTitle = explicitTitle,
                useKeywordMatching = true,
                isAmbiguous = false,
                ambiguityReasonRu = null
            )
        }

        // Случай 2: в тексте есть явные заголовки "Глава ...", но нет ключевого слова "Акт"
        if (chapterKeywordHeadings.isNotEmpty()) {
            val chLvl = chapterKeywordHeadings.groupingBy { it.level }.eachCount().maxBy { it.value }.key
            val higherLevels = remainingHeadings.filter { it.level < chLvl }.map { it.level }.distinct().sorted()
            val actLvl = higherLevels.firstOrNull() ?: HeadingLevelRule.NO_ACTS
            val ambiguous = higherLevels.size > 1
            return StructureLevelDetection(
                actLevel = actLvl,
                chapterLevel = chLvl,
                titleHeadingLineIndex = titleLineIndex,
                explicitBookTitle = explicitTitle,
                useKeywordMatching = actLvl == HeadingLevelRule.NO_ACTS,
                isAmbiguous = ambiguous,
                ambiguityReasonRu = if (ambiguous) {
                    "Найдено несколько уровней заголовков выше глав. Проверьте выбранный уровень заголовков для актов."
                } else {
                    null
                }
            )
        }

        // Случай 3: ключевых слов "Акт"/"Глава" нет — определяем по иерархии уровней H1/H2/H3
        val distinctLevels = remainingHeadings.map { it.level }.distinct().sorted()
        return when {
            distinctLevels.size >= 2 -> {
                val actLvl = distinctLevels[0]
                val chLvl = distinctLevels[1]
                StructureLevelDetection(
                    actLevel = actLvl,
                    chapterLevel = chLvl,
                    titleHeadingLineIndex = titleLineIndex,
                    explicitBookTitle = explicitTitle,
                    useKeywordMatching = false,
                    isAmbiguous = true,
                    ambiguityReasonRu = "Заголовки не содержат слов «Акт» / «Глава». Автоматически выбраны H$actLvl для актов и H$chLvl для глав — при необходимости укажите другие уровни перед импортом."
                )
            }
            distinctLevels.size == 1 -> {
                val chLvl = distinctLevels[0]
                StructureLevelDetection(
                    actLevel = HeadingLevelRule.NO_ACTS,
                    chapterLevel = chLvl,
                    titleHeadingLineIndex = titleLineIndex,
                    explicitBookTitle = explicitTitle,
                    useKeywordMatching = false,
                    isAmbiguous = true,
                    ambiguityReasonRu = "Обнаружен один уровень заголовков (H$chLvl) без деления на акты. Вы можете подтвердить структуру или выбрать уровни заголовков."
                )
            }
            else -> {
                StructureLevelDetection(
                    actLevel = HeadingLevelRule.NO_ACTS,
                    chapterLevel = 1,
                    titleHeadingLineIndex = null,
                    explicitBookTitle = null,
                    useKeywordMatching = false,
                    isAmbiguous = true,
                    ambiguityReasonRu = "Структура заголовков неоднозначна. Укажите уровни заголовков для актов и глав."
                )
            }
        }
    }

    /**
     * Разбивает список блоков главы на порционные шаги для режима «Пошаговый»:
     * - разбиение строго по границам существующих абзацев и разделителей (`---` / `***`);
     * - ни одно предложение или абзац не разрезается;
     * - в одном шаге до 3 абзацев/цитат (либо меньше, если встретился разделитель сцены или подзаголовок).
     */
    private fun groupDraftsIntoStepChunks(drafts: List<RawBlockDraft>): List<List<RawBlockDraft>> {
        if (drafts.isEmpty()) return emptyList()
        val steps = mutableListOf<List<RawBlockDraft>>()
        var currentGroup = mutableListOf<RawBlockDraft>()
        var textBlocksInCurrentStep = 0

        fun flushStep() {
            if (currentGroup.isNotEmpty()) {
                steps.add(currentGroup.toList())
                currentGroup = mutableListOf()
                textBlocksInCurrentStep = 0
            }
        }

        for (draft in drafts) {
            when (draft.type) {
                NovelBlockType.DIVIDER -> {
                    currentGroup.add(draft)
                    flushStep()
                }
                NovelBlockType.SUBHEADING -> {
                    if (textBlocksInCurrentStep > 0) {
                        flushStep()
                    }
                    currentGroup.add(draft)
                }
                NovelBlockType.PARAGRAPH,
                NovelBlockType.BLOCKQUOTE,
                NovelBlockType.SAFE_RAW -> {
                    currentGroup.add(draft)
                    textBlocksInCurrentStep += 1
                    if (textBlocksInCurrentStep >= 3) {
                        flushStep()
                    }
                }
            }
        }
        flushStep()
        return steps
    }

    /**
     * Преобразует встроенную Markdown-разметку (`**жирный**`, `*курсив*`, `***жирный курсив***`, `` `код` ``,
     * `![alt](url)`, `[текст](url)`) в чистый авторский текст `plainText` и список диапазонов оформления `NovelInlineSpan`.
     *
     * Безопасность (Update v1.1 п. 7):
     * - Внешние изображения `![alt](url)` НЕ загружаются по сети и преобразуются в безопасную текстовую подпись `[Изображение: alt]`.
     * - Ссылки `[текст](url)` оставляют видимый авторский текст `текст`.
     * - Сырой HTML не исполняется и остаётся обычным безопасным текстом без потери содержимого.
     */
    fun parseInlineFormatting(rawMarkdown: String): Pair<String, List<NovelInlineSpan>> {
        if (rawMarkdown.isEmpty()) return "" to emptyList()

        // 1. Безопасно заменяем внешние изображения ![alt](url) -> [Изображение: alt] без сетевых обращений
        val imageRegex = Regex("""!\[([^\]]*)]\(([^)]*)\)""")
        var preProcessed = imageRegex.replace(rawMarkdown) { match ->
            val alt = match.groupValues[1].trim().ifEmpty { "иллюстрация" }
            "[Изображение: $alt]"
        }

        // 2. Безопасно преобразуем ссылки [текст](url) -> текст
        val linkRegex = Regex("""\[([^\]]+)]\(([^)]*)\)""")
        preProcessed = linkRegex.replace(preProcessed) { match ->
            match.groupValues[1]
        }

        val sb = StringBuilder(preProcessed.length)
        val spans = mutableListOf<NovelInlineSpan>()
        var i = 0
        val len = preProcessed.length

        while (i < len) {
            // Обработка экранированных символов Markdown (\*, \_, \#, \[ и т.д.)
            if (preProcessed[i] == '\\' && i + 1 < len && preProcessed[i + 1] in "*_~`[]()#+-!") {
                sb.append(preProcessed[i + 1])
                i += 2
                continue
            }

            // ***жирный курсив*** или ___жирный курсив___
            if (i + 3 <= len && (preProcessed.startsWith("***", i) || preProcessed.startsWith("___", i))) {
                val marker = preProcessed.substring(i, i + 3)
                val closeIdx = preProcessed.indexOf(marker, i + 3)
                if (closeIdx > i + 3) {
                    val inner = preProcessed.substring(i + 3, closeIdx)
                    val startOut = sb.length
                    sb.append(inner)
                    val endOut = sb.length
                    if (endOut > startOut) {
                        spans.add(NovelInlineSpan(startOut, endOut, NovelInlineStyle.BOLD_ITALIC))
                    }
                    i = closeIdx + 3
                    continue
                }
            }

            // **жирный** или __жирный__
            if (i + 2 <= len && (preProcessed.startsWith("**", i) || preProcessed.startsWith("__", i))) {
                val marker = preProcessed.substring(i, i + 2)
                val closeIdx = preProcessed.indexOf(marker, i + 2)
                if (closeIdx > i + 2) {
                    val inner = preProcessed.substring(i + 2, closeIdx)
                    val startOut = sb.length
                    sb.append(inner)
                    val endOut = sb.length
                    if (endOut > startOut) {
                        spans.add(NovelInlineSpan(startOut, endOut, NovelInlineStyle.BOLD))
                    }
                    i = closeIdx + 2
                    continue
                }
            }

            // `встроенный код`
            if (preProcessed[i] == '`') {
                val closeIdx = preProcessed.indexOf('`', i + 1)
                if (closeIdx > i + 1) {
                    val inner = preProcessed.substring(i + 1, closeIdx)
                    val startOut = sb.length
                    sb.append(inner)
                    val endOut = sb.length
                    if (endOut > startOut) {
                        spans.add(NovelInlineSpan(startOut, endOut, NovelInlineStyle.INLINE_CODE))
                    }
                    i = closeIdx + 1
                    continue
                }
            }

            // *курсив* или _курсив_ (с учётом границ слова для '_', чтобы не искажать CH_1_END или snake_case)
            if ((preProcessed[i] == '*' || preProcessed[i] == '_') &&
                i + 1 < len &&
                !preProcessed[i + 1].isWhitespace()
            ) {
                val markerChar = preProcessed[i]
                val validStartBoundary = markerChar == '*' || i == 0 || !preProcessed[i - 1].isLetterOrDigit()
                if (validStartBoundary) {
                    val closeIdx = preProcessed.indexOf(markerChar, i + 1)
                    val validEndBoundary = closeIdx > i + 1 &&
                        !preProcessed[closeIdx - 1].isWhitespace() &&
                        (markerChar == '*' || closeIdx + 1 >= len || !preProcessed[closeIdx + 1].isLetterOrDigit())
                    if (validEndBoundary) {
                        val inner = preProcessed.substring(i + 1, closeIdx)
                        val startOut = sb.length
                        sb.append(inner)
                        val endOut = sb.length
                        if (endOut > startOut) {
                            spans.add(NovelInlineSpan(startOut, endOut, NovelInlineStyle.ITALIC))
                        }
                        i = closeIdx + 1
                        continue
                    }
                }
            }

            sb.append(preProcessed[i])
            i += 1
        }

        return sb.toString() to spans
    }
}

/**
 * Генератор нейтральных тестовых Markdown-файлов для автоматических и диагностических проверок (Update v1.1 п. 5, п. 8).
 * Не содержит художественных сюжетов, выборов или ветвлений; проверяет структуру 3 актов и 12 глав,
 * кириллицу, пустые строки, длинные абзацы, цитаты, выделение, курсив и разделители.
 */
object NeutralTestMarkdownFactory {

    const val DEFAULT_TEST_FILE_NAME = "neutral_test_novel_3acts_12chapters.md"

    fun buildThreeActsTwelveChaptersMarkdown(
        explicitTitle: String? = "Нейтральный технический макет (3 акта, 12 глав)"
    ): String {
        val sb = StringBuilder()
        if (explicitTitle != null) {
            sb.append("# ").append(explicitTitle).append("\n\n")
        }

        var globalChapter = 1
        for (actNum in 1..3) {
            val roman = when (actNum) {
                1 -> "I"
                2 -> "II"
                else -> "III"
            }
            sb.append("## Акт $roman. Раздел структуры $actNum\n\n")

            for (chInAct in 1..4) {
                val chNum = globalChapter++
                sb.append("### Глава $chNum. Проверка последовательности (Акт $actNum, часть $chInAct)\n\n")

                sb.append(
                    "Абзац $chNum.1. Контрольный кириллический текст главы $chNum в акте $actNum. " +
                        "Здесь проверяется **полужирное выделение**, *курсивное начертание* и точная передача знаков препинания: " +
                        "тире — кавычек «ёлочек», многоточия… и скобок (без потери ни одного слова).\n\n"
                )

                sb.append(
                    "Абзац $chNum.2 (длинный абзац для проверки прокрутки и переноса строк). " +
                        "В режиме «Книга» и в режиме «Пошаговый» этот абзац должен сохранять абсолютно идентичный авторский текст " +
                        "без разрыва предложений посередине. Последовательность блоков проверяется программно по порядку следования " +
                        "и полному совпадению каждого абзаца от начала первой главы до финала двенадцатой главы.\n\n"
                )

                sb.append(
                    "> Цитата $chNum.3: «Спокойный ритм чтения и точность сохранения позиции по стабильному идентификатору блока».\n\n"
                )

                sb.append("---\n\n")

                sb.append(
                    "Абзац $chNum.4 после горизонтального разделителя сцены. " +
                        "Финальный контрольный блок главы $chNum (маркер целостности: CH_${chNum}_END).\n\n"
                )
            }
        }
        return sb.toString()
    }
}
