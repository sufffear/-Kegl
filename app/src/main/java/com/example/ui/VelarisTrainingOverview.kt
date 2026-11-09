package com.example.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.*
import com.example.domain.*
import com.example.ui.theme.ReferencePlum as P
import com.example.ui.theme.ReferencePlumTheme

internal fun trainingDurationMs(slot: DailySlotWithCompletion): Long = slot.slot.let {
    DemoProtocolConfig(it.id,it.titleRu,it.contractionMs,it.relaxationMs,it.repetitions).totalActiveDurationMs
}
internal fun trainingTime(ms: Long): String = (ms / 1000).let { "${it / 60}:${(it % 60).toString().padStart(2,'0')}" }

/** Static layered bloom: no bitmaps, RenderEffect/API31, or perpetual animation. */
private fun Modifier.plumPanel(accent: Color, radius: Int = 16, glow: Float = 0f, colors: List<Color> = listOf(P.SurfaceTop,P.Surface)): Modifier {
    val shape = RoundedCornerShape(radius.dp)
    return drawWithCache {
        onDrawBehind {
            if (glow > 0f) for (i in 8 downTo 1) drawRoundRect(accent.copy(alpha = glow * .022f),
                cornerRadius = CornerRadius(radius.dp.toPx()), style = Stroke(i * 1.2.dp.toPx()))
        }
    }.clip(shape).background(Brush.verticalGradient(colors))
        .border(BorderStroke(1.dp,Brush.linearGradient(listOf(accent.copy(alpha=.85f),accent.copy(alpha=.28f),accent.copy(alpha=.55f)))),shape)
}

@Composable
internal fun VelarisTrainingOverview(
    snapshot: PhaseEngineSnapshot, dailySlots: List<DailySlotWithCompletion>,
    onStartSlot: (DailySlotWithCompletion) -> Unit, onOpenDetails: (DailySlotWithCompletion) -> Unit,
    onOpenConstructor: () -> Unit, onSaveProgram: (CustomWorkoutProgram) -> Unit,
    onOpenSettings: () -> Unit, isDemo: Boolean = false, modifier: Modifier = Modifier,
    extraTemplates: List<FreeIntervalTemplateEntity> = ExtraWorkoutPresets.all,
    extraCycle: ExtraTrainingCycle = ExtraTrainingCycle(), extrasReady: Boolean = true,
    customPrograms: List<CustomWorkoutProgram> = emptyList(),
    onOpenExtra: (FreeIntervalTemplateEntity)->Unit = {}, onStartExtra: (FreeIntervalTemplateEntity)->Unit = {},
    onOpenProgram: (CustomWorkoutProgram)->Unit = {}, onStartProgram: (CustomWorkoutProgram)->Unit = {}
) = ReferencePlumTheme {
    val grouped = dailySlots.groupBy { PlanGroup.fromCode(it.slot.groupCode) }
    val target = PlanGroup.entries.firstOrNull { group -> grouped[group].orEmpty().let { it.isNotEmpty() && it.any { !it.isCompleted } } }
        ?: if(dailySlots.isEmpty()) PlanGroup.MORNING else PlanGroup.EVENING
    var selected by rememberSaveable { mutableStateOf(target.code) }
    var lastAutoTarget by rememberSaveable { mutableStateOf(target.code) }
    LaunchedEffect(target,dailySlots.isNotEmpty()) {
        if(dailySlots.isNotEmpty() && lastAutoTarget!=target.code) { selected=target.code; lastAutoTarget=target.code }
    }
    val slots = grouped[PlanGroup.fromCode(selected)].orEmpty()
    Column(modifier.fillMaxWidth().background(P.Background).testTag("plan_screen")) {
        TrainingHero(onOpenSettings,isDemo)
        Column(Modifier.padding(horizontal=16.dp).padding(bottom=12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                PlanGroup.entries.forEach { group ->
                    val entries = grouped[group].orEmpty()
                    val done = entries.count { it.isCompleted }
                    TrainingPeriod(group,done,entries.size,selected==group.code, { selected=group.code },Modifier.weight(1f))
                }
            }
            val holdSeconds = slots.sumOf { it.slot.contractionMs * it.slot.repetitions } / 1000
            Row(Modifier.fillMaxWidth().plumPanel(P.Gold.copy(alpha=.6f),12, colors=P.InfoGradient)
                .padding(horizontal=12.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
                Icon(Icons.Default.TrackChanges,null,tint=P.Gold,modifier=Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if(slots.isEmpty()) "На этот период нет упражнений" else "${slots.size} упражнения · $holdSeconds с сжатия",
                    color=P.Text,fontSize=13.sp,modifier=Modifier.testTag("training_period_summary"))
            }
            Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                slots.forEachIndexed { index,item -> TrainingExerciseCard(item,index,snapshot.isActiveOrPaused,
                    { onOpenDetails(item) }, { onStartSlot(item) }) }
            }
            Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                extraTemplates.forEachIndexed { index,template ->
                    val title = listOf("Быстрые сокращения","Силовые сокращения","Длительные удержания")[index]
                    val completed = extraCycle.isLocked(template.slotIndex)
                    TrainingCompactCard("extra_${template.slotIndex}",title,
                        "${template.calculatedCycles} × (${template.contractionSec}с / ${template.relaxationSec}с)",
                        template.actualDurationSec*1000L,index,completed,snapshot.isActiveOrPaused || !extrasReady,
                        {onOpenExtra(template)},{onStartExtra(template)},cold=true,disableCompletedBody=true)
                }
                customPrograms.forEach { program ->
                    val parameters = program.blocks.singleOrNull()?.takeIf { it.type!=WorkoutBlockType.CUSTOM_SEQUENCE }?.let {
                        "${it.repetitions} × (${it.contractionSec}с / ${it.relaxationSec}с)"
                    } ?: "${program.blocks.size} блоков · ${program.totalCycles} повторов"
                    TrainingCompactCard("program_${program.id}",program.title,parameters,program.totalDurationMs,
                        2,false,snapshot.isActiveOrPaused,{onOpenProgram(program)},{onStartProgram(program)},cold=true)
                }
            }
            Row(Modifier.fillMaxWidth().heightIn(min=48.dp).plumPanel(P.Gold,12)
                .clickable(role=Role.Button,onClick=onOpenConstructor).testTag("btn_other_workouts")
                .padding(horizontal=12.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome,null,tint=P.Gold,modifier=Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Конструктор упражнений",color=P.Gold,fontSize=14.sp,lineHeight=18.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight,null,tint=P.Gold)
            }
        }
    }
}

