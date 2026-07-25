package com.ahsanrehmat.pulseplan.domain

import com.ahsanrehmat.pulseplan.model.DailyWorkout
import com.ahsanrehmat.pulseplan.model.Equipment
import com.ahsanrehmat.pulseplan.model.Exercise
import com.ahsanrehmat.pulseplan.model.ExperienceLevel
import com.ahsanrehmat.pulseplan.model.FitnessGoal
import com.ahsanrehmat.pulseplan.model.MovementPreference
import com.ahsanrehmat.pulseplan.model.UserFitnessProfile
import com.ahsanrehmat.pulseplan.model.WeekDayPlan
import com.ahsanrehmat.pulseplan.model.WorkoutPersonality
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

object PlanGenerator {
    private enum class MovementDemand {
        FAST_TRANSITION,
        FLOOR,
        OVERHEAD,
        WRIST_LOADING,
        KNEELING,
        BALANCE_CHALLENGE,
    }

    private val bodyweightStrength = listOf(
        exercise("squat", "Bodyweight squat", "3 × 10", "Keep your knees tracking over your toes"),
        exercise("push_up", "Incline push-up", "3 × 8", "Keep a straight line from shoulders to heels"),
        exercise("reverse_lunge", "Reverse lunge", "3 × 8 each", "Step back softly and keep your front heel down"),
        exercise("glute_bridge", "Glute bridge", "3 × 12", "Pause and squeeze at the top"),
        exercise("dead_bug", "Dead bug", "3 × 8 each", "Keep your lower back gently pressed down"),
        exercise("plank", "Forearm plank", "3 × 25 sec", "Brace as if preparing for a gentle punch"),
    )

    private val dumbbellStrength = listOf(
        exercise("db_goblet_squat", "Goblet squat", "3 × 10", "Hold the dumbbell close to your chest"),
        exercise("db_floor_press", "Dumbbell floor press", "3 × 10", "Lower with control until your arms touch the floor"),
        exercise("db_row", "One-arm dumbbell row", "3 × 10 each", "Pull your elbow toward your back pocket"),
        exercise("db_rdl", "Dumbbell Romanian deadlift", "3 × 10", "Push your hips back with a neutral spine"),
        exercise("db_press", "Seated dumbbell press", "3 × 8", "Keep ribs down as you press"),
        exercise("farmer_carry", "Farmer carry", "3 × 30 sec", "Walk tall with quiet, controlled steps"),
    )

    private val gymStrength = listOf(
        exercise("leg_press", "Leg press", "3 × 10", "Use a range you can control without lifting your hips"),
        exercise("chest_press", "Machine chest press", "3 × 10", "Keep shoulders relaxed and wrists straight"),
        exercise("lat_pulldown", "Lat pulldown", "3 × 10", "Pull toward your upper chest without swinging"),
        exercise("cable_row", "Seated cable row", "3 × 10", "Finish with elbows close to your sides"),
        exercise("hamstring_curl", "Hamstring curl", "3 × 12", "Move smoothly through the full comfortable range"),
        exercise("pallof_press", "Pallof press", "3 × 10 each", "Resist rotation and breathe normally"),
    )

    private val conditioning = listOf(
        exercise("march", "Fast march", "3 × 45 sec", "Land softly and swing your arms"),
        exercise("step_jack", "Low-impact step jack", "3 × 40 sec", "Keep a steady pace you can sustain"),
        exercise("squat_reach", "Squat to reach", "3 × 12", "Stand tall and reach without leaning back"),
        exercise("mountain_climber", "Elevated mountain climber", "3 × 30 sec", "Keep shoulders stacked over your hands"),
        exercise("skater_step", "Skater step", "3 × 40 sec", "Step wide while staying controlled"),
        exercise("slow_burpee", "Walk-out burpee", "3 × 8", "Move one step at a time and keep breathing"),
    )

    private val mobility = listOf(
        exercise("cat_cow", "Cat-cow", "2 × 8", "Move slowly with your breath"),
        exercise("world_stretch", "World's greatest stretch", "2 × 5 each", "Use a comfortable range without forcing it"),
        exercise("hip_90_90", "90/90 hip switches", "2 × 8", "Stay tall and rotate under control"),
        exercise("wall_slide", "Wall slide", "2 × 10", "Keep your ribs gently tucked"),
        exercise("ankle_rock", "Ankle rocks", "2 × 10 each", "Keep your heel planted"),
        exercise("child_pose", "Child's pose breathing", "2 × 45 sec", "Take slow breaths into your sides and back"),
    )

