package com.sweatlock.workout

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class WorkoutUiState(
    val exercise: Exercise = Exercise.PUSHUP,
    val goal: Int = 10,
    val state: RepState = RepState(0, RepPhase.UP, 180f, false),
    val landmarks: Map<Int, Landmark> = emptyMap(),
    val completed: Boolean = false
)

@HiltViewModel
class WorkoutViewModel @Inject constructor() : ViewModel() {
    private var counter = RepCounter(Exercise.PUSHUP)
    private val _ui = MutableStateFlow(WorkoutUiState())
    val ui: StateFlow<WorkoutUiState> = _ui.asStateFlow()

    fun onLandmarks(lm: Map<Int, Landmark>) {
        val s = counter.update(lm, SystemClock.elapsedRealtime())
        _ui.update { it.copy(state = s, landmarks = lm, completed = s.reps >= it.goal) }
    }

    fun setExercise(e: Exercise) {
        counter = RepCounter(e)
        _ui.update { WorkoutUiState(exercise = e, goal = it.goal) }
    }

    fun restart() = setExercise(_ui.value.exercise)
}
