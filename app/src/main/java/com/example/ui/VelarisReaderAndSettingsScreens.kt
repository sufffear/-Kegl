package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import com.example.data.BackupPreviewInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AudioDiagnosticEntry
import com.example.data.AudioHardwareStatus
import com.example.data.BenchSignalSettings
import com.example.data.StoryReaderUiState
import com.example.domain.AppBuildInfo
import com.example.domain.BenchEventLogEntry
import com.example.domain.DemoProtocolConfig
import com.example.domain.HeadingLevelRule
import com.example.domain.MarkdownImportPreview
import com.example.domain.MetronomeTickStyle
import com.example.domain.NovelBlock
import com.example.domain.NovelBlockType
import com.example.domain.NovelInlineStyle
import com.example.domain.PhaseEngineSnapshot
import com.example.domain.ReaderFontSize
import com.example.domain.ReaderLineSpacing
import com.example.domain.ReaderMode
import com.example.domain.SignalType
import com.example.domain.SoundPack
import com.example.ui.theme.LocalVelarisPalette
import com.example.ui.theme.VelarisPalette
import com.example.ui.theme.VelarisThemeId
import com.example.ui.theme.paletteForTheme

private val MARKDOWN_DOCUMENT_MIME_TYPES = arrayOf(
    "text/markdown",
    "text/x-markdown",
    "text/plain",
    "application/octet-stream",
    "*/*"
)

private val BACKUP_DOCUMENT_MIME_TYPES = arrayOf(
    "application/zip",
    "application/octet-stream",
    "*/*"
)

