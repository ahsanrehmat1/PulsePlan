package com.ahsanrehmat.pulseplan

import android.app.Application
import com.ahsanrehmat.pulseplan.notifications.WorkoutReminder
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.crashlytics.FirebaseCrashlytics

class PulsePlanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeFirebaseIfConfigured()
        configureCrashReporting()
        WorkoutReminder.createChannel(this)
    }

    private fun initializeFirebaseIfConfigured() {
        val valuesArePresent = listOf(
            BuildConfig.FIREBASE_API_KEY,
            BuildConfig.FIREBASE_APP_ID,
            BuildConfig.FIREBASE_PROJECT_ID,
        ).all(String::isNotBlank)

        if (valuesArePresent && FirebaseApp.getApps(this).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setApiKey(BuildConfig.FIREBASE_API_KEY)
                .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                .build()
            FirebaseApp.initializeApp(this, options)
        }
    }

    private fun configureCrashReporting() {
        if (FirebaseApp.getApps(this).isEmpty()) return

        FirebaseCrashlytics.getInstance().apply {
            setCrashlyticsCollectionEnabled(true)
            setCustomKey("app_version", BuildConfig.VERSION_NAME)
            setCustomKey("build_type", BuildConfig.BUILD_TYPE)
            log("PulsePlan started")
        }
    }
}
