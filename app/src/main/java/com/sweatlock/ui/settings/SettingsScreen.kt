package com.sweatlock.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sweatlock.blocking.Perms
import com.sweatlock.ui.MainViewModel
import com.sweatlock.ui.theme.*
import com.sweatlock.workout.Exercise

@Composable
fun SettingsScreen(onPermissions: () -> Unit, vm: MainViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Ink)

        GlassCard(Modifier.fillMaxWidth()) {
            Stepper("Reps required", s.reps, "", vm::setReps, 1, 100, 1)
            Stepper("Unlock duration", s.durationMin, "min", vm::setDuration, 5, 180, 5)
            Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Exercise", fontSize = 17.sp, color = Ink)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassButton("Pushups", { vm.setExercise(Exercise.PUSHUP) }, primary = s.exercise == Exercise.PUSHUP)
                    GlassButton("Squats", { vm.setExercise(Exercise.SQUAT) }, primary = s.exercise == Exercise.SQUAT)
                }
            }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Setup", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Text("If blocking stops working, re-check permissions and let SweatLock run unrestricted in the background.", color = Muted, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassButton("Permissions", onPermissions, primary = true)
                    GlassButton("Battery", { runCatching { ctx.startActivity(Perms.batteryIntent()) } })
                }
            }
        }
    }
}

@Composable
private fun Stepper(label: String, value: Int, unit: String, onChange: (Int) -> Unit, min: Int, max: Int, step: Int) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 17.sp, color = Ink)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RoundGlassButton("\u2212") { onChange((value - step).coerceAtLeast(min)) }
            Text("$value $unit".trim(), fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 17.sp)
            RoundGlassButton("+") { onChange((value + step).coerceAtMost(max)) }
        }
    }
}