    private val substitutions = mapOf(
        "squat" to exercise(
            "chair_squat",
            "Chair squat",
            "3 × 10",
            "Tap a stable chair softly, then stand through your whole foot",
        ),
        "push_up" to exercise(
            "wall_push_up",
            "Wall push-up",
            "3 × 10",
            "Keep one straight line as your chest moves toward the wall",
        ),
        "reverse_lunge" to exercise(
            "supported_split_squat",
            "Supported split squat",
            "3 × 8 each",
            "Use a stable support and lower straight down with control",
        ),
        "glute_bridge" to exercise(
            "standing_hip_extension",
            "Standing hip extension",
            "3 × 10 each",
            "Keep your torso tall as the leg moves gently behind you",
        ),
        "dead_bug" to exercise(
            "bird_dog",
            "Bird dog",
            "3 × 8 each",
            "Reach long without rotating your hips or arching your back",
        ),
        "plank" to exercise(
            "incline_plank",
            "Incline plank",
            "3 × 30 sec",
            "Brace gently and keep shoulders, hips, and heels aligned",
        ),
        "db_goblet_squat" to exercise(
            "db_split_squat",
            "Dumbbell split squat",
            "3 × 8 each",
            "Keep both feet planted and lower between them with control",
        ),
        "db_floor_press" to exercise(
            "db_close_grip_floor_press",
            "Close-grip dumbbell floor press",
            "3 × 10",
            "Keep the weights close and lower until your upper arms meet the floor",
        ),
        "db_row" to exercise(
            "db_two_arm_row",
            "Two-arm dumbbell row",
            "3 × 10",
            "Hinge with a long spine and draw both elbows toward your hips",
        ),
        "db_rdl" to exercise(
            "db_sumo_deadlift",
            "Dumbbell sumo deadlift",
            "3 × 10",
            "Keep the weight close and push the floor away to stand",
        ),
        "db_press" to exercise(
            "db_single_arm_press",
            "Single-arm seated dumbbell press",
            "3 × 8 each",
            "Stay tall and resist leaning as you press one side",
        ),
        "farmer_carry" to exercise(
            "suitcase_carry",
            "Suitcase carry",
            "3 × 25 sec each",
            "Walk tall without leaning toward or away from the weight",
        ),
        "leg_press" to exercise(
            "hack_squat",
            "Hack squat machine",
            "3 × 10",
            "Keep your back supported and use a comfortable knee range",
        ),
        "chest_press" to exercise(
            "cable_chest_press",
            "Standing cable chest press",
            "3 × 10",
            "Brace your stance and press without shrugging your shoulders",
        ),
        "lat_pulldown" to exercise(
            "assisted_pull_up",
            "Assisted pull-up",
            "3 × 8",
            "Lead with your chest and lower until your arms are long",
        ),
        "cable_row" to exercise(
            "chest_supported_row",
            "Chest-supported machine row",
            "3 × 10",
            "Keep your chest supported while drawing elbows behind you",
        ),
        "hamstring_curl" to exercise(
            "seated_hamstring_curl",
            "Seated hamstring curl",
            "3 × 12",
            "Keep your hips against the pad and return the weight slowly",
        ),
        "pallof_press" to exercise(
            "pallof_hold",
            "Cable Pallof hold",
            "3 × 20 sec each",
            "Hold your hands forward without letting the cable turn you",
        ),
        "march" to exercise(
            "heel_dig",
            "Alternating heel dig",
            "3 × 45 sec",
            "Reach one heel forward at a steady low-impact pace",
        ),
        "step_jack" to exercise(
            "side_step_reach",
            "Side step and reach",
            "3 × 40 sec",
            "Step wide and sweep your arms only through a comfortable range",
        ),
        "squat_reach" to exercise(
            "sit_to_stand_reach",
            "Sit-to-stand reach",
            "3 × 10",
            "Touch the chair softly and stand tall before reaching",
        ),
        "mountain_climber" to exercise(
            "standing_knee_drive",
            "Standing knee drive",
            "3 × 30 sec",
            "Drive alternating knees while staying tall and controlled",
        ),
        "skater_step" to exercise(
            "lateral_toe_tap",
            "Lateral toe tap",
            "3 × 40 sec",
            "Shift side to side and tap lightly without twisting the knees",
        ),
        "slow_burpee" to exercise(
            "wall_walkout",
            "Wall walkout",
            "3 × 8",
            "Walk your hands down the wall only as far as you can control",
        ),
        "cat_cow" to exercise(
            "seated_cat_cow",
            "Seated cat-cow",
            "2 × 8",
            "Round and lengthen your spine gently with each breath",
        ),
        "world_stretch" to exercise(
            "half_kneeling_stretch",
            "Half-kneeling hip stretch",
            "2 × 30 sec each",
            "Tuck the pelvis gently before shifting forward",
        ),
        "hip_90_90" to exercise(
            "seated_hip_rotation",
            "Seated hip rotation",
            "2 × 8 each",
            "Rotate from the hips while keeping your feet relaxed",
        ),
        "wall_slide" to exercise(
            "shoulder_circle",
            "Shoulder circles",
            "2 × 10 each way",
            "Make smooth circles without lifting your ribs",
        ),
        "ankle_rock" to exercise(
            "wall_calf_stretch",
            "Wall calf stretch",
            "2 × 30 sec each",
            "Keep the back heel planted and both feet pointing forward",
        ),
        "child_pose" to exercise(
            "knee_to_chest",
            "Knee-to-chest breathing",
            "2 × 30 sec each",
            "Relax your shoulders and breathe without forcing the hip",
        ),
    )

