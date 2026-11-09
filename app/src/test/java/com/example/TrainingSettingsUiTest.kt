package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],qualifiers="w320dp-h640dp")
class TrainingSettingsUiTest {
    @get:Rule val compose=createComposeRule()
    private fun click(tag: String) {compose.onNodeWithTag(tag).performScrollTo().performClick();compose.waitForIdle()}
    @Test fun editorUsesExplicitSaveRecalculatesAndResetDoesNotStartWorkout() {
        val factory=TrainingConfiguration("mandatory:real_v2_morning_fast",1,1,100)
        var saved by mutableStateOf(factory);var starts=0;var saves=0
        compose.setContent {VelarisTheme {Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            TrainingSettingsDetails("Быстрая тренировка",saved,factory,false,false,
                onSave={value,result->saved=value;saves++;result(true)},onReset={_,result->saved=factory;result(true)},onStart={starts++})
        }}}
        click("training_hold_plus")
        assertEquals(factory,saved)
        compose.onNodeWithTag("training_settings_duration").assertTextEquals("Длительность: 5:00 · сжатие: 200 с")
        compose.onNodeWithTag("btn_details_start").assertIsNotEnabled()
        click("training_save_parameters")
        assertEquals(2,saved.contractionSec);assertEquals(1,saves)
        compose.onNodeWithTag("btn_details_start").assertIsEnabled()
        click("training_reset_parameters")
        assertEquals(factory,saved);assertEquals(0,starts)
        compose.onNodeWithTag("training_settings_duration").assertTextEquals("Длительность: 3:20 · сжатие: 100 с")
        click("btn_details_start");assertEquals(1,starts)
    }
    @Test fun failedSaveKeepsDraftAndNeverClaimsSuccessOrStarts() {
        val factory=TrainingConfiguration("extra:101",1,1,90)
        compose.setContent {VelarisTheme {Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            TrainingSettingsDetails("Быстрые сокращения",factory,factory,false,false,{_,result->result(false)},{_,result->result(false)},{fail("Unsaved draft started")})
        }}}
        click("training_hold_plus");click("training_save_parameters")
        compose.onNodeWithTag("training_settings_message").assertTextContains("Не удалось сохранить",substring=true)
        compose.onNodeWithTag("btn_details_start").assertIsNotEnabled()
    }
    @Test fun unifiedListHasSixStandardCardsCustomThenConstructorAndCycleLocksBothTargets() {
        var opened=0;var started=0;var programOpened=0;var programStarted=0
        var cycle by mutableStateOf(ExtraTrainingCycle(mask=1))
        val data=(0..2).map {index->DailySlotWithCompletion(DailySlotEntity("s$index","day","2026-11-09","type$index","MORNING",index,
            listOf("Быстрая тренировка","Силовой контроль","Выносливость")[index],1000,1000,10,false,false),null)}
        val program=CustomWorkoutProgram(id="mine",title="Моя программа",blocks=listOf(WorkoutBlock()))
        compose.setContent {VelarisTheme {Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            VelarisTrainingOverview(PhaseEngineSnapshot(),data,{},{},{},{},{},extraCycle=cycle,
                customPrograms=listOf(program),onOpenExtra={opened++},onStartExtra={started++},onOpenProgram={programOpened++},onStartProgram={programStarted++})
        }}}
        compose.onNodeWithText("Velaris").assertDoesNotExist();compose.onNodeWithText("Тренировки").assertDoesNotExist()
        compose.onNodeWithTag("training_extras_toggle").assertDoesNotExist()
        compose.onNodeWithText("Дополнительные упражнения").assertDoesNotExist()
        for(id in 0..2) compose.onNodeWithTag("daily_slot_type$id").assertExists()
        for(id in 101..103) compose.onNodeWithTag("daily_slot_extra_$id").assertExists()
        compose.onNodeWithTag("daily_slot_extra_101").assertIsNotEnabled()
        compose.onNodeWithTag("btn_start_slot_extra_101").assertIsNotEnabled()
        click("daily_slot_extra_102");assertEquals(1,opened);assertEquals(0,started)
        click("btn_start_slot_extra_102");assertEquals(1,opened);assertEquals(1,started)
        click("daily_slot_program_mine");assertEquals(1,programOpened);assertEquals(0,programStarted)
        click("btn_start_slot_program_mine");assertEquals(1,programStarted)
        compose.runOnIdle {cycle=ExtraTrainingCycle(round=1)};compose.waitForIdle()
        compose.onNodeWithTag("daily_slot_extra_101").assertIsEnabled()
        compose.onNodeWithTag("btn_start_slot_extra_101").assertIsEnabled()
        compose.onNodeWithTag("btn_other_workouts").performScrollTo().assertIsDisplayed()
    }
}