@Composable
private fun TrainingHero(onOpenSettings: () -> Unit,isDemo: Boolean) {
    val height=(LocalConfiguration.current.screenWidthDp * .52f).coerceIn(180f,220f).dp
    Box(Modifier.fillMaxWidth().height(height).testTag("training_hero")) {
        Image(painterResource(R.drawable.chantal_training_hero),"Шанталь в белом платье в тёплом интерьере",
            Modifier.fillMaxSize(),contentScale=ContentScale.Crop,alignment=Alignment.TopStart)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent,.60f to P.Background.copy(alpha=.06f),1f to P.Background)))
        if(isDemo) Text("Демо · без зачёта",color=P.Gold,fontSize=12.sp,
            modifier=Modifier.align(Alignment.BottomStart).padding(16.dp).testTag("training_demo_badge"))
        IconButton(onClick=onOpenSettings,modifier=Modifier.align(Alignment.BottomEnd).padding(end=8.dp,bottom=6.dp).size(48.dp).testTag("open_settings")) {
            Icon(Icons.Default.Settings,"Настройки",tint=P.Gold,modifier=Modifier.size(22.dp))
        }
    }
}

@Composable
private fun TrainingPeriod(group: PlanGroup,done: Int,total: Int,active: Boolean,onClick: () -> Unit,modifier: Modifier) {
    val large=LocalDensity.current.fontScale>1.3f
    val complete=total>0 && done==total
    val accent=when { complete->P.Success; active->P.GoldBright; else->P.Outline }
    val colors=when { complete->P.CompletedPeriodGradient; active->P.ActivePeriodGradient; else->listOf(P.SurfaceTop,P.Surface) }
    val icon=when(group) {PlanGroup.MORNING->Icons.Default.WbTwilight;PlanGroup.NOON->Icons.Default.LightMode;PlanGroup.EVENING->Icons.Default.DarkMode}
    val contents: @Composable () -> Unit = {
        Text(group.titleRu,color=P.Text,fontSize=14.sp,lineHeight=18.sp,fontWeight=FontWeight.SemiBold,
            maxLines=1,softWrap=false,textAlign=TextAlign.Center,
            modifier=Modifier.fillMaxWidth().testTag("training_period_label_${group.name}"))
        Text(if(total>0) "$done / $total" else "—",color=if(complete) P.Success else P.Muted,fontSize=12.sp,lineHeight=16.sp)
    }
    Box(modifier.heightIn(min=56.dp).plumPanel(accent,14,if(active)1f else 0f,colors)
        .clickable(role=Role.Tab,onClick=onClick).semantics { selected=active }
        .testTag("plan_group_tab_${group.name}").padding(horizontal=8.dp,vertical=8.dp),contentAlignment=Alignment.Center) {
        if(large) Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(3.dp)) {
            Icon(icon,null,tint=if(complete) P.Success else if(active) P.GoldBright else P.Lilac,modifier=Modifier.size(20.dp))
            contents()
        } else Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
            Icon(icon,null,tint=if(complete) P.Success else if(active) P.GoldBright else P.Lilac,modifier=Modifier.size(23.dp))
            Spacer(Modifier.width(6.dp))
            Column(horizontalAlignment=Alignment.CenterHorizontally) {contents()}
        }
    }
}

