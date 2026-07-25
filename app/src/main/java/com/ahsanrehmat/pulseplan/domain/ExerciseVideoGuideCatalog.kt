package com.ahsanrehmat.pulseplan.domain

import com.ahsanrehmat.pulseplan.model.Exercise
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class ExerciseVideoGuide(
    val searchQuery: String,
    val searchUrl: String,
)

object ExerciseVideoGuideCatalog {
    private val searchQueries = mapOf(
        "squat" to "bodyweight squat proper form beginner exercise demonstration",
        "push_up" to "incline push up proper form beginner exercise demonstration",
        "reverse_lunge" to "bodyweight reverse lunge proper form beginner exercise demonstration",
        "glute_bridge" to "glute bridge proper form beginner exercise demonstration",
        "dead_bug" to "dead bug exercise proper form beginner demonstration",
        "plank" to "forearm plank proper form beginner exercise demonstration",
        "db_goblet_squat" to "dumbbell goblet squat proper form exercise demonstration",
        "db_floor_press" to "dumbbell floor press proper form exercise demonstration",
        "db_row" to "one arm dumbbell row proper form exercise demonstration",
        "db_rdl" to "dumbbell Romanian deadlift proper form exercise demonstration",
        "db_press" to "seated dumbbell shoulder press proper form demonstration",
        "farmer_carry" to "dumbbell farmer carry proper form exercise demonstration",
        "leg_press" to "leg press machine proper form beginner demonstration",
        "chest_press" to "chest press machine proper form beginner demonstration",
        "lat_pulldown" to "lat pulldown machine proper form beginner demonstration",
        "cable_row" to "seated cable row proper form beginner demonstration",
        "hamstring_curl" to "hamstring curl machine proper form beginner demonstration",
        "pallof_press" to "cable Pallof press proper form beginner demonstration",
        "march" to "low impact march in place proper form exercise demonstration",
        "step_jack" to "low impact step jack exercise demonstration",
        "squat_reach" to "bodyweight squat to overhead reach exercise demonstration",
        "mountain_climber" to "incline mountain climber low impact demonstration",
        "skater_step" to "low impact skater step exercise demonstration",
        "slow_burpee" to "low impact step back burpee beginner demonstration",
        "cat_cow" to "cat cow mobility exercise proper form demonstration",
        "world_stretch" to "world's greatest stretch proper form demonstration",
        "hip_90_90" to "90 90 hip mobility exercise proper form demonstration",
        "wall_slide" to "wall slide shoulder mobility proper form demonstration",
        "ankle_rock" to "ankle rock dorsiflexion mobility exercise demonstration",
        "child_pose" to "child's pose proper form beginner demonstration",
        "chair_squat" to "chair squat sit to stand proper form beginner demonstration",
        "wall_push_up" to "wall push up proper form beginner demonstration",
        "supported_split_squat" to "supported split squat proper form beginner demonstration",
        "standing_hip_extension" to "standing hip extension bodyweight exercise demonstration",
        "bird_dog" to "bird dog exercise proper form beginner demonstration",
        "incline_plank" to "incline plank proper form beginner demonstration",
        "db_split_squat" to "dumbbell split squat proper form demonstration",
        "db_close_grip_floor_press" to "close grip dumbbell floor press proper form",
        "db_two_arm_row" to "two arm dumbbell bent over row proper form",
        "db_sumo_deadlift" to "dumbbell sumo deadlift proper form demonstration",
        "db_single_arm_press" to "single arm seated dumbbell shoulder press proper form",
        "suitcase_carry" to "dumbbell suitcase carry proper form demonstration",
        "hack_squat" to "hack squat machine proper form beginner demonstration",
        "cable_chest_press" to "standing cable chest press proper form demonstration",
        "assisted_pull_up" to "assisted pull up machine proper form beginner",
        "chest_supported_row" to "chest supported machine row proper form",
        "seated_hamstring_curl" to "seated hamstring curl machine proper form",
        "pallof_hold" to "cable Pallof hold proper form beginner",
        "heel_dig" to "low impact alternating heel digs exercise demonstration",
        "side_step_reach" to "low impact side step and reach exercise demonstration",
        "sit_to_stand_reach" to "chair sit to stand with overhead reach demonstration",
        "standing_knee_drive" to "low impact standing knee drive exercise demonstration",
        "lateral_toe_tap" to "low impact lateral toe tap exercise demonstration",
        "wall_walkout" to "wall walkout beginner exercise demonstration",
        "seated_cat_cow" to "seated cat cow mobility exercise demonstration",
        "half_kneeling_stretch" to "half kneeling hip flexor stretch proper form",
        "seated_hip_rotation" to "seated hip internal external rotation mobility exercise",
        "shoulder_circle" to "shoulder circles mobility proper form demonstration",
        "wall_calf_stretch" to "standing wall calf stretch proper form",
        "forearm_wall_press" to "forearm wall press proper form beginner demonstration",
        "standing_cross_crawl" to "standing cross crawl exercise beginner demonstration",
        "standing_core_brace" to "standing abdominal bracing exercise proper form",
        "db_chest_squeeze" to "standing dumbbell chest squeeze proper form",
        "db_lateral_raise" to "dumbbell lateral raise below shoulder proper form",
        "supported_march" to "supported standing march chair beginner demonstration",
        "supported_side_step" to "supported side step chair low impact demonstration",
        "supported_knee_drive" to "supported standing knee drive beginner demonstration",
        "supported_lateral_tap" to "supported lateral toe tap beginner demonstration",
        "standing_hip_flexor_stretch" to "standing supported hip flexor stretch proper form",
        "shoulder_blade_squeeze" to "standing shoulder blade squeeze proper form",
        "seated_breathing" to "seated diaphragmatic breathing beginner demonstration",
        "knee_to_chest" to "single knee to chest stretch proper form",
    )

    fun forExercise(exercise: Exercise): ExerciseVideoGuide {
        val query = searchQueries[exercise.id]
            ?: "${exercise.name} proper form beginner exercise demonstration"
        return ExerciseVideoGuide(
            searchQuery = query,
            searchUrl = "https://www.youtube.com/results?search_query=${encode(query)}",
        )
    }

    fun hasVideoGuide(exerciseId: String): Boolean = exerciseId in searchQueries

    private fun encode(query: String): String =
        URLEncoder.encode(query, StandardCharsets.UTF_8.name())
}
