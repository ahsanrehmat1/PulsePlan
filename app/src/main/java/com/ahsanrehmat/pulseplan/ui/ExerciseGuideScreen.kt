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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ahsanrehmat.pulseplan.domain.ExerciseGuide
import com.ahsanrehmat.pulseplan.domain.ExerciseVideoGuideCatalog
import com.ahsanrehmat.pulseplan.model.Exercise
import com.ahsanrehmat.pulseplan.ui.theme.Canvas
import com.ahsanrehmat.pulseplan.ui.theme.Ink
import com.ahsanrehmat.pulseplan.ui.theme.InkSoft
import com.ahsanrehmat.pulseplan.ui.theme.Line
import com.ahsanrehmat.pulseplan.ui.theme.Muted
import com.ahsanrehmat.pulseplan.ui.theme.PulseGreen
import com.ahsanrehmat.pulseplan.ui.theme.Warm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseGuideScreen(
    exercise: Exercise,
    guide: ExerciseGuide,
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
                        text = "Exercise guide",
                        fontWeight = FontWeight.Black,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to workout",
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                GuideHero(exercise = exercise)
            }

            item {
                RealVideoGuideCard(exercise = exercise)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GuideFact(
                        label = "TARGET AREA",
                        value = guide.targetArea,
                        modifier = Modifier.weight(1f),
                    )
                    GuideFact(
                        label = "EQUIPMENT",
                        value = guide.equipment,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                SectionHeading(
                    icon = {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = Ink,
                        )
                    },
                    title = "How to do it",
                )
            }

            guide.steps.forEachIndexed { index, step ->
                item {
                    GuideStep(
                        number = index + 1,
                        text = step,
                    )
                }
            }

            item {
                Surface(
                    color = PulseGreen.copy(alpha = 0.24f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, PulseGreen, RoundedCornerShape(20.dp)),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Text(
                            text = "COACHING CUE",
                            color = InkSoft,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            text = exercise.coachingCue,
                            color = Ink,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            item {
                Surface(
                    color = Warm.copy(alpha = 0.24f),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            Icons.Default.ReportProblem,
                            contentDescription = null,
                            tint = Ink,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Common mistake",
                                color = Ink,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = guide.commonMistake,
                                color = InkSoft,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Line, RoundedCornerShape(20.dp)),
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = Muted,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "This is general exercise guidance, not treatment for an injury or condition. Stop immediately if you feel pain or become unwell. Ask a qualified professional if you are unsure whether an exercise is suitable for you.",
                            color = Muted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun RealVideoGuideCard(exercise: Exercise) {
    val uriHandler = LocalUriHandler.current
    val videoGuide = ExerciseVideoGuideCatalog.forExercise(exercise)

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Line, RoundedCornerShape(24.dp)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Warm),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.OndemandVideo,
                        contentDescription = null,
                        tint = Ink,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "REAL VIDEO GUIDE",
                        color = Muted,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = "Watch the full movement",
                        color = Ink,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                    )
                }
            }

            Text(
                text = "See a real person demonstrate ${exercise.name.lowercase()} before you begin.",
                color = InkSoft,
                style = MaterialTheme.typography.bodyMedium,
            )

            Button(
                onClick = { uriHandler.openUri(videoGuide.searchUrl) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Ink,
                    contentColor = Color.White,
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Default.OpenInNew,
                    contentDescription = null,
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = "Find video on YouTube",
                    fontWeight = FontWeight.Bold,
                )
            }

            Text(
                text = "Opens an exact video search. Compare the demonstration with the written steps and safety cues below.",
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun GuideHero(exercise: Exercise) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Ink),
        shape = RoundedCornerShape(26.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
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
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "${exercise.prescription} · ${exercise.restSeconds} sec rest",
                    color = PulseGreen,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun GuideFact(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(15.dp)) {
            Text(
                text = label,
                color = Muted,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = value,
                color = Ink,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SectionHeading(
    icon: @Composable () -> Unit,
    title: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        icon()
        Text(
            text = title,
            color = Ink,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun GuideStep(
    number: Int,
    text: String,
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Line, RoundedCornerShape(18.dp)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(PulseGreen, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = number.toString(),
                    color = Ink,
                    fontWeight = FontWeight.Black,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                color = Ink,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
