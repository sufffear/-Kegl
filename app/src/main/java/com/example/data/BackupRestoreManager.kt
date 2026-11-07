package com.example.data

import android.content.Context
import com.example.domain.MarkdownNovelParser
import com.example.domain.MetronomeTickStyle
import com.example.domain.ReaderFontSize
import com.example.domain.ReaderLineSpacing
import com.example.domain.ReaderMode
import com.example.domain.SoundPack
import com.example.domain.TimeProvider
import com.example.ui.theme.VelarisThemeId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Менеджер резервного копирования и атомарного восстановления Velaris (Архитектура v1.2 п. 1–3).
 *
 * Принципы:
 * - 100% локальное исполнение без сети, серверов и сторонних API;
 * - экспорт только реальных тренировок (`isDemo = false`), настроек и неизменного оригинала романа;
 * - полное исключение демонстрационных записей, временных файлов и журналов диагностики;
 * - строгая валидация манифеста, контрольных сумм SHA-256 и структуры архива;
 * - защита от Zip Slip (path traversal) и атак распаковки (zip bomb);
 * - транзакция Room и компенсирующий откат настроек, Шанталь и книги при ошибках записи;
 *   аварийное завершение процесса между разными хранилищами требует отдельного журнала восстановления;
 * - запрет выполнения резервного копирования и восстановления во время активного подхода.
 */
