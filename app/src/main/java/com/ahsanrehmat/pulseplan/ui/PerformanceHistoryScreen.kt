package com.ahsanrehmat.pulseplan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ahsanrehmat.pulseplan.domain.DatedExerciseResult
import com.ahsanrehmat.pulseplan.domain.ExercisePerformanceSummary
import com.ahsanrehmat.pulseplan.domain.PerformanceSnapshot
import com.ahsanrehmat.pulseplan.domain.PerformanceTracker
import com.ahsanrehmat.pulseplan.domain.ProgressionCoach
import com.ahsanrehmat.pulseplan.domain.ProgressionSuggestion
import com.ahsanrehmat.pulseplan.model.FitnessGoal
import com.ahsanrehmat.pulseplan.ui.theme.Canvas
import com.ahsanrehmat.pulseplan.ui.theme.Ink
import com.ahsanrehmat.pulseplan.ui.theme.InkSoft
import com.ahsanrehmat.pulseplan.ui.theme.Line
import com.ahsanrehmat.pulseplan.ui.theme.Muted
import com.ahsanrehmat.pulseplan.ui.theme.PulseGreen
import com.ahsanrehmat.pulseplan.ui.theme.Warm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceHistoryScreen(
    performance: PerformanceSnapshot,
    fitnessGoal: FitnessGoal,
    onDeleteResult: (java.time.LocalDate, String) -> Unit,
    onBack: () -> Unit,
) {
    var selectedExerciseId by remember(performance.exercises) {
        mutableStateOf(performance.exercises.firstOrNull()?.exerciseId)
    }
    val selected = performance.exercises.firstOrNull {
        it.exerciseId == selectedExerciseId
    }
    var pendingDelete by remember { mutableStateOf<DatedExerciseResult?>(null) }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this result?") },
            text = {
                Text(
                    "This removes the logged performance, effort, and note. " +
                        "The exercise completion remains unchanged.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteResult(entry.date, entry.result.exerciseId)
                        pendingDelete = null
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Keep result")
                }
            },
        )
    }

    Scaffold(
        containerColor = Canvas,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Performance log",
                        color = Ink,
                        fontWeight = FontWeight.Black,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to dashboard",
                            tint = Ink,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Canvas),
                modifier = Modifier.statusBarsPadding(),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                PerformanceHero(performance)
            }

            if (performance.exercises.isEmpty()) {
                item {
                    EmptyPerformanceCard()
                }
            } else {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionTitle(
                            eyebrow = "EXERCISE HISTORY",
                            title = "Choose an exercise",
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(
                                items = performance.exercises,
                                key = ExercisePerformanceSummary::exerciseId,
                            ) { exercise ->
                                FilterChip(
                                    selected = exercise.exerciseId == selectedExerciseId,
                                    onClick = {
                                        selectedExerciseId = exercise.exerciseId
                                    },
                                    label = { Text(exercise.exerciseName) },
                                )
                            }
                        }
                    }
                }

                selected?.let { exercise ->
                    item {
                        ExerciseSummaryCard(exercise)
                    }
                    ProgressionCoach
                        .suggest(fitnessGoal, exercise.latest.result)
                        ?.let { suggestion ->
                            item {
                                ProgressionSuggestionCard(suggestion)
                            }
                        }
                    item {
                        ResultTrendCard(exercise)
                    }
                    item {
                        SectionTitle(
                            eyebrow = "LOGGED RESULTS",
                            title = "${exercise.exerciseName} history",
                        )
                    }
                    items(
                        items = exercise.history,
                        key = { result ->
                            "${result.date}-${result.result.loggedAtEpochMillis}"
                        },
                    ) { result ->
                        ResultHistoryCard(
                            entry = result,
                            onDelete = { pendingDelete = result },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressionSuggestionCard(suggestion: ProgressionSuggestion) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = PulseGreen.copy(alpha = 0.16f),
        ),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PulseGreen),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "NEXT TARGET",
                color = Muted,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = suggestion.title,
                color = Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = suggestion.targetLabel,
                color = Ink,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = suggestion.explanation,
                color = InkSoft,
            )
            Text(
                text = "This is an optional training suggestion, not medical advice. " +
                    "Do not increase the target if form or comfort would suffer.",
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun PerformanceHero(performance: PerformanceSnapshot) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Ink),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(PulseGreen),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Ink)
                }
                Column {
                    Text(
                        text = "Your measurable progress",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "Based only on results you record",
                        color = Color(0xFFB6BEB2),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HeroMetric(
                    value = performance.totalLoggedResults.toString(),
                    label = "Results",
                    modifier = Modifier.weight(1f),
                )
                HeroMetric(
                    value = performance.exercisesTracked.toString(),
                    label = "Exercises",
                    modifier = Modifier.weight(1f),
                )
                HeroMetric(
                    value = performance.personalBestMoments.toString(),
                    label = "Best moments",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HeroMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color(0xFF2D352E),
        shape = RoundedCornerShape(17.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 13.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                color = PulseGreen,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = label,
                color = Color(0xFFB6BEB2),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun EmptyPerformanceCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = Muted,
                modifier = Modifier.size(42.dp),
            )
            Text(
                text = "Your first result starts here",
                color = Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Start a workout and log reps, time, or distance. " +
                    "Completing without numbers still works.",
                color = InkSoft,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ExerciseSummaryCard(summary: ExercisePerformanceSummary) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Warm.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Ink)
                Text(
                    text = summary.exerciseName,
                    color = Ink,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SummaryMetric(
                    label = "LATEST",
                    value = PerformanceTracker.resultLabel(summary.latest.result),
                    supporting = PerformanceTracker.shortDate(summary.latest.date),
                    modifier = Modifier.weight(1f),
                )
                SummaryMetric(
                    label = "PERSONAL BEST",
                    value = summary.personalBest
                        ?.let { PerformanceTracker.resultLabel(it.result) }
                        ?: "Not measured yet",
                    supporting = summary.personalBest
                        ?.let { PerformanceTracker.shortDate(it.date) }
                        .orEmpty(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    supporting: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(17.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(label, color = Muted, style = MaterialTheme.typography.labelSmall)
            Text(
                text = value,
                color = Ink,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (supporting.isNotBlank()) {
                Text(supporting, color = InkSoft, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ResultTrendCard(summary: ExercisePerformanceSummary) {
    val results = summary.history
        .filter {
            it.result.metricType == summary.latest.result.metricType &&
                PerformanceTracker.chartValue(it.result) > 0f
        }
        .take(6)
        .asReversed()
    val maxValue = results.maxOfOrNull { PerformanceTracker.chartValue(it.result) }
        ?.coerceAtLeast(1f)
        ?: 1f

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionTitle(
                eyebrow = "LAST SIX RESULTS",
                title = "Performance trend",
            )
            if (results.isEmpty()) {
                Text(
                    text = "Add reps, time, or distance to create this chart.",
                    color = InkSoft,
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    results.forEach { entry ->
                        val value = PerformanceTracker.chartValue(entry.result)
                        val barHeight = (value / maxValue * 105f).coerceAtLeast(10f)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = PerformanceTracker.resultLabel(entry.result),
                                color = Ink,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                            )
                            Spacer(Modifier.height(5.dp))
                            Box(
                                modifier = Modifier
                                    .width(25.dp)
                                    .height(barHeight.dp)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(
                                        if (entry.isPersonalBest) PulseGreen else Line,
                                    ),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = PerformanceTracker.shortDate(entry.date),
                                color = Muted,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultHistoryCard(
    entry: DatedExerciseResult,
    onDelete: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (entry.isPersonalBest) {
                PulseGreen.copy(alpha = 0.16f)
            } else {
                Color.White
            },
        ),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (entry.isPersonalBest) PulseGreen else Line,
        ),
    ) {
        Column(
            modifier = Modifier.padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = PerformanceTracker.resultLabel(entry.result),
                        color = Ink,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${PerformanceTracker.shortDate(entry.date)} - " +
                            "${entry.result.effort.label} effort",
                        color = InkSoft,
                    )
                }
                if (entry.isPersonalBest) {
                    Surface(
                        color = PulseGreen,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = "PERSONAL BEST",
                            color = Ink,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        )
                    }
                }
            }
            if (entry.result.sets.isNotEmpty()) {
                Surface(
                    color = Canvas,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            text = "SET BREAKDOWN",
                            color = Muted,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        entry.result.sets.forEach { set ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Set ${set.setNumber}", color = InkSoft)
                                Text(
                                    text = PerformanceTracker.setLabel(set),
                                    color = Ink,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
            if (entry.result.notes.isNotBlank()) {
                Text(
                    text = entry.result.notes,
                    color = InkSoft,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("Delete result")
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
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = title,
            color = Ink,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
        )
    }
}
