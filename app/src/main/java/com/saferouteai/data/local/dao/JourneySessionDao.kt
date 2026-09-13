package com.saferouteai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.saferouteai.data.local.entity.JourneySessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for persisting and querying journey sessions.
 */
@Dao
interface JourneySessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: JourneySessionEntity)

    @Update
    suspend fun updateSession(session: JourneySessionEntity)

    @Query("SELECT * FROM journey_sessions WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: String): JourneySessionEntity?

    @Query("SELECT * FROM journey_sessions WHERE status IN ('STARTING', 'ACTIVE', 'CHECKPOINT_DUE', 'CHECKPOINT_ACKNOWLEDGED', 'COMPLETING') ORDER BY startedAtEpochMs DESC LIMIT 1")
    fun observeActiveSession(): Flow<JourneySessionEntity?>

    @Query("SELECT * FROM journey_sessions WHERE status IN ('STARTING', 'ACTIVE', 'CHECKPOINT_DUE', 'CHECKPOINT_ACKNOWLEDGED', 'COMPLETING') ORDER BY startedAtEpochMs DESC LIMIT 1")
    suspend fun getActiveSession(): JourneySessionEntity?

    @Query("SELECT * FROM journey_sessions ORDER BY startedAtEpochMs DESC LIMIT 1")
    fun observeLatestSession(): Flow<JourneySessionEntity?>

    @Query("SELECT * FROM journey_sessions ORDER BY startedAtEpochMs DESC")
    fun observeAllSessions(): Flow<List<JourneySessionEntity>>

    @Query("DELETE FROM journey_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM journey_sessions")
    suspend fun deleteAllSessions()
}