class BackupRestoreManager(
    private val context: Context,
    private val dao: VelarisDao,
    private val preferencesRepository: PreferencesRepository,
    private val storyRepository: StoryRepository,
    private val timeProvider: TimeProvider
) {

    private val chantalRepository = ChantalProgressRepository(context)
    private val targetBooksDir = File(context.applicationContext.filesDir, "imported_books")
    private val stagingDir = File(context.applicationContext.cacheDir, "restore_staging")

    /**
     * Создаёт резервную копию и записывает её в переданный [outputStream].
     * Не включает демонстрационные данные (`isDemo = true`), технические журналы и временные файлы.
     */
    suspend fun createBackup(
        outputStream: OutputStream,
        appVersion: String = APP_VERSION,
        appVersionCode: Int = APP_VERSION_CODE
    ): Result<BackupManifest> = serialized {
        runCatching {
            val nowEpochMs = timeProvider.currentWallTimeMs()
            val dateRu = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(nowEpochMs))

            // 1. Получаем реальные данные из Room
            val plans = dao.getRealTrainingPlans()
            val assignments = dao.getRealPlanAssignments()
            val dailyPlans = dao.getRealDailyPlans()
            val slots = dao.getRealDailySlots()
            val runs = dao.getRealWorkoutRuns()
            val completions = dao.getRealWorkoutCompletions()

            val activeNovel = dao.getActiveNovel()
            val chapters = if (activeNovel != null) dao.getNovelChapters(activeNovel.bookId) else emptyList()
            val blocks = if (activeNovel != null) dao.getNovelBlocksForBook(activeNovel.bookId) else emptyList()
            val progress = if (activeNovel != null) dao.getNovelReadingProgress(activeNovel.bookId) else null
            val freeTemplates = dao.getAllFreeTemplates()

            val novelSourceBytes = if (activeNovel != null) {
                storyRepository.readStoredOriginalBytes()
            } else {
                null
            }

            // 2. Получаем настройки из DataStore
            val currentSettings = preferencesRepository.signalSettingsFlow.first()
            val prefsSnapshot = BackupPreferencesSnapshot(
                soundEnabled = currentSettings.soundEnabled,
                vibrationEnabled = currentSettings.vibrationEnabled,
                metronomeEnabled = currentSettings.metronomeEnabled,
                signalVolume = currentSettings.signalVolume,
                metronomeVolume = currentSettings.metronomeVolume,
                soundPackId = currentSettings.soundPack.id,
                metronomeTickStyleId = currentSettings.metronomeTickStyle.id,
                themeId = currentSettings.themeId.id,
                readerModeId = currentSettings.readerMode.id,
                readerFontSizeId = currentSettings.readerFontSize.id,
                readerLineSpacingId = currentSettings.readerLineSpacing.id,
                isRealProgramEnabled = currentSettings.isRealProgramEnabled,
                realProgramSource = currentSettings.realProgramSource,
                duckOtherApps = currentSettings.duckOtherApps,
                frozenDates = currentSettings.frozenDates
            )

            // 3. Формируем снимок базы данных
            val dbSnapshot = BackupDatabaseSnapshot(
                plans = plans,
                assignments = assignments,
                dailyPlans = dailyPlans,
                slots = slots,
                runs = runs,
                completions = completions,
                novel = activeNovel,
                novelChapters = chapters,
                novelBlocks = blocks,
                novelReadingProgress = progress,
                freeTemplates = freeTemplates
            )

            val dbBytes = dbSnapshot.toJson().toByteArray(Charsets.UTF_8)
            val prefsBytes = prefsSnapshot.toJson().toByteArray(Charsets.UTF_8)

            val chantalBytes = chantalRepository.export().toByteArray(Charsets.UTF_8)

            val filesMap = mutableMapOf<String, String>()
            filesMap[BackupManifest.DATABASE_EXPORT_FILE_NAME] = MarkdownNovelParser.computeSha256Hex(dbBytes)
            filesMap[BackupManifest.PREFERENCES_FILE_NAME] = MarkdownNovelParser.computeSha256Hex(prefsBytes)

            if (novelSourceBytes != null && activeNovel != null) {
                filesMap[BackupManifest.BOOK_SOURCE_FILE_NAME] = MarkdownNovelParser.computeSha256Hex(novelSourceBytes)
            }

            filesMap[BackupManifest.CHANTAL_FILE_NAME] = MarkdownNovelParser.computeSha256Hex(chantalBytes)

            // 4. Формируем манифест
            val manifest = BackupManifest(
                formatVersion = BackupManifest.CURRENT_FORMAT_VERSION,
                appVersion = appVersion,
                appVersionCode = appVersionCode,
                schemaVersion = 5,
                createdAtEpochMs = nowEpochMs,
                createdAtRu = dateRu,
                workoutCount = completions.size,
                hasBook = activeNovel != null,
                bookTitle = activeNovel?.displayTitle,
                bookFileName = activeNovel?.fileName,
                chapterCount = chapters.size,
                actCount = activeNovel?.actCount ?: 0,
                files = filesMap
            )
            val manifestBytes = manifest.toJson().toByteArray(Charsets.UTF_8)

            // 5. Упаковываем в ZIP-архив
            ZipOutputStream(outputStream).use { zip ->
                // manifest.json
                zip.putNextEntry(ZipEntry(BackupManifest.MANIFEST_FILE_NAME))
                zip.write(manifestBytes)
                zip.closeEntry()

                // database_export.json
                zip.putNextEntry(ZipEntry(BackupManifest.DATABASE_EXPORT_FILE_NAME))
                zip.write(dbBytes)
                zip.closeEntry()

                // preferences.json
                zip.putNextEntry(ZipEntry(BackupManifest.PREFERENCES_FILE_NAME))
                zip.write(prefsBytes)
                zip.closeEntry()

                zip.putNextEntry(ZipEntry(BackupManifest.CHANTAL_FILE_NAME))
                zip.write(chantalBytes)
                zip.closeEntry()

                // book_source.md (если есть)
                if (novelSourceBytes != null) {
                    zip.putNextEntry(ZipEntry(BackupManifest.BOOK_SOURCE_FILE_NAME))
                    zip.write(novelSourceBytes)
                    zip.closeEntry()
                }

                zip.finish()
                zip.flush()
            }

            manifest
        }
    }

    /**
     * Быстрая предварительная проверка архива резервной копии перед показом диалога восстановления (v1.2 п. 2).
     * Проверяет формат, читает манифест и возвращает сводку (дата, количество тренировок, название книги).
     */
    suspend fun inspectBackup(
        inputStream: InputStream,
        archiveSizeBytes: Long = 0L
    ): Result<BackupPreviewInfo> = withContext(Dispatchers.IO) {
        runCatching {
            var manifestBytes: ByteArray? = null
            var totalUncompressedSize = 0L

            ZipInputStream(inputStream).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    val entryName = entry.name
                    // Защита от Zip Slip
                    if (entryName.contains("..") || entryName.startsWith("/") || entryName.startsWith("\\")) {
                        throw SecurityException("Обнаружена недопустимая относительная ссылка в архиве: $entryName")
                    }

                    if (entryName == BackupManifest.MANIFEST_FILE_NAME) {
                        manifestBytes = readEntryBytes(zip, MAX_MANIFEST_SIZE_BYTES)
                        totalUncompressedSize += manifestBytes.size
                    } else {
                        // Быстро считываем/пропускаем с проверкой лимита
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (zip.read(buffer).also { read = it } != -1) {
                            totalUncompressedSize += read
                            if (totalUncompressedSize > MAX_TOTAL_UNCOMPRESSED_SIZE_BYTES) {
                                throw IllegalStateException("Размер содержимого архива превышает допустимый лимит безопасности (60 МБ).")
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            val rawManifest = manifestBytes ?: throw IllegalArgumentException("Архив не содержит обязательного файла манифеста (manifest.json).")
            val manifest = BackupManifest.fromJson(String(rawManifest, Charsets.UTF_8))

            if (manifest.formatVersion !in 1..BackupManifest.CURRENT_FORMAT_VERSION) {
                throw IllegalArgumentException("Неподдерживаемая версия формата резервной копии: ${manifest.formatVersion} (ожидается ${BackupManifest.CURRENT_FORMAT_VERSION}).")
            }

            BackupPreviewInfo(
                formatVersion = manifest.formatVersion,
                appVersion = manifest.appVersion,
                createdAtEpochMs = manifest.createdAtEpochMs,
                createdAtRu = manifest.createdAtRu.ifBlank {
                    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(manifest.createdAtEpochMs))
                },
                workoutCount = manifest.workoutCount,
                hasBook = manifest.hasBook,
                bookTitle = manifest.bookTitle,
                bookFileName = manifest.bookFileName,
                chapterCount = manifest.chapterCount,
                archiveSizeBytes = archiveSizeBytes
            )
        }
    }

    /**
     * Восстановление с транзакцией Room и компенсирующим откатом внешних хранилищ.
     *
     * Гарантии:
     * 1. Полная проверка архива: контрольные суммы SHA-256 каждого распакованного файла, схема Room, целостность связей;
     * 2. Защита от Zip Slip (canonicalPath check) и zip bomb (лимит 60 МБ);
     * 3. Если любой этап (распаковка, сверка контрольных сумм, транзакция Room, обновление настроек) прерывается ошибкой:
     *    Room откатывается, для внешних хранилищ выполняется откат. Ошибки отката сохраняются в исключении;
     * 4. После восстановления приложение не запускает таймер самостоятельно.
     */
    suspend fun restoreBackup(
        inputStream: InputStream,
        isWorkoutActiveOrPaused: Boolean
    ): Result<BackupManifest> = serialized {
        if (isWorkoutActiveOrPaused) {
            return@serialized Result.failure(
                IllegalStateException("Нельзя восстанавливать данные во время активной тренировки. Завершите или остановите подход.")
            )
        }

        // Подготавливаем каталог временной распаковки
        clearDirectory(stagingDir)
        if (!stagingDir.exists()) {
            stagingDir.mkdirs()
        }

        // Сохраняем исходное состояние для отката
        var backupNovelFile: File? = null
        val originalTargetFile = File(targetBooksDir, StoryRepository.ACTIVE_SOURCE_FILE_NAME)
        var initialSettings: BenchSignalSettings? = null
        var initialChantal: String? = null
        var externalWriteStarted = false
        var bookWriteStarted = false
        val originalBookExisted = originalTargetFile.exists()

        try {
            // 1. Распаковываем файлы в stagingDir с проверкой Zip Slip
            val extractedFiles = mutableMapOf<String, File>()
            var totalUncompressedBytes = 0L

            ZipInputStream(inputStream).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    val entryName = entry.name
                    if (entryName.contains("..") || entryName.startsWith("/") || entryName.startsWith("\\")) {
                        throw SecurityException("Попытка выхода за пределы каталога при распаковке (Zip Slip): $entryName")
                    }

                    // Разрешены только целевые файлы резервной копии
                    if (entryName !in ALLOWED_BACKUP_ENTRY_NAMES) {
                        throw IllegalArgumentException("Архив содержит неизвестный файл: $entryName")
                    }

                    require(!extractedFiles.containsKey(entryName)) { "Повторяющийся файл в архиве: $entryName" }
                    val destFile = File(stagingDir, entryName)
                    // Каноническая проверка пути
                    if (!destFile.canonicalPath.startsWith(stagingDir.canonicalPath)) {
                        throw SecurityException("Недопустимый путь распаковки файла: $entryName")
                    }

                    FileOutputStream(destFile).use { fos ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (zip.read(buffer).also { read = it } != -1) {
                            totalUncompressedBytes += read
                            if (totalUncompressedBytes > MAX_TOTAL_UNCOMPRESSED_SIZE_BYTES) {
                                throw IllegalStateException("Размер содержимого архива превышает лимит безопасности (60 МБ).")
                            }
                            fos.write(buffer, 0, read)
                        }
                        fos.flush()
                    }

                    extractedFiles[entryName] = destFile
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            // 2. Проверяем наличие и формат manifest.json
            val manifestFile = extractedFiles[BackupManifest.MANIFEST_FILE_NAME]
                ?: throw IllegalArgumentException("Архив не содержит обязательного файла манифеста (manifest.json).")

            val manifest = BackupManifest.fromJson(manifestFile.readText(Charsets.UTF_8))
            if (manifest.formatVersion !in 1..BackupManifest.CURRENT_FORMAT_VERSION) {
                throw IllegalArgumentException("Неподдерживаемая версия формата резервной копии: ${manifest.formatVersion}.")
            }

            val requiredFiles = mutableSetOf(BackupManifest.DATABASE_EXPORT_FILE_NAME, BackupManifest.PREFERENCES_FILE_NAME)
            if (manifest.hasBook) requiredFiles += BackupManifest.BOOK_SOURCE_FILE_NAME
            if (manifest.formatVersion >= 2) requiredFiles += BackupManifest.CHANTAL_FILE_NAME
            require(manifest.files.keys.containsAll(requiredFiles)) { "В манифесте отсутствуют обязательные контрольные суммы" }
            require(extractedFiles.keys - BackupManifest.MANIFEST_FILE_NAME == manifest.files.keys) {
                "Список файлов архива не совпадает с манифестом"
            }

            // 3. Проверяем контрольные суммы SHA-256 для каждого файла из манифеста
            for ((relPath, expectedSha) in manifest.files) {
                val file = extractedFiles[relPath]
                    ?: throw IllegalArgumentException("В архиве отсутствует файл, указанный в манифесте: $relPath")
                val actualSha = MarkdownNovelParser.computeSha256Hex(file.readBytes())
                if (!actualSha.equals(expectedSha, ignoreCase = true)) {
                    throw IllegalStateException("Нарушена целостность файла «$relPath»: контрольная сумма SHA-256 не совпадает.")
                }
            }

            // 4. Считываем и валидируем снимок настроек
            val prefsFile = extractedFiles[BackupManifest.PREFERENCES_FILE_NAME]
                ?: throw IllegalArgumentException("Архив не содержит настроек пользователя (preferences.json).")
            val prefsSnapshot = BackupPreferencesSnapshot.fromJson(prefsFile.readText(Charsets.UTF_8))

            // 5. Считываем и валидируем снимок базы данных
            val dbFile = extractedFiles[BackupManifest.DATABASE_EXPORT_FILE_NAME]
                ?: throw IllegalArgumentException("Архив не содержит данных базы (database_export.json).")
            val dbSnapshot = BackupDatabaseSnapshot.fromJson(dbFile.readText(Charsets.UTF_8))

            val chantalSnapshot = extractedFiles[BackupManifest.CHANTAL_FILE_NAME]?.readText(Charsets.UTF_8)
            chantalSnapshot?.let { ChantalProgressRepository.validate(it) }
            require(manifest.hasBook == (dbSnapshot.novel != null)) { "Метаданные книги не согласованы" }

            // Проверяем структуру романа и исходный файл книги (если книга есть в копии)
            var preparedNovelEntity: ImportedNovelEntity? = null
            var stagedBookFile: File? = null

            if (manifest.hasBook && dbSnapshot.novel != null) {
                val bookFile = extractedFiles[BackupManifest.BOOK_SOURCE_FILE_NAME]
                    ?: throw IllegalArgumentException("В резервной копии заявлена книга, но файл book_source.md отсутствует.")
                val bookBytes = bookFile.readBytes()
                val bookSha = MarkdownNovelParser.computeSha256Hex(bookBytes)
                if (!bookSha.equals(dbSnapshot.novel.sha256Hex, ignoreCase = true)) {
                    throw IllegalStateException("Контрольная сумма текста книги book_source.md не совпадает с метаданными романа.")
                }
                stagedBookFile = bookFile

                // Подготавливаем обновлённый путь к файлу книги для текущего устройства
                val destBookFile = File(targetBooksDir, StoryRepository.ACTIVE_SOURCE_FILE_NAME)
                preparedNovelEntity = dbSnapshot.novel.copy(
                    internalFilePath = destBookFile.absolutePath,
                    byteSize = bookBytes.size
                )
            }

            initialSettings = preferencesRepository.signalSettingsFlow.first()
            initialChantal = context.getSharedPreferences("chantal_progress", Context.MODE_PRIVATE).getString("snapshot", null)

            // 6. Подготавливаем откат для дискового файла книги
            if (originalTargetFile.exists()) {
                val tempBackup = File(context.applicationContext.cacheDir, "original_book_backup_${System.currentTimeMillis()}.tmp")
                originalTargetFile.copyTo(tempBackup, overwrite = true)
                backupNovelFile = tempBackup
            }

            // 7. Выполняем атомарное обновление Room-базы
            dao.restoreUserDataAtomically(
                plans = dbSnapshot.plans,
                assignments = dbSnapshot.assignments,
                dailyPlans = dbSnapshot.dailyPlans,
                slots = dbSnapshot.slots,
                runs = dbSnapshot.runs,
                completions = dbSnapshot.completions,
                novel = preparedNovelEntity,
                chapters = dbSnapshot.novelChapters,
                blocks = dbSnapshot.novelBlocks,
                progress = dbSnapshot.novelReadingProgress,
                freeTemplates = dbSnapshot.freeTemplates,
                beforeCommit = {
                    externalWriteStarted = true

                    // 8. Обновляем настройки в DataStore
                    preferencesRepository.updateSettings(
                        BenchSignalSettings(
                            soundEnabled = prefsSnapshot.soundEnabled,
                            vibrationEnabled = prefsSnapshot.vibrationEnabled,
                            metronomeEnabled = prefsSnapshot.metronomeEnabled,
                            signalVolume = prefsSnapshot.signalVolume,
                            metronomeVolume = prefsSnapshot.metronomeVolume,
                            soundPack = SoundPack.fromId(prefsSnapshot.soundPackId),
                            metronomeTickStyle = MetronomeTickStyle.fromId(prefsSnapshot.metronomeTickStyleId),
                            themeId = VelarisThemeId.fromId(prefsSnapshot.themeId),
                            readerMode = ReaderMode.fromId(prefsSnapshot.readerModeId),
                            readerFontSize = ReaderFontSize.fromId(prefsSnapshot.readerFontSizeId),
                            readerLineSpacing = ReaderLineSpacing.fromId(prefsSnapshot.readerLineSpacingId),
                            isRealProgramEnabled = prefsSnapshot.isRealProgramEnabled,
                            realProgramSource = prefsSnapshot.realProgramSource,
                            duckOtherApps = prefsSnapshot.duckOtherApps,
                            frozenDates = prefsSnapshot.frozenDates
                        )
                    )

                    // Old archives never contained Chantal: preserve existing companion progress.
                    if (chantalSnapshot != null) chantalRepository.replace(chantalSnapshot)

                    // 9. Replace the book while the Room transaction is still open.
                    bookWriteStarted = true
                    check(targetBooksDir.isDirectory || targetBooksDir.mkdirs()) { "Не удалось открыть каталог книги" }
                    if (stagedBookFile != null) {
                        stagedBookFile.copyTo(originalTargetFile, overwrite = true)
                    } else {
                        if (originalTargetFile.exists()) {
                            check(originalTargetFile.delete()) { "Не удалось заменить файл книги" }
                        }
                    }

                }
            )

            // Успешно: удаляем резервную копию отката и каталог staging
            runCatching { backupNovelFile?.delete(); clearDirectory(stagingDir) }

            Result.success(manifest)
        } catch (e: Exception) {
            // Room rolls back automatically. Complete external rollback even on cancellation.
            withContext(NonCancellable) {
                suspend fun rollback(action: suspend () -> Unit) {
                    try { action() } catch (failure: Exception) { e.addSuppressed(failure) }
                }
                if (externalWriteStarted) {
                    rollback { preferencesRepository.updateSettings(requireNotNull(initialSettings)) }
                    rollback { chantalRepository.replace(initialChantal) }
                }
                if (bookWriteStarted) rollback {
                    if (originalBookExisted) {
                        requireNotNull(backupNovelFile).copyTo(originalTargetFile, overwrite = true)
                    } else if (originalTargetFile.exists()) {
                        check(originalTargetFile.delete()) { "Не удалось удалить новый файл книги при откате" }
                    }
                }
                if (e.suppressed.isEmpty()) backupNovelFile?.delete()
                clearDirectory(stagingDir)
            }
            if (e is CancellationException) throw e
            Result.failure(if (e.suppressed.isEmpty()) e else IllegalStateException(
                "Восстановление не завершено, откат также встретил ошибку. Сохраните исходный архив и повторите восстановление.", e
            ))
        }
    }

    private suspend fun <T> serialized(block: suspend () -> T): T = withContext(Dispatchers.IO) {
        operationMutex.withLock { block() }
    }

    private fun readEntryBytes(zip: ZipInputStream, maxBytes: Long): ByteArray {
        val bos = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        var totalRead = 0L
        var read: Int
        while (zip.read(buffer).also { read = it } != -1) {
            totalRead += read
            if (totalRead > maxBytes) {
                throw IllegalStateException("Размер записи превышает допустимый предел: $maxBytes байт")
            }
            bos.write(buffer, 0, read)
        }
        return bos.toByteArray()
    }

    private fun clearDirectory(dir: File) {
        if (dir.exists()) {
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory) clearDirectory(file)
                file.delete()
            }
        }
    }

    companion object {
        private val operationMutex = Mutex()
        const val APP_VERSION = "0.5.0-v1.2"
        const val APP_VERSION_CODE = 5

        const val MAX_MANIFEST_SIZE_BYTES = 64 * 1024L // 64 КБ
        const val MAX_TOTAL_UNCOMPRESSED_SIZE_BYTES = 60 * 1024 * 1024L // 60 МБ

        val ALLOWED_BACKUP_ENTRY_NAMES = setOf(
            BackupManifest.MANIFEST_FILE_NAME,
            BackupManifest.DATABASE_EXPORT_FILE_NAME,
            BackupManifest.PREFERENCES_FILE_NAME,
            BackupManifest.BOOK_SOURCE_FILE_NAME,
            BackupManifest.CHANTAL_FILE_NAME
        )
    }
}
