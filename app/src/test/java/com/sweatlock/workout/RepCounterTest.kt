package com.sweatlock.workout

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class RepCounterTest {
    /** Full-body pose whose left elbow has the given angle. */
    private fun pose(elbowDeg: Double): Map<Int, Landmark> {
        val m = mutableMapOf<Int, Landmark>()
        for (i in (11..16) + (23..28)) m[i] = Landmark(0.5f, 0.5f, 0.9f)
        m[11] = Landmark(0.3f, 0.3f, 0.9f)
        m[13] = Landmark(0.3f, 0.5f, 0.9f)
        val r = Math.toRadians(elbowDeg)
        m[15] = Landmark((0.3 + 0.2 * sin(r)).toFloat(), (0.5 - 0.2 * cos(r)).toFloat(), 0.9f)
        return m
    }

    @Test fun countsOneSlowRep() {
        val c = RepCounter(Exercise.PUSHUP)
        c.update(pose(170.0), 0); c.update(pose(70.0), 1500)
        assertEquals(1, c.update(pose(170.0), 3000).reps)
    }

    @Test fun rejectsFastRep() {
        val c = RepCounter(Exercise.PUSHUP)
        c.update(pose(170.0), 0); c.update(pose(70.0), 1500); c.update(pose(170.0), 3000)
        c.update(pose(70.0), 3100)
        val s = c.update(pose(170.0), 3200)
        assertEquals(1, s.reps); assertTrue(s.lastRepRejected)
    }

    @Test fun ignoresWhenBodyOutOfFrame() {
        val c = RepCounter(Exercise.PUSHUP)
        val s = c.update(pose(170.0) - 27, 0)
        assertFalse(s.fullBodyVisible)
    }
}