    private val movementPreferenceExercises = listOf(
        exercise(
            "forearm_wall_press",
            "Forearm wall press",
            "3 × 10",
            "Keep wrists relaxed as your forearms press gently into the wall",
        ),
        exercise(
            "standing_cross_crawl",
            "Supported cross-crawl",
            "3 × 8 each",
            "Use a stable support and bring opposite elbow and knee toward each other",
        ),
        exercise(
            "standing_core_brace",
            "Standing core brace",
            "3 × 20 sec",
            "Stand tall, exhale, and gently brace without holding your breath",
        ),
        exercise(
            "db_chest_squeeze",
            "Dumbbell chest squeeze",
            "3 × 20 sec",
            "Hold one dumbbell at chest height and squeeze inward with steady pressure",
        ),
        exercise(
            "db_lateral_raise",
            "Dumbbell lateral raise",
            "3 × 10",
            "Lift light weights only to a comfortable height below the shoulders",
        ),
        exercise(
            "supported_march",
            "Supported march",
            "3 × 40 sec",
            "Keep one or both hands lightly on a stable support while marching",
        ),
        exercise(
            "supported_side_step",
            "Supported side step",
            "3 × 40 sec",
            "Use a stable support and take small quiet steps from side to side",
        ),
        exercise(
            "supported_knee_drive",
            "Supported knee drive",
            "3 × 30 sec",
            "Hold a stable support and lift alternating knees without leaning back",
        ),
        exercise(
            "supported_lateral_tap",
            "Supported lateral tap",
            "3 × 40 sec",
            "Keep one hand supported as you tap each foot gently to the side",
        ),
        exercise(
            "standing_hip_flexor_stretch",
            "Standing hip flexor stretch",
            "2 × 30 sec each",
            "Use a split stance and gently tuck the pelvis while staying tall",
        ),
        exercise(
            "shoulder_blade_squeeze",
            "Shoulder-blade squeeze",
            "2 × 10",
            "Keep arms relaxed as you draw the shoulder blades gently together",
        ),
        exercise(
            "seated_breathing",
            "Seated recovery breathing",
            "2 × 45 sec",
            "Sit supported and take slow breaths into your sides and back",
        ),
    )
    private val movementPreferenceExerciseById =
        movementPreferenceExercises.associateBy(Exercise::id)

