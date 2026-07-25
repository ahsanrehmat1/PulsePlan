package com.ahsanrehmat.pulseplan.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ahsanrehmat.pulseplan.domain.ProgressSnapshot
import com.ahsanrehmat.pulseplan.domain.WorkoutHistoryDay
import com.ahsanrehmat.pulseplan.ui.theme.Canvas
import com.ahsanrehmat.pulseplan.ui.theme.Ink
import com.ahsanrehmat.pulseplan.ui.theme.InkSoft
import com.ahsanrehmat.pulseplan.ui.theme.Line
import com.ahsanrehmat.pulseplan.ui.theme.Muted
import com.ahsanrehmat.pulseplan.ui.theme.PulseGreen
import com.ahsanrehmat.pulseplan.ui.theme.Warm
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
                        text = "Progress & history",
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                ProgressHero(progress = progress)
            }

            item {
                Text(
                    text = "LAST 28 DAYS",
                    color = Muted,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            items(
                items = progress.history,
                key = WorkoutHistoryDay::date,
            ) { day ->
                HistoryDayCard(day = day)
            }

            item {
                Text(
                    text = "Recovery days never break your streak. A workout due today stays pending until the day is over.",
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
                        .size(46.dp)
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
                        text = "Consecutive scheduled workouts completed",
                        color = Color(0xFFADB7A8),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProgressMetric(
                    value = "${progress.completedThisWeek}/${progress.plannedThisWeek}",
                    label = "This week",
                    modifier = Modifier.weight(1f),
                )
                ProgressMetric(
                    value = progress.completedInHistory.toString(),
                    label = "Last 28 days",
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
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
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
            )
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
                        imageVector = if (day.isWorkoutDay) {
                            Icons.Default.Check
                        } else {
                            Icons.Default.Spa
                        },
                        contentDescription = null,
                        tint = appearance.accentColor,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = day.date.format(DAY_FORMAT) + if (day.isToday) " · Today" else "",
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

private data class HistoryAppearance(
    val label: String,
    val supportingText: String,
    val containerColor: Color,
    val accentColor: Color,
)

private val DAY_FORMAT = DateTimeFormatter.ofPattern("EEE, MMM d")
