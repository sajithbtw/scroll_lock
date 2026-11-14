package com.sweatlock.blocking

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import com.sweatlock.ui.theme.SweatLockTheme
import com.sweatlock.ui.workout.WorkoutScreen
import dagger.hilt.android.AndroidEntryPoint

/** Full-screen "do your reps" screen shown over a blocked app. */
@AndroidEntryPoint
class LockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pkg = intent.getStringExtra(EXTRA_PKG)
        val label = pkg?.let {
            runCatching { packageManager.getApplicationLabel(packageManager.getApplicationInfo(it, 0)).toString() }
                .getOrNull()
        } ?: "this app"

        // Back must not return to the blocked app; send the user home instead.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })

        setContent {
            SweatLockTheme {
                WorkoutScreen(
                    lockedPackage = pkg,
                    lockedLabel = label,
                    onUnlocked = {
                        pkg?.let { packageManager.getLaunchIntentForPackage(it)?.let(::startActivity) }
                        finish()
                    },
                    onClose = ::goHome
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate() // a different blocked app triggered the lock: rebuild with the new package
    }

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    companion object { const val EXTRA_PKG = "pkg" }
}
