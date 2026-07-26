package com.ahsanrehmat.pulseplan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ahsanrehmat.pulseplan.ui.PulsePlanApp
import com.ahsanrehmat.pulseplan.ui.PulsePlanViewModel
import com.ahsanrehmat.pulseplan.ui.theme.PulsePlanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PulsePlanTheme {
                val viewModel: PulsePlanViewModel = viewModel()
                PulsePlanApp(viewModel = viewModel)
            }
        }
        if (
            BuildConfig.DEBUG &&
            intent.getBooleanExtra(CRASHLYTICS_TEST_EXTRA, false)
        ) {
            window.decorView.post {
                error("PulsePlan Crashlytics verification")
            }
        }
    }

    private companion object {
        const val CRASHLYTICS_TEST_EXTRA = "crashlytics_test"
    }
}
