package com.example.data

import android.content.Context
import com.example.domain.HeadingLevelRule
import com.example.domain.MarkdownImportPreview
import com.example.domain.MarkdownNovelParser
import com.example.domain.NeutralTestMarkdownFactory
import com.example.domain.NovelAct
import com.example.domain.NovelBlock
import com.example.domain.NovelBlockType
import com.example.domain.NovelChapter
import com.example.domain.NovelInlineSpan
import com.example.domain.NovelStepChunk
import com.example.domain.ParsedLinearNovel
import com.example.domain.ReaderMode
import com.example.domain.TimeProvider
import java.io.File

/**
 * Полное состояние экрана чтения и карточки «История» на главной (Update v1.1 п. 5–8).
 */
data class StoryReaderUiState(
    val hasImportedBook: Boolean = false,
    val bookId: String = "",
    val fileName: String = "",
    val explicitBookTitle: String? = null,
    val displayTitleRu: String = "Книга не добавлена",
    val sha256Hex: String = "",
    val byteSize: Int = 0,
    val originalBytesChecksumVerified: Boolean = false,
    val actCount: Int = 0,
    val chapterCount: Int = 0,
    val acts: List<NovelAct> = emptyList(),
    val chapters: List<NovelChapter> = emptyList(),
    val currentAct: NovelAct? = null,
    val currentChapter: NovelChapter? = null,
    val currentStepIndex: Int = 0,
    val totalStepsInCurrentChapter: Int = 0,
    val currentStepBlocks: List<NovelBlock> = emptyList(),
    val currentChapterBlocks: List<NovelBlock> = emptyList(),
    val currentBlockId: String = "",
    val chapterProgressFraction: Float = 0f,
    val chapterProgressLabelRu: String = "",
    val savedPositionSummaryRu: String = "Книга ещё не импортирована",
    val isCompleted: Boolean = false,
    val hasPreviousStep: Boolean = false,
    val hasNextStep: Boolean = false,
    val hasPreviousChapter: Boolean = false,
    val hasNextChapter: Boolean = false,
    val pendingImportPreview: MarkdownImportPreview? = null,
    val statusBannerMessageRu: String? = null,
    val errorMessageRu: String? = null
)

/**
 * Офлайн-репозиторий чтения и локального импорта линейного романа в Markdown (Update v1.1 п. 5–8).
 *
 * Гарантирует:
 * - сохранение точной побайтовой копии выбранного `.md` / `.markdown` файла во внутреннем хранилище
 *   приложения (`filesDir/imported_books/active_novel_source.md`) с проверкой SHA-256;
 * - раздельное хранение исходных байтов и производных блоков отображения в Room;
 * - сохранение прежней книги и её позиции при любой ошибке чтения/кодировки/структуры нового файла;
 * - отсутствие дублирования и сохранение позиции при повторном импорте идентичного файла (по SHA-256);
 * - полное совпадение авторского текста между режимами «Книга» и «Пошаговый»;
 * - отсутствие вывода текста книги в логи и отсутствие любых сетевых запросов.
 */
