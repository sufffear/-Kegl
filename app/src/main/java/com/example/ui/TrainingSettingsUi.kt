package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TrainingConfiguration
import com.example.data.ExtraTrainingCycle
import com.example.domain.CustomWorkoutProgram
import com.example.ui.theme.ReferencePlum as P
import com.example.ui.theme.ReferencePlumTheme
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun TrainingSettingsDetails(title: String,configuration: TrainingConfiguration,defaults: TrainingConfiguration,
    locked: Boolean,completed: Boolean,onSave: (TrainingConfiguration,(Boolean)->Unit)->Unit,
    onReset: (String,(Boolean)->Unit)->Unit,onStart: ()->Unit) = ReferencePlumTheme {
    var hold by rememberSaveable(configuration.key) { mutableStateOf(configuration.contractionSec.toString()) }
    var rest by rememberSaveable(configuration.key) { mutableStateOf(configuration.relaxationSec.toString()) }
    var reps by rememberSaveable(configuration.key) { mutableStateOf(configuration.repetitions.toString()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val draft=configuration.copy(contractionSec=hold.toIntOrNull()?:0,relaxationSec=rest.toIntOrNull()?:0,repetitions=reps.toIntOrNull()?:0)
    val dirty=draft!=configuration
    Column(Modifier.fillMaxWidth().padding(12.dp).testTag("training_exercise_details"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(title,color=P.Text,fontSize=23.sp)
        TrainingParameter("Сжатие, с","hold",hold,120,busy) {hold=it;message=""}
        TrainingParameter("Отдых, с","rest",rest,120,busy) {rest=it;message=""}
        TrainingParameter("Повторы","reps",reps,300,busy) {reps=it;message=""}
        Text(if(draft.isValid) "Длительность: ${trainingTime(draft.durationMs)} · сжатие: ${draft.holdMs/1000} с" else "Сжатие и отдых: 1–120 с. Повторы: 1–300. Не более 90 минут.",
            color=P.Gold,fontSize=17.sp,modifier=Modifier.testTag("training_settings_duration"))
        Text("Отдых включён после каждого сжатия, в том числе последнего.",color=P.Muted)
        Button(onClick={busy=true;onSave(draft) {ok->busy=false;message=if(ok) "Параметры сохранены" else "Не удалось сохранить. Попробуй ещё раз."}},
            enabled=draft.isValid && dirty && !busy,modifier=Modifier.fillMaxWidth().testTag("training_save_parameters")) {Text("Сохранить параметры")}
        OutlinedButton(onClick={busy=true;onReset(configuration.key) {ok->busy=false;if(ok) {
            hold=defaults.contractionSec.toString();rest=defaults.relaxationSec.toString();reps=defaults.repetitions.toString();message="Исходные параметры восстановлены"
        } else message="Не удалось сбросить параметры."}},enabled=!busy,
            modifier=Modifier.fillMaxWidth().testTag("training_reset_parameters")) {Text("По умолчанию")}
        if(message.isNotEmpty()) Text(message,color=P.Text,modifier=Modifier.testTag("training_settings_message"))
        if(dirty) Text("Сохрани параметры перед запуском. Несохранённые изменения не применяются.",color=P.Muted)
        if(completed) Text("Упражнение выполнено.",color=P.Success)
        var dispatched by remember(configuration.key,locked,completed) {mutableStateOf(false)}
        LaunchedEffect(dispatched) {if(dispatched) {kotlinx.coroutines.delay(600);dispatched=false}}
        Button(onClick={if(!dispatched) {dispatched=true;onStart()}},enabled=!locked && !completed && !busy && !dirty && !dispatched,
            modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("btn_details_start")) {Text("Начать тренировку")}
    }
}

@Composable
private fun TrainingParameter(label: String,id: String,value: String,max: Int,disabled: Boolean,onChange: (String)->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text(label,color=P.Text)
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick={onChange(((value.toIntOrNull()?:1)-1).coerceIn(1,max).toString())},enabled=!disabled,
                modifier=Modifier.sizeIn(minWidth=48.dp,minHeight=48.dp).testTag("training_${id}_minus")) {Text("−")}
            OutlinedTextField(value=value,onValueChange={if(it.length<=3 && it.all(Char::isDigit)) onChange(it)},enabled=!disabled,
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,
                modifier=Modifier.weight(1f).testTag("training_parameter_$id"))
            OutlinedButton(onClick={onChange(((value.toIntOrNull()?:0)+1).coerceIn(1,max).toString())},enabled=!disabled,
                modifier=Modifier.sizeIn(minWidth=48.dp,minHeight=48.dp).testTag("training_${id}_plus")) {Text("+")}
        }
    }
}

@Composable
internal fun TrainingProgramDetails(program: CustomWorkoutProgram?,locked: Boolean,onSave: (CustomWorkoutProgram)->Unit,
    onDelete: (String)->Unit,onStart: (CustomWorkoutProgram)->Unit) = ReferencePlumTheme {
    var editing by rememberSaveable {mutableStateOf(false)}
    if(program==null) Text("Программа удалена. Вернись к списку.",color=P.Text)
    else {
        Column(Modifier.fillMaxWidth().padding(16.dp).testTag("training_program_details"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(program.title,color=P.Text,fontSize=24.sp)
            Text("Длительность: ${trainingTime(program.totalDurationMs)}",color=P.Gold)
            program.blocks.forEach { block -> Text("${block.name} · ${trainingTime(block.totalDurationMs)}",color=P.Text) }
            OutlinedButton(onClick={editing=true},modifier=Modifier.fillMaxWidth().testTag("training_edit_program")) {Text("Изменить программу")}
            OutlinedButton(onClick={onDelete(program.id)},modifier=Modifier.fillMaxWidth().testTag("training_delete_program")) {Text("Удалить программу")}
            var dispatched by remember(program.id,locked) {mutableStateOf(false)}
            LaunchedEffect(dispatched) {if(dispatched) {kotlinx.coroutines.delay(600);dispatched=false}}
            Button(onClick={if(!dispatched) {dispatched=true;onStart(program)}},enabled=!locked && !dispatched,
                modifier=Modifier.fillMaxWidth().testTag("btn_details_start")) {Text("Начать тренировку")}
        }
        if(editing) EditProgramDialog(program,P.palette,trainingLayout=true,onDismiss={editing=false},onSave={onSave(it);editing=false})
    }
}

/** Three small bursts; Canvas consumes no gestures and opens no dialog. Persistence owns unlock. */
@Composable
internal fun ExtraCycleFireworks(cycle: ExtraTrainingCycle,modifier: Modifier=Modifier) {
    if(cycle.mask!=7) return
    val progress=remember(cycle.round) { Animatable(0f) }
    LaunchedEffect(cycle.round) {progress.animateTo(1f,tween(3000))}
    Canvas(modifier.fillMaxSize().testTag("extra_cycle_fireworks")) {
        val t=progress.value
        for(burst in 0..2) {
            val age=((t-burst*.16f)/.68f).coerceIn(0f,1f)
            if(age>0 && age<1) for(spark in 0 until 20) {
                val angle=spark*Math.PI*2/20
                val distance=size.minDimension*.18f*age
                val origin=Offset(size.width*(.25f+burst*.25f),size.height*(.28f+burst%2*.14f))
                val location=origin+Offset((cos(angle)*distance).toFloat(),(sin(angle)*distance).toFloat()+age*age*55.dp.toPx())
                drawCircle((if(spark%2==0) P.GoldBright else P.Lilac).copy(alpha=1-age),2.dp.toPx()*(1-age*.6f),location)
            }
        }
    }
}