    private val movementAlternatives: Map<String, List<Exercise>> = mapOf(
        "push_up" to listOf(preferenceExercise("forearm_wall_press")),
        "reverse_lunge" to listOf(substitutions.getValue("reverse_lunge")),
        "glute_bridge" to listOf(substitutions.getValue("glute_bridge")),
        "dead_bug" to listOf(preferenceExercise("standing_cross_crawl")),
        "plank" to listOf(preferenceExercise("standing_core_brace")),
        "db_floor_press" to listOf(preferenceExercise("db_chest_squeeze")),
        "db_press" to listOf(preferenceExercise("db_lateral_raise")),
        "lat_pulldown" to listOf(substitutions.getValue("cable_row")),
        "march" to listOf(preferenceExercise("supported_march")),
        "step_jack" to listOf(
            preferenceExercise("supported_side_step"),
            substitutions.getValue("march"),
        ),
        "squat_reach" to listOf(substitutions.getValue("squat")),
        "mountain_climber" to listOf(preferenceExercise("supported_knee_drive")),
        "skater_step" to listOf(
            preferenceExercise("supported_lateral_tap"),
            substitutions.getValue("march"),
        ),
        "slow_burpee" to listOf(
            substitutions.getValue("squat"),
            preferenceExercise("supported_march"),
            substitutions.getValue("march"),
        ),
        "cat_cow" to listOf(substitutions.getValue("cat_cow")),
        "world_stretch" to listOf(preferenceExercise("standing_hip_flexor_stretch")),
        "hip_90_90" to listOf(substitutions.getValue("hip_90_90")),
        "wall_slide" to listOf(preferenceExercise("shoulder_blade_squeeze")),
        "child_pose" to listOf(preferenceExercise("seated_breathing")),
    )

    private val movementDemands = mapOf(
        "push_up" to setOf(MovementDemand.WRIST_LOADING),
        "reverse_lunge" to setOf(MovementDemand.BALANCE_CHALLENGE),
        "glute_bridge" to setOf(MovementDemand.FLOOR),
        "dead_bug" to setOf(MovementDemand.FLOOR),
        "plank" to setOf(MovementDemand.FLOOR),
        "db_floor_press" to setOf(MovementDemand.FLOOR),
        "db_press" to setOf(MovementDemand.OVERHEAD),
        "lat_pulldown" to setOf(MovementDemand.OVERHEAD),
        "march" to setOf(MovementDemand.BALANCE_CHALLENGE),
        "step_jack" to setOf(
            MovementDemand.OVERHEAD,
            MovementDemand.BALANCE_CHALLENGE,
        ),
        "squat_reach" to setOf(MovementDemand.OVERHEAD),
        "mountain_climber" to setOf(
            MovementDemand.FAST_TRANSITION,
            MovementDemand.WRIST_LOADING,
        ),
        "skater_step" to setOf(MovementDemand.BALANCE_CHALLENGE),
        "slow_burpee" to setOf(
            MovementDemand.FAST_TRANSITION,
            MovementDemand.FLOOR,
            MovementDemand.WRIST_LOADING,
        ),
        "cat_cow" to setOf(
            MovementDemand.FLOOR,
            MovementDemand.WRIST_LOADING,
            MovementDemand.KNEELING,
        ),
        "world_stretch" to setOf(
            MovementDemand.FLOOR,
            MovementDemand.WRIST_LOADING,
            MovementDemand.KNEELING,
        ),
        "hip_90_90" to setOf(MovementDemand.FLOOR),
        "wall_slide" to setOf(MovementDemand.OVERHEAD),
        "child_pose" to setOf(
            MovementDemand.FLOOR,
            MovementDemand.KNEELING,
        ),
        "wall_push_up" to setOf(MovementDemand.WRIST_LOADING),
        "bird_dog" to setOf(
            MovementDemand.FLOOR,
            MovementDemand.WRIST_LOADING,
            MovementDemand.KNEELING,
        ),
        "incline_plank" to setOf(MovementDemand.WRIST_LOADING),
        "db_split_squat" to setOf(MovementDemand.BALANCE_CHALLENGE),
        "db_close_grip_floor_press" to setOf(MovementDemand.FLOOR),
        "db_single_arm_press" to setOf(MovementDemand.OVERHEAD),
        "suitcase_carry" to setOf(MovementDemand.BALANCE_CHALLENGE),
        "assisted_pull_up" to setOf(MovementDemand.OVERHEAD),
        "side_step_reach" to setOf(MovementDemand.OVERHEAD),
        "sit_to_stand_reach" to setOf(MovementDemand.OVERHEAD),
        "standing_knee_drive" to setOf(MovementDemand.BALANCE_CHALLENGE),
        "lateral_toe_tap" to setOf(MovementDemand.BALANCE_CHALLENGE),
        "wall_walkout" to setOf(
            MovementDemand.FAST_TRANSITION,
            MovementDemand.WRIST_LOADING,
        ),
        "half_kneeling_stretch" to setOf(
            MovementDemand.FLOOR,
            MovementDemand.KNEELING,
        ),
        "knee_to_chest" to setOf(MovementDemand.FLOOR),
    )