class StoryRepository(
    private val dao: VelarisDao,
    private val timeProvider: TimeProvider,
    private val internalStorageDir: File
) {

    constructor(
        context: Context,
        dao: VelarisDao,
        timeProvider: TimeProvider
    ) : this(
        dao = dao,
        timeProvider = timeProvider,
        internalStorageDir = File(context.applicationContext.filesDir, "imported_books")
    )

    private var pendingRawBytes: ByteArray? = null
    private var pendingFileName: String? = null
    private var pendingHeadingRule: HeadingLevelRule = HeadingLevelRule()
    private var pendingParsedNovel: ParsedLinearNovel? = null

    /**
     * Загружает актуальное состояние импортированной книги и сохранённой позиции из Room и внутреннего хранилища.
     */
    suspend fun loadReaderState(
        statusBannerRu: String? = null,
        errorBannerRu: String? = null
    ): StoryReaderUiState {
        return try {
            val activeNovel = dao.getActiveNovel()
            val preview = buildPendingPreviewModel(activeNovel)

            if (activeNovel == null) {
                return StoryReaderUiState(
                    hasImportedBook = false,
                    pendingImportPreview = preview,
                    statusBannerMessageRu = statusBannerRu,
                    errorMessageRu = errorBannerRu
                )
            }

            val chapterEntities = dao.getNovelChapters(activeNovel.bookId)
            val blockEntities = dao.getNovelBlocksForBook(activeNovel.bookId)

            if (chapterEntities.isEmpty() || blockEntities.isEmpty()) {
                return StoryReaderUiState(
                    hasImportedBook = false,
                    pendingImportPreview = preview,
                    statusBannerMessageRu = statusBannerRu,
                    errorMessageRu = errorBannerRu ?: "Производные блоки книги не найдены. Импортируйте файл повторно."
                )
            }

            val blocksByChapterId: Map<String, List<NovelBlock>> = blockEntities
                .map { entity ->
                    NovelBlock(
                        blockId = entity.blockId,
                        chapterId = entity.chapterId,
                        orderInChapter = entity.orderInChapter,
                        globalOrder = entity.globalOrder,
                        stepIndex = entity.stepIndex,
                        stepId = entity.stepId,
                        type = NovelBlockType.fromName(entity.blockType),
                        subheadingLevel = entity.subheadingLevel,
                        plainText = entity.plainText,
                        inlineSpans = NovelInlineSpan.decodeList(entity.inlineSpansEncoded)
                    )
                }
                .groupBy { it.chapterId }

            val chapters: List<NovelChapter> = chapterEntities.map { chEntity ->
                val chBlocks = (blocksByChapterId[chEntity.chapterId] ?: emptyList())
                    .sortedBy { it.orderInChapter }
                val chSteps = chBlocks
                    .groupBy { it.stepIndex }
                    .entries
                    .sortedBy { it.key }
                    .map { (stepIdx, stepBlocks) ->
                        NovelStepChunk(
                            stepIndex = stepIdx,
                            stepId = stepBlocks.first().stepId,
                            chapterId = chEntity.chapterId,
                            blocks = stepBlocks
                        )
                    }
                NovelChapter(
                    chapterId = chEntity.chapterId,
                    chapterNumber = chEntity.chapterNumber,
                    actId = chEntity.actId,
                    actNumber = chEntity.actNumber,
                    actTitle = chEntity.actTitle,
                    title = chEntity.title,
                    blocks = chBlocks,
                    steps = chSteps
                )
            }

            val hasExplicitActs = chapters.any { it.actTitle.isNotBlank() }
            val acts: List<NovelAct> = if (hasExplicitActs) {
                chapters
                    .groupBy { it.actNumber }
                    .entries
                    .sortedBy { it.key }
                    .map { (actNum, chList) ->
                        val firstCh = chList.first()
                        NovelAct(
                            actId = firstCh.actId,
                            actNumber = actNum,
                            title = firstCh.actTitle.ifBlank { "Акт $actNum" },
                            chapters = chList
                        )
                    }
            } else {
                emptyList()
            }

            // Проверяем неизменность исходного файла по контрольной сумме SHA-256
            val sourceFile = File(activeNovel.internalFilePath)
            val checksumVerified = if (sourceFile.exists() && sourceFile.isFile) {
                runCatching {
                    val diskSha = MarkdownNovelParser.computeSha256Hex(sourceFile.readBytes())
                    diskSha.equals(activeNovel.sha256Hex, ignoreCase = true)
                }.getOrDefault(false)
            } else {
                false
            }

            var progress = dao.getNovelReadingProgress(activeNovel.bookId)
            val firstChapter = chapters.first()
            val firstBlock = firstChapter.blocks.first()

            val currentChapter = if (progress != null) {
                chapters.firstOrNull { it.chapterId == progress.currentChapterId } ?: firstChapter
            } else {
                firstChapter
            }

            val resolvedBlock = if (progress != null) {
                currentChapter.blocks.firstOrNull { it.blockId == progress.currentBlockId }
                    ?: currentChapter.steps.getOrNull(progress.currentStepIndex)?.blocks?.firstOrNull()
                    ?: currentChapter.blocks.first()
            } else {
                firstBlock
            }

            val resolvedStepIndex = resolvedBlock.stepIndex.coerceIn(0, (currentChapter.steps.size - 1).coerceAtLeast(0))
            val resolvedStep = currentChapter.steps.getOrElse(resolvedStepIndex) { currentChapter.steps.first() }

            if (progress == null ||
                progress.currentChapterId != currentChapter.chapterId ||
                progress.currentBlockId != resolvedBlock.blockId ||
                progress.currentStepIndex != resolvedStepIndex
            ) {
                val updatedProgress = NovelReadingProgressEntity(
                    bookId = activeNovel.bookId,
                    currentActId = currentChapter.actId,
                    currentChapterId = currentChapter.chapterId,
                    currentBlockId = resolvedBlock.blockId,
                    currentStepIndex = resolvedStepIndex,
                    readerModeId = progress?.readerModeId ?: ReaderMode.BOOK.id,
                    isCompleted = progress?.isCompleted ?: false,
                    updatedAtEpochMs = timeProvider.currentWallTimeMs()
                )
                dao.upsertNovelReadingProgress(updatedProgress)
                progress = updatedProgress
            }

            val currentAct = acts.firstOrNull { it.actId == currentChapter.actId }
            val chapterIndex = chapters.indexOfFirst { it.chapterId == currentChapter.chapterId }.coerceAtLeast(0)
            val totalSteps = currentChapter.steps.size.coerceAtLeast(1)
            val progressFraction = ((resolvedStepIndex + 1).toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)
            val percent = (progressFraction * 100).toInt()

            val actPrefix = if (currentAct != null) "${currentAct.title} · " else ""
            val chapterProgressLabel =
                "${actPrefix}${currentChapter.title} · Фрагмент ${resolvedStepIndex + 1} из $totalSteps ($percent%)"

            val savedSummary = if (progress.isCompleted) {
                "${currentChapter.title} · История прочитана"
            } else {
                "${currentChapter.title} · Фрагмент ${resolvedStepIndex + 1} из $totalSteps"
            }

            val hasPrevChapter = chapterIndex > 0
            val hasNextChapter = chapterIndex < chapters.lastIndex
            val hasPrevStep = resolvedStepIndex > 0 || hasPrevChapter
            val hasNextStep = resolvedStepIndex < currentChapter.steps.lastIndex || hasNextChapter

            StoryReaderUiState(
                hasImportedBook = true,
                bookId = activeNovel.bookId,
                fileName = activeNovel.fileName,
                explicitBookTitle = activeNovel.explicitTitle,
                displayTitleRu = activeNovel.displayTitle,
                sha256Hex = activeNovel.sha256Hex,
                byteSize = activeNovel.byteSize,
                originalBytesChecksumVerified = checksumVerified,
                actCount = activeNovel.actCount,
                chapterCount = activeNovel.chapterCount,
                acts = acts,
                chapters = chapters,
                currentAct = currentAct,
                currentChapter = currentChapter,
                currentStepIndex = resolvedStepIndex,
                totalStepsInCurrentChapter = totalSteps,
                currentStepBlocks = resolvedStep.blocks,
                currentChapterBlocks = currentChapter.blocks,
                currentBlockId = resolvedBlock.blockId,
                chapterProgressFraction = progressFraction,
                chapterProgressLabelRu = chapterProgressLabel,
                savedPositionSummaryRu = savedSummary,
                isCompleted = progress.isCompleted,
                hasPreviousStep = hasPrevStep,
                hasNextStep = hasNextStep,
                hasPreviousChapter = hasPrevChapter,
                hasNextChapter = hasNextChapter,
                pendingImportPreview = preview,
                statusBannerMessageRu = statusBannerRu,
                errorMessageRu = errorBannerRu
            )
        } catch (e: Exception) {
            StoryReaderUiState(
                hasImportedBook = false,
                errorMessageRu = "Ошибка чтения локального хранилища книги (${e.javaClass.simpleName}). Тренировки работают в штатном режиме."
            )
        }
    }

    /**
     * Шаг 1 локального импорта: проверяет кодировку и структуру выбранного файла и формирует
     * предпросмотр (`MarkdownImportPreview`) перед окончательным подтверждением импорта.
     * При ошибке уже установленная книга и её позиция полностью сохраняются.
     */
    suspend fun prepareImportPreview(
        rawBytes: ByteArray,
        fileName: String,
        headingRule: HeadingLevelRule = HeadingLevelRule()
    ): StoryReaderUiState {
        val cleanName = fileName.substringAfterLast('/').ifBlank { "book.md" }
        val parseResult = MarkdownNovelParser.parseMarkdownNovel(
            rawBytes = rawBytes,
            fileName = cleanName,
            headingRule = headingRule
        )
        return parseResult.fold(
            onSuccess = { parsed ->
                pendingRawBytes = rawBytes.copyOf()
                pendingFileName = cleanName
                pendingHeadingRule = headingRule
                pendingParsedNovel = parsed
                loadReaderState()
            },
            onFailure = { err ->
                // Не сбрасываем установленную книгу при ошибке разбора нового файла
                clearPendingImportCache()
                loadReaderState(
                    errorBannerRu = "Не удалось подготовить файл «$cleanName» к импорту: ${err.message ?: "некорректный формат"}. Текущая книга и прогресс сохранены."
                )
            }
        )
    }

    /**
     * Позволяет пользователю изменить уровни заголовков для актов и глав в окне предпросмотра,
     * если структура распознана неоднозначно.
     */
    suspend fun updatePreviewHeadingLevels(
        actHeadingLevel: Int,
        chapterHeadingLevel: Int
    ): StoryReaderUiState {
        val raw = pendingRawBytes ?: return loadReaderState()
        val name = pendingFileName ?: "book.md"
        val newRule = HeadingLevelRule(
            actHeadingLevel = actHeadingLevel,
            chapterHeadingLevel = chapterHeadingLevel
        )
        val parseResult = MarkdownNovelParser.parseMarkdownNovel(
            rawBytes = raw,
            fileName = name,
            headingRule = newRule
        )
        return parseResult.fold(
            onSuccess = { parsed ->
                pendingHeadingRule = newRule
                pendingParsedNovel = parsed
                loadReaderState()
            },
            onFailure = { err ->
                // Оставляем окно предпросмотра открытым с прежним распознаванием и показываем подсказку
                loadReaderState(
                    errorBannerRu = err.message ?: "На выбранном уровне заголовков главы не найдены."
                )
            }
        )
    }

    suspend fun cancelPendingImport(): StoryReaderUiState {
        clearPendingImportCache()
        return loadReaderState()
    }

    /**
     * Шаг 2 локального импорта: окончательно подтверждает импорт подготовленной книги
     * (или выполняет прямой импорт при вызове `importMarkdownDirect`).
     */
    suspend fun confirmPendingImport(): StoryReaderUiState {
        val raw = pendingRawBytes ?: return loadReaderState(
            errorBannerRu = "Нет ожидающего подтверждения файла для импорта."
        )
        val parsed = pendingParsedNovel ?: return loadReaderState(
            errorBannerRu = "Структура книги не подготовлена."
        )
        return commitParsedNovel(rawBytes = raw, parsed = parsed)
    }

    /**
     * Прямой импорт Markdown-файла (используется в автоматических тестах и для загрузки нейтрального тестового макета).
     * Соблюдает все гарантии целостности: при ошибке сохраняет прежнюю книгу, при совпадении SHA-256 не сбрасывает позицию.
     */
    suspend fun importMarkdownDirect(
        rawBytes: ByteArray,
        fileName: String,
        headingRule: HeadingLevelRule = HeadingLevelRule()
    ): StoryReaderUiState {
        val cleanName = fileName.substringAfterLast('/').ifBlank { "book.md" }
        val parseResult = MarkdownNovelParser.parseMarkdownNovel(
            rawBytes = rawBytes,
            fileName = cleanName,
            headingRule = headingRule
        )
        return parseResult.fold(
            onSuccess = { parsed ->
                commitParsedNovel(rawBytes = rawBytes.copyOf(), parsed = parsed)
            },
            onFailure = { err ->
                clearPendingImportCache()
                loadReaderState(
                    errorBannerRu = "Импорт отклонён («$cleanName»): ${err.message ?: "ошибка структуры"}. Установленная книга и её позиция сохранены."
                )
            }
        )
    }

    suspend fun importNeutralTestSample(): StoryReaderUiState {
        val sampleText = NeutralTestMarkdownFactory.buildThreeActsTwelveChaptersMarkdown()
        return importMarkdownDirect(
            rawBytes = sampleText.toByteArray(Charsets.UTF_8),
            fileName = NeutralTestMarkdownFactory.DEFAULT_TEST_FILE_NAME
        )
    }

    private suspend fun commitParsedNovel(
        rawBytes: ByteArray,
        parsed: ParsedLinearNovel
    ): StoryReaderUiState {
        return try {
            val existing = dao.getActiveNovel()
            val existingProgress = if (existing != null) {
                dao.getNovelReadingProgress(existing.bookId)
            } else {
                null
            }

            // Повторный импорт идентичного файла (по SHA-256 и тем же уровням структуры)
            // НЕ создаёт дубликат и НЕ сбрасывает сохранённую позицию чтения (Update v1.1 п. 6)
            if (existing != null &&
                existing.sha256Hex.equals(parsed.sha256Hex, ignoreCase = true) &&
                existing.actHeadingLevel == parsed.detectedActHeadingLevel &&
                existing.chapterHeadingLevel == parsed.detectedChapterHeadingLevel &&
                existingProgress != null
            ) {
                ensureUnmodifiedBytesOnDisk(rawBytes, parsed.sha256Hex)
                clearPendingImportCache()
                return loadReaderState(
                    statusBannerRu = "Этот файл уже импортирован (SHA-256 совпадает). Дубликат не создан, сохранённая позиция чтения не сброшена."
                )
            }

            val savedFile = ensureUnmodifiedBytesOnDisk(rawBytes, parsed.sha256Hex)
            val now = timeProvider.currentWallTimeMs()

            val novelEntity = ImportedNovelEntity(
                slotKey = ImportedNovelEntity.ACTIVE_SLOT_KEY,
                bookId = parsed.bookId,
                fileName = parsed.fileName,
                explicitTitle = parsed.explicitBookTitle,
                displayTitle = parsed.displayTitle,
                sha256Hex = parsed.sha256Hex,
                byteSize = parsed.byteSize,
                internalFilePath = savedFile.absolutePath,
                actHeadingLevel = parsed.detectedActHeadingLevel,
                chapterHeadingLevel = parsed.detectedChapterHeadingLevel,
                actCount = parsed.acts.size,
                chapterCount = parsed.chapters.size,
                totalBlockCount = parsed.allBlocks.size,
                importedAtEpochMs = now
            )

            val chapterEntities = parsed.chapters.map { ch ->
                NovelChapterEntity(
                    chapterId = ch.chapterId,
                    bookId = parsed.bookId,
                    chapterNumber = ch.chapterNumber,
                    actId = ch.actId,
                    actNumber = ch.actNumber,
                    actTitle = ch.actTitle,
                    title = ch.title,
                    blockCount = ch.blocks.size,
                    stepCount = ch.steps.size
                )
            }

            val blockEntities = parsed.allBlocks.map { blk ->
                NovelBlockEntity(
                    blockId = blk.blockId,
                    bookId = parsed.bookId,
                    chapterId = blk.chapterId,
                    orderInChapter = blk.orderInChapter,
                    globalOrder = blk.globalOrder,
                    stepIndex = blk.stepIndex,
                    stepId = blk.stepId,
                    blockType = blk.type.name,
                    subheadingLevel = blk.subheadingLevel,
                    plainText = blk.plainText,
                    inlineSpansEncoded = NovelInlineSpan.encodeList(blk.inlineSpans)
                )
            }

            val firstChapter = parsed.chapters.first()
            val firstBlock = firstChapter.blocks.first()
            val initialProgress = NovelReadingProgressEntity(
                bookId = parsed.bookId,
                currentActId = firstChapter.actId,
                currentChapterId = firstChapter.chapterId,
                currentBlockId = firstBlock.blockId,
                currentStepIndex = 0,
                readerModeId = ReaderMode.BOOK.id,
                isCompleted = false,
                updatedAtEpochMs = now
            )

            dao.replaceActiveNovelAtomically(
                novel = novelEntity,
                chapters = chapterEntities,
                blocks = blockEntities,
                initialProgress = initialProgress
            )

            clearPendingImportCache()
            val actsLabel = if (parsed.acts.isNotEmpty()) "${parsed.acts.size} акта(ов), " else ""
            loadReaderState(
                statusBannerRu = "Книга «${parsed.displayTitle}» успешно импортирована офлайн ($actsLabel${parsed.chapters.size} глав)."
            )
        } catch (e: Exception) {
            clearPendingImportCache()
            loadReaderState(
                errorBannerRu = "Ошибка сохранения книги во внутреннем хранилище (${e.javaClass.simpleName}). Прежняя книга сохранена."
            )
        }
    }

    /**
     * Записывает исходные байты без каких-либо изменений во внутреннее хранилище приложения
     * и проверяет контрольную сумму SHA-256 записанного файла.
     */
    private fun ensureUnmodifiedBytesOnDisk(rawBytes: ByteArray, expectedSha256: String): File {
        if (!internalStorageDir.exists()) {
            internalStorageDir.mkdirs()
        }
        val tempFile = File(internalStorageDir, "active_novel_source.tmp")
        val targetFile = File(internalStorageDir, ACTIVE_SOURCE_FILE_NAME)

        tempFile.writeBytes(rawBytes)
        val writtenBytes = tempFile.readBytes()
        val actualSha256 = MarkdownNovelParser.computeSha256Hex(writtenBytes)
        if (!actualSha256.equals(expectedSha256, ignoreCase = true) || !writtenBytes.contentEquals(rawBytes)) {
            tempFile.delete()
            throw IllegalStateException("Проверка контрольной суммы SHA-256 при записи файла не пройдена.")
        }

        if (targetFile.exists()) {
            targetFile.delete()
        }
        if (!tempFile.renameTo(targetFile)) {
            targetFile.writeBytes(rawBytes)
            tempFile.delete()
        }
        return targetFile
    }

    /**
     * Возвращает неизменённые исходные байты импортированной книги из внутреннего хранилища.
     */
    suspend fun readStoredOriginalBytes(): ByteArray? {
        val activeNovel = dao.getActiveNovel() ?: return null
        val file = File(activeNovel.internalFilePath)
        if (!file.exists()) return null
        return file.readBytes()
    }

    /**
     * Переход к следующему шагу («Пошаговый» режим) или завершение книги на последнем фрагменте последней главы.
     * По требованию Update v1.1 п. 7: завершение чтения помечает `isCompleted = true`,
     * но НЕ сбрасывает сохранённое место автоматически.
     */
    suspend fun nextStep(): StoryReaderUiState {
        val state = loadReaderState()
        if (!state.hasImportedBook) return state
        val currentChapter = state.currentChapter ?: return state
        val chapters = state.chapters
        val chIndex = chapters.indexOfFirst { it.chapterId == currentChapter.chapterId }
        if (chIndex < 0) return state

        val now = timeProvider.currentWallTimeMs()
        val existingProgress = dao.getNovelReadingProgress(state.bookId)
        val modeId = existingProgress?.readerModeId ?: ReaderMode.STEP_BY_STEP.id

        return if (state.currentStepIndex + 1 < currentChapter.steps.size) {
            val targetStep = currentChapter.steps[state.currentStepIndex + 1]
            val isLastChapterAndLastStep =
                (chIndex == chapters.lastIndex && targetStep.stepIndex == currentChapter.steps.lastIndex)
            dao.upsertNovelReadingProgress(
                NovelReadingProgressEntity(
                    bookId = state.bookId,
                    currentActId = currentChapter.actId,
                    currentChapterId = currentChapter.chapterId,
                    currentBlockId = targetStep.blocks.first().blockId,
                    currentStepIndex = targetStep.stepIndex,
                    readerModeId = modeId,
                    isCompleted = (existingProgress?.isCompleted == true) || isLastChapterAndLastStep,
                    updatedAtEpochMs = now
                )
            )
            loadReaderState()
        } else if (chIndex + 1 < chapters.size) {
            val nextChapter = chapters[chIndex + 1]
            val firstStep = nextChapter.steps.first()
            val isLastChapterAndSingleStep =
                (chIndex + 1 == chapters.lastIndex && nextChapter.steps.size == 1)
            dao.upsertNovelReadingProgress(
                NovelReadingProgressEntity(
                    bookId = state.bookId,
                    currentActId = nextChapter.actId,
                    currentChapterId = nextChapter.chapterId,
                    currentBlockId = firstStep.blocks.first().blockId,
                    currentStepIndex = 0,
                    readerModeId = modeId,
                    isCompleted = (existingProgress?.isCompleted == true) || isLastChapterAndSingleStep,
                    updatedAtEpochMs = now
                )
            )
            loadReaderState()
        } else {
            // Мы уже на последнем фрагменте последней главы -> отмечаем «История прочитана», не сбрасывая позицию
            val lastStep = currentChapter.steps.last()
            dao.upsertNovelReadingProgress(
                NovelReadingProgressEntity(
                    bookId = state.bookId,
                    currentActId = currentChapter.actId,
                    currentChapterId = currentChapter.chapterId,
                    currentBlockId = lastStep.blocks.first().blockId,
                    currentStepIndex = lastStep.stepIndex,
                    readerModeId = modeId,
                    isCompleted = true,
                    updatedAtEpochMs = now
                )
            )
            loadReaderState()
        }
    }

    /**
     * Переход к предыдущему шагу («Пошаговый» режим).
     */
    suspend fun previousStep(): StoryReaderUiState {
        val state = loadReaderState()
        if (!state.hasImportedBook) return state
        val currentChapter = state.currentChapter ?: return state
        val chapters = state.chapters
        val chIndex = chapters.indexOfFirst { it.chapterId == currentChapter.chapterId }
        if (chIndex < 0) return state

        val now = timeProvider.currentWallTimeMs()
        val existingProgress = dao.getNovelReadingProgress(state.bookId)
        val modeId = existingProgress?.readerModeId ?: ReaderMode.STEP_BY_STEP.id

        if (state.currentStepIndex > 0) {
            val prevStep = currentChapter.steps[state.currentStepIndex - 1]
            dao.upsertNovelReadingProgress(
                NovelReadingProgressEntity(
                    bookId = state.bookId,
                    currentActId = currentChapter.actId,
                    currentChapterId = currentChapter.chapterId,
                    currentBlockId = prevStep.blocks.first().blockId,
                    currentStepIndex = prevStep.stepIndex,
                    readerModeId = modeId,
                    isCompleted = existingProgress?.isCompleted ?: false,
                    updatedAtEpochMs = now
                )
            )
        } else if (chIndex > 0) {
            val prevChapter = chapters[chIndex - 1]
            val lastStep = prevChapter.steps.last()
            dao.upsertNovelReadingProgress(
                NovelReadingProgressEntity(
                    bookId = state.bookId,
                    currentActId = prevChapter.actId,
                    currentChapterId = prevChapter.chapterId,
                    currentBlockId = lastStep.blocks.first().blockId,
                    currentStepIndex = lastStep.stepIndex,
                    readerModeId = modeId,
                    isCompleted = existingProgress?.isCompleted ?: false,
                    updatedAtEpochMs = now
                )
            )
        }
        return loadReaderState()
    }

    /**
     * Переход к следующей главе (в режиме «Книга» или из навигации).
     */
    suspend fun nextChapter(): StoryReaderUiState {
        val state = loadReaderState()
        if (!state.hasImportedBook) return state
        val currentChapter = state.currentChapter ?: return state
        val chapters = state.chapters
        val chIndex = chapters.indexOfFirst { it.chapterId == currentChapter.chapterId }
        if (chIndex < 0) return state

        return if (chIndex + 1 < chapters.size) {
            selectChapter(chapters[chIndex + 1].chapterId)
        } else {
            // Конец последней главы в режиме «Книга» -> отмечаем «История прочитана», сохраняя текущую главу и блок
            val existingProgress = dao.getNovelReadingProgress(state.bookId)
            if (existingProgress != null) {
                dao.upsertNovelReadingProgress(
                    existingProgress.copy(
                        isCompleted = true,
                        updatedAtEpochMs = timeProvider.currentWallTimeMs()
                    )
                )
            }
            loadReaderState()
        }
    }

    /**
     * Переход к предыдущей главе.
     */
    suspend fun previousChapter(): StoryReaderUiState {
        val state = loadReaderState()
        if (!state.hasImportedBook) return state
        val currentChapter = state.currentChapter ?: return state
        val chapters = state.chapters
        val chIndex = chapters.indexOfFirst { it.chapterId == currentChapter.chapterId }
        if (chIndex <= 0) return state
        return selectChapter(chapters[chIndex - 1].chapterId)
    }

    /**
     * Выбор главы из навигации «Акт → Глава».
     */
    suspend fun selectChapter(chapterId: String): StoryReaderUiState {
        val state = loadReaderState()
        if (!state.hasImportedBook) return state
        val targetChapter = state.chapters.firstOrNull { it.chapterId == chapterId } ?: return state
        val firstBlock = targetChapter.blocks.firstOrNull() ?: return state
        val existingProgress = dao.getNovelReadingProgress(state.bookId)

        dao.upsertNovelReadingProgress(
            NovelReadingProgressEntity(
                bookId = state.bookId,
                currentActId = targetChapter.actId,
                currentChapterId = targetChapter.chapterId,
                currentBlockId = firstBlock.blockId,
                currentStepIndex = 0,
                readerModeId = existingProgress?.readerModeId ?: ReaderMode.BOOK.id,
                isCompleted = existingProgress?.isCompleted ?: false,
                updatedAtEpochMs = timeProvider.currentWallTimeMs()
            )
        )
        return loadReaderState()
    }

    /**
     * Сохраняет точный выбранный/просматриваемый блок в режиме «Книга»,
     * чтобы переключение между режимами «Книга» и «Пошаговый» возвращало к тому же самому фрагменту.
     */
    suspend fun saveBlockPosition(blockId: String): StoryReaderUiState {
        val state = loadReaderState()
        if (!state.hasImportedBook) return state
        val currentChapter = state.currentChapter ?: return state
        val block = currentChapter.blocks.firstOrNull { it.blockId == blockId } ?: return state
        val existingProgress = dao.getNovelReadingProgress(state.bookId)

        dao.upsertNovelReadingProgress(
            NovelReadingProgressEntity(
                bookId = state.bookId,
                currentActId = currentChapter.actId,
                currentChapterId = currentChapter.chapterId,
                currentBlockId = block.blockId,
                currentStepIndex = block.stepIndex,
                readerModeId = existingProgress?.readerModeId ?: ReaderMode.BOOK.id,
                isCompleted = existingProgress?.isCompleted ?: false,
                updatedAtEpochMs = timeProvider.currentWallTimeMs()
            )
        )
        return loadReaderState()
    }

    /**
     * Явное действие «Перечитать с начала» (переводит в Главу 1, фрагмент 1 по запросу пользователя).
     */
    suspend fun restartFromBeginning(): StoryReaderUiState {
        val state = loadReaderState()
        if (!state.hasImportedBook) return state
        val firstChapter = state.chapters.firstOrNull() ?: return state
        val firstBlock = firstChapter.blocks.firstOrNull() ?: return state
        val existingProgress = dao.getNovelReadingProgress(state.bookId)

        dao.upsertNovelReadingProgress(
            NovelReadingProgressEntity(
                bookId = state.bookId,
                currentActId = firstChapter.actId,
                currentChapterId = firstChapter.chapterId,
                currentBlockId = firstBlock.blockId,
                currentStepIndex = 0,
                readerModeId = existingProgress?.readerModeId ?: ReaderMode.BOOK.id,
                isCompleted = false,
                updatedAtEpochMs = timeProvider.currentWallTimeMs()
            )
        )
        return loadReaderState(
            statusBannerRu = "Чтение переведено к началу первой главы."
        )
    }

    /**
     * Полное удаление импортированной книги из внутреннего хранилища и Room (по явному подтверждению в Настройках → Данные).
     */
    suspend fun removeImportedBook(): StoryReaderUiState {
        return try {
            val existing = dao.getActiveNovel()
            if (existing != null) {
                runCatching { File(existing.internalFilePath).delete() }
            }
            dao.clearImportedNovelAtomically()
            clearPendingImportCache()
            loadReaderState(
                statusBannerRu = "Импортированная книга удалена с устройства."
            )
        } catch (_: Exception) {
            loadReaderState()
        }
    }

    private fun buildPendingPreviewModel(activeNovel: ImportedNovelEntity?): MarkdownImportPreview? {
        val parsed = pendingParsedNovel ?: return null
        val isIdentical = activeNovel != null &&
            activeNovel.sha256Hex.equals(parsed.sha256Hex, ignoreCase = true) &&
            activeNovel.actHeadingLevel == parsed.detectedActHeadingLevel &&
            activeNovel.chapterHeadingLevel == parsed.detectedChapterHeadingLevel

        val isReplacing = activeNovel != null && !isIdentical

        return MarkdownImportPreview(
            fileName = parsed.fileName,
            explicitTitle = parsed.explicitBookTitle,
            displayTitle = parsed.displayTitle,
            actCount = parsed.acts.size,
            chapterCount = parsed.chapters.size,
            totalBlockCount = parsed.allBlocks.size,
            sha256Hex = parsed.sha256Hex,
            byteSize = parsed.byteSize,
            isIdenticalToCurrentBook = isIdentical,
            isReplacingExistingBook = isReplacing,
            isStructureAmbiguous = parsed.isStructureAmbiguous,
            ambiguityReasonRu = parsed.ambiguityReasonRu,
            selectedActHeadingLevel = pendingHeadingRule.actHeadingLevel,
            selectedChapterHeadingLevel = pendingHeadingRule.chapterHeadingLevel,
            detectedActHeadingLevel = parsed.detectedActHeadingLevel,
            detectedChapterHeadingLevel = parsed.detectedChapterHeadingLevel,
            headingLevelsSummaryRu = parsed.headingLevelsSummaryRu
        )
    }

    private fun clearPendingImportCache() {
        pendingRawBytes = null
        pendingFileName = null
        pendingHeadingRule = HeadingLevelRule()
        pendingParsedNovel = null
    }

    companion object {
        const val ACTIVE_SOURCE_FILE_NAME = "active_novel_source.md"
    }
}
