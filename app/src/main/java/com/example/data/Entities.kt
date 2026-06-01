package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_days")
data class ReadingDay(
    @PrimaryKey val dayIndex: Int, // 0 to 89 (representing Days 1 to 90)
    val passage: String,
    val description: String,
    val completed: Boolean = false,
    val completedTimestamp: Long? = null
)

@Entity(tableName = "alarm_config")
data class AlarmConfig(
    @PrimaryKey val id: Int = 1,
    val enabled: Boolean = false,
    val timeHour: Int = 7,
    val timeMinute: Int = 0,
    val sound: String = "gentle"
)

@Entity(tableName = "cached_verse")
data class CachedVerse(
    @PrimaryKey val id: Int = 1,
    val verseText: String,
    val verseRef: String,
    val timestamp: Long = System.currentTimeMillis()
)