    fun workoutFor(profile: UserFitnessProfile, date: LocalDate = LocalDate.now()): DailyWorkout {
        val isTrainingDay = isWorkoutDay(profile, date)
        val base = if (!isTrainingDay) {
            mobility
        } else {
            when (profile.goal) {
                FitnessGoal.IMPROVE_MOBILITY -> mobility
                FitnessGoal.LOSE_WEIGHT -> conditioning
                FitnessGoal.BUILD_STRENGTH,
                FitnessGoal.GENERAL_FITNESS,
                -> when (profile.equipment) {
                    Equipment.BODYWEIGHT -> bodyweightStrength
                    Equipment.DUMBBELLS -> dumbbellStrength
                    Equipment.FULL_GYM -> gymStrength
                }
            }
        }

        val count = when {
            profile.sessionMinutes <= 20 -> 4
            profile.sessionMinutes <= 35 -> 5
            else -> 6
        }
        val rotation = if (profile.personality == WorkoutPersonality.VARIETY) {
            date.dayOfYear % base.size
        } else {
            0
        }
        val selected = applyMovementPreferences(
            profile = profile,
            exercises = List(count) { index -> base[(index + rotation) % base.size] },
        )
            .map { it.adjustFor(profile.experience) }
            .map { it.adjustFor(profile.personality) }

        val title = if (!isTrainingDay) {
            "Recovery reset"
        } else {
            when (profile.goal) {
                FitnessGoal.BUILD_STRENGTH -> "Total-body strength"
                FitnessGoal.LOSE_WEIGHT -> "Full-body conditioning"
                FitnessGoal.IMPROVE_MOBILITY -> "Mobility reset"
                FitnessGoal.GENERAL_FITNESS -> "Everyday fitness"
            }
        }

        return DailyWorkout(
            date = date,
            title = title,
            focus = if (isTrainingDay) {
                "${profile.goal.label} · ${profile.personality.label} pace"
            } else {
                "Gentle movement · recovery day"
            },
            estimatedMinutes = profile.sessionMinutes,
            exercises = selected,
        )
    }

    fun weekFor(
        profile: UserFitnessProfile,
        today: LocalDate = LocalDate.now(),
    ): List<WeekDayPlan> {
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val workoutIndexes = workoutIndexes(profile.daysPerWeek)

        return (0 until 7).map { index ->
            val date = monday.plusDays(index.toLong())
            val isWorkout = index in workoutIndexes
            WeekDayPlan(
                shortDay = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                focus = if (isWorkout) workoutFor(profile, date).title else "Recovery",
                isWorkoutDay = isWorkout,
                isToday = date == today,
            )
        }
    }

    fun substitutionFor(
        profile: UserFitnessProfile,
        exerciseId: String,
    ): Exercise? {
        val preferred = substitutions[exerciseId]
            ?.takeIf { isCompatible(it.id, profile.movementPreferences) }
            ?: movementAlternatives[exerciseId]
                ?.firstOrNull { isCompatible(it.id, profile.movementPreferences) }
            ?: return null
        return preferred
            .copy(sourceExerciseId = exerciseId)
            .adjustFor(profile.experience)
            .adjustFor(profile.personality)
    }

    fun sanitizeSubstitutions(
        profile: UserFitnessProfile,
        workout: DailyWorkout,
        selectedSubstitutions: Map<String, String>,
    ): Map<String, String> {
        val workoutSlots = workout.exercises.associateBy(Exercise::sourceExerciseId)
        return selectedSubstitutions.filter { (originalId, replacementId) ->
            val workoutExercise = workoutSlots[originalId]
            workoutExercise != null &&
                workoutExercise.adjustmentReason == null &&
                substitutionFor(profile, originalId)?.id == replacementId
        }
    }

    fun applySubstitutions(
        profile: UserFitnessProfile,
        workout: DailyWorkout,
        selectedSubstitutions: Map<String, String>,
    ): DailyWorkout {
        val validSubstitutions = sanitizeSubstitutions(
            profile = profile,
            workout = workout,
            selectedSubstitutions = selectedSubstitutions,
        )
        return workout.copy(
            exercises = workout.exercises.map { exercise ->
                val originalId = exercise.sourceExerciseId
                if (originalId in validSubstitutions) {
                    substitutionFor(profile, originalId) ?: exercise
                } else {
                    exercise
                }
            },
        )
    }

