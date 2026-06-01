package com.example

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val readingDays by viewModel.readingDays.collectAsStateWithLifecycle()
    val alarmConfig by viewModel.alarmConfig.collectAsStateWithLifecycle()
    val cachedVerse by viewModel.cachedVerse.collectAsStateWithLifecycle()

    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val isLoadingVerse by viewModel.isLoadingVerse.collectAsStateWithLifecycle()
    val currentVerseText by viewModel.currentVerseText.collectAsStateWithLifecycle()
    val currentVerseRef by viewModel.currentVerseRef.collectAsStateWithLifecycle()
    val alarmStatusText by viewModel.alarmStatusText.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    var activeTab by remember { mutableStateOf(0) } // 0: Daily Tracker, 1: Report & Data, 2: Alarm config

    // Screen size detection for responsive layout
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    Scaffold(
        bottomBar = {
            if (!isTablet) {
                NavigationBar(
                    containerColor = DarkCard,
                    contentColor = GoldMedium
                ) {
                    NavigationBarItem(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = "ዕለታዊ") },
                        label = { Text("ዕለታዊ", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DarkAtmosphere,
                            selectedTextColor = GoldMedium,
                            indicatorColor = GoldMedium,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                    NavigationBarItem(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        icon = { Icon(Icons.Default.DateRange, contentDescription = "ሪፖርት") },
                        label = { Text("ሪፖርት (ዳታ)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DarkAtmosphere,
                            selectedTextColor = GoldMedium,
                            indicatorColor = GoldMedium,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                    NavigationBarItem(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        icon = { Icon(Icons.Default.Notifications, contentDescription = "ማንቂያ") },
                        label = { Text("ማንቂያ", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DarkAtmosphere,
                            selectedTextColor = GoldMedium,
                            indicatorColor = GoldMedium,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        },
        containerColor = DarkAtmosphere
    ) { innerPadding ->
        if (isTablet) {
            // Responsive Master-Detail / Side-By-Side layout for wide screens
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Left Column - Quick Tracker & Daily scripture (38% width)
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.38f)
                        .background(DarkCard)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HeroSection()

                    StatsBarHorizontal(readingDays)

                    TodayTaskSection(
                        readingDays = readingDays,
                        onCheckClick = { dayIndex -> viewModel.toggleDay(dayIndex) }
                    )

                    BibleVerseSection(
                        text = currentVerseText,
                        ref = currentVerseRef,
                        isLoading = isLoadingVerse,
                        onRefresh = { viewModel.fetchDailyVerse() }
                    )

                    AlarmSection(
                        alarmConfig = alarmConfig,
                        statusText = alarmStatusText,
                        onSave = { enabled, h, m, sound -> viewModel.saveAlarm(enabled, h, m, sound) }
                    )
                }

                // Vertical Divider between columns
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(Color.Gray.copy(0.2f))
                )

                // Right Column - Reporting, search management & progress grid (62% width)
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.62f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📋 የ90 ቀናት እድገት ሪፖርት እና ዳታ አስተዳደር",
                            color = GoldMedium,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Button(
                            onClick = { viewModel.resetPlan() },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonDark),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("እንደገና አስጀምር (Reset)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Progress chart or analytics drawn with custom canvas
                    WeeklyProgressChartCard(readingDays = readingDays)

                    // Data filter and search
                    DataManagementFilterRow(
                        selectedFilter = selectedFilter,
                        searchQuery = searchQuery,
                        onFilterChange = { viewModel.setFilter(it) },
                        onSearchChange = { viewModel.setSearchQuery(it) }
                    )

                    // Progress Checklist Grid
                    DaysProgressGrid(
                        readingDays = readingDays,
                        filter = selectedFilter,
                        searchQuery = searchQuery,
                        onDayToggle = { viewModel.toggleDay(it) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            // Mobile (Compact screen) layout using standard bottom tab navigation
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Fixed top app logo/hero
                HeroHeaderCompact()

                CrossProgressBar(readingDays)

                // Lazy contents switching based on bottom tabs
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    when (activeTab) {
                        0 -> { // Daily portal
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Spacer(Modifier.height(8.dp))

                                StatsBarCompactGrid(readingDays)

                                TodayTaskSection(
                                    readingDays = readingDays,
                                    onCheckClick = { dayIndex -> viewModel.toggleDay(dayIndex) }
                                )

                                BibleVerseSection(
                                    text = currentVerseText,
                                    ref = currentVerseRef,
                                    isLoading = isLoadingVerse,
                                    onRefresh = { viewModel.fetchDailyVerse() }
                                )

                                Spacer(Modifier.height(16.dp))
                            }
                        }
                        1 -> { // Reports & Data Management
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Spacer(Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "የንባብ ዳታና ሪፖርት",
                                        color = GoldMedium,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    TextButton(
                                        onClick = { viewModel.resetPlan() },
                                        colors = ButtonDefaults.textButtonColors(contentColor = CrimsonBright)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("አጽዳ (Reset)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                WeeklyProgressChartCard(readingDays = readingDays)

                                DataManagementFilterRow(
                                    selectedFilter = selectedFilter,
                                    searchQuery = searchQuery,
                                    onFilterChange = { viewModel.setFilter(it) },
                                    onSearchChange = { viewModel.setSearchQuery(it) }
                                )

                                DaysProgressGrid(
                                    readingDays = readingDays,
                                    filter = selectedFilter,
                                    searchQuery = searchQuery,
                                    onDayToggle = { viewModel.toggleDay(it) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        2 -> { // Alarm configuration
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Spacer(Modifier.height(8.dp))

                                AlarmSection(
                                    alarmConfig = alarmConfig,
                                    statusText = alarmStatusText,
                                    onSave = { enabled, h, m, sound -> viewModel.saveAlarm(enabled, h, m, sound) }
                                )

                                AboutPlanCard()

                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: HERO/BRANDING HEADER (COMPACT)
// ==========================================
@Composable
fun HeroHeaderCompact() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DarkAtmosphere, CrimsonDark.copy(0.3f), DarkAtmosphere)
                )
            )
    ) {
        // Decorative background cross "✞" symbol
        Text(
            text = "✞",
            color = Color.Red.copy(0.04f),
            fontSize = 160.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.Center)
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elegant gold bordered logo representing Biblical readings
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .border(2.dp, GoldMedium, CircleShape)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "90",
                    color = GoldMedium,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(16.dp))

            Column {
                Text(
                    text = "በክርስቶስ ተወዳጅነዱ",
                    color = GoldMedium,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "ዮሐንስ ወንጌል 13፥1 · 90 ቀን የንባብ እቅድ",
                    color = Color.LightGray.copy(0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ==========================================
// COMPONENT: HERO SECTION (TABLET/WIDE)
// ==========================================
@Composable
fun HeroSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkAtmosphere),
        border = BorderStroke(1.dp, CrimsonDark.copy(0.4f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "✞",
                color = Color.Red.copy(0.05f),
                fontSize = 220.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.Center)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(3.dp, GoldMedium, CircleShape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "90",
                        color = GoldMedium,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "በክርስቶስ ተወዳጅነዱ",
                    color = GoldMedium,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 3.sp
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "የወደዳቸውን እስከ መጨረሻው ወደዳቸው — ዮሐ 13፥1",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ==========================================
// COMPONENT: STATS GRID
// ==========================================
@Composable
fun StatsBarCompactGrid(readingDays: List<ReadingDay>) {
    val completed = readingDays.count { it.completed }
    val remaining = 90 - completed
    val pct = if (readingDays.isEmpty()) 0 else (completed * 100) / 90

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .border(BorderStroke(1.dp, CrimsonDark.copy(0.2f)), RoundedCornerShape(12.dp)),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatItemCompact(value = "$completed", label = "ተጠናቋል", modifier = Modifier.weight(1f))
        DividerVertical()
        StatItemCompact(value = "90", label = "ጠቅላላ", modifier = Modifier.weight(1f))
        DividerVertical()
        StatItemCompact(value = "$remaining", label = "ቀሪ", modifier = Modifier.weight(1f))
        DividerVertical()
        StatItemCompact(value = "$pct%", label = "እድገት", modifier = Modifier.weight(1f))
    }
}

@Composable
fun StatsBarHorizontal(readingDays: List<ReadingDay>) {
    val completed = readingDays.count { it.completed }
    val remaining = 90 - completed
    val pct = if (readingDays.isEmpty()) 0 else (completed * 100) / 90

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .border(BorderStroke(1.dp, CrimsonDark.copy(0.2f)), RoundedCornerShape(12.dp)),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatItemCompact(value = "$completed", label = "ተጠናቋል", modifier = Modifier.weight(1f))
        DividerVertical()
        StatItemCompact(value = "90", label = "ጠቅላላ", modifier = Modifier.weight(1f))
        DividerVertical()
        StatItemCompact(value = "$remaining", label = "ቀሪ", modifier = Modifier.weight(1f))
        DividerVertical()
        StatItemCompact(value = "$pct%", label = "እድገት", modifier = Modifier.weight(1f))
    }
}

@Composable
fun StatItemCompact(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = value,
            color = GoldMedium,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.LightGray.copy(0.4f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun DividerVertical() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(Color.Gray.copy(0.12f))
    )
}

// ==========================================
// COMPONENT: PROGRESS BAR with CHRISTIAN CROSS MARKER
// ==========================================
@Composable
fun CrossProgressBar(readingDays: List<ReadingDay>) {
    val completed = readingDays.count { it.completed }
    val pctFloat = if (readingDays.isEmpty()) 0.0f else completed.toFloat() / 90.0f
    val pctPct = (pctFloat * 100).toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ጠቅላላ የንባብ እድገት",
                color = Color.LightGray.copy(0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "$pctPct% ተጠቃሏል",
                color = GoldMedium,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(6.dp))

        // Custom drawn progression bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color.Gray.copy(0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(if (pctFloat > 0.01f) pctFloat else 0.01f)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(CrimsonDark, GoldDark)
                        )
                    )
            )
        }
    }
}

// ==========================================
// COMPONENT: BIBLE VERSE (AI ASSISTED COOPERATIVE MODE)
// ==========================================
@Composable
fun BibleVerseSection(
    text: String,
    ref: String,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CrimsonDark.copy(0.12f)),
        border = BorderStroke(1.dp, CrimsonDark.copy(0.35f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "✦ ዕለታዊ ጥቅስ (Gemini AI)",
                    color = GoldDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(Modifier.height(10.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = GoldMedium,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    Text(
                        text = "“ $text ”",
                        color = TextLight,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 22.sp
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = ref,
                        color = GoldMedium,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Verse",
                            tint = GoldMedium,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: TODAY'S ACTIVE TASK / PILL CHECKLIST
// ==========================================
@Composable
fun TodayTaskSection(
    readingDays: List<ReadingDay>,
    onCheckClick: (Int) -> Unit
) {
    // Collect progress
    val nextPendingDay = readingDays.find { !it.completed }

    if (nextPendingDay == null) {
        // Achievement card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CrimsonMedium.copy(0.15f)),
            border = BorderStroke(1.dp, GoldMedium.copy(0.4f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎉 ክብር ለእግዚአብሔር ይሁን!",
                    color = GoldMedium,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "የ90 ቀናት የአዲስ ኪዳን ንባብ ሙሉ በሙሉ አጠናቀሃል። “የወደዳቸውን እስከ መጨረሻው ወደዳቸው።” (ዮሐ 13:1)",
                    color = TextLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
    } else {
        val nextIndex = nextPendingDay.dayIndex
        val passage = nextPendingDay.passage
        val desc = nextPendingDay.description

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, Color.Gray.copy(0.12f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "✞ የዛሬው ተግባር (Today's Reading)",
                    color = CrimsonBright,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "ቀን ${nextIndex + 1} / 90",
                    color = Color.LightGray.copy(0.4f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = passage,
                    color = TextLight,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = desc,
                    color = Color.LightGray.copy(0.6f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(Modifier.height(14.dp))

                // Chapter checklist pills to make reading visual & realistic
                val chapters = passage.split(Regex("[·&,]")).map { it.trim() }.filter { it.isNotEmpty() }
                
                // Track selected sub-chapter interactive states using simple mutable list
                val checkedChaptersMap = remember { mutableStateMapOf<String, Boolean>() }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chapters.forEach { chap ->
                        val isChapChecked = checkedChaptersMap[chap] ?: false
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isChapChecked) CrimsonMedium else DarkSurface)
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isChapChecked) CrimsonBright else Color.Gray.copy(0.2f)
                                    ),
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { checkedChaptersMap[chap] = !isChapChecked }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isChapChecked) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Checked",
                                        tint = GoldLight,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text(
                                    text = chap,
                                    color = if (isChapChecked) GoldLight else Color.LightGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        // Clear active sub-pills
                        checkedChaptersMap.clear()
                        onCheckClick(nextIndex)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonColors(
                        containerColor = CrimsonDark,
                        contentColor = TextLight,
                        disabledContainerColor = CrimsonDark.copy(0.3f),
                        disabledContentColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Done")
                    Spacer(Modifier.width(8.dp))
                    Text(text = "ይህን ቀን አነበብኩ (Complete)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: ALARM UNIT with local Tone Simulation
// ==========================================
@Composable
fun AlarmSection(
    alarmConfig: AlarmConfig,
    statusText: String,
    onSave: (Boolean, Int, Int, String) -> Unit
) {
    var hourText by remember { mutableStateOf(alarmConfig.timeHour.toString()) }
    var minuteText by remember { mutableStateOf(alarmConfig.timeMinute.toString()) }
    var selectedSound by remember { mutableStateOf(alarmConfig.sound) }
    var isEnabled by remember { mutableStateOf(alarmConfig.enabled) }

    // Sync state when DB updates
    LaunchedEffect(alarmConfig) {
        hourText = alarmConfig.timeHour.toString()
        minuteText = alarmConfig.timeMinute.toString()
        selectedSound = alarmConfig.sound
        isEnabled = alarmConfig.enabled
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, Color.Gray.copy(0.1f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "⏰ እለታዊ የንባብ ማስታወሻ (Reminders)",
                color = Color.LightGray.copy(0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time fields
                OutlinedTextField(
                    value = hourText,
                    onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) hourText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(60.dp),
                    label = { Text("ሰዓት", fontSize = 10.sp, color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldMedium,
                        unfocusedBorderColor = Color.Gray.copy(0.3f)
                    ),
                    singleLine = true
                )

                Spacer(Modifier.width(8.dp))
                Text(":", color = Color.Gray, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))

                OutlinedTextField(
                    value = minuteText,
                    onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) minuteText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(60.dp),
                    label = { Text("ደቂቃ", fontSize = 10.sp, color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldMedium,
                        unfocusedBorderColor = Color.Gray.copy(0.3f)
                    ),
                    singleLine = true
                )

                Spacer(Modifier.width(12.dp))

                // Sound select
                var expanded by remember { mutableStateOf(false) }
                Box {
                    Button(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = when (selectedSound) {
                                "gentle" -> "🔔 ገር ደወል"
                                "church" -> "⛪ ቤተክርስቲያን"
                                else -> "📢 ዲጂታል"
                            },
                            fontSize = 12.sp,
                            color = TextLight
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("🔔 ገር ደወል (Gentle Ring)", color = TextLight, fontSize = 12.sp) },
                            onClick = {
                                selectedSound = "gentle"
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("⛪ ቤተክርስቲያን (Church Bell)", color = TextLight, fontSize = 12.sp) },
                            onClick = {
                                selectedSound = "church"
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("📢 ዲጂታል (Digital Beep)", color = TextLight, fontSize = 12.sp) },
                            onClick = {
                                selectedSound = "digital"
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val h = hourText.toIntOrNull() ?: 7
                            val m = minuteText.toIntOrNull() ?: 0
                            onSave(true, h, m, selectedSound)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonDark),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("እሺ አስጀምር", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextLight)
                    }

                    OutlinedButton(
                        onClick = {
                            val h = hourText.toIntOrNull() ?: 7
                            val m = minuteText.toIntOrNull() ?: 0
                            onSave(false, h, m, selectedSound)
                        },
                        border = BorderStroke(1.dp, Color.Gray.copy(0.3f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("አጥፋ", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                    }
                }

                // Bell sound test simulator button
                IconButton(
                    onClick = {
                        try {
                            val toneType = when (selectedSound) {
                                "church" -> ToneGenerator.TONE_CDMA_PIP
                                "digital" -> ToneGenerator.TONE_SUP_DIAL
                                else -> ToneGenerator.TONE_DTMF_D
                            }
                            val alarmTone = ToneGenerator(AudioManager.STREAM_ALARM, 60)
                            alarmTone.startTone(toneType, 400)
                        } catch (e: Exception) {
                            // Suppress sound on unsupported environments
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(0.1f))
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Test Sound",
                        tint = GoldMedium,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = statusText,
                color = Color.LightGray.copy(0.45f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray.copy(0.08f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

// ==========================================
// COMPONENT: ABOUT STUDY PLAN CAPTURE CARD
// ==========================================
@Composable
fun AboutPlanCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CrimsonDark.copy(0.08f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CrimsonDark.copy(0.25f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "የእቅዱ ዋና ዓላማ",
                color = GoldMedium,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "ይህ ዕቅድ በየቀኑ በአማካኝ የ 3 ምዕራፍ ምንባቦችን በማንበብ ሙሉውን የአዲስ ኪዳን መጽሐፍት በ 90 ቀናት ውስጥ ለማጠናቀቅ የሚያስችል የእምነትና የጽናት መከታተያ ነው።\n\n“ሰው ከሥጋ መብል ብቻ አይኖርም፤ ነገር ግን ከእግዚአብሔር አፍ ከሚወጣው ቃል ሁሉ እንጂ።” (ማቴ 4:4)",
                color = TextLight.copy(0.85f),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

// ==========================================
// COMPONENT: VISUAL REPORTING CUSTOM CANVAS GRAPH (WEEKLY PROGRESS)
// ==========================================
@Composable
fun WeeklyProgressChartCard(readingDays: List<ReadingDay>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, Color.Gray.copy(0.12f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "📊 ሳምንታዊ የንባብ ሪፖርት (የአጠናቀቁት ቀናት ብዛት በየሳምንቱ)",
                color = GoldMedium,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(16.dp))

            // Compute actual completions per week (90 days total = 13 weeks of 7 days each)
            val weeklyCompletions = remember(readingDays) {
                val list = IntArray(13) { 0 }
                readingDays.forEach { day ->
                    val weekIdx = day.dayIndex / 7
                    if (weekIdx in 0..12 && day.completed) {
                        list[weekIdx]++
                    }
                }
                list
            }

            // Draw our beautiful visual graph cleanly inside canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(horizontal = 8.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val barCount = 13
                    val paddingHorizontal = 12f
                    val barWidth = (w - (paddingHorizontal * (barCount - 1))) / barCount

                    // Draw baseline grid lines
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val yVal = h - (h / gridLines) * i
                        drawLine(
                            color = Color.Gray.copy(0.1f),
                            start = Offset(0f, yVal),
                            end = Offset(w, yVal),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }

                    // Render Bars
                    for (week in 0 until barCount) {
                        val completedCount = weeklyCompletions[week]
                        val barHeightRatio = completedCount / 7.0f
                        val currentBarHeight = h * barHeightRatio

                        val xStart = week * (barWidth + paddingHorizontal)
                        val yStart = h - currentBarHeight

                        // Elegant Crimson to Gold Gradient bar fill
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(GoldMedium, CrimsonBright)
                            ),
                            topLeft = Offset(xStart, if (yStart < h - 4) yStart else h - 4),
                            size = Size(barWidth, if (currentBarHeight > 4) currentBarHeight else 4f),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Labels under graph
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (w in 1..13) {
                    Text(
                        text = "ሳ$w",
                        color = Color.LightGray.copy(0.4f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(20.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: SEARCH AND DATA MANAGE ROW
// ==========================================
@Composable
fun DataManagementFilterRow(
    selectedFilter: String,
    searchQuery: String,
    onFilterChange: (String) -> Unit,
    onSearchChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("ምዕራፍ ፈልግ (ለምሳሌ: ማቴዎስ)", color = Color.Gray, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldDark,
                unfocusedBorderColor = Color.Gray.copy(0.2f),
                focusedContainerColor = DarkCard,
                unfocusedContainerColor = DarkCard
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // Filter pills row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(
                "All" to "ሁሉም (90)",
                "Completed" to "ያለቁ",
                "Pending" to "የሚቀሩ"
            )

            filters.forEach { (key, display) ->
                val isSelected = selectedFilter == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) GoldMedium else DarkCard)
                        .border(
                            BorderStroke(
                                1.dp,
                                if (isSelected) GoldMedium else Color.Gray.copy(0.12f)
                            ),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { onFilterChange(key) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = display,
                        color = if (isSelected) DarkAtmosphere else Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: GRID CHECKLIST OF ALL 90 DAYS
// ==========================================
@Composable
fun DaysProgressGrid(
    readingDays: List<ReadingDay>,
    filter: String,
    searchQuery: String,
    onDayToggle: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Apply search and filter criteria
    val filteredDays = remember(readingDays, filter, searchQuery) {
        readingDays.filter { day ->
            val matchesFilter = when (filter) {
                "Completed" -> day.completed
                "Pending" -> !day.completed
                else -> true
            }
            val matchesSearch = if (searchQuery.trim().isEmpty()) {
                true
            } else {
                day.passage.contains(searchQuery.trim(), ignoreCase = true) ||
                        day.description.contains(searchQuery.trim(), ignoreCase = true)
            }
            matchesFilter && matchesSearch
        }
    }

    if (filteredDays.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "ምንም የሚዛመድ የንባብ ቀን አልተገኘም!",
                color = Color.Gray,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            modifier = modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredDays, key = { it.dayIndex }) { day ->
                val isCompleted = day.completed
                val dayNum = day.dayIndex + 1

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDayToggle(day.dayIndex) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCompleted) CrimsonDark.copy(0.15f) else DarkCard
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isCompleted) CrimsonBright.copy(0.35f) else Color.Gray.copy(0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Check circle
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isCompleted) CrimsonBright else DarkSurface)
                                .border(
                                    BorderStroke(1.5.dp, CrimsonBright.copy(0.6f)),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Done",
                                    tint = TextLight,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "ቀን $dayNum",
                                color = if (isCompleted) GoldMedium else Color.LightGray.copy(0.5f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = day.passage,
                                color = TextLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
