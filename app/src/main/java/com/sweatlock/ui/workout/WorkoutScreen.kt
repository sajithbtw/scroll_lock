package com.sweatlock.ui.workout

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sweatlock.ui.theme.*
import com.sweatlock.workout.*
import java.util.concurrent.Executors

@Composable
fun WorkoutScreen(vm: WorkoutViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var hasCamera by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCamera = it }
    LaunchedEffect(Unit) { if (!hasCamera) launcher.launch(Manifest.permission.CAMERA) }

    Column(
        Modifier.fillMaxSize().background(Bg).statusBarsPadding().navigationBarsPadding().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Do ${ui.goal} ${if (ui.exercise == Exercise.PUSHUP) "pushups" else "squats"}",
            fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("to unlock your apps", color = Color.Gray)
        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Exercise.values().forEach { e ->
                val sel = e == ui.exercise
                Button(
                    onClick = { vm.setExercise(e) },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (sel) Accent else Card,
                        contentColor = if (sel) Color.White else Color.Black)
                ) { Text(if (e == Exercise.PUSHUP) "Pushups" else "Squats") }
            }
        }
        Spacer(Modifier.height(14.dp))

        Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(28.dp)).background(Color.Black)) {
            if (hasCamera) {
                CameraPreview(vm::onLandmarks, Modifier.fillMaxSize())
                SkeletonOverlay(ui.landmarks, ui.state.fullBodyVisible, Modifier.fillMaxSize())
            } else {
                // Edge case: permission denied -> explain + deep-link to app settings
                Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Camera access is needed to count your reps.", color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", ctx.packageName, null)))
                    }, shape = RoundedCornerShape(50)) { Text("Open settings") }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Card),
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${ui.state.reps} / ${ui.goal}", fontSize = 48.sp, fontWeight = FontWeight.Bold)
                val msg = when {
                    ui.completed -> "Unlocked! \uD83C\uDF89"
                    !ui.state.fullBodyVisible -> "Step back: show your full body"
                    ui.state.lastRepRejected -> "Too fast, slow down"
                    else -> "Phase: ${ui.state.phase.name} - angle ${ui.state.angle.toInt()}\u00B0"
                }
                Text(msg, color = when {
                    ui.completed -> Good
                    !ui.state.fullBodyVisible || ui.state.lastRepRejected -> Warn
                    else -> Color.Gray
                })
                if (ui.completed) {
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = vm::restart, shape = RoundedCornerShape(50)) { Text("Go again") }
                }
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