    fun substitutionExerciseIds(): Set<String> =
        (substitutions.values + movementPreferenceExercises)
            .map(Exercise::id)
            .toSet()

    fun isCompatible(
        exerciseId: String,
        preferences: Set<MovementPreference>,
    ): Boolean {
        val blockedDemands = preferences.map { it.blockedDemand }.toSet()
        return movementDemands[exerciseId].orEmpty().none(blockedDemands::contains)
    }

    private fun applyMovementPreferences(
        profile: UserFitnessProfile,
        exercises: List<Exercise>,
    ): List<Exercise> {
        if (profile.movementPreferences.isEmpty()) return exercises

        val usedExerciseIds = mutableSetOf<String>()
        return exercises.map { original ->
            val chosen = if (isCompatible(original.id, profile.movementPreferences)) {
                original
            } else {
                movementAlternatives[original.id]
                    ?.firstOrNull { candidate ->
                        candidate.id !in usedExerciseIds &&
                            isCompatible(candidate.id, profile.movementPreferences)
                    }
                    ?: movementAlternatives[original.id]
                        ?.firstOrNull { candidate ->
                            isCompatible(candidate.id, profile.movementPreferences)
                        }
                    ?: preferenceExercise("standing_core_brace")
            }
            usedExerciseIds += chosen.id
            if (chosen.id == original.id) {
                original
            } else {
                val reasons = profile.movementPreferences
                    .filter { preference ->
                        preference.blockedDemand in movementDemands[original.id].orEmpty()
                    }
                    .joinToString { it.label }
                chosen.copy(
                    sourceExerciseId = original.id,
                    adjustmentReason = "Chosen for: $reasons",
                )
            }
        }
    }

    private fun preferenceExercise(id: String): Exercise =
        movementPreferenceExerciseById.getValue(id)

    private val MovementPreference.blockedDemand: MovementDemand
        get() = when (this) {
            MovementPreference.GENTLE_TRANSITIONS -> MovementDemand.FAST_TRANSITION
            MovementPreference.NO_FLOOR_EXERCISES -> MovementDemand.FLOOR
            MovementPreference.LIMIT_OVERHEAD -> MovementDemand.OVERHEAD
            MovementPreference.LIMIT_WRIST_LOADING -> MovementDemand.WRIST_LOADING
            MovementPreference.NO_KNEELING -> MovementDemand.KNEELING
            MovementPreference.EXTRA_BALANCE_SUPPORT -> MovementDemand.BALANCE_CHALLENGE
        }

    private fun exercise(
        id: String,
        name: String,
        prescription: String,
        cue: String,
    ) = Exercise(
        id = id,
        name = name,
        prescription = prescription,
        restSeconds = 45,
        coachingCue = cue,
    )

    private fun Exercise.adjustFor(level: ExperienceLevel): Exercise = when (level) {
        ExperienceLevel.BEGINNER -> copy(
            prescription = prescription.replace("3 ×", "2 ×"),
            restSeconds = 60,
        )
        ExperienceLevel.INTERMEDIATE -> this
        ExperienceLevel.ADVANCED -> copy(
            prescription = prescription.replace("3 ×", "4 ×"),
            restSeconds = 45,
        )
    }

    private fun Exercise.adjustFor(personality: WorkoutPersonality): Exercise = when (personality) {
        WorkoutPersonality.STEADY,
        WorkoutPersonality.VARIETY,
        -> this
        WorkoutPersonality.CHALLENGE -> copy(
            prescription = prescription.replace("3 ×", "4 ×"),
            restSeconds = (restSeconds - 10).coerceAtLeast(30),
            coachingCue = "$coachingCue. Keep the final reps controlled.",
        )
        WorkoutPersonality.GUIDED -> copy(
            restSeconds = restSeconds.coerceAtLeast(60),
            coachingCue = "Move slowly: $coachingCue.",
        )
    }

    fun isWorkoutDay(profile: UserFitnessProfile, date: LocalDate): Boolean =
        (date.dayOfWeek.value - 1) in workoutIndexes(profile.daysPerWeek)

    private fun workoutIndexes(daysPerWeek: Int): Set<Int> = when (daysPerWeek) {
        2 -> setOf(0, 3)
        3 -> setOf(0, 2, 4)
        4 -> setOf(0, 1, 3, 5)
        5 -> setOf(0, 1, 2, 4, 5)
        else -> (0 until 7).toSet()
    }
}
