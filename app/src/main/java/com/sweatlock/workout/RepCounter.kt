package com.sweatlock.workout

import kotlin.math.abs
import kotlin.math.atan2

enum class Exercise { PUSHUP, SQUAT }
enum class RepPhase { UP, DOWN }

/** Normalized (0..1) landmark. [visibility] is ML Kit's inFrameLikelihood. */
data class Landmark(val x: Float, val y: Float, val visibility: Float)

data class RepState(
    val reps: Int,
    val phase: RepPhase,
    val angle: Float,
    val fullBodyVisible: Boolean,
    val lastRepRejected: Boolean = false // true when a rep was too fast
)

/**
 * Pure Kotlin (no Android imports) so it is trivially unit-testable.
 * Feed it landmarks + a timestamp; it returns the current rep state.
 */
class RepCounter(
    exercise: Exercise,
    private val minRepIntervalMs: Long = 1000L, // anti-cheat: reject reps faster than 1s
    private val upThreshold: Float = 160f
) {
    private val isPushup = exercise == Exercise.PUSHUP
    private val downThreshold = if (isPushup) 90f else 100f

    private var reps = 0
    private var phase = RepPhase.UP
    private var lastRepTime = -minRepIntervalMs
    private var lastAngle = 180f

    private companion object {
        // ML Kit PoseLandmark indices: shoulder/elbow/wrist and hip/knee/ankle
        val ARM_L = Triple(11, 13, 15); val ARM_R = Triple(12, 14, 16)
        val LEG_L = Triple(23, 25, 27); val LEG_R = Triple(24, 26, 28)
        val REQUIRED = (11..16) + (23..28)
        const val MIN_VISIBILITY = 0.5f
    }

    fun update(lm: Map<Int, Landmark>, timestampMs: Long): RepState {
        // Anti-cheat: full body must be in frame, otherwise nothing counts.
        if (!fullBodyInFrame(lm)) return RepState(reps, phase, lastAngle, false)

        val angle = jointAngle(lm)
        lastAngle = angle
        var rejected = false

        when (phase) {
            RepPhase.UP -> if (angle < downThreshold) phase = RepPhase.DOWN
            RepPhase.DOWN -> if (angle > upThreshold) {
                phase = RepPhase.UP
                if (timestampMs - lastRepTime >= minRepIntervalMs) {
                    reps++
                    lastRepTime = timestampMs
                } else rejected = true
            }
        }
        return RepState(reps, phase, angle, true, rejected)
    }

    private fun fullBodyInFrame(lm: Map<Int, Landmark>) = REQUIRED.all { i ->
        lm[i]?.let { it.visibility >= MIN_VISIBILITY && it.x in 0f..1f && it.y in 0f..1f } == true
    }

    /** Uses the more visible side (pushups are usually filmed side-on). */
    private fun jointAngle(lm: Map<Int, Landmark>): Float {
        val (l, r) = if (isPushup) ARM_L to ARM_R else LEG_L to LEG_R
        fun vis(t: Triple<Int, Int, Int>) =
            lm[t.first]!!.visibility + lm[t.second]!!.visibility + lm[t.third]!!.visibility
        val t = if (vis(l) >= vis(r)) l else r
        return angle(lm[t.first]!!, lm[t.second]!!, lm[t.third]!!)
    }

    private fun angle(a: Landmark, b: Landmark, c: Landmark): Float {
        val rad = atan2(c.y - b.y, c.x - b.x) - atan2(a.y - b.y, a.x - b.x)
        var deg = abs(Math.toDegrees(rad.toDouble())).toFloat()
        if (deg > 180f) deg = 360f - deg
        return deg
    }
}
