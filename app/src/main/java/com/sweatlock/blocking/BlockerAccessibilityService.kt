package com.sweatlock.blocking

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.sweatlock.data.AppSettings
import com.sweatlock.data.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.*

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BlockerEntryPoint {
    fun settings(): SettingsRepository
    fun unlocks(): UnlockManager
}

/**
 * Watches which app comes to the front. If it is a blocked app and its unlock timer
 * is not running, opens LockActivity. A 1.5s ticker also re-locks when a timer expires
 * while the user is still inside the app.
 */
class BlockerAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var unlocks: UnlockManager
    @Volatile private var cfg = AppSettings()
    private var currentPkg: String? = null
    private var lastLaunch = 0L

    override fun onServiceConnected() {
        val ep = EntryPointAccessors.fromApplication(applicationContext, BlockerEntryPoint::class.java)
        unlocks = ep.unlocks()
        scope.launch { unlocks.load() }
        scope.launch { ep.settings().settings.collect { cfg = it } }
        scope.launch {
            while (isActive) {
                delay(1500)
                // Accessibility events are primary; Usage Access is only a fallback.
                checkAndLock(currentPkg ?: Perms.foregroundFromUsage(this@BlockerAccessibilityService))
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) { currentPkg = pkg; return }
        // Notification shade / keyboard windows are not "the app the user is in".
        if (pkg == "com.android.systemui" || pkg.contains("inputmethod")) return
        currentPkg = pkg
        checkAndLock(pkg)
    }

    private fun checkAndLock(pkg: String?) {
        if (pkg == null || !::unlocks.isInitialized) return
        if (!cfg.enabled || pkg !in cfg.blocked || unlocks.isUnlocked(pkg)) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastLaunch < 2000) return           // debounce
        lastLaunch = now
        startActivity(
            Intent(this, LockActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(LockActivity.EXTRA_PKG, pkg)
        )
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
