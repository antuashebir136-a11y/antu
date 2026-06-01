package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = ReadingRepository(db.readingDao())

    // UI state for reading days
    val readingDays: StateFlow<List<ReadingDay>> = repository.allDays
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Alarm configuration state
    val alarmConfig: StateFlow<AlarmConfig> = repository.alarmConfig
        .map { it ?: AlarmConfig() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AlarmConfig()
        )

    // Cached daily verse state
    val cachedVerse: StateFlow<CachedVerse?> = repository.cachedVerse
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Filter and search parameters
    private val _selectedFilter = MutableStateFlow("All") // "All", "Completed", "Pending"
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Loading and transient UI states desensitized from DB
    private val _isLoadingVerse = MutableStateFlow(false)
    val isLoadingVerse: StateFlow<Boolean> = _isLoadingVerse.asStateFlow()

    private val _currentVerseText = MutableStateFlow("ዮሐንስ ወንጌል 13፥1 — የወደዳቸውን እስከ መጨረሻው ወደዳቸው።")
    val currentVerseText: StateFlow<String> = _currentVerseText.asStateFlow()

    private val _currentVerseRef = MutableStateFlow("ዮሐ 13:1")
    val currentVerseRef: StateFlow<String> = _currentVerseRef.asStateFlow()

    private val _alarmStatusText = MutableStateFlow("⏳ ምንም ንቁ አላርም የለም")
    val alarmStatusText: StateFlow<String> = _alarmStatusText.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializePlanIfNeeded()
            // Pull the cached verse if it exists and populate UI
            repository.cachedVerse.firstOrNull()?.let {
                if (it != null) {
                    _currentVerseText.value = it.verseText
                    _currentVerseRef.value = it.verseRef
                }
            }
        }
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleDay(dayIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = readingDays.value
            val currentDay = list.find { it.dayIndex == dayIndex } ?: return@launch
            repository.updateDayStatus(dayIndex, !currentDay.completed)
        }
    }

    fun markDayAsCompleted(dayIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateDayStatus(dayIndex, true)
        }
    }

    fun resetPlan() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.resetAllDays()
            // Reset alarm
            repository.saveAlarmConfig(AlarmConfig(id = 1, enabled = false))
            _alarmStatusText.value = "⏳ ምንም ንቁ አላርም የለም"
        }
    }

    fun saveAlarm(enabled: Boolean, hour: Int, minute: Int, sound: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveAlarmConfig(AlarmConfig(id = 1, enabled = enabled, timeHour = hour, timeMinute = minute, sound = sound))
            if (enabled) {
                val timeStr = String.format("%02d:%02d", hour, minute)
                _alarmStatusText.value = "✅ ተዘጋጅቷል — $timeStr ሰዓት ላይ ያሳስባል"
            } else {
                _alarmStatusText.value = "⏸️ ቋርጧል"
            }
        }
    }

    fun fetchDailyVerse() {
        viewModelScope.launch {
            _isLoadingVerse.value = true
            val prompt = "ከአዲስ ኪዳን አንድ አጭር ጥቅስ ምረጥ። ጥቅሱን በአማርኛ ስጠኝ (የተረጎመውን)፣ ከዚያ ማጣቀሻ (ለምሳሌ ዮሐ 3:16) ስጠኝ። ጽሁፉ አበረታች እንዲሆን ግለጽልኝ። በ 1 አጭር መስመር ብቻ።"
            
            // Try fetching via Gemini API, with robust offline fallback
            try {
                // If there's an API key in BuildConfig, call Retrofit
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY") {
                    val request = GenerateContentRequest(
                        contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                        generationConfig = GenerationConfig(temperature = 0.7f)
                    )
                    val response = GeminiRetrofitClient.service.generateContent(apiKey, request)
                    val generatedText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (!generatedText.isNullOrEmpty()) {
                        // Extract verse and citation if possible, or parse cleanly
                        val parsedText = generatedText.trim()
                        val refStartIndex = parsedText.lastIndexOf("—")
                        var verseText = parsedText
                        var verseRef = "አዲስ ኪዳን"
                        if (refStartIndex != -1) {
                            verseText = parsedText.substring(0, refStartIndex).trim()
                            verseRef = parsedText.substring(refStartIndex + 1).trim()
                        } else {
                            // Find common scriptures brackets or parentheses
                            val braceIndex = parsedText.lastIndexOf("(")
                            if (braceIndex != -1) {
                                verseText = parsedText.substring(0, braceIndex).trim()
                                verseRef = parsedText.substring(braceIndex).replace("(", "").replace(")", "").trim()
                            }
                        }
                        _currentVerseText.value = verseText
                        _currentVerseRef.value = verseRef
                        repository.saveCachedVerse(verseText, verseRef)
                    } else {
                        useFallbackVerse()
                    }
                } else {
                    useFallbackVerse()
                }
            } catch (e: Exception) {
                useFallbackVerse()
            } finally {
                _isLoadingVerse.value = false
            }
        }
    }

    private suspend fun useFallbackVerse() {
        val fallbacks = listOf(
            Pair("እርሱ ኃጢአታችንን የሚያስተሰርይ ማስተስረያ ነው፤ እርሱም ፍቅሩን ይገልጻል።", "1 ዮሐ 2:2"),
            Pair("እኔ ቅዱስ ነኝና እናንተም ቅዱሳን ሁኑ።", "1 ጴ 1:16"),
            Pair("እግዚአብሔርንም ለሚወዱት እንደ አሳቡም ለተጠሩት ነገር ሁሉ ለበጎ እንዲደረግ እናውቃለን።", "ሮሜ 8:28"),
            Pair("በዚህ ዓለም ያሉትን ወገኖቹን የወደዳቸውን እስከ መጨረሻው ወደዳቸው።", "ዮሐ 13:1"),
            Pair("እንግዲህ እምነት ተስፋ ፍቅር እነዚህ ሦስቱ ጸንተው ይኖራሉ፤ ከእነዚህም የሚበልጠው ፍቅር ነው።", "1 ቆሮ 13:13"),
            Pair("ሰው ዓለሙን ሁሉ ቢያተርፍ ነፍሱንም ቢያጣ ምን ይጠቅመዋል?", "ማቴ 16:26")
        )
        val selected = fallbacks.random()
        _currentVerseText.value = selected.first
        _currentVerseRef.value = selected.second
        repository.saveCachedVerse(selected.first, selected.second)
    }
}
