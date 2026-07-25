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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ahsanrehmat.pulseplan.domain.WorkoutSessionPhase
import com.ahsanrehmat.pulseplan.domain.WorkoutSessionState
import com.ahsanrehmat.pulseplan.model.DailyWorkout
import com.ahsanrehmat.pulseplan.model.Exercise
import com.ahsanrehmat.pulseplan.ui.theme.Canvas
import com.ahsanrehmat.pulseplan.ui.theme.Ink
import com.ahsanrehmat.pulseplan.ui.theme.InkSoft
import com.ahsanrehmat.pulseplan.ui.theme.Line
import com.ahsanrehmat.pulseplan.ui.theme.Muted
import com.ahsanrehmat.pulseplan.ui.theme.PulseGreen
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    workout: DailyWorkout,
    session: WorkoutSessionState,
    completedExerciseIds: Set<String>,
    onCompleteExercise: () -> Unit,
    onSkipExercise: () -> Unit,
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

    Scaffold(
        containerColor = Canvas,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (session.phase) {
                            WorkoutSessionPhase.EXERCISE -> "ACTIVE WORKOUT"
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
            WorkoutSessionPhase.EXERCISE -> ExerciseSessionContent(
                workout = workout,
                session = session,
                onCompleteExercise = onCompleteExercise,
                onSkipExercise = onSkipExercise,
                onShowExerciseGuide = onShowExerciseGuide,
                modifier = Modifier.padding(innerPadding),
            )

            WorkoutSessionPhase.REST -> RestSessionContent(
                workout = workout,
                session = session,
                onToggleRestTimer = onToggleRestTimer,
                onSkipRest = onSkipRest,
                modifier = Modifier.padding(innerPadding),
            )

            WorkoutSessionPhase.COMPLETE -> SessionCompleteContent(
                completed = completedExerciseIds.size,
                total = workout.exercises.size,
                onExit = onExit,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun ExerciseSessionContent(
    workout: DailyWorkout,
    session: WorkoutSessionState,
    onCompleteExercise: () -> Unit,
    onSkipExercise: () -> Unit,
    onShowExerciseGuide: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val exercise = workout.exercises.getOrNull(session.currentExerciseIndex) ?: return
    val exerciseNumber = session.currentExerciseIndex + 1
    val progress = exerciseNumber.toFloat() / workout.exercises.size.coerceAtLeast(1)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        SessionProgress(
            current = exerciseNumber,
            total = workout.exercises.size,
            progress = progress,
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Ink),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
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
                Column {
                    Text(
                        text = exercise.name,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = exercise.prescription,
                        color = PulseGreen,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = Color(0xFFADB7A8),
                    )
                    Text(
                        text = "${exercise.restSeconds} seconds rest after this exercise",
                        color = Color(0xFFADB7A8),
                    )
                }
            }
        }

        CoachingCard(exercise = exercise)

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

        Button(
            onClick = onCompleteExercise,
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
                text = "Complete exercise",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        TextButton(
            onClick = onSkipExercise,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.SkipNext, contentDescription = null)
            Spacer(Modifier.size(7.dp))
            Text("Skip for now")
        }
    }
}

@Composable
private fun SessionProgress(
    current: Int,
    total: Int,
    progress: Float,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Exercise $current of $total",
                color = Ink,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                color = Muted,
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(CircleShape),
            color = PulseGreen,
            trackColor = Line,
        )
    }
}

@Composable
private fun CoachingCard(exercise: Exercise) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.border(1.dp, Line, RoundedCornerShape(22.dp)),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
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
private fun RestSessionContent(
    workout: DailyWorkout,
    session: WorkoutSessionState,
    onToggleRestTimer: () -> Unit,
    onSkipRest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val finishedExercise = workout.exercises.getOrNull(session.currentExerciseIndex)
    val nextExercise = workout.exercises.getOrNull(session.currentExerciseIndex + 1)
    val totalRestSeconds = finishedExercise?.restSeconds?.coerceAtLeast(1) ?: 1
    val progress = session.restSecondsRemaining.toFloat() / totalRestSeconds

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
                text = "Nice work.",
                color = Ink,
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Recover before the next movement.",
                color = Muted,
                textAlign = TextAlign.Center,
            )
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
                nextExercise?.let {
                    Text(it.prescription, color = InkSoft)
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
                Text("Skip rest")
            }
        }
    }
}

@Composable
private fun SessionCompleteContent(
    completed: Int,
    total: Int,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val allComplete = total > 0 && completed >= total
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(PulseGreen),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Ink,
                modifier = Modifier.size(58.dp),
            )
        }
        Spacer(Modifier.height(26.dp))
        Text(
            text = if (allComplete) "Workout complete!" else "Session finished",
            color = Ink,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "$completed of $total exercises completed",
            color = Muted,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (allComplete) {
                "Excellent work. Today's progress is saved."
            } else {
                "Your completed exercises are saved. You can return for the unfinished ones."
            },
            color = InkSoft,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(34.dp))
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

private fun formatCountdown(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