@Composable
internal fun TrainingExerciseCard(item: DailySlotWithCompletion,index: Int,locked: Boolean,onDetails: () -> Unit,onStart: () -> Unit) =
    TrainingCompactCard(item.slot.assignmentId,item.slot.titleRu,
        "${item.slot.repetitions} × (${item.slot.contractionMs/1000}с / ${item.slot.relaxationMs/1000}с)",
        trainingDurationMs(item),index,item.isCompleted,locked,onDetails,onStart)

@Composable
internal fun TrainingCompactCard(id: String,title: String,parameters: String,durationMs: Long,index: Int,
    completed: Boolean,locked: Boolean,onDetails: ()->Unit,onStart: ()->Unit,
    cold: Boolean=false,disableCompletedBody: Boolean=false) {
    val large=LocalDensity.current.fontScale>1.3f
    val accent=if(completed) P.Success else if(cold) Color(0xFF9DA6FF) else if(index==0) P.Gold else if(index==1) P.Lilac else P.Purple
    val icon=when(index) {0->Icons.Default.Bolt;1->Icons.Default.FitnessCenter;else->Icons.Default.AllInclusive}
    var dispatched by remember(id,completed,locked) { mutableStateOf(false) }
    LaunchedEffect(dispatched) {if(dispatched) {kotlinx.coroutines.delay(600);dispatched=false}}
    Row(Modifier.fillMaxWidth().heightIn(min=56.dp).plumPanel(accent,15,if(index==0) .65f else .06f,
        if(completed) P.CompletedPeriodGradient else if(cold) listOf(Color(0xFF272740),Color(0xFF171726)) else when(index) {0->P.FastGradient;1->P.StrengthGradient;else->P.EnduranceGradient})
        .clickable(enabled=!(disableCompletedBody && completed),role=Role.Button,onClick=onDetails).testTag("daily_slot_${id}")
        .padding(horizontal=8.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(if(large) 36.dp else 40.dp).clip(CircleShape)
            .background(Brush.radialGradient(listOf(accent.copy(alpha=.4f),accent.copy(alpha=.09f))))
            .border(1.dp,accent.copy(alpha=.35f),CircleShape),contentAlignment=Alignment.Center) {
            Icon(if(completed) Icons.Default.Check else icon,null,tint=accent,modifier=Modifier.size(28.dp).then(if(index==1 && !completed) Modifier.rotate(-45f) else Modifier))
        }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(title,color=P.Text,fontSize=14.sp,lineHeight=18.sp,fontWeight=FontWeight.SemiBold)
            Text(parameters,color=if(cold) P.Lilac else P.Gold,fontSize=12.sp,lineHeight=16.sp)
        }
        val enabled=!locked && !completed && !dispatched
        val play: @Composable () -> Unit = {
            TrainingStartButton(enabled,completed,id,title) {
                if(!dispatched) { dispatched=true; onStart() }
            }
        }
        if(large) Column(Modifier.width(60.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text(trainingTime(durationMs),color=P.Text,fontSize=15.sp,lineHeight=18.sp,fontWeight=FontWeight.Medium)
            play()
        } else {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                Icon(Icons.Default.Timer,null,tint=P.Muted,modifier=Modifier.size(13.dp))
                Text(trainingTime(durationMs),color=P.Text,fontSize=15.sp,fontWeight=FontWeight.Medium)
            }
            play()
        }
    }
}

