package com.ahsanrehmat.pulseplan.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ahsanrehmat.pulseplan.domain.ProgressMilestone
import com.ahsanrehmat.pulseplan.domain.ProgressSnapshot
import com.ahsanrehmat.pulseplan.domain.WeeklyProgress
import com.ahsanrehmat.pulseplan.domain.WorkoutHistoryDay
import com.ahsanrehmat.pulseplan.ui.theme.Canvas
import com.ahsanrehmat.pulseplan.ui.theme.Ink
import com.ahsanrehmat.pulseplan.ui.theme.InkSoft
import com.ahsanrehmat.pulseplan.ui.theme.Line
import com.ahsanrehmat.pulseplan.ui.theme.Muted
import com.ahsanrehmat.pulseplan.ui.theme.PulseGreen
import com.ahsanrehmat.pulseplan.ui.theme.Warm
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressHistoryScreen(
    progress: ProgressSnapshot,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = Canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Progress insights",
                        fontWeight = FontWeight.Black,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to dashboard",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Canvas,
                    titleContentColor = Ink,
                    navigationIconContentColor = Ink,
                ),
                modifier = Modifier.statusBarsPadding(),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                ProgressHero(progress = progress)
            }

            item {
                InsightMetrics(progress = progress)
            }

            item {
                WeeklyTrendCard(weeks = progress.weeklyProgress)
            }

            item {
                ProgressCalendar(progress = progress)
            }

            item {
                MilestonesCard(milestones = progress.milestones)
            }

            item {
                SectionTitle(
                    eyebrow = "RECENT ACTIVITY",
                    title = "Your latest seven days",
                )
            }

            items(
                items = progress.history.take(7),
                key = WorkoutHistoryDay::date,
            ) { day ->
                HistoryDayCard(day = day)
            }

            item {
                Text(
                    text = "Recovery days do not break streaks. Today's workout stays open until midnight.",
                    color = Muted,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun ProgressHero(progress: ProgressSnapshot) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Ink),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(PulseGreen.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = PulseGreen,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "${progress.currentStreak} workout streak",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "Keep completing the workouts your plan schedules",
                        color = Color(0xFFADB7A8),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ProgressMetric(
                    value = "${progress.completedThisWeek}/${progress.plannedThisWeek}",
                    label = "This week",
                    modifier = Modifier.weight(1f),
                )
                ProgressMetric(
                    value = progress.bestStreak.toString(),
                    label = "Best streak",
                    modifier = Modifier.weight(1f),
                )
                ProgressMetric(
                    value = progress.totalCompletedWorkouts.toString(),
                    label = "Past year",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ProgressMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF30382F),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                color = PulseGreen,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = label,
                color = Color(0xFFADB7A8),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun InsightMetrics(progress: ProgressSnapshot) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(
            eyebrow = "AT A GLANCE",
            title = "Your honest progress",
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            InsightMetricCard(
                value = progress.totalCompletedExercises.toString(),
                label = "Exercises, past year",
                modifier = Modifier.weight(1f),
            )
            InsightMetricCard(
                value = "${progress.completionRate}%",
                label = "28-day completion",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun InsightMetricCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(
                Icons.Default.TrendingUp,
                contentDescription = null,
                tint = InkSoft,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = value,
                color = Ink,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = label,
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun WeeklyTrendCard(weeks: List<WeeklyProgress>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(22.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
    ) {
        Column(Modifier.padding(18.dp)) {
            SectionTitle(
                eyebrow = "SIX-WEEK TREND",
                title = "Consistency by week",
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                weeks.forEach { week ->
                    val fraction = if (week.plannedWorkouts == 0) {
                        0f
                    } else {
                        week.completedWorkouts.toFloat() / week.plannedWorkouts
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "${week.completedWorkouts}/${week.plannedWorkouts}",
                            color = InkSoft,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Spacer(Modifier.height(5.dp))
                        Box(
                            modifier = Modifier
                                .width(26.dp)
                                .height(84.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Canvas),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((84f * fraction).coerceAtLeast(5f).dp)
                                    .background(
                                        if (fraction >= 1f) PulseGreen else InkSoft,
                                    ),
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = week.weekStart.format(WEEK_FORMAT),
                            color = Muted,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressCalendar(progress: ProgressSnapshot) {
    if (progress.calendarHistory.isEmpty()) return

    val historyByDate = remember(progress.calendarHistory) {
        progress.calendarHistory.associateBy(WorkoutHistoryDay::date)
    }
    val today = progress.calendarHistory.firstOrNull(WorkoutHistoryDay::isToday)
        ?.date
        ?: progress.calendarHistory.first().date
    val earliestDate = progress.calendarHistory.last().date
    val earliestMonth = YearMonth.from(earliestDate)
    val latestMonth = YearMonth.from(today)
    var visibleMonth by remember(progress.calendarHistory) {
        mutableStateOf(latestMonth)
    }
    var selectedDate by remember(progress.calendarHistory) {
        mutableStateOf(today)
    }
    val selectedDay = historyByDate[selectedDate]

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(22.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
    ) {
        Column(Modifier.padding(18.dp)) {
            SectionTitle(
                eyebrow = "WORKOUT CALENDAR",
                title = "Choose a day to review",
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        val previous = visibleMonth.minusMonths(1)
                        visibleMonth = previous
                        selectedDate = latestAvailableDate(
                            month = previous,
                            today = today,
                            historyByDate = historyByDate,
                        )
                    },
                    enabled = visibleMonth > earliestMonth,
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Previous month",
                    )
                }
                Text(
                    text = visibleMonth.format(MONTH_FORMAT),
                    color = Ink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(
                    onClick = {
                        val next = visibleMonth.plusMonths(1)
                        visibleMonth = next
                        selectedDate = latestAvailableDate(
                            month = next,
                            today = today,
                            historyByDate = historyByDate,
                        )
                    },
                    enabled = visibleMonth < latestMonth,
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Next month",
                    )
                }
            }
            CalendarWeekdayHeader()
            Spacer(Modifier.height(6.dp))
            CalendarMonthGrid(
                month = visibleMonth,
                today = today,
                selectedDate = selectedDate,
                historyByDate = historyByDate,
                onSelect = { selectedDate = it },
            )
            Spacer(Modifier.height(12.dp))
            CalendarLegend()
            selectedDay?.let { day ->
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = Line,
                )
                SelectedDayDetails(day = day)
            }
        }
    }
}

@Composable
private fun CalendarWeekdayHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        WEEKDAY_LABELS.forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = Muted,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CalendarMonthGrid(
    month: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate,
    historyByDate: Map<LocalDate, WorkoutHistoryDay>,
    onSelect: (LocalDate) -> Unit,
) {
    val cells = remember(month) {
        buildList<LocalDate?> {
            repeat(month.atDay(1).dayOfWeek.value - 1) { add(null) }
            repeat(month.lengthOfMonth()) { index ->
                add(month.atDay(index + 1))
            }
            while (size % 7 != 0) add(null)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                week.forEach { date ->
                    if (date == null) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                        )
                    } else {
                        CalendarDayCell(
                            date = date,
                            day = historyByDate[date],
                            isFuture = date > today,
                            isSelected = date == selectedDate,
                            onClick = { onSelect(date) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    day: WorkoutHistoryDay?,
    isFuture: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = calendarStatus(day = day, isFuture = isFuture)
    val containerColor = when {
        isFuture || day == null -> Canvas.copy(alpha = 0.55f)
        !day.isWorkoutDay -> Color(0xFFECEF_E8)
        day.isComplete -> PulseGreen.copy(alpha = 0.55f)
        day.completedExercises > 0 -> Warm.copy(alpha = 0.55f)
        else -> Color.White
    }
    val borderColor = when {
        isSelected -> Ink
        day?.isToday == true -> PulseGreen
        else -> Line
    }
    val dotColor = when {
        day == null || isFuture -> Color.Transparent
        !day.isWorkoutDay -> InkSoft
        day.isComplete -> Ink
        day.completedExercises > 0 -> InkSoft
        else -> Muted.copy(alpha = 0.45f)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(enabled = day != null && !isFuture, onClick = onClick)
            .semantics {
                contentDescription = "${date.format(FULL_DATE_FORMAT)}: $status"
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                color = if (isFuture) Muted.copy(alpha = 0.55f) else Ink,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (day?.isToday == true) FontWeight.Black else FontWeight.Medium,
            )
            Spacer(Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(dotColor, CircleShape),
            )
        }
    }
}

@Composable
private fun CalendarLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CalendarLegendItem(
            color = PulseGreen.copy(alpha = 0.65f),
            label = "Complete",
        )
        CalendarLegendItem(
            color = Warm.copy(alpha = 0.75f),
            label = "Partial",
        )
        CalendarLegendItem(
            color = InkSoft.copy(alpha = 0.4f),
            label = "Recovery",
        )
    }
}

@Composable
private fun CalendarLegendItem(
    color: Color,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(color, CircleShape),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            color = Muted,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun SelectedDayDetails(day: WorkoutHistoryDay) {
    val status = calendarStatus(day = day, isFuture = false)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = day.date.format(SELECTED_DATE_FORMAT),
                    color = Ink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (day.isWorkoutDay) day.title else "Recovery day",
                    color = Muted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Surface(
                color = if (day.isComplete) {
                    PulseGreen.copy(alpha = 0.4f)
                } else {
                    Canvas
                },
                shape = CircleShape,
            ) {
                Text(
                    text = status,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    color = InkSoft,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        if (day.isWorkoutDay) {
            Text(
                text = "${day.completedExercises} of ${day.totalExercises} exercises completed",
                color = InkSoft,
                style = MaterialTheme.typography.bodyMedium,
            )
            day.exercises.forEach { exercise ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                color = if (exercise.isCompleted) {
                                    PulseGreen.copy(alpha = 0.5f)
                                } else {
                                    Canvas
                                },
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (exercise.isCompleted) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = Ink,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = exercise.name,
                            color = Ink,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = exercise.prescription,
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        exercise.resultSummary?.let { result ->
                            Text(
                                text = "Logged: $result",
                                color = Ink,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        } else {
            Text(
                text = "Recovery days support consistency and never count as missed workouts.",
                color = InkSoft,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun MilestonesCard(milestones: List<ProgressMilestone>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Warm.copy(alpha = 0.22f)),
        shape = RoundedCornerShape(22.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
    ) {
        Column(Modifier.padding(18.dp)) {
            SectionTitle(
                eyebrow = "MILESTONES",
                title = "Consistency worth celebrating",
            )
            Spacer(Modifier.height(14.dp))
            milestones.forEachIndexed { index, milestone ->
                MilestoneRow(milestone = milestone)
                if (index != milestones.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 14.dp),
                        color = Line,
                    )
                }
            }
        }
    }
}

@Composable
private fun MilestoneRow(milestone: ProgressMilestone) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = if (milestone.isUnlocked) {
                        PulseGreen.copy(alpha = 0.5f)
                    } else {
                        Color.White
                    },
                    shape = RoundedCornerShape(13.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (milestone.isUnlocked) {
                    Icons.Default.EmojiEvents
                } else {
                    Icons.Default.FitnessCenter
                },
                contentDescription = null,
                tint = if (milestone.isUnlocked) Ink else Muted,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = milestone.title,
                    color = Ink,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (milestone.isUnlocked) {
                        "Unlocked"
                    } else {
                        milestone.progressLabel
                    },
                    color = InkSoft,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = milestone.description,
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(milestone.progressFraction)
                        .height(6.dp)
                        .background(
                            if (milestone.isUnlocked) PulseGreen else InkSoft,
                        ),
                )
            }
        }
    }
}

@Composable
private fun HistoryDayCard(day: WorkoutHistoryDay) {
    val appearance = when {
        !day.isWorkoutDay -> HistoryAppearance(
            label = "Recovery day",
            supportingText = "Rest and gentle movement",
            containerColor = Warm.copy(alpha = 0.22f),
            accentColor = InkSoft,
        )
        day.isComplete -> HistoryAppearance(
            label = "Complete",
            supportingText = "${day.totalExercises} of ${day.totalExercises} exercises",
            containerColor = PulseGreen.copy(alpha = 0.24f),
            accentColor = Ink,
        )
        day.completedExercises > 0 -> HistoryAppearance(
            label = if (day.isToday) "In progress" else "Partially completed",
            supportingText = "${day.completedExercises} of ${day.totalExercises} exercises",
            containerColor = Color.White,
            accentColor = Ink,
        )
        day.isToday -> HistoryAppearance(
            label = "Ready for today",
            supportingText = "0 of ${day.totalExercises} exercises",
            containerColor = Color.White,
            accentColor = Ink,
        )
        else -> HistoryAppearance(
            label = "Not completed",
            supportingText = "0 of ${day.totalExercises} exercises",
            containerColor = Color.White,
            accentColor = Muted,
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = appearance.containerColor),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (day.isToday) PulseGreen else Line,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            color = appearance.accentColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = when {
                            !day.isWorkoutDay -> Icons.Default.Spa
                            day.isComplete -> Icons.Default.Check
                            else -> Icons.Default.FitnessCenter
                        },
                        contentDescription = null,
                        tint = appearance.accentColor,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = day.date.format(DAY_FORMAT) +
                            if (day.isToday) " - Today" else "",
                        color = Ink,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (day.isWorkoutDay) day.title else appearance.supportingText,
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = appearance.label,
                    color = appearance.accentColor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                if (day.isWorkoutDay) {
                    Text(
                        text = appearance.supportingText,
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(
    eyebrow: String,
    title: String,
) {
    Column {
        Text(
            text = eyebrow,
            color = Muted,
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = title,
            color = Ink,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
        )
    }
}

private fun latestAvailableDate(
    month: YearMonth,
    today: LocalDate,
    historyByDate: Map<LocalDate, WorkoutHistoryDay>,
): LocalDate {
    val lastDay = minOf(month.atEndOfMonth(), today)
    return generateSequence(lastDay) { date ->
        date.minusDays(1).takeIf { YearMonth.from(it) == month }
    }.firstOrNull(historyByDate::containsKey) ?: lastDay
}

private fun calendarStatus(
    day: WorkoutHistoryDay?,
    isFuture: Boolean,
): String = when {
    isFuture -> "Upcoming"
    day == null -> "Outside saved history"
    !day.isWorkoutDay -> "Recovery"
    day.isComplete -> "Complete"
    day.completedExercises > 0 && day.isToday -> "In progress"
    day.completedExercises > 0 -> "Partial"
    day.isToday -> "Ready today"
    else -> "Not completed"
}

private data class HistoryAppearance(
    val label: String,
    val supportingText: String,
    val containerColor: Color,
    val accentColor: Color,
)

private val WEEKDAY_LABELS = listOf("M", "T", "W", "T", "F", "S", "S")
private val DAY_FORMAT = DateTimeFormatter.ofPattern("EEE, MMM d")
private val FULL_DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy")
private val SELECTED_DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, MMMM d")
private val MONTH_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy")
private val WEEK_FORMAT = DateTimeFormatter.ofPattern("MMM d")
