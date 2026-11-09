package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.domain.*
import com.example.ui.*
import com.example.ui.theme.VelarisTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk=[28],qualifiers="w360dp-h800dp")
class VelarisPlumTrainingTest {
    @get:Rule val compose=createComposeRule()
    private fun fixture(group: PlanGroup,index: Int,done: Boolean=false): DailySlotWithCompletion {
        val names=listOf("Быстрая тренировка","Силовой контроль","Выносливость")
        val hold=listOf(1000L,10000L,25000L)[index]; val rest=listOf(1000L,5000L,10000L)[index]
        val id="${group.code}_$index"
        return DailySlotWithCompletion(DailySlotEntity(id,"day","2026-11-09",id,group.code,index,names[index],hold,rest,if(index==0)100 else 10,false,false),
            if(done) WorkoutCompletionEntity("run_$id",id,"2026-11-09",1,null,false) else null)
    }
    private fun all()=PlanGroup.entries.flatMap { g-> (0..2).map{fixture(g,it)} }
    private fun click(tag: String) {compose.onNodeWithTag(tag).performScrollTo().performClick();compose.waitForIdle()}
    @Test fun exactDurationsBodyAndPlayAreSeparateAndDispatchOnce() {
        var details=0; var starts=0
        val data=all()
        assertEquals(listOf(200000L,150000L,350000L),data.take(3).map(::trainingDurationMs))
        compose.setContent {VelarisTheme {Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            VelarisTrainingOverview(PhaseEngineSnapshot(),data,{starts++},{details++},{},{},{})
        }}}
        compose.onNodeWithTag("training_period_summary").assertTextEquals("3 упражнения · 450 с сжатия")
        compose.onNodeWithText("3:20").assertExists();compose.onNodeWithText("2:30").assertExists();compose.onAllNodesWithText("5:50").onFirst().assertExists()
        click("daily_slot_MORNING_0");assertEquals(1,details);assertEquals(0,starts)
        val node=compose.onNodeWithTag("btn_start_slot_MORNING_0").performScrollTo()
        val action=node.fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        node.performClick();compose.runOnIdle {action();action()};compose.waitForIdle()
        assertEquals(1,starts);assertEquals(1,details)
        compose.onAllNodes(hasText("Кампания",substring=true)).assertCountEquals(0)
    }
    @Test fun completedPeriodsAdvanceAndAllFourConstructorBlockEditorsRemainAvailable() {
        var data by mutableStateOf(all()); var saves=0
        var draft by mutableStateOf<CustomWorkoutProgram?>(null)
        compose.setContent {VelarisTheme {Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            VelarisTrainingOverview(PhaseEngineSnapshot(),data,{fail("Completed workout must not start")},{},{},{saves++},{})
            draft?.let {program -> EditProgramDialog(program,com.example.ui.theme.ReferencePlum.palette,trainingLayout=true,
                onDismiss={draft=null},onSave={saves++;draft=null})}
        }}}
        compose.runOnIdle {data=PlanGroup.entries.flatMap{g->(0..2).map{fixture(g,it,g==PlanGroup.MORNING)}}};compose.waitForIdle()
        compose.onNodeWithTag("plan_group_tab_NOON").assertIsSelected()
        click("plan_group_tab_MORNING")
        compose.onNodeWithTag("btn_start_slot_MORNING_0").assertIsNotEnabled()
        WorkoutBlockType.entries.forEach {type->
            compose.runOnIdle {draft=trainingDraft(type)};compose.waitForIdle()
            compose.onNodeWithText("Настройка программы").assertExists()
            compose.onAllNodesWithText(type.titleRu,substring=true).onFirst().assertExists()
            compose.onNodeWithText("Отмена").performClick();compose.waitForIdle()
        }
        assertEquals(0,saves)
        compose.onNodeWithTag("training_extras_toggle").assertDoesNotExist()
        compose.onNodeWithTag("daily_slot_extra_101").assertExists()
        compose.onNodeWithTag("btn_other_workouts").assertExists()
    }
    @Test fun portraitScreenshotAndLargeFontRemainScrollableWithoutNestedLists() {
        var scale by mutableStateOf(1f)
        var root: android.view.View?=null
        compose.setContent {VelarisTheme {CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,scale)) {
            root=LocalView.current
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                VelarisTrainingOverview(PhaseEngineSnapshot(),all(),{},{},{},{},{})
            }
        }}}
        compose.waitForIdle()
        fun checkLabels() {
            for(group in PlanGroup.entries) {
                val layouts=mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                compose.onNodeWithTag("training_period_label_${group.name}",useUnmergedTree=true).performScrollTo()
                    .performSemanticsAction(SemanticsActions.GetTextLayoutResult) {it(layouts)}
                assertEquals(1,layouts.single().lineCount)
                val titleLayout=layouts.single()
                assertFalse("${group.name} scale=$scale size=${titleLayout.size} intrinsic=${titleLayout.multiParagraph.maxIntrinsicWidth} lines=${titleLayout.lineCount} width=${titleLayout.didOverflowWidth} height=${titleLayout.didOverflowHeight}",titleLayout.hasVisualOverflow)
            }
            for(id in 101..103) {
                compose.onNodeWithTag("daily_slot_extra_$id").performScrollTo().assertIsDisplayed()
                compose.onNodeWithTag("btn_start_slot_extra_$id").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
            }
        }
        checkLabels()
        compose.onNodeWithTag("training_hero").performScrollTo()
        compose.runOnIdle {
            val view=root!!;val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888);view.draw(Canvas(bitmap))
            val file=File("build/reports/training-reference/portrait-100.png");file.parentFile?.mkdirs()
            file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
        }
        compose.runOnIdle {scale=1.5f};compose.waitForIdle()
        checkLabels()
        for(tag in listOf("plan_group_tab_MORNING","daily_slot_MORNING_0","btn_start_slot_MORNING_0","daily_slot_extra_103","btn_other_workouts"))
            compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("btn_start_slot_MORNING_0").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
    }
}