/**
 * Экран 4: Универсальный офлайн-читатель линейного романа из Markdown (Update v1.1 п. 5–7).
 * Поддерживает:
 * - локальный импорт `.md` и `.markdown` через системный выбор документа Android без широких разрешений хранилища;
 * - предпросмотр перед импортом (имя файла, найденное название, число актов и глав, выбор уровней заголовков, подтверждение замены);
 * - режимы «Книга» (последовательная прокрутка главы, переход между главами) и «Пошаговый» (несколько абзацев, «Назад» / «Далее»);
 * - навигацию «Акт → Глава» и индикатор прогресса по текущей главе;
 * - сохранение точного места по стабильным ID главы и блока при переключении режимов и перезапуске;
 * - состояние «История прочитана» в конце книги с возможностью перечитать без автоматического сброса места.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VelarisReaderScreen(
    storyState: StoryReaderUiState,
    settings: BenchSignalSettings,
    snapshot: PhaseEngineSnapshot,
    onImportMarkdownUri: (Uri) -> Unit,
    onUpdatePreviewHeadingLevels: (Int, Int) -> Unit,
    onConfirmPendingImport: () -> Unit,
    onCancelPendingImport: () -> Unit,
    onReaderModeChange: (ReaderMode) -> Unit,
    onReaderFontSizeChange: (ReaderFontSize) -> Unit,
    onReaderLineSpacingChange: (ReaderLineSpacing) -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onNextChapter: () -> Unit,
    onPreviousChapter: () -> Unit,
    onSelectChapter: (String) -> Unit,
    onSaveBlockPosition: (String) -> Unit,
    onRestartFromBeginning: () -> Unit,
    onReturnToWorkout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVelarisPalette.current
    val fontSizeSp = settings.readerFontSize.bodySp.sp
    val lineHeightSp = (settings.readerFontSize.bodySp * settings.readerLineSpacing.multiplier).sp

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportMarkdownUri(uri)
        }
    }

    var selectedActIdForNav by remember(storyState.currentAct?.actId, storyState.acts.size) {
        mutableStateOf(storyState.currentAct?.actId ?: storyState.acts.firstOrNull()?.actId.orEmpty())
    }
    var showTocExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("story_reader_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // Баннер статуса или ошибки импорта (прежняя книга при ошибке сохраняется)
        if (storyState.errorMessageRu != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.errorAccent, RoundedCornerShape(16.dp))
                    .testTag("reader_error_banner"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = palette.surfaceVariant)
            ) {
                Text(
                    text = storyState.errorMessageRu,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.errorAccent,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        if (storyState.statusBannerMessageRu != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.secondaryAccent.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .testTag("reader_status_banner"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = palette.surfaceVariant)
            ) {
                Text(
                    text = storyState.statusBannerMessageRu,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textPrimary,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        // Предпросмотр перед окончательным импортом (Update v1.1 п. 6)
        val preview = storyState.pendingImportPreview
        if (preview != null) {
            MarkdownImportPreviewCard(
                preview = preview,
                onUpdateHeadingLevels = onUpdatePreviewHeadingLevels,
                onConfirmImport = onConfirmPendingImport,
                onCancelImport = onCancelPendingImport
            )
        }

        if (!storyState.hasImportedBook) {
            // Состояние, когда книга ещё не добавлена
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reader_empty_import_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = palette.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = palette.primaryAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Чтение книги",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Text(
                        text = "Выберите файл книги на телефоне (.md или .markdown). Текст книги сохранится для спокойного чтения без интернета в режимах «Книга» и «Пошаговый».",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )

                    Button(
                        onClick = { openDocumentLauncher.launch(MARKDOWN_DOCUMENT_MIME_TYPES) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primaryAccent,
                            contentColor = palette.onPrimaryAccent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("btn_open_book_from_file")
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Добавить книгу", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Верхняя карточка книги, прогресса, навигации Акт → Глава и настроек отображения
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = palette.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = storyState.displayTitleRu,
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("reader_book_title")
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = storyState.chapterProgressLabelRu,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.secondaryAccent,
                                modifier = Modifier.testTag("reader_chapter_progress_label")
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedButton(
                            onClick = { openDocumentLauncher.launch(MARKDOWN_DOCUMENT_MIME_TYPES) },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("btn_open_book_from_file")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileOpen,
                                contentDescription = null,
                                tint = palette.primaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Добавить книгу",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textPrimary
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { storyState.chapterProgressFraction.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .testTag("reader_chapter_progress_bar"),
                        color = palette.primaryAccent,
                        trackColor = palette.surfaceVariant
                    )

                    // Навигация «Акт → Глава»
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val structureInfo = if (storyState.actCount > 0) {
                            "Оглавление: ${storyState.actCount} акта · ${storyState.chapterCount} глав"
                        } else {
                            "Оглавление: ${storyState.chapterCount} глав"
                        }
                        Text(
                            text = structureInfo,
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textSecondary
                        )
                        FilledTonalButton(
                            onClick = { showTocExpanded = !showTocExpanded },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = palette.surfaceVariant,
                                contentColor = palette.primaryAccent
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("btn_toggle_toc")
                        ) {
                            Text(
                                text = if (showTocExpanded) "Скрыть главы" else "Акт → Глава",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (showTocExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(palette.backgroundTop.copy(alpha = 0.55f))
                                .padding(12.dp)
                                .testTag("reader_toc_panel"),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (storyState.acts.isNotEmpty()) {
                                Text(
                                    text = "Выберите акт:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    storyState.acts.forEach { act ->
                                        val isActSelected = selectedActIdForNav == act.actId
                                        FilledTonalButton(
                                            onClick = { selectedActIdForNav = act.actId },
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = if (isActSelected) palette.primaryAccent else palette.surfaceVariant,
                                                contentColor = if (isActSelected) palette.onPrimaryAccent else palette.textPrimary
                                            ),
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("reader_act_${act.actId}")
                                        ) {
                                            Text(
                                                text = act.title,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isActSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }

                            val chaptersToShow = if (storyState.acts.isNotEmpty()) {
                                storyState.acts.firstOrNull { it.actId == selectedActIdForNav }?.chapters
                                    ?: storyState.chapters
                            } else {
                                storyState.chapters
                            }

                            Text(
                                text = "Главы:",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                chaptersToShow.forEach { ch ->
                                    val isCurrentChapter = storyState.currentChapter?.chapterId == ch.chapterId
                                    OutlinedButton(
                                        onClick = { onSelectChapter(ch.chapterId) },
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("reader_chapter_${ch.chapterId}")
                                    ) {
                                        Text(
                                            text = ch.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isCurrentChapter) palette.primaryAccent else palette.textPrimary,
                                            fontWeight = if (isCurrentChapter) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Переключатель режима чтения: «Книга» / «Пошаговый»
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReaderMode.entries.forEach { mode ->
                            val selected = settings.readerMode == mode
                            OutlinedButton(
                                onClick = { onReaderModeChange(mode) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("reader_mode_${mode.id}")
                            ) {
                                Text(
                                    text = "Режим: ${mode.titleRu}",
                                    color = if (selected) palette.primaryAccent else palette.textSecondary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Настройки размера шрифта и межстрочного интервала
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ReaderFontSize.entries.forEach { sizeOption ->
                            val selected = settings.readerFontSize == sizeOption
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) palette.primaryAccent.copy(alpha = 0.22f) else palette.surfaceVariant,
                                modifier = Modifier
                                    .clickable { onReaderFontSizeChange(sizeOption) }
                                    .testTag("reader_font_${sizeOption.id}")
                            ) {
                                Text(
                                    text = "Шрифт: ${sizeOption.titleRu}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) palette.primaryAccent else palette.textSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }
                        ReaderLineSpacing.entries.forEach { spacingOption ->
                            val selected = settings.readerLineSpacing == spacingOption
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) palette.secondaryAccent.copy(alpha = 0.22f) else palette.surfaceVariant,
                                modifier = Modifier
                                    .clickable { onReaderLineSpacingChange(spacingOption) }
                                    .testTag("reader_spacing_${spacingOption.id}")
                            ) {
                                Text(
                                    text = "Интервал: ${spacingOption.titleRu}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) palette.secondaryAccent else palette.textSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Поверхность чтения (ровный тёмный лист без пестроты под текстом)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.outline.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
                    .testTag("reader_text_surface"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = palette.readerSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val currentChapter = storyState.currentChapter
                    if (currentChapter != null) {
                        if (currentChapter.actTitle.isNotBlank()) {
                            Text(
                                text = currentChapter.actTitle,
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.secondaryAccent,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag("reader_current_act_title")
                            )
                        }

                        Text(
                            text = currentChapter.title,
                            fontSize = settings.readerFontSize.titleSp.sp,
                            color = palette.primaryAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("reader_current_chapter_title")
                        )

                        if (settings.readerMode == ReaderMode.BOOK) {
                            // Режим «Книга»: последовательная прокрутка всех блоков текущей главы
                            storyState.currentChapterBlocks.forEach { block ->
                                val isCurrentSavedBlock = block.blockId == storyState.currentBlockId
                                NovelBlockItem(
                                    block = block,
                                    fontSizeSp = fontSizeSp,
                                    lineHeightSp = lineHeightSp,
                                    palette = palette,
                                    isHighlightedBookmark = isCurrentSavedBlock,
                                    onTapBlock = { onSaveBlockPosition(block.blockId) }
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Кнопки перехода к предыдущей и следующей главе
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onPreviousChapter,
                                    enabled = storyState.hasPreviousChapter,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 52.dp)
                                        .testTag("btn_reader_prev_chapter")
                                ) {
                                    Text(
                                        text = "Предыдущая глава",
                                        color = if (storyState.hasPreviousChapter) palette.textPrimary else palette.textSecondary
                                    )
                                }

                                Button(
                                    onClick = onNextChapter,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.primaryAccent,
                                        contentColor = palette.onPrimaryAccent
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 52.dp)
                                        .testTag("btn_reader_next_chapter")
                                ) {
                                    Text(
                                        text = if (storyState.hasNextChapter) "Следующая глава" else "Завершить чтение",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            // Режим «Пошаговый»: несколько исходных абзацев на фрагмент, кнопки «Назад» и «Далее»
                            storyState.currentStepBlocks.forEach { block ->
                                NovelBlockItem(
                                    block = block,
                                    fontSizeSp = fontSizeSp,
                                    lineHeightSp = lineHeightSp,
                                    palette = palette,
                                    isHighlightedBookmark = false,
                                    onTapBlock = null
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onPreviousStep,
                                    enabled = storyState.hasPreviousStep,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 52.dp)
                                        .testTag("btn_reader_prev_step")
                                ) {
                                    Text(
                                        text = "Назад",
                                        color = if (storyState.hasPreviousStep) palette.textPrimary else palette.textSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Button(
                                    onClick = onNextStep,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.primaryAccent,
                                        contentColor = palette.onPrimaryAccent
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 52.dp)
                                        .testTag("btn_reader_next")
                                ) {
                                    Text(
                                        text = if (storyState.hasNextStep) "Далее" else "Завершить чтение",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Конец книги: «История прочитана», возможность перечитать без автоматического сброса позиции
                        if (storyState.isCompleted) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = palette.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reader_end_of_content_banner")
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = palette.successAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Книга прочитана",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = palette.successAccent,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "Вы дошли до конца книги. Текущая позиция сохранена; вы можете выбрать любую главу в оглавлении или перечитать книгу с начала.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary
                                    )
                                    OutlinedButton(
                                        onClick = onRestartFromBeginning,
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("btn_reader_restart")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = palette.primaryAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Перечитать с начала",
                                            color = palette.textPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Карточка предварительной проверки файла перед окончательным импортом (Update v1.1 п. 6):
 * показывает имя файла, найденное название (если явно задано), количество актов и глав,
 * выбор уровней заголовков при неоднозначности и подтверждение замены текущей книги.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MarkdownImportPreviewCard(
    preview: MarkdownImportPreview,
    onUpdateHeadingLevels: (Int, Int) -> Unit,
    onConfirmImport: () -> Unit,
    onCancelImport: () -> Unit
) {
    val palette = LocalVelarisPalette.current
    var showHeadingSelector by remember(preview.isStructureAmbiguous) {
        mutableStateOf(preview.isStructureAmbiguous)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, palette.primaryAccent, RoundedCornerShape(20.dp))
            .testTag("markdown_import_preview_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Проверка книги перед импортом",
                style = MaterialTheme.typography.titleMedium,
                color = palette.primaryAccent,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Имя файла: ${preview.fileName}",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("import_preview_file_name")
            )

            Text(
                text = if (preview.explicitTitle != null) {
                    "Найденное название: ${preview.explicitTitle}"
                } else {
                    "Найденное название: явно не задано (будет использовано имя файла)"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textPrimary,
                modifier = Modifier.testTag("import_preview_title")
            )

            Text(
                text = "Распознанная структура: актов — ${preview.actCount}, глав — ${preview.chapterCount} (текстовых блоков: ${preview.totalBlockCount})",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.secondaryAccent,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("import_preview_counts")
            )

            Text(
                text = "Уровни заголовков в файле: ${preview.headingLevelsSummaryRu}",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary
            )

            if (preview.isStructureAmbiguous && preview.ambiguityReasonRu != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.warningAccent.copy(alpha = 0.16f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = preview.ambiguityReasonRu,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.warningAccent,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (preview.isIdenticalToCurrentBook) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.successAccent.copy(alpha = 0.16f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Эта книга уже открыта в приложении. Повторное добавление не создаст дубликат и сохранит текущее место чтения.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.successAccent,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else if (preview.isReplacingExistingBook) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.warningAccent.copy(alpha = 0.18f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_replace_warning_banner")
                ) {
                    Text(
                        text = "Внимание: в приложении уже установлена другая книга. Подтверждение заменит текущую книгу новым файлом.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.warningAccent,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            OutlinedButton(
                onClick = { showHeadingSelector = !showHeadingSelector },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("btn_toggle_heading_levels")
            ) {
                Text(
                    text = if (showHeadingSelector) {
                        "Скрыть выбор уровней заголовков"
                    } else {
                        "Указать уровни заголовков для актов и глав"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textPrimary
                )
            }

            if (showHeadingSelector) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.surfaceVariant)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Уровень заголовков для Актов:",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textPrimary
                    )
                    val actOptions = listOf(
                        HeadingLevelRule.AUTO to "Авто",
                        HeadingLevelRule.NO_ACTS to "Без актов",
                        1 to "H1 (#)",
                        2 to "H2 (##)",
                        3 to "H3 (###)"
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        actOptions.forEach { (lvl, label) ->
                            val selected = preview.selectedActHeadingLevel == lvl
                            FilledTonalButton(
                                onClick = {
                                    onUpdateHeadingLevels(lvl, preview.selectedChapterHeadingLevel)
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (selected) palette.primaryAccent else palette.surface,
                                    contentColor = if (selected) palette.onPrimaryAccent else palette.textPrimary
                                ),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Text(
                        text = "Уровень заголовков для Глав:",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textPrimary
                    )
                    val chapterOptions = listOf(
                        HeadingLevelRule.AUTO to "Авто",
                        1 to "H1 (#)",
                        2 to "H2 (##)",
                        3 to "H3 (###)",
                        4 to "H4 (####)"
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        chapterOptions.forEach { (lvl, label) ->
                            val selected = preview.selectedChapterHeadingLevel == lvl
                            FilledTonalButton(
                                onClick = {
                                    onUpdateHeadingLevels(preview.selectedActHeadingLevel, lvl)
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (selected) palette.primaryAccent else palette.surface,
                                    contentColor = if (selected) palette.onPrimaryAccent else palette.textPrimary
                                ),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelImport,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("btn_cancel_markdown_import")
                ) {
                    Text("Отмена", color = palette.textPrimary)
                }

                Button(
                    onClick = onConfirmImport,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primaryAccent,
                        contentColor = palette.onPrimaryAccent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("btn_confirm_markdown_import")
                ) {
                    Text(
                        text = if (preview.isReplacingExistingBook) {
                            "Заменить книгу"
                        } else {
                            "Импортировать"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun NovelBlockItem(
    block: NovelBlock,
    fontSizeSp: androidx.compose.ui.unit.TextUnit,
    lineHeightSp: androidx.compose.ui.unit.TextUnit,
    palette: VelarisPalette,
    isHighlightedBookmark: Boolean,
    onTapBlock: (() -> Unit)?
) {
    val annotated = remember(block.plainText, block.inlineSpans) {
        buildAnnotatedText(block)
    }

    val clickModifier = if (onTapBlock != null) {
        Modifier.clickable { onTapBlock() }
    } else {
        Modifier
    }

    when (block.type) {
        NovelBlockType.DIVIDER -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .testTag("reader_block_${block.blockId}"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(
                    modifier = Modifier.width(120.dp),
                    color = palette.outline.copy(alpha = 0.55f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "* * *",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        NovelBlockType.SUBHEADING -> {
            Text(
                text = annotated,
                fontSize = (fontSizeSp.value + 2f).sp,
                lineHeight = lineHeightSp,
                color = palette.secondaryAccent,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(clickModifier)
                    .testTag("reader_block_${block.blockId}")
            )
        }

        NovelBlockType.BLOCKQUOTE -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.surfaceVariant.copy(alpha = 0.45f))
                    .then(clickModifier)
                    .padding(12.dp)
                    .testTag("reader_block_${block.blockId}")
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(24.dp)
                        .background(palette.secondaryAccent)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = annotated,
                    fontSize = fontSizeSp,
                    lineHeight = lineHeightSp,
                    fontStyle = FontStyle.Italic,
                    color = palette.textPrimary
                )
            }
        }

        NovelBlockType.SAFE_RAW -> {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = palette.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(clickModifier)
                    .testTag("reader_block_${block.blockId}")
            ) {
                Text(
                    text = block.plainText,
                    fontSize = (fontSizeSp.value - 1f).sp,
                    lineHeight = lineHeightSp,
                    fontFamily = FontFamily.Monospace,
                    color = palette.textPrimary,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        NovelBlockType.PARAGRAPH -> {
            val borderMod = if (isHighlightedBookmark) {
                Modifier
                    .border(
                        width = 1.dp,
                        color = palette.primaryAccent.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            } else {
                Modifier
            }
            Text(
                text = annotated,
                fontSize = fontSizeSp,
                lineHeight = lineHeightSp,
                color = palette.textPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(borderMod)
                    .then(clickModifier)
                    .testTag("reader_block_${block.blockId}")
            )
        }
    }
}

private fun buildAnnotatedText(block: NovelBlock): AnnotatedString {
    if (block.inlineSpans.isEmpty()) {
        return AnnotatedString(block.plainText)
    }
    return buildAnnotatedString {
        append(block.plainText)
        val len = block.plainText.length
        for (span in block.inlineSpans) {
            val s = span.start.coerceIn(0, len)
            val e = span.end.coerceIn(0, len)
            if (e <= s) continue
            val spanStyle = when (span.style) {
                NovelInlineStyle.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                NovelInlineStyle.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
                NovelInlineStyle.BOLD_ITALIC -> SpanStyle(
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic
                )
                NovelInlineStyle.INLINE_CODE -> SpanStyle(fontFamily = FontFamily.Monospace)
            }
            addStyle(spanStyle, s, e)
        }
    }
}

/**
 * Экран 5: Настройки (Update v1.1 п. 3).
 * Группы:
 * 1. Темы («Рассвет», «Небо», «Северное сияние», «Закат», «Сакура»);
 * 2. Существующие параметры плана (с явным обозначением демонстрационного режима; реальная нагрузка не назначается самостоятельно);
 * 3. Звук и вибрация (3 набора, 2 варианта метронома, раздельные уровни, образцы);
 * 4. Чтение (режим, шрифт, межстрочный интервал);
 * 5. Данные (локальный импорт романа из Markdown, проверка SHA-256, удаление книги с подтверждением);
 * 6. Диагностика (переход во вложенный экран «Настройки → Диагностика»).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VelarisSettingsScreen(
    settings: BenchSignalSettings,
    snapshot: PhaseEngineSnapshot,
    storyState: StoryReaderUiState,
    availableProtocols: List<DemoProtocolConfig>,
    onSelectProtocol: (DemoProtocolConfig) -> Unit,
    onThemeChange: (VelarisThemeId) -> Unit,
    onSoundPackChange: (SoundPack) -> Unit,
    onMetronomeTickStyleChange: (MetronomeTickStyle) -> Unit,
    onSoundEnabledChange: (Boolean) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onMetronomeEnabledChange: (Boolean) -> Unit,
    onDuckOtherAppsChange: (Boolean) -> Unit = {},
    onSignalVolumeChange: (Float) -> Unit,
    onMetronomeVolumeChange: (Float) -> Unit,
    onPlaySample: (SignalType) -> Unit,
    onReaderModeChange: (ReaderMode) -> Unit,
    onReaderFontSizeChange: (ReaderFontSize) -> Unit,
    onReaderLineSpacingChange: (ReaderLineSpacing) -> Unit,
    onImportMarkdownUri: (Uri) -> Unit,
    onRemoveImportedBook: () -> Unit,
    pendingRestorePreview: BackupPreviewInfo? = null,
    backupOperationBanner: String? = null,
    backupErrorMessage: String? = null,
    onCreateBackupUri: (Uri) -> Unit = {},
    onInspectBackupUri: (Uri) -> Unit = {},
    onConfirmRestore: () -> Unit = {},
    onCancelRestore: () -> Unit = {},
    onDismissBackupBanners: () -> Unit = {},
    onOpenDiagnostics: () -> Unit,
    onReturnToWorkout: () -> Unit,
    settingsCategory: String? = null,
    onOpenSettingsCategory: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalVelarisPalette.current
    var showRemoveBookDialog by remember { mutableStateOf(false) }
    var showBackupNoticeDialog by remember { mutableStateOf(false) }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportMarkdownUri(uri)
        }
    }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            onCreateBackupUri(uri)
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onInspectBackupUri(uri)
        }
    }

    // Предупреждение перед экспортом (v1.2 п. 1): «Копия содержит историю занятий и текст вашей книги. Сохраните её в надёжном месте»
    if (showBackupNoticeDialog) {
        AlertDialog(
            onDismissRequest = { showBackupNoticeDialog = false },
            containerColor = palette.surface,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = {
                Text("Резервная копия", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Копия содержит историю ваших занятий, шаблоны и настройки. Сохраните её в надёжном месте.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBackupNoticeDialog = false
                        val dateSuffix = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                        createBackupLauncher.launch("velaris_backup_$dateSuffix.zip")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primaryAccent,
                        contentColor = palette.onPrimaryAccent
                    ),
                    modifier = Modifier.testTag("btn_confirm_export_dialog")
                ) {
                    Text("Выбрать куда сохранить", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showBackupNoticeDialog = false },
                    modifier = Modifier.testTag("btn_cancel_export_dialog")
                ) {
                    Text("Отмена", color = palette.textPrimary)
                }
            }
        )
    }

    // Диалог подтверждения восстановления из копии (v1.2 п. 2)
    if (pendingRestorePreview != null) {
        val preview = pendingRestorePreview
        AlertDialog(
            onDismissRequest = onCancelRestore,
            containerColor = palette.surface,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = {
                Text("Восстановление из копии", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Дата создания: ${preview.createdAtRu}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Тренировок в копии: ${preview.workoutCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.primaryAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = palette.errorAccent.copy(alpha = 0.16f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Внимание: текущие данные в приложении будут полностью заменены данными из резервной копии. Действие необратимо.",
                            color = palette.errorAccent,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirmRestore,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.errorAccent,
                        contentColor = palette.onPrimaryAccent
                    ),
                    modifier = Modifier.testTag("btn_confirm_restore")
                ) {
                    Text("Восстановить данные", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onCancelRestore,
                    modifier = Modifier.testTag("btn_cancel_restore")
                ) {
                    Text("Отмена", color = palette.textPrimary)
                }
            },
            modifier = Modifier.testTag("restore_confirm_dialog")
        )
    }

    if (showRemoveBookDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveBookDialog = false },
            containerColor = palette.surface,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = {
                Text("Удалить импортированную книгу?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Копия книги во внутреннем хранилище и сохранённая позиция чтения будут удалены. Тренировочные записи не изменятся.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRemoveBookDialog = false
                        onRemoveImportedBook()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.errorAccent,
                        contentColor = palette.onPrimaryAccent
                    )
                ) {
                    Text("Удалить книгу")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRemoveBookDialog = false }) {
                    Text("Отмена", color = palette.textPrimary)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (settingsCategory == null) {
            VelarisSettingsCategories(onOpenSettingsCategory)
            if (backupOperationBanner != null || backupErrorMessage != null) {
                Text(backupErrorMessage ?: backupOperationBanner.orEmpty(), color = palette.textPrimary)
                OutlinedButton(onClick = { onOpenSettingsCategory("backup") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Открыть резервные копии")
                }
            }
            return@Column
        }
        if (settingsCategory == "appearance") {
            // Оформление: все существующие темы доступны сразу.
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.outline.copy(alpha = 0.32f), RoundedCornerShape(18.dp))
                    .testTag("theme_selector_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = palette.surface)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("theme_selector_header"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(palette.primaryAccent.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = palette.primaryAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Оформление",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${settings.themeId.titleRu} · Нажмите для выбора",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            val currentPreview = paletteForTheme(settings.themeId)
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(currentPreview.backgroundMid)
                                    .border(1.dp, currentPreview.outline, CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(currentPreview.primaryAccent)
                            )

                        }
                    }


                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HorizontalDivider(color = palette.outline.copy(alpha = 0.25f))
                            VelarisThemeId.entries.forEach { themeOption ->
                                val isSelected = settings.themeId == themeOption
                                val previewPalette = paletteForTheme(themeOption)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isSelected) palette.selectedContainer else palette.surfaceElevated
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) palette.selectedBorder else palette.outline.copy(alpha = 0.30f),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable { onThemeChange(themeOption) }
                                        .padding(horizontal = 10.dp, vertical = 9.dp)
                                        .testTag("theme_option_${themeOption.id}"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { onThemeChange(themeOption) },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = palette.primaryAccent,
                                                unselectedColor = palette.textSecondary
                                            ),
                                            modifier = Modifier.size(34.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = themeOption.titleRu,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = palette.textPrimary,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = themeOption.subtitleRu,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.textSecondary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(previewPalette.backgroundMid)
                                                .border(1.dp, previewPalette.outline, CircleShape)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(previewPalette.primaryAccent)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(previewPalette.secondaryAccent)
                                        )
                                    }
                                }
                            }
                        }

                }
            }

        }
        if (settingsCategory == "sound") {
            // Звук, профили и вибрация.
            val soundSummaryRu = buildString {
                append(if (settings.soundEnabled) settings.soundPack.titleRu else "Без звука")
                append(" · ${(settings.signalVolume * 100).toInt()}%")
                append(if (settings.vibrationEnabled) " · Вибрация ВКЛ" else " · Вибрация ВЫКЛ")
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.outline.copy(alpha = 0.32f), RoundedCornerShape(18.dp))
                    .testTag("sound_settings_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = palette.surface)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsSectionHeader(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        title = "Звук, профили и вибрация",
                        subtitle = soundSummaryRu,
                        testTag = "sound_settings_header"
                    )


                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HorizontalDivider(color = palette.outline.copy(alpha = 0.25f))

                            // Базовые переключатели
                            SettingsSwitchRow(
                                title = "Звуковые сигналы фаз",
                                checked = settings.soundEnabled,
                                onCheckedChange = onSoundEnabledChange,
                                testTag = "switch_sound_enabled"
                            )
                            SettingsSwitchRow(
                                title = "Вибрация при смене фаз",
                                checked = settings.vibrationEnabled,
                                onCheckedChange = onVibrationEnabledChange,
                                testTag = "switch_vibration_enabled"
                            )
                            SettingsSwitchRow(
                                title = "Секундный метроном внутри фазы",
                                checked = settings.metronomeEnabled,
                                onCheckedChange = onMetronomeEnabledChange,
                                testTag = "switch_metronome_enabled"
                            )

                            // Громкость команд с кнопкой мгновенной проверки
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(palette.surfaceElevated)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Громкость команд: ${(settings.signalVolume * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    FilledTonalButton(
                                        onClick = { onPlaySample(SignalType.CONTRACTION) },
                                        enabled = !snapshot.isRunningOrPreparing,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = palette.selectedContainer,
                                            contentColor = palette.primaryAccent
                                        ),
                                        modifier = Modifier.heightIn(min = 32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Тест",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Slider(
                                    value = settings.signalVolume,
                                    onValueChange = onSignalVolumeChange,
                                    valueRange = 0f..1f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = palette.primaryAccent,
                                        activeTrackColor = palette.primaryAccent
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .semantics { contentDescription = "Громкость команд фаз" }
                                        .testTag("slider_signal_volume")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Громкость метронома",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.textSecondary
                                    )
                                    Text(
                                        text = "${(settings.metronomeVolume * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.secondaryAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Slider(
                                    value = settings.metronomeVolume,
                                    onValueChange = onMetronomeVolumeChange,
                                    valueRange = 0f..1f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = palette.secondaryAccent,
                                        activeTrackColor = palette.secondaryAccent
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .semantics { contentDescription = "Громкость метронома" }
                                        .testTag("slider_metronome_volume")
                                )
                            }

                            // Выбор звукового профиля (мягкие приятные + звонкие) с мгновенным сравнением «Сжать» и «Отдых»
                            Text(
                                text = "Звуковой профиль («Сжать» и «Отдых» кардинально отличаются):",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textSecondary,
                                fontWeight = FontWeight.SemiBold
                            )

                            SoundPack.entries.forEach { pack ->
                                val isSelected = settings.soundPack == pack
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isSelected) palette.selectedContainer else palette.surfaceElevated
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) palette.selectedBorder else palette.outline.copy(alpha = 0.28f),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            onSoundPackChange(pack)
                                            if (!snapshot.isRunningOrPreparing) {
                                                onPlaySample(SignalType.CONTRACTION)
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 9.dp)
                                        .testTag("sound_pack_${pack.id}"),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                onSoundPackChange(pack)
                                                if (!snapshot.isRunningOrPreparing) {
                                                    onPlaySample(SignalType.CONTRACTION)
                                                }
                                            },
                                            colors = RadioButtonDefaults.colors(selectedColor = palette.primaryAccent),
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = pack.titleRu,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = palette.textPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (pack.isSoftProfile) {
                                                        palette.successAccent.copy(alpha = 0.16f)
                                                    } else {
                                                        palette.primaryAccent.copy(alpha = 0.14f)
                                                    }
                                                ) {
                                                    Text(
                                                        text = if (pack.isSoftProfile) "Мягкий" else "Звонкий",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (pack.isSoftProfile) palette.successAccent else palette.primaryAccent,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = pack.descriptionRu,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.textSecondary
                                            )
                                        }
                                    }

                                    if (pack.contractionSummaryRu.isNotEmpty() && pack.relaxationSummaryRu.isNotEmpty()) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(palette.surface.copy(alpha = 0.72f))
                                                .padding(horizontal = 9.dp, vertical = 6.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = pack.contractionSummaryRu,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.phaseHoldAccent,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = pack.relaxationSummaryRu,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.phaseRelaxAccent,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilledTonalButton(
                                            onClick = {
                                                onSoundPackChange(pack)
                                                if (!snapshot.isRunningOrPreparing) {
                                                    onPlaySample(SignalType.CONTRACTION)
                                                }
                                            },
                                            enabled = !snapshot.isRunningOrPreparing,
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = palette.phaseHoldAccent.copy(alpha = 0.14f),
                                                contentColor = palette.phaseHoldAccent
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 34.dp)
                                                .testTag("sound_pack_preview_hold_${pack.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "▲ Тест: Сжать",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }

                                        FilledTonalButton(
                                            onClick = {
                                                onSoundPackChange(pack)
                                                if (!snapshot.isRunningOrPreparing) {
                                                    onPlaySample(SignalType.RELAXATION)
                                                }
                                            },
                                            enabled = !snapshot.isRunningOrPreparing,
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = palette.phaseRelaxAccent.copy(alpha = 0.14f),
                                                contentColor = palette.phaseRelaxAccent
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 34.dp)
                                                .testTag("sound_pack_preview_relax_${pack.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "▼ Тест: Отдых",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            // Вариант тика метронома
                            Text(
                                text = "Звук тика метронома:",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textSecondary,
                                fontWeight = FontWeight.SemiBold
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetronomeTickStyle.entries.forEach { style ->
                                    val isSelected = settings.metronomeTickStyle == style
                                    FilledTonalButton(
                                        onClick = {
                                            onMetronomeTickStyleChange(style)
                                            if (!snapshot.isRunningOrPreparing) {
                                                onPlaySample(SignalType.METRONOME_TICK)
                                            }
                                        },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = if (isSelected) palette.primaryAccent else palette.surfaceElevated,
                                            contentColor = if (isSelected) palette.onPrimaryAccent else palette.textPrimary
                                        ),
                                        modifier = Modifier
                                            .heightIn(min = 40.dp)
                                            .testTag("metronome_style_${style.id}")
                                    ) {
                                        Text(
                                            text = style.titleRu,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            // Переключатель приглушения других приложений (YouTube / музыка)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(palette.surfaceElevated)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Приглушать YouTube / музыку",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (settings.duckOtherApps) {
                                            "Фоновое медиа кратковременно приглушается во время сигнала"
                                        } else {
                                            "Звуки звучат поверх YouTube без пауз и приглушения"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = settings.duckOtherApps,
                                    onCheckedChange = onDuckOtherAppsChange,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = palette.onPrimaryAccent,
                                        checkedTrackColor = palette.primaryAccent,
                                        uncheckedThumbColor = palette.textSecondary,
                                        uncheckedTrackColor = palette.surfaceVariant
                                    ),
                                    modifier = Modifier.testTag("switch_duck_other_apps")
                                )
                            }
                        }

                }
            }

        }
        if (settingsCategory == "samples") {
            // Образцы команд.
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.outline.copy(alpha = 0.32f), RoundedCornerShape(18.dp))
                    .testTag("sound_samples_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = palette.surface)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsSectionHeader(
                        icon = Icons.Default.MusicNote,
                        title = "Прослушать образцы команд",
                        subtitle = "Тест сигналов удержания, отдыха, метронома и финала",
                        testTag = "sound_samples_header"
                    )


                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HorizontalDivider(color = palette.outline.copy(alpha = 0.25f))
                            Text(
                                text = "Нажмите на любую команду, чтобы проверить звонкость текущего профиля (${settings.soundPack.titleRu}):",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    SignalType.CONTRACTION,
                                    SignalType.RELAXATION,
                                    SignalType.METRONOME_TICK,
                                    SignalType.SET_COMPLETED,
                                    SignalType.GROUP_COMPLETED
                                ).forEach { signal ->
                                    FilledTonalButton(
                                        onClick = { onPlaySample(signal) },
                                        enabled = !snapshot.isRunningOrPreparing,
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = palette.surfaceElevated,
                                            contentColor = palette.textPrimary
                                        ),
                                        modifier = Modifier
                                            .heightIn(min = 42.dp)
                                            .testTag("btn_sample_${signal.name.lowercase(Locale.ROOT)}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = palette.primaryAccent,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = signal.labelRu,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                }
            }
        }

        if (settingsCategory == "backup") {
            // Резервные копии и данные.
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.outline.copy(alpha = 0.32f), RoundedCornerShape(18.dp))
                    .testTag("data_settings_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = palette.surface)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsSectionHeader(
                        icon = Icons.Default.SettingsBackupRestore,
                        title = "Резервные копии и данные",
                        subtitle = "Экспорт истории, шаблонов и восстановление из ZIP",
                        testTag = "data_settings_header"
                    )


                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HorizontalDivider(color = palette.outline.copy(alpha = 0.25f))

                            if (backupOperationBanner != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = palette.successAccent.copy(alpha = 0.14f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("backup_success_banner")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = palette.successAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = backupOperationBanner,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.successAccent,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = onDismissBackupBanners,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Закрыть",
                                                tint = palette.textSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (backupErrorMessage != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = palette.errorAccent.copy(alpha = 0.14f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("backup_error_banner")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = palette.errorAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = backupErrorMessage,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.errorAccent,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = onDismissBackupBanners,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Закрыть",
                                                tint = palette.textSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (snapshot.isActiveOrPaused) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = palette.warningAccent.copy(alpha = 0.14f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("backup_active_workout_warning")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = palette.warningAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Завершите или остановите текущую тренировку перед резервным копированием или восстановлением.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.warningAccent,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Копия сохраняет историю занятий, шаблоны, авторские программы и настройки в один ZIP-файл.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showBackupNoticeDialog = true },
                                    enabled = !snapshot.isActiveOrPaused,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.primaryAccent,
                                        contentColor = palette.onPrimaryAccent
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp)
                                        .testTag("btn_create_backup")
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Сохранить копию",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                OutlinedButton(
                                    onClick = { restoreBackupLauncher.launch(BACKUP_DOCUMENT_MIME_TYPES) },
                                    enabled = !snapshot.isActiveOrPaused,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp)
                                        .testTag("btn_restore_backup")
                                ) {
                                    Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Восстановить",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (!snapshot.isActiveOrPaused) palette.textPrimary else palette.textSecondary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                }
            }

        }
        if (settingsCategory == "about") {
            // 5. Карточка «О приложении и Диагностика»
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.outline.copy(alpha = 0.32f), RoundedCornerShape(18.dp))
                    .testTag("about_app_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = palette.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "О приложении и система",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Сведения о версии и проверка работы приложения",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedButton(
                        onClick = onOpenDiagnostics,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .testTag("btn_open_diagnostics")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = palette.secondaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Диагностика",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(icon: ImageVector, title: String, subtitle: String, testTag: String) {
    val palette = LocalVelarisPalette.current
    Row(Modifier.fillMaxWidth().padding(14.dp).testTag(testTag), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = palette.primaryAccent)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val palette = LocalVelarisPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 42.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.onPrimaryAccent,
                checkedTrackColor = palette.primaryAccent,
                uncheckedThumbColor = palette.textSecondary,
                uncheckedTrackColor = palette.surfaceVariant
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

/**
 * Вложенный экран «Настройки → Диагностика» (Update v1.1 п. 2, п. 3).
 * Сюда вынесены все служебные сведения о сборке, диагностика громкости и аудиомаршрута,
 * загрузка нейтрального тестового макета Markdown (3 акта, 12 глав), проверка N=0 и системный журнал.
 */
