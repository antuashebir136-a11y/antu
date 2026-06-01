package com.example.data

import kotlinx.coroutines.flow.Flow

class ReadingRepository(private val readingDao: ReadingDao) {

    val allDays: Flow<List<ReadingDay>> = readingDao.getAllDays()

    val alarmConfig: Flow<AlarmConfig?> = readingDao.getAlarmConfig()

    val cachedVerse: Flow<CachedVerse?> = readingDao.getCachedVerse()

    suspend fun initializePlanIfNeeded() {
        if (readingDao.getDaysCount() == 0) {
            readingDao.insertDays(BibleReadingPlan.items)
        }
    }

    suspend fun updateDayStatus(dayIndex: Int, completed: Boolean) {
        val timestamp = if (completed) System.currentTimeMillis() else null
        readingDao.updateDayStatus(dayIndex, completed, timestamp)
    }

    suspend fun resetAllDays() {
        readingDao.resetAllDays()
    }

    suspend fun saveAlarmConfig(config: AlarmConfig) {
        readingDao.saveAlarmConfig(config)
    }

    suspend fun saveCachedVerse(verseText: String, verseRef: String) {
        readingDao.saveCachedVerse(CachedVerse(verseText = verseText, verseRef = verseRef))
    }
}
