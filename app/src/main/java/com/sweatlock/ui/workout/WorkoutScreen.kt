package com.sweatlock.ui.workout

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sweatlock.blocking.Perms
import com.sweatlock.ui.theme.*
import com.sweatlock.workout.*
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

/**
 * Used in two modes:
 *  - practice (lockedPackage == null): exercise toggle visible, nothing gets unlocked
 *  - locked (from LockActivity): exercise/reps come from Settings; finishing unlocks [lockedPackage]
 */
@Composable
fun WorkoutScreen(
    lockedPackage: String? = null,
    lockedLabel: String? = null,
    onUnlocked: () -> Unit = {},
    onClose: (() -> Unit)? = null,
    vm: WorkoutViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var hasCamera by remember { mutableStateOf(Perms.camera(ctx)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCamera = it }

    LaunchedEffect(Unit) { if (!hasCamera) launcher.launch(Manifest.permission.CAMERA) }
    LaunchedEffect(lockedPackage) { vm.setLockedPackage(lockedPackage) }
    LaunchedEffect(ui.completed) {
        if (ui.completed && lockedPackage != null) { delay(1200); onUnlocked() }
    }

    val name = if (ui.exercise == Exercise.PUSHUP) "pushups" else "squats"

    GlassBackground {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Do ${ui.goal} $name", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(
                if (lockedLabel != null) "to unlock $lockedLabel" else "practice mode",
                color = Muted, fontSize = 16.sp
            )
            Spacer(Modifier.height(14.dp))

            if (lockedPackage == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassButton("Pushups", { vm.setExercise(Exercise.PUSHUP) }, primary = ui.exercise == Exercise.PUSHUP)
                    GlassButton("Squats", { vm.setExercise(Exercise.SQUAT) }, primary = ui.exercise == Exercise.SQUAT)
                }
                Spacer(Modifier.height(14.dp))
            }

            GlassCard(Modifier.fillMaxWidth()) {
                Box(
                    Modifier.padding(6.dp).fillMaxWidth().aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(22.dp)).background(Color.Black)
                ) {
                    if (hasCamera) {
                        CameraPreview(vm::onLandmarks, Modifier.fillMaxSize())
                        SkeletonOverlay(ui.landmarks, ui.state.fullBodyVisible, Modifier.fillMaxSize())
                    } else {
                        // Edge case: permission denied -> explain + deep-link to app settings
                        Column(
                            Modifier.align(Alignment.Center).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Camera access is needed to count your reps.", color = Color.White, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(12.dp))
                            GlassButton("Open settings", { ctx.startActivity(Perms.appDetailsIntent(ctx)) }, primary = true)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${ui.state.reps} / ${ui.goal}", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (ui.state.reps.toFloat() / ui.goal).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                        color = if (ui.completed) Good else Accent,
                        trackColor = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(10.dp))
                    val msg = when {
                        ui.completed && lockedPackage != null -> "Unlocked for ${ui.durationMin} min \uD83C\uDF89"
                        ui.completed -> "Nice work! \uD83C\uDF89"
                        !ui.state.fullBodyVisible -> "Step back: show your full body"
                        ui.state.lastRepRejected -> "Too fast, slow down"
                        else -> "${ui.state.phase.name} \u00B7 angle ${ui.state.angle.toInt()}\u00B0"
                    }
                    Text(
                        msg, textAlign = TextAlign.Center,
                        color = when {
                            ui.completed -> Good
                            !ui.state.fullBodyVisible || ui.state.lastRepRejected -> Warn
                            else -> Muted
                        }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (lockedPackage == null && ui.completed) GlassButton("Go again", vm::restart, primary = true)
                onClose?.let { GlassButton(if (lockedPackage != null) "Not now" else "Close", it) }
            }
        }
    }
}

@Composable
private fun CameraPreview(onLandmarks: (Map<Int, Landmark>) -> Unit, modifier: Modifier) {
    val owner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    AndroidView(modifier = modifier, factory = { c ->
        val pv = PreviewView(c).apply { scaleType = PreviewView.ScaleType.FIT_CENTER }
        val future = ProcessCameraProvider.getInstance(c)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                .also { it.setAnalyzer(executor, PoseAnalyzer(onLandmarks)) }
            provider.unbindAll()
            provider.bindToLifecycle(owner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)
        }, ContextCompat.getMainExecutor(c))
        pv
    })
}

private val BONES = listOf(
    11 to 12, 11 to 13, 13 to 15, 12 to 14, 14 to 16,
    11 to 23, 12 to 24, 23 to 24, 23 to 25, 25 to 27, 24 to 26, 26 to 28
)

/** Skeleton overlay. x is mirrored because the front camera preview is mirrored. */
@Composable
private fun SkeletonOverlay(lm: Map<Int, Landmark>, fullBody: Boolean, modifier: Modifier) {
    val color = if (fullBody) Good else Warn
    Canvas(modifier) {
        fun pt(l: Landmark) = Offset((1f - l.x) * size.width, l.y * size.height)
        BONES.forEach { (a, b) ->
            val la = lm[a]; val lb = lm[b]
            if (la != null && lb != null) drawLine(color, pt(la), pt(lb), 8f, StrokeCap.Round)
        }
        lm.values.forEach { drawCircle(Color.White, 7f, pt(it)) }
    }
}