@Composable
fun VelarisDiagnosticsScreen(
    buildInfo: AppBuildInfo,
    snapshot: PhaseEngineSnapshot,
    hardwareStatus: AudioHardwareStatus,
    audioDiagnostics: List<AudioDiagnosticEntry>,
    eventLog: List<BenchEventLogEntry>,
    isWakeLockHeld: Boolean,
    isServiceForeground: Boolean,
    isZeroSlotDemoMode: Boolean,
    demoCompletionsCount: Int,
    realCompletionsCount: Int,
    onZeroSlotDemoModeChange: (Boolean) -> Unit,
    onImportNeutralTestMarkdown: () -> Unit,
    onRefreshHardware: () -> Unit,
    onClearAudioDiagnostics: () -> Unit,
    onClearEventLog: () -> Unit,
    onBackToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVelarisPalette.current
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("diagnostics_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Диагностика и техстенд",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Настройки → Диагностика (без личных данных и без текста книги)",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
            OutlinedButton(
                onClick = onBackToSettings,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("btn_back_from_diagnostics")
            ) {
                Text("К настройкам", color = palette.textPrimary)
            }
        }

        // 1. Аудио-телеметрия
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("audio_diagnostics_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = palette.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Диагностика громкости и аудиофокуса",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.primaryAccent,
                        fontWeight = FontWeight.Bold
                    )
                    IconButtonOnSurface(
                        label = "Очистить",
                        onClick = onClearAudioDiagnostics,
                        testTag = "btn_clear_audio_diag"
                    )
                }

                Text(
                    text = "Маршрут: ${hardwareStatus.outputRouteLabelRu} · Режим звонка: ${hardwareStatus.ringerModeLabelRu} · DND: ${if (hardwareStatus.isDndActive) "ВКЛ" else "ВЫКЛ"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
                Text(
                    text = "Тракт: USAGE_MEDIA + CONTENT_TYPE_MUSIC · AUDIOFOCUS_GAIN_TRANSIENT (willPauseWhenDucked=true) · AudioTrack gain=1.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.secondaryAccent
                )

                if (audioDiagnostics.isEmpty()) {
                    Text(
                        text = "Событий пока нет. Нажмите образец в настройках или запустите подход.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.backgroundTop)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        audioDiagnostics.take(12).forEach { entry ->
                            Text(
                                text = entry.formatCompactLine(),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = palette.textPrimary
                            )
                        }
                    }
                }
            }
        }

        // 2. Тестовые инструменты стенда (N=0 и нейтральный макет Markdown 3 акта / 12 глав)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("diagnostics_test_tools_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = palette.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Инструменты проверки граничных состояний",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                SettingsSwitchRow(
                    title = "Тестовый режим N=0 («Сегодня без тренировок»)",
                    checked = isZeroSlotDemoMode,
                    onCheckedChange = onZeroSlotDemoModeChange,
                    testTag = "switch_zero_slot_mode"
                )
                OutlinedButton(
                    onClick = onImportNeutralTestMarkdown,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("btn_load_neutral_test_markdown")
                ) {
                    Text(
                        text = "Загрузить нейтральный тестовый Markdown (3 акта, 12 глав)",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textPrimary
                    )
                }
                Text(
                    text = "Счётчики Room: демо-завершений = $demoCompletionsCount, реальных завершений = $realCompletionsCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    modifier = Modifier.testTag("persistence_isolation_summary")
                )
            }
        }

        // 3. Сведения о среде, службе и сборке
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("diagnostics_build_info_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = palette.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Сведения о сборке и фоновой службе",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    IconButtonOnSurface(
                        label = "Обновить",
                        onClick = onRefreshHardware,
                        testTag = "btn_refresh_hardware"
                    )
                }
                Text(
                    text = "Application ID: ${buildInfo.applicationId} · Версия: ${buildInfo.versionName} (code ${buildInfo.versionCode})",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
                Text(
                    text = "Устройство: ${buildInfo.deviceManufacturer} ${buildInfo.deviceModel} · Android ${buildInfo.androidRelease} (API ${buildInfo.androidSdkInt})",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
                Text(
                    text = "Foreground Service: ${if (isServiceForeground) "АКТИВЕН" else "ОСТАНОВЛЕН"} · WakeLock: ${if (isWakeLockHeld) "УДЕРЖИВАЕТСЯ" else "СВОБОДЕН"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondaryAccent
                )
                Text(
                    text = "Задержка тика: ${snapshot.lastSchedulerDriftMs} мс (макс. ${snapshot.maxSchedulerDriftMs} мс)",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
        }

        // 4. Журнал событий (с копированием в буфер обмена)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("event_log_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = palette.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Журнал событий (${eventLog.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButtonOnSurface(
                            label = "Копировать",
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val text = eventLog.joinToString("\n") {
                                    "[+${it.elapsedMs}ms][${it.category}] ${it.message}"
                                }
                                clipboard?.setPrimaryClip(ClipData.newPlainText("Velaris Log", text))
                            },
                            testTag = "btn_copy_log"
                        )
                        IconButtonOnSurface(
                            label = "Очистить",
                            onClick = onClearEventLog,
                            testTag = "btn_clear_log"
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.backgroundTop)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (eventLog.isEmpty()) {
                        Text(
                            text = "Журнал пуст.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    } else {
                        eventLog.take(20).forEach { item ->
                            Text(
                                text = "[+${item.elapsedMs}ms][${item.category}] ${item.message}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = palette.textPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconButtonOnSurface(
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    val palette = LocalVelarisPalette.current
    FilledTonalButton(
        onClick = onClick,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = palette.surfaceVariant,
            contentColor = palette.textPrimary
        ),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = when (label) {
                "Копировать" -> Icons.Default.ContentCopy
                "Обновить" -> Icons.Default.Refresh
                else -> Icons.Default.DeleteOutline
            },
            contentDescription = label,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}
