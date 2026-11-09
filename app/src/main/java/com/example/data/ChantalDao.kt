package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/** A null payload marks a completed import of an empty legacy save. */
@Entity(tableName = "chantal_aggregate")
data class ChantalAggregateEntity(
    @PrimaryKey val id: Int = 1,
    val revision: Long,
    val payload: String?
)

/** Both a journey episode and its accepted decision may be confirmed only once. */
@Entity(tableName = "chantal_confirmations", indices = [Index(value = ["decisionId"], unique = true)])
data class ChantalConfirmationEntity(
    @PrimaryKey val episodeId: String,
    val decisionId: String
)

@Entity(tableName = "chantal_journal_events")
data class ChantalJournalEventEntity(
    @PrimaryKey val eventId: String,
    val payload: String
)

/** Persisted with the Room restore transaction; external recovery uses this commit receipt. */
@Entity(tableName = "chantal_restore_markers")
data class ChantalRestoreMarkerEntity(@PrimaryKey val operationId: String)

/** Synchronous methods run on repository IO workers, inside its Room transaction when writing. */
@Dao
interface ChantalDao {
    @Query("SELECT * FROM chantal_aggregate WHERE id = 1")
    suspend fun aggregateAsync(): ChantalAggregateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAggregateAsync(value: ChantalAggregateEntity)

    @Query("DELETE FROM chantal_confirmations")
    suspend fun clearReceiptsAsync()

    @Query("DELETE FROM chantal_journal_events")
    suspend fun clearEventsAsync()

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReceiptAsync(value: ChantalConfirmationEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEventAsync(value: ChantalJournalEventEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM chantal_restore_markers WHERE operationId = :operationId)")
    suspend fun markerPresentAsync(operationId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMarkerAsync(value: ChantalRestoreMarkerEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM chantal_restore_markers WHERE operationId = :operationId)")
    fun markerPresent(operationId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertMarker(value: ChantalRestoreMarkerEntity)

    @Query("SELECT * FROM chantal_aggregate WHERE id = 1")
    fun aggregate(): ChantalAggregateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun putAggregate(value: ChantalAggregateEntity)

    @Query("SELECT * FROM chantal_confirmations WHERE episodeId = :episodeId")
    fun receipt(episodeId: String): ChantalConfirmationEntity?

    @Query("SELECT * FROM chantal_confirmations WHERE decisionId = :decisionId")
    fun receiptForDecision(decisionId: String): ChantalConfirmationEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertReceipt(value: ChantalConfirmationEntity)

    @Query("SELECT * FROM chantal_journal_events ORDER BY eventId")
    fun events(): List<ChantalJournalEventEntity>

    @Query("SELECT * FROM chantal_journal_events WHERE eventId = :eventId")
    fun event(eventId: String): ChantalJournalEventEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertEvent(value: ChantalJournalEventEntity)

    /** Used only by validated full archive replacement, inside the same transaction as the aggregate. */
    @Query("DELETE FROM chantal_confirmations")
    fun clearReceipts()

    @Query("DELETE FROM chantal_journal_events")
    fun clearEvents()
}