@Composable
internal fun TrainingExerciseDetails(item: DailySlotWithCompletion?,locked: Boolean,onStart: (DailySlotWithCompletion)->Unit) = ReferencePlumTheme {
    Column(Modifier.fillMaxWidth().plumPanel(P.Purple,18).padding(16.dp).testTag("training_exercise_details"),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        if(item==null) Text("Упражнение больше недоступно. Вернись к плану дня.",color=P.Text)
        else {
            Text(item.slot.titleRu,color=P.Text,fontSize=24.sp,fontWeight=FontWeight.Bold)
            Text("${item.slot.repetitions} повторов · сжатие ${item.slot.contractionMs/1000} с · отдых ${item.slot.relaxationMs/1000} с",color=P.Muted,fontSize=17.sp)
            Text("Длительность: ${trainingTime(trainingDurationMs(item))}",color=P.Gold,fontSize=20.sp)
            Text(if(item.isCompleted) "Упражнение выполнено." else "Отдых включён после каждого сжатия, в том числе последнего.",color=P.Muted)
            var dispatched by remember(item.slot.id) { mutableStateOf(false) }
            Button(onClick={ if(!dispatched) {dispatched=true;onStart(item)} },enabled=!locked && !item.isCompleted && !dispatched,
                modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("btn_details_start")) {Text("Начать тренировку")}
        }
    }
}

@Composable
private fun TrainingFeather(modifier: Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val path=androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width*.16f,size.height*.86f)
            cubicTo(size.width*.20f,size.height*.36f,size.width*.50f,size.height*.05f,size.width*.94f,size.height*.04f)
            cubicTo(size.width*.93f,size.height*.44f,size.width*.63f,size.height*.76f,size.width*.27f,size.height*.78f)
            close()
        }
        drawPath(path,P.Gold)
        drawLine(P.Background,androidx.compose.ui.geometry.Offset(size.width*.18f,size.height*.84f),
            androidx.compose.ui.geometry.Offset(size.width*.78f,size.height*.23f),strokeWidth=1.2.dp.toPx())
        drawLine(P.Gold,androidx.compose.ui.geometry.Offset(size.width*.05f,size.height*.97f),
            androidx.compose.ui.geometry.Offset(size.width*.23f,size.height*.77f),strokeWidth=1.5.dp.toPx())
    }
}

internal fun trainingDraft(type: WorkoutBlockType): CustomWorkoutProgram = CustomWorkoutProgram(
    title=type.titleRu,
    blocks=listOf(if(type==WorkoutBlockType.CUSTOM_SEQUENCE) WorkoutBlock(type=type,
        customSteps=listOf(CustomSequenceStep(),CustomSequenceStep(phaseType=PhaseType.RELAXATION))) else WorkoutBlock(type=type))
)

@Composable
private fun TrainingStartButton(enabled: Boolean,completed: Boolean,id: String,title: String,onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clip(RoundedCornerShape(13.dp)).clickable(enabled=enabled,role=Role.Button,onClick=onClick)
        .semantics { contentDescription=if(completed) "$title: выполнено" else "Начать: $title" }
        .testTag("btn_start_slot_$id"),contentAlignment=Alignment.Center) {
        Box(Modifier.size(39.dp).plumPanel(if(enabled) P.Lilac else P.Disabled,11,if(enabled)1.7f else 0f,
            if(enabled) P.PlayGradient else listOf(P.Surface,P.PurpleDeep)),contentAlignment=Alignment.Center) {
            Icon(if(completed) Icons.Default.Check else Icons.Default.PlayArrow,null,
                tint=if(enabled) P.Text else P.Disabled,modifier=Modifier.size(27.dp))
        }
    }
}
