package com.ahsanrehmat.pulseplan.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ahsanrehmat.pulseplan.domain.PerformanceTracker
import com.ahsanrehmat.pulseplan.domain.ProgressionSuggestion
import com.ahsanrehmat.pulseplan.domain.WorkoutPrescriptionParser
import com.ahsanrehmat.pulseplan.domain.WorkoutRestKind
import com.ahsanrehmat.pulseplan.domain.WorkoutSessionPhase
import com.ahsanrehmat.pulseplan.domain.WorkoutSessionState
import com.ahsanrehmat.pulseplan.model.DailyWorkout
import com.ahsanrehmat.pulseplan.model.Exercise
import com.ahsanrehmat.pulseplan.model.ExerciseEffort
import com.ahsanrehmat.pulseplan.model.ExerciseMetricType
import com.ahsanrehmat.pulseplan.model.ExerciseResult
import com.ahsanrehmat.pulseplan.model.ExerciseSetDraft
import com.ahsanrehmat.pulseplan.model.ExerciseSetResult
import com.ahsanrehmat.pulseplan.ui.theme.Canvas
import com.ahsanrehmat.pulseplan.ui.theme.Ink
import com.ahsanrehmat.pulseplan.ui.theme.InkSoft
import com.ahsanrehmat.pulseplan.ui.theme.Line
import com.ahsanrehmat.pulseplan.ui.theme.Muted
import com.ahsanrehmat.pulseplan.ui.theme.PulseGreen
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    workout: DailyWorkout,
    session: WorkoutSessionState,
    completedExerciseIds: Set<String>,
    exerciseResultsToday: Map<String, ExerciseResult>,
    previousResult: ExerciseResult?,
    progressionSuggestion: ProgressionSuggestion?,
    lastResultMessage: String?,
    onUpdateSetDraft: (ExerciseSetDraft) -> Unit,
    onUpdateExerciseFeedback: (ExerciseEffort, String) -> Unit,
    onCompleteSet: () -> Unit,
    onUndoLastSet: () -> Unit,
    onSkipExercise: () -> Unit,
    onSetTimerTick: () -> Unit,
    onToggleSetTimer: () -> Unit,
    onResetSetTimer: () -> Unit,
    onRestTick: () -> Unit,
    onToggleRestTimer: () -> Unit,
    onSkipRest: () -> Unit,
    onShowExerciseGuide: (String) -> Unit,
    onExit: () -> Unit,
) {
    BackHandler(onBack = onExit)

    LaunchedEffect(
        session.phase,
        session.restSecondsRemaining,
        session.isRestTimerRunning,
    ) {
        if (
            session.phase == WorkoutSessionPhase.REST &&
            session.isRestTimerRunning &&
            session.restSecondsRemaining > 0
        ) {
            delay(1_000)
            onRestTick()
        }
    }

    LaunchedEffect(
        session.phase,
        session.setTimerSecondsRemaining,
        session.isSetTimerRunning,
    ) {
        if (
            session.phase == WorkoutSessionPhase.EXERCISE &&
            session.isSetTimerRunning &&
            session.setTimerSecondsRemaining > 0
        ) {
            delay(1_000)
            onSetTimerTick()
        }
    }

    Scaffold(
        containerColor = Canvas,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (session.phase) {
                            WorkoutSessionPhase.EXERCISE -> "SET-BY-SET WORKOUT"
                            WorkoutSessionPhase.REST -> "REST"
                            WorkoutSessionPhase.COMPLETE -> "SESSION SUMMARY"
                        },
                        fontWeight = FontWeight.Black,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onExit) {
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
        when (session.phase) {
            WorkoutSessionPhase.EXERCISE -> SetExerciseContent(
                workout = workout,
                session = session,
                previousResult = previousResult,
                progressionSuggestion = progressionSuggestion,
                onUpdateSetDraft = onUpdateSetDraft,
                onUpdateExerciseFeedback = onUpdateExerciseFeedback,
                onCompleteSet = onCompleteSet,
                onUndoLastSet = onUndoLastSet,
                onSkipExercise = onSkipExercise,
                onToggleSetTimer = onToggleSetTimer,
                onResetSetTimer = onResetSetTimer,
                onShowExerciseGuide = onShowExerciseGuide,
                modifier = Modifier.padding(innerPadding),
            )

            WorkoutSessionPhase.REST -> RestContent(
                workout = workout,
                session = session,
                resultMessage = lastResultMessage,
                onToggleRestTimer = onToggleRestTimer,
                onSkipRest = onSkipRest,
                onUndoLastSet = onUndoLastSet,
                modifier = Modifier.padding(innerPadding),
            )

            WorkoutSessionPhase.COMPLETE -> SessionSummaryContent(
                workout = workout,
                session = session,
                completedExerciseIds = completedExerciseIds,
                exerciseResultsToday = exerciseResultsToday,
                resultMessage = lastResultMessage,
                onExit = onExit,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun SetExerciseContent(
    workout: DailyWorkout,
    session: WorkoutSessionState,
    previousResult: ExerciseResult?,
    progressionSuggestion: ProgressionSuggestion?,
    onUpdateSetDraft: (ExerciseSetDraft) -> Unit,
    onUpdateExerciseFeedback: (ExerciseEffort, String) -> Unit,
    onCompleteSet: () -> Unit,
    onUndoLastSet: () -> Unit,
    onSkipExercise: () -> Unit,
    onToggleSetTimer: () -> Unit,
    onResetSetTimer: () -> Unit,
    onShowExerciseGuide: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val exercise = workout.exercises.getOrNull(session.currentExerciseIndex) ?: return
    val prescription = WorkoutPrescriptionParser.parse(exercise)
    val draft = session.currentSetDraft
        ?: WorkoutPrescriptionParser.plannedDraft(exercise)
    val exerciseNumber = session.currentExerciseIndex + 1
    val setNumber = session.currentSetIndex + 1

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        SessionProgress(
            current = exerciseNumber,
            total = workout.exercises.size,
        )

        ExerciseHeaderCard(
            exercise = exercise,
            setNumber = setNumber,
            setCount = prescription.setCount,
        )

        SetProgressCard(
            setCount = prescription.setCount,
            currentSetIndex = session.currentSetIndex,
            completedSets = session.completedSets,
            onUndoLastSet = onUndoLastSet,
        )

        CoachingCard(exercise)

        OutlinedButton(
            onClick = { onShowExerciseGuide(exercise.id) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Icon(Icons.Default.MenuBook, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("How to do it")
        }

        SetEntryCard(
            exercise = exercise,
            setNumber = setNumber,
            setCount = prescription.setCount,
            draft = draft,
            previousSet = previousResult?.sets?.getOrNull(session.currentSetIndex),
            previousResult = previousResult,
            progressionSuggestion = progressionSuggestion,
            effort = session.exerciseEffort,
            notes = session.exerciseNotes,
            timerSecondsRemaining = session.setTimerSecondsRemaining,
            isSetTimerRunning = session.isSetTimerRunning,
            onDraftChange = onUpdateSetDraft,
            onFeedbackChange = onUpdateExerciseFeedback,
            onToggleSetTimer = onToggleSetTimer,
            onResetSetTimer = onResetSetTimer,
        )

        Button(
            onClick = onCompleteSet,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PulseGreen,
                contentColor = Ink,
            ),
            shape = RoundedCornerShape(18.dp),
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(Modifier.size(9.dp))
            Text(
                text = if (setNumber == prescription.setCount) {
                    "Complete final set"
                } else {
                    "Complete set $setNumber"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        Text(
            text = "Completing a set starts the ${exercise.restSeconds}-second rest timer automatically.",
            color = Muted,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        TextButton(
            onClick = onSkipExercise,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.SkipNext, contentDescription = null)
            Spacer(Modifier.size(7.dp))
            Text("Skip remaining sets for now")
        }
    }
}

@Composable
private fun SessionProgress(
    current: Int,
    total: Int,
) {
    val progress = current.toFloat() / total.coerceAtLeast(1)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Exercise $current of $total",
                color = Ink,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(9.dp))
            LinearProgressIndicator(
                progress = { progress },
                color = PulseGreen,
                trackColor = Line,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(CircleShape),
            )
        }
        Text(
            text = "${(progress * 100).roundToInt()}%",
            color = Muted,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun ExerciseHeaderCard(
    exercise: Exercise,
    setNumber: Int,
    setCount: Int,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Ink),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(PulseGreen),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = Ink,
                    )
                }
                Surface(
                    color = PulseGreen.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = "SET $setNumber OF $setCount",
                        color = PulseGreen,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            Text(
                text = exercise.name,
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = exercise.prescription,
                color = PulseGreen,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun SetProgressCard(
    setCount: Int,
    currentSetIndex: Int,
    completedSets: List<ExerciseSetResult>,
    onUndoLastSet: () -> Unit,
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.border(1.dp, Line, RoundedCornerShape(22.dp)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "SET PROGRESS",
                color = Muted,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                repeat(setCount) { index ->
                    val completed = completedSets.any { it.setNumber == index + 1 }
                    val active = index == currentSetIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(
                                when {
                                    completed -> PulseGreen
                                    active -> PulseGreen.copy(alpha = 0.18f)
                                    else -> Canvas
                                },
                            )
                            .border(
                                width = if (active) 2.dp else 1.dp,
                                color = if (active) PulseGreen else Line,
                                shape = RoundedCornerShape(15.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (completed) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Set ${index + 1} complete",
                                tint = Ink,
                            )
                        } else {
                            Text(
                                text = "${index + 1}",
                                color = if (active) Ink else Muted,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
            }
            completedSets.forEach { set ->
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
            if (completedSets.isNotEmpty()) {
                TextButton(
                    onClick = onUndoLastSet,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Undo and edit last set")
                }
            }
        }
    }
}

@Composable
private fun CoachingCard(exercise: Exercise) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.border(1.dp, Line, RoundedCornerShape(20.dp)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = "COACHING CUE",
                color = Muted,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = exercise.coachingCue,
                color = Ink,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun SetEntryCard(
    exercise: Exercise,
    setNumber: Int,
    setCount: Int,
    draft: ExerciseSetDraft,
    previousSet: ExerciseSetResult?,
    previousResult: ExerciseResult?,
    progressionSuggestion: ProgressionSuggestion?,
    effort: ExerciseEffort,
    notes: String,
    timerSecondsRemaining: Int,
    isSetTimerRunning: Boolean,
    onDraftChange: (ExerciseSetDraft) -> Unit,
    onFeedbackChange: (ExerciseEffort, String) -> Unit,
    onToggleSetTimer: () -> Unit,
    onResetSetTimer: () -> Unit,
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.border(1.dp, Line, RoundedCornerShape(22.dp)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "LOG SET $setNumber OF $setCount",
                    color = Muted,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Your planned target is ready. Adjust it to match what you actually complete.",
                    color = InkSoft,
                    style = MaterialTheme.typography.bodyMedium,
                )
                previousSet?.let {
                    Text(
                        text = "Last workout, set $setNumber: ${PerformanceTracker.setLabel(it)}",
                        color = Ink,
                        fontWeight = FontWeight.Bold,
                    )
                } ?: previousResult?.let {
                    Text(
                        text = "Previous: ${PerformanceTracker.resultLabel(it)}",
                        color = Ink,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            progressionSuggestion?.let { suggestion ->
                ProgressionCoachCard(
                    suggestion = suggestion,
                    onUseSuggestion = {
                        onDraftChange(
                            ExerciseSetDraft(
                                metricType = suggestion.metricType,
                                reps = suggestion.reps,
                                weightKg = suggestion.weightKg,
                                durationSeconds = suggestion.durationSeconds,
                                distanceKm = suggestion.distanceKm,
                            ),
                        )
                    },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                ExerciseMetricType.entries.forEach { option ->
                    FilterChip(
                        selected = draft.metricType == option,
                        onClick = {
                            onDraftChange(
                                when (option) {
                                    ExerciseMetricType.REPS -> ExerciseSetDraft(
                                        metricType = option,
                                        reps = draft.reps,
                                        weightKg = draft.weightKg,
                                    )

                                    ExerciseMetricType.TIME -> ExerciseSetDraft(
                                        metricType = option,
                                        durationSeconds = draft.durationSeconds,
                                    )

                                    ExerciseMetricType.DISTANCE -> ExerciseSetDraft(
                                        metricType = option,
                                        durationSeconds = draft.durationSeconds,
                                        distanceKm = draft.distanceKm,
                                    )
                                },
                            )
                        },
                        label = { Text(option.label) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            SetMetricFields(
                draft = draft,
                onDraftChange = onDraftChange,
            )

            if (draft.metricType == ExerciseMetricType.TIME) {
                SetTimerCard(
                    secondsRemaining = timerSecondsRemaining,
                    isRunning = isSetTimerRunning,
                    onToggle = onToggleSetTimer,
                    onReset = onResetSetTimer,
                )
            }

            TextButton(
                onClick = {
                    onDraftChange(WorkoutPrescriptionParser.plannedDraft(exercise))
                },
            ) {
                Text("Restore planned target")
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "How did this exercise feel?",
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    ExerciseEffort.entries.forEach { option ->
                        FilterChip(
                            selected = effort == option,
                            onClick = { onFeedbackChange(option, notes) },
                            label = { Text(option.label) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { onFeedbackChange(effort, it.take(160)) },
                label = { Text("Workout note (optional)") },
                supportingText = { Text("${notes.length}/160") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ProgressionCoachCard(
    suggestion: ProgressionSuggestion,
    onUseSuggestion: () -> Unit,
) {
    Surface(
        color = PulseGreen.copy(alpha = 0.18f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.border(1.dp, PulseGreen, RoundedCornerShape(18.dp)),
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "PROGRESSION COACH",
                color = Muted,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${suggestion.title}: ${suggestion.targetLabel}",
                color = Ink,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = suggestion.explanation,
                color = InkSoft,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Optional. Keep the previous target if form or comfort would suffer.",
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = onUseSuggestion) {
                Text("Use suggestion for this set")
            }
        }
    }
}

@Composable
private fun SetMetricFields(
    draft: ExerciseSetDraft,
    onDraftChange: (ExerciseSetDraft) -> Unit,
) {
    when (draft.metricType) {
        ExerciseMetricType.REPS -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = draft.reps?.toString().orEmpty(),
                onValueChange = { value ->
                    onDraftChange(
                        draft.copy(
                            reps = value.numericInput(4).toIntOrNull()?.takeIf { it > 0 },
                        ),
                    )
                },
                label = { Text("Reps") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = draft.weightKg?.let(::formatInputDecimal).orEmpty(),
                onValueChange = { value ->
                    onDraftChange(
                        draft.copy(
                            weightKg = value.decimalInput(6)
                                .toDoubleOrNull()
                                ?.takeIf { it > 0.0 },
                        ),
                    )
                },
                label = { Text("Weight kg") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }

        ExerciseMetricType.TIME -> OutlinedTextField(
            value = draft.durationSeconds?.toString().orEmpty(),
            onValueChange = { value ->
                onDraftChange(
                    draft.copy(
                        durationSeconds = value.numericInput(5)
                            .toIntOrNull()
                            ?.takeIf { it > 0 },
                    ),
                )
            },
            label = { Text("Seconds") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        ExerciseMetricType.DISTANCE -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = draft.distanceKm?.let(::formatInputDecimal).orEmpty(),
                onValueChange = { value ->
                    onDraftChange(
                        draft.copy(
                            distanceKm = value.decimalInput(7)
                                .toDoubleOrNull()
                                ?.takeIf { it > 0.0 },
                        ),
                    )
                },
                label = { Text("Distance km") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = draft.durationSeconds
                    ?.div(60.0)
                    ?.let(::formatInputDecimal)
                    .orEmpty(),
                onValueChange = { value ->
                    onDraftChange(
                        draft.copy(
                            durationSeconds = value.decimalInput(6)
                                .toDoubleOrNull()
                                ?.takeIf { it > 0.0 }
                                ?.times(60)
                                ?.roundToInt(),
                        ),
                    )
                },
                label = { Text("Minutes") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SetTimerCard(
    secondsRemaining: Int,
    isRunning: Boolean,
    onToggle: () -> Unit,
    onReset: () -> Unit,
) {
    Surface(
        color = Ink,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "EXERCISE TIMER",
                color = PulseGreen,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = formatCountdown(secondsRemaining),
                color = Color.White,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onToggle,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PulseGreen,
                        contentColor = Ink,
                    ),
                ) {
                    Icon(
                        if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                    )
                    Spacer(Modifier.size(5.dp))
                    Text(if (isRunning) "Pause" else "Start timer")
                }
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Reset")
                }
            }
        }
    }
}

@Composable
private fun RestContent(
    workout: DailyWorkout,
    session: WorkoutSessionState,
    resultMessage: String?,
    onToggleRestTimer: () -> Unit,
    onSkipRest: () -> Unit,
    onUndoLastSet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val finishedExercise = workout.exercises.getOrNull(session.currentExerciseIndex)
    val betweenSets = session.restKind == WorkoutRestKind.BETWEEN_SETS
    val nextExercise = if (betweenSets) {
        finishedExercise
    } else {
        workout.exercises.getOrNull(session.currentExerciseIndex + 1)
    }
    val totalRestSeconds = finishedExercise?.restSeconds?.coerceAtLeast(1) ?: 1
    val progress = session.restSecondsRemaining.toFloat() / totalRestSeconds
    val setCount = finishedExercise
        ?.let(WorkoutPrescriptionParser::parse)
        ?.setCount
        ?: 1

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (betweenSets) {
                    "Set ${session.currentSetIndex} of $setCount complete."
                } else {
                    "Exercise complete."
                },
                color = Ink,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (betweenSets) {
                    "Recover, then continue the same exercise."
                } else {
                    "Recover before the next movement."
                },
                color = Muted,
                textAlign = TextAlign.Center,
            )
            resultMessage?.let {
                Surface(
                    color = PulseGreen.copy(alpha = 0.24f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        text = it,
                        color = Ink,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    )
                }
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(250.dp),
        ) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = PulseGreen,
                trackColor = Line,
                strokeWidth = 16.dp,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatCountdown(session.restSecondsRemaining),
                    color = Ink,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = if (session.isRestTimerRunning) "RESTING" else "PAUSED",
                    color = Muted,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        Surface(
            color = Color.White,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Line, RoundedCornerShape(20.dp)),
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    text = "NEXT UP",
                    color = Muted,
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = nextExercise?.name ?: "Session summary",
                    color = Ink,
                    style = MaterialTheme.typography.titleLarge,
                )
                if (betweenSets) {
                    Text(
                        text = "Set ${session.currentSetIndex + 1} of $setCount",
                        color = InkSoft,
                    )
                } else {
                    nextExercise?.let { Text(it.prescription, color = InkSoft) }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Button(
                onClick = onToggleRestTimer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Ink,
                    contentColor = Color.White,
                ),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(
                    imageVector = if (session.isRestTimerRunning) {
                        Icons.Default.Pause
                    } else {
                        Icons.Default.PlayArrow
                    },
                    contentDescription = null,
                )
                Spacer(Modifier.size(8.dp))
                Text(if (session.isRestTimerRunning) "Pause timer" else "Resume timer")
            }
            OutlinedButton(
                onClick = onSkipRest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text(
                    if (betweenSets) {
                        "Skip rest and start set ${session.currentSetIndex + 1}"
                    } else {
                        "Skip rest"
                    },
                )
            }
            if (betweenSets) {
                TextButton(
                    onClick = onUndoLastSet,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Undo and edit set ${session.currentSetIndex}")
                }
            }
        }
    }
}

@Composable
private fun SessionSummaryContent(
    workout: DailyWorkout,
    session: WorkoutSessionState,
    completedExerciseIds: Set<String>,
    exerciseResultsToday: Map<String, ExerciseResult>,
    resultMessage: String?,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val workoutIds = workout.exercises.map(Exercise::sourceExerciseId).toSet()
    val results = exerciseResultsToday
        .filterKeys { it in workoutIds }
        .values
    val completed = completedExerciseIds.count { it in workoutIds }
    val totalSets = results.sumOf { result ->
        result.sets.size.takeIf { it > 0 } ?: 1
    }
    val totalReps = results.sumOf(PerformanceTracker::totalReps)
    val totalVolume = results.sumOf(PerformanceTracker::totalVolumeKg)
    val totalTimedSeconds = results.sumOf(PerformanceTracker::totalTimedSeconds)
    val totalDistance = results.sumOf(PerformanceTracker::totalDistanceKm)
    val elapsedSeconds = ((System.currentTimeMillis() - session.sessionStartedAtEpochMillis)
        .coerceAtLeast(0L) / 1_000L).toInt()
    val allComplete = workout.exercises.isNotEmpty() && completed >= workout.exercises.size

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(PulseGreen),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Ink,
                modifier = Modifier.size(56.dp),
            )
        }
        Text(
            text = if (allComplete) "Workout complete!" else "Session finished",
            color = Ink,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "$completed of ${workout.exercises.size} exercises completed",
            color = Muted,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )

        Surface(
            color = Ink,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "TODAY'S SET SUMMARY",
                    color = PulseGreen,
                    fontWeight = FontWeight.Black,
                )
                SummaryRow("Sets completed", totalSets.toString())
                if (totalReps > 0) SummaryRow("Total reps", totalReps.toString())
                if (totalVolume > 0.0) {
                    SummaryRow("Training volume", "${formatSummaryDecimal(totalVolume)} kg")
                }
                if (totalTimedSeconds > 0) {
                    SummaryRow("Timed work", formatCountdown(totalTimedSeconds))
                }
                if (totalDistance > 0.0) {
                    SummaryRow("Distance", "${formatSummaryDecimal(totalDistance)} km")
                }
                SummaryRow("Session duration", formatCountdown(elapsedSeconds))
            }
        }

        resultMessage?.let {
            Surface(
                color = PulseGreen.copy(alpha = 0.24f),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    text = it,
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }

        Text(
            text = "Every completed set is saved in your Performance Log and can guide your next target.",
            color = InkSoft,
            textAlign = TextAlign.Center,
        )

        Button(
            onClick = onExit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Ink,
                contentColor = Color.White,
            ),
            contentPadding = PaddingValues(horizontal = 20.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text("Back to dashboard")
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Color(0xFFADB7A8))
        Text(value, color = Color.White, fontWeight = FontWeight.Black)
    }
}

private fun String.numericInput(maxLength: Int): String =
    filter(Char::isDigit).take(maxLength)

private fun String.decimalInput(maxLength: Int): String {
    val filtered = filter { it.isDigit() || it == '.' }.take(maxLength)
    val firstDecimal = filtered.indexOf('.')
    return if (firstDecimal == -1) {
        filtered
    } else {
        filtered.take(firstDecimal + 1) +
            filtered.drop(firstDecimal + 1).replace(".", "")
    }
}

private fun formatInputDecimal(value: Double): String =
    if (value == value.roundToInt().toDouble()) {
        value.roundToInt().toString()
    } else {
        String.format(java.util.Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    }

private fun formatSummaryDecimal(value: Double): String =
    if (value == value.roundToInt().toDouble()) {
        value.roundToInt().toString()
    } else {
        String.format(java.util.Locale.US, "%.1f", value)
    }

private fun formatCountdown(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}
