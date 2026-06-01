package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingDao {
    @Query("SELECT COUNT(*) FROM reading_days")
    suspend fun getDaysCount(): Int

    @Query("SELECT * FROM reading_days ORDER BY dayIndex ASC")
    fun getAllDays(): Flow<List<ReadingDay>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDays(days: List<ReadingDay>)

    @Query("UPDATE reading_days SET completed = :completed, completedTimestamp = :timestamp WHERE dayIndex = :dayIndex")
    suspend fun updateDayStatus(dayIndex: Int, completed: Boolean, timestamp: Long?)

    @Query("UPDATE reading_days SET completed = 0, completedTimestamp = null")
    suspend fun resetAllDays()

    // Alarm config
    @Query("SELECT * FROM alarm_config WHERE id = 1 LIMIT 1")
    fun getAlarmConfig(): Flow<AlarmConfig?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAlarmConfig(config: AlarmConfig)

    // Cached Daily Verse
    @Query("SELECT * FROM cached_verse WHERE id = 1 LIMIT 1")
    fun getCachedVerse(): Flow<CachedVerse?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCachedVerse(verse: CachedVerse)
}
