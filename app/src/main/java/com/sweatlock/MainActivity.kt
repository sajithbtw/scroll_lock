package com.sweatlock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sweatlock.ui.theme.SweatLockTheme
import com.sweatlock.ui.workout.WorkoutScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SweatLockTheme { WorkoutScreen() } }
    }
}
