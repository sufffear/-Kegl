package com.example.ui

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.CustomSequenceStep
import com.example.domain.CustomWorkoutProgram
import com.example.domain.PhaseType
import com.example.domain.WorkoutBlock
import com.example.domain.WorkoutBlockType
import com.example.ui.theme.LocalVelarisPalette
import com.example.ui.theme.VelarisPalette
import java.util.UUID

/**
 * Основной экран списка программ продвинутого конструктора тренировок.
 */
@Composable
fun WorkoutConstructorTabContent(
    programs: List<CustomWorkoutProgram>,
    onStartProgram: (CustomWorkoutProgram) -> Unit,
    onSaveProgram: (CustomWorkoutProgram) -> Unit,
    onDeleteProgram: (String) -> Unit,
    onDuplicateProgram: (CustomWorkoutProgram) -> Unit,
    onReorderPrograms: (List<CustomWorkoutProgram>) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalVelarisPalette.current
    var editingProgram by remember { mutableStateOf<CustomWorkoutProgram?>(null) }
    var previewProgram by remember { mutableStateOf<CustomWorkoutProgram?>(null) }
    var programToDelete by remember { mutableStateOf<CustomWorkoutProgram?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Заголовок и кнопка создания
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Конструктор программ",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "4 типа блоков с точным хронометражем",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Button(
                onClick = {
                    editingProgram = CustomWorkoutProgram(
                        id = UUID.randomUUID().toString(),
                        title = "Новая программа",
                        description = "",
                        blocks = listOf(
                            WorkoutBlock(
                                id = UUID.randomUUID().toString(),
                                type = WorkoutBlockType.REGULAR,
                                name = "Обычные сокращения",
                                contractionSec = 5,
                                relaxationSec = 5,
                                repetitions = 8
                            )
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primaryAccent,
                    contentColor = palette.onPrimaryAccent
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .heightIn(min = 38.dp)
                    .testTag("btn_create_custom_program")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Создать", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }

        if (programs.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Нет созданных программ",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Нажмите «Создать», чтобы собрать тренировку из обычных сокращений, коротких импульсов, удержаний и свободных цепочек.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            programs.forEachIndexed { index, prog ->
                CustomProgramCard(
                    program = prog,
                    index = index,
                    totalCount = programs.size,
                    palette = palette,
                    onStart = { onStartProgram(prog) },
                    onEdit = { editingProgram = prog },
                    onDuplicate = { onDuplicateProgram(prog) },
                    onPreview = { previewProgram = prog },
                    onDelete = { programToDelete = prog },
                    onMoveUp = {
                        if (index > 0) {
                            val list = programs.toMutableList()
                            val temp = list[index]
                            list[index] = list[index - 1]
                            list[index - 1] = temp
                            onReorderPrograms(list)
                        }
                    },
                    onMoveDown = {
                        if (index < programs.size - 1) {
                            val list = programs.toMutableList()
                            val temp = list[index]
                            list[index] = list[index + 1]
                            list[index + 1] = temp
                            onReorderPrograms(list)
                        }
                    }
                )
            }
        }
    }

    // Редактор программы
    editingProgram?.let { prog ->
        EditProgramDialog(
            program = prog,
            palette = palette,
            onDismiss = { editingProgram = null },
            onSave = { saved ->
                onSaveProgram(saved)
                editingProgram = null
            }
        )
    }

    // Предпросмотр программы
    previewProgram?.let { prog ->
        PreviewProgramDialog(
            program = prog,
            palette = palette,
            onDismiss = { previewProgram = null },
            onStart = {
                previewProgram = null
                onStartProgram(prog)
            }
        )
    }

    // Подтверждение удаления
    programToDelete?.let { prog ->
        AlertDialog(
            onDismissRequest = { programToDelete = null },
            containerColor = palette.surface,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = { Text("Удалить программу?", fontWeight = FontWeight.Bold) },
            text = { Text("Программа «${prog.title}» будет удалена без возможности восстановления.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProgram(prog.id)
                        programToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.errorAccent,
                        contentColor = palette.onPrimaryAccent
                    )
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { programToDelete = null }) {
                    Text("Отмена", color = palette.textPrimary)
                }
            }
        )
    }
}

/**
 * Карточка программы в списке с визуальной шкалой ритма блоков.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomProgramCard(
    program: CustomWorkoutProgram,
    index: Int,
    totalCount: Int,
    palette: VelarisPalette,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onPreview: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, palette.outline.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            .testTag("program_card_${program.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Верхняя строка: заголовок и кнопки перемещения/дублирования/удаления
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = program.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (program.description.isNotBlank()) {
                        Text(
                            text = program.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (index > 0) {
                        IconButton(onClick = onMoveUp, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Вверх", tint = palette.textSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                    if (index < totalCount - 1) {
                        IconButton(onClick = onMoveDown, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Вниз", tint = palette.textSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(onClick = onDuplicate, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Копировать", tint = palette.secondaryAccent, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = palette.errorAccent, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Бейджи хронометража и блоков
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = palette.primaryAccent.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "⏱ ${program.formattedTotalDuration()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.primaryAccent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = palette.secondaryAccent.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "Блоков: ${program.blocks.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.secondaryAccent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = palette.surfaceElevated
                ) {
                    Text(
                        text = "Циклов: ${program.totalCycles}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Визуальная цветовая шкала ритма блоков (Timeline Rhythm Bar)
            if (program.blocks.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(palette.surfaceElevated),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    program.blocks.forEach { block ->
                        val weightVal = block.totalDurationSec.coerceAtLeast(5).toFloat()
                        Box(
                            modifier = Modifier
                                .weight(weightVal)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    when (block.type) {
                                        WorkoutBlockType.REGULAR -> palette.primaryAccent
                                        WorkoutBlockType.SHORT_PULSE -> palette.secondaryAccent
                                        WorkoutBlockType.LONG_HOLD -> palette.phaseHoldAccent
                                        WorkoutBlockType.CUSTOM_SEQUENCE -> palette.successAccent
                                    }
                                )
                        )
                    }
                }
            }

            // Список блоков программы (краткий список)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surfaceElevated)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                program.blocks.forEachIndexed { bIdx, block ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (block.type) {
                                            WorkoutBlockType.REGULAR -> palette.primaryAccent
                                            WorkoutBlockType.SHORT_PULSE -> palette.secondaryAccent
                                            WorkoutBlockType.LONG_HOLD -> palette.phaseHoldAccent
                                            WorkoutBlockType.CUSTOM_SEQUENCE -> palette.successAccent
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${bIdx + 1}. ${block.name}",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = when (block.type) {
                                WorkoutBlockType.REGULAR,
                                WorkoutBlockType.SHORT_PULSE,
                                WorkoutBlockType.LONG_HOLD -> "${block.contractionSec}с/${block.relaxationSec}с × ${block.repetitions}"

                                WorkoutBlockType.CUSTOM_SEQUENCE -> "${block.customSteps.size} шаг. × ${block.sequenceCycles}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                            maxLines = 1
                        )
                    }
                }
            }

            // Нижние кнопки действий: Обзор, Изменить, Начать
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPreview,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(11.dp),
                    modifier = Modifier.weight(0.95f).heightIn(min = 40.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Обзор", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(11.dp),
                    modifier = Modifier.weight(1.05f).heightIn(min = 40.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Изменить", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }

                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primaryAccent,
                        contentColor = palette.onPrimaryAccent
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(11.dp),
                    modifier = Modifier.weight(1.15f).heightIn(min = 40.dp).testTag("btn_start_program_${program.id}")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Начать", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}

/**
 * Диалог редактирования программы и её блоков.
 */
@Composable
fun EditProgramDialog(
    program: CustomWorkoutProgram,
    palette: VelarisPalette,
    onDismiss: () -> Unit,
    onSave: (CustomWorkoutProgram) -> Unit
) {
    var title by remember { mutableStateOf(program.title) }
    var description by remember { mutableStateOf(program.description) }
    var blocks by remember { mutableStateOf(program.blocks) }
    var editingBlockIndex by remember { mutableStateOf<Int?>(null) }
    var showAddBlockTypeDialog by remember { mutableStateOf(false) }

    val currentTotalSec = remember(blocks) { blocks.sumOf { it.totalDurationSec } }
    val formattedDuration = remember(currentTotalSec) {
        val min = currentTotalSec / 60
        val sec = currentTotalSec % 60
        if (min > 0) "$min мин $sec с" else "$sec с"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textSecondary,
        title = {
            Text(
                text = "Настройка программы",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название программы") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание (необязательно)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                // Сводка по времени
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.surfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Длительность: $formattedDuration",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.primaryAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Блоков: ${blocks.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textSecondary
                        )
                    }
                }

                // Список блоков
                Text(
                    text = "Блоки программы:",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold
                )

                blocks.forEachIndexed { bIdx, b ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.surfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, palette.outline.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${bIdx + 1}. ${b.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${b.type.titleRu} · ${b.totalDurationSec} с",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (bIdx > 0) {
                                    IconButton(
                                        onClick = {
                                            val list = blocks.toMutableList()
                                            val temp = list[bIdx]
                                            list[bIdx] = list[bIdx - 1]
                                            list[bIdx - 1] = temp
                                            blocks = list
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Вверх", tint = palette.textSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }
                                if (bIdx < blocks.size - 1) {
                                    IconButton(
                                        onClick = {
                                            val list = blocks.toMutableList()
                                            val temp = list[bIdx]
                                            list[bIdx] = list[bIdx + 1]
                                            list[bIdx + 1] = temp
                                            blocks = list
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Вниз", tint = palette.textSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }
                                IconButton(onClick = { editingBlockIndex = bIdx }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Изменить", tint = palette.primaryAccent, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = {
                                        blocks = blocks.filterIndexed { i, _ -> i != bIdx }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = palette.errorAccent, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = { showAddBlockTypeDialog = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Добавить блок")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val validTitle = title.ifBlank { "Моя программа" }
                    val finalProg = program.copy(
                        title = validTitle,
                        description = description,
                        blocks = blocks,
                        updatedAtEpochMs = System.currentTimeMillis()
                    )
                    onSave(finalProg)
                },
                enabled = blocks.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primaryAccent,
                    contentColor = palette.onPrimaryAccent
                )
            ) {
                Text("Сохранить", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Отмена", color = palette.textPrimary)
            }
        }
    )

    // Диалог выбора типа добавляемого блока
    if (showAddBlockTypeDialog) {
        AlertDialog(
            onDismissRequest = { showAddBlockTypeDialog = false },
            containerColor = palette.surface,
            title = { Text("Выберите тип блока", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WorkoutBlockType.entries.forEach { bType ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = palette.surfaceElevated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newBlock = WorkoutBlock(
                                        id = UUID.randomUUID().toString(),
                                        type = bType,
                                        name = bType.titleRu,
                                        contractionSec = when (bType) {
                                            WorkoutBlockType.REGULAR -> 5
                                            WorkoutBlockType.SHORT_PULSE -> 2
                                            WorkoutBlockType.LONG_HOLD -> 20
                                            WorkoutBlockType.CUSTOM_SEQUENCE -> 5
                                        },
                                        relaxationSec = when (bType) {
                                            WorkoutBlockType.REGULAR -> 5
                                            WorkoutBlockType.SHORT_PULSE -> 2
                                            WorkoutBlockType.LONG_HOLD -> 15
                                            WorkoutBlockType.CUSTOM_SEQUENCE -> 5
                                        },
                                        repetitions = when (bType) {
                                            WorkoutBlockType.REGULAR -> 8
                                            WorkoutBlockType.SHORT_PULSE -> 12
                                            WorkoutBlockType.LONG_HOLD -> 3
                                            WorkoutBlockType.CUSTOM_SEQUENCE -> 1
                                        },
                                        customSteps = if (bType == WorkoutBlockType.CUSTOM_SEQUENCE) {
                                            listOf(
                                                CustomSequenceStep(phaseType = PhaseType.CONTRACTION, durationSec = 5),
                                                CustomSequenceStep(phaseType = PhaseType.RELAXATION, durationSec = 5),
                                                CustomSequenceStep(phaseType = PhaseType.CONTRACTION, durationSec = 10),
                                                CustomSequenceStep(phaseType = PhaseType.RELAXATION, durationSec = 8)
                                            )
                                        } else emptyList()
                                    )
                                    blocks = blocks + newBlock
                                    showAddBlockTypeDialog = false
                                }
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(bType.titleRu, style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary, fontWeight = FontWeight.Bold)
                                Text(bType.descriptionRu, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(onClick = { showAddBlockTypeDialog = false }) {
                    Text("Отмена", color = palette.textPrimary)
                }
            }
        )
    }

    // Редактирование конкретного блока
    editingBlockIndex?.let { bIdx ->
        if (bIdx in blocks.indices) {
            EditBlockDetailsDialog(
                block = blocks[bIdx],
                palette = palette,
                onDismiss = { editingBlockIndex = null },
                onSave = { updatedBlock ->
                    val list = blocks.toMutableList()
                    list[bIdx] = updatedBlock
                    blocks = list
                    editingBlockIndex = null
                }
            )
        }
    }
}

/**
 * Редактирование параметров одного блока (удержание, отдых, повторы или шаги).
 */
@Composable
fun EditBlockDetailsDialog(
    block: WorkoutBlock,
    palette: VelarisPalette,
    onDismiss: () -> Unit,
    onSave: (WorkoutBlock) -> Unit
) {
    var name by remember { mutableStateOf(block.name) }
    var contractionSecStr by remember { mutableStateOf(block.contractionSec.toString()) }
    var relaxationSecStr by remember { mutableStateOf(block.relaxationSec.toString()) }
    var repetitionsStr by remember { mutableStateOf(block.repetitions.toString()) }
    var sequenceCyclesStr by remember { mutableStateOf(block.sequenceCycles.toString()) }
    var customSteps by remember { mutableStateOf(block.customSteps) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textSecondary,
        title = { Text("Настройка: ${block.type.titleRu}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название блока") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (block.type != WorkoutBlockType.CUSTOM_SEQUENCE) {
                    OutlinedTextField(
                        value = contractionSecStr,
                        onValueChange = { contractionSecStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Удержание (секунд, 1..120)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = relaxationSecStr,
                        onValueChange = { relaxationSecStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Расслабление / отдых (секунд, 1..120)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = repetitionsStr,
                        onValueChange = { repetitionsStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Количество повторений (1..100)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = sequenceCyclesStr,
                        onValueChange = { sequenceCyclesStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Количество циклов всей цепочки (1..50)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Шаги последовательности:",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    customSteps.forEachIndexed { sIdx, step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.surfaceElevated)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${sIdx + 1}. ${step.phaseType.labelRu} (${step.durationSec} с)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (step.phaseType == PhaseType.CONTRACTION) palette.phaseHoldAccent else palette.phaseRelaxAccent,
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(
                                onClick = {
                                    customSteps = customSteps.filterIndexed { i, _ -> i != sIdx }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = palette.errorAccent, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                customSteps = customSteps + CustomSequenceStep(phaseType = PhaseType.CONTRACTION, durationSec = 5)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+ Удержание (5с)", style = MaterialTheme.typography.labelSmall)
                        }

                        FilledTonalButton(
                            onClick = {
                                customSteps = customSteps + CustomSequenceStep(phaseType = PhaseType.RELAXATION, durationSec = 5)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+ Отдых (5с)", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cSec = contractionSecStr.toIntOrNull()?.coerceIn(1, 120) ?: 5
                    val rSec = relaxationSecStr.toIntOrNull()?.coerceIn(1, 120) ?: 5
                    val reps = repetitionsStr.toIntOrNull()?.coerceIn(1, 100) ?: 10
                    val cycles = sequenceCyclesStr.toIntOrNull()?.coerceIn(1, 50) ?: 1

                    val updated = block.copy(
                        name = name.ifBlank { block.type.titleRu },
                        contractionSec = cSec,
                        relaxationSec = rSec,
                        repetitions = reps,
                        sequenceCycles = cycles,
                        customSteps = customSteps
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primaryAccent,
                    contentColor = palette.onPrimaryAccent
                )
            ) {
                Text("Применить", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Отмена", color = palette.textPrimary)
            }
        }
    )
}

/**
 * Диалог предпросмотра всей программы перед стартом.
 */
@Composable
fun PreviewProgramDialog(
    program: CustomWorkoutProgram,
    palette: VelarisPalette,
    onDismiss: () -> Unit,
    onStart: () -> Unit
) {
    val allSteps = remember(program) { program.expandAllSteps() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textSecondary,
        title = {
            Column {
                Text(program.title, fontWeight = FontWeight.Bold)
                Text(
                    "Хронометраж: ${program.formattedTotalDuration()} · ${allSteps.size} фаз",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.primaryAccent
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("План выполнения по блокам:", style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary, fontWeight = FontWeight.Bold)

                program.blocks.forEachIndexed { bIdx, b ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = palette.surfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Блок ${bIdx + 1}: ${b.name} (${b.type.titleRu})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Длительность блока: ${b.totalDurationSec} сек",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.secondaryAccent
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primaryAccent,
                    contentColor = palette.onPrimaryAccent
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Начать тренировку", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Закрыть", color = palette.textPrimary)
            }
        }
    )
}
