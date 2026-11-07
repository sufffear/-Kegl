package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.workDataOf
import androidx.work.testing.TestWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.data.ChantalProgressRepository
import com.example.service.ChantalReturnScheduler
import com.example.service.ChantalReturnWorker
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf
import android.app.NotificationManager
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class WorkoutReturnNotificationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val executor = Executors.newSingleThreadExecutor()
    @Before fun setup() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        context.getSharedPreferences("chantal_notifications", Context.MODE_PRIVATE).edit().clear().commit()
        ChantalProgressRepository(context).replace(null)
    }
    @After fun close() { executor.shutdownNow() }
    private fun deliver(start: Long) =
        TestWorkerBuilder.from(context, ChantalReturnWorker::class.java, executor)
            .setInputData(workDataOf("start" to start)).build().doWork()
    @Test fun staleOrUnfinishedJourneyDoesNotNotify() {
        val start = System.currentTimeMillis()
        ChantalProgressRepository(context).save(JSONObject().put("activeJourneyStartMs", start))
        assertEquals(androidx.work.ListenableWorker.Result.retry(), deliver(start))
        assertEquals(androidx.work.ListenableWorker.Result.success(), deliver(start - 100))
        assertEquals(0, shadowOf(context.getSystemService(NotificationManager::class.java)).size())
    }
    @Test fun returnedJourneyNotifiesOnceWithoutIssuingRewards() {
        val start = System.currentTimeMillis() - 3_600_000L
        ChantalProgressRepository(context).save(JSONObject().put("activeJourneyStartMs", start).put("coins", 17).put("activeRegionTitleRu", "Лес"))
        deliver(start)
        deliver(start)
        val manager = shadowOf(context.getSystemService(NotificationManager::class.java))
        assertEquals(1, manager.size())
        assertEquals(17, ChantalProgressRepository(context).load().getInt("coins"))
        assertEquals(start, context.getSharedPreferences("chantal_notifications", Context.MODE_PRIVATE).getLong("notifiedStart", 0))
    }
}
