package com.ashupaybox.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ashupaybox.data.local.entity.ProcessedEventEntity

@Dao
interface ProcessedEventDao {

    @Query("SELECT COUNT(*) > 0 FROM processed_events WHERE eventId = :eventId")
    suspend fun isEventProcessed(eventId: String): Boolean

    @Query("SELECT * FROM processed_events WHERE eventId = :eventId LIMIT 1")
    suspend fun getProcessedEvent(eventId: String): ProcessedEventEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProcessedEvent(event: ProcessedEventEntity): Long

    @Query("UPDATE processed_events SET announced = 1 WHERE eventId = :eventId")
    suspend fun markAnnounced(eventId: String)

    @Query("DELETE FROM processed_events WHERE eventId LIKE 'test_%' OR eventId LIKE 'sim_%'")
    suspend fun clearTestEvents(): Int
}
