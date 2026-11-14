package com.sweatlock.workout

import com.sweatlock.data.computeStreak
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class RepCounterTest {
    /** Full-body pose (spread over the frame) whose left elbow has the given angle. */
    private fun pose(elbowDeg: Double): Map<Int, Landmark> {
        val m = mutableMapOf<Int, Landmark>()
        for (i in listOf(12, 14, 16)) m[i] = Landmark(0.5f, 0.3f, 0.9f)
        for (i in listOf(23, 24)) m[i] = Landmark(0.5f, 0.6f, 0.9f)
        for (i in listOf(25, 26)) m[i] = Landmark(0.5f, 0.75f, 0.9f)
        for (i in listOf(27, 28)) m[i] = Landmark(0.5f, 0.9f, 0.9f)
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
        assertFalse(c.update(pose(170.0) - 27, 0).fullBodyVisible)
    }

    @Test fun ignoresTinyBody() {
        val c = RepCounter(Exercise.PUSHUP)
        val tiny = pose(170.0).mapValues { (_, l) -> Landmark(0.5f + (l.x - 0.5f) * 0.2f, 0.5f + (l.y - 0.5f) * 0.2f, l.visibility) }
        assertFalse(c.update(tiny, 0).fullBodyVisible)
    }

    @Test fun streakCountsConsecutiveDays() {
        assertEquals(3, computeStreak(listOf(10, 9, 8, 5), 10))
        assertEquals(2, computeStreak(listOf(9, 8), 10)) // not done yet today, streak still alive
        assertEquals(0, computeStreak(listOf(5), 10))
    }
}
