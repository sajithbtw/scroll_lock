package com.sweatlock.workout

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sweatlock.blocking.UnlockManager
import com.sweatlock.data.SettingsRepository
import com.sweatlock.data.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutUiState(
    val exercise: Exercise = Exercise.PUSHUP,
    val goal: Int = 10,
    val durationMin: Int = 15,
    val state: RepState = RepState(0, RepPhase.UP, 180f, false),
    val landmarks: Map<Int, Landmark> = emptyMap(),
    val completed: Boolean = false
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val stats: StatsRepository,
    private val unlocks: UnlockManager
) : ViewModel() {

    private var counter = RepCounter(Exercise.PUSHUP)
    private var lockedPkg: String? = null
    private var logged = 0
    private var finished = false

    private val _ui = MutableStateFlow(WorkoutUiState())
    val ui: StateFlow<WorkoutUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val s = settingsRepo.settings.first()
            counter = RepCounter(s.exercise)
            _ui.update { WorkoutUiState(exercise = s.exercise, goal = s.reps, durationMin = s.durationMin) }
        }
    }

    fun setLockedPackage(pkg: String?) { lockedPkg = pkg }

    fun onLandmarks(lm: Map<Int, Landmark>) {
        if (finished) { _ui.update { it.copy(landmarks = lm) }; return }
        val s = counter.update(lm, SystemClock.elapsedRealtime())

        val delta = s.reps - logged
        if (delta > 0) { logged = s.reps; viewModelScope.launch { stats.addReps(delta) } }

        val done = s.reps >= _ui.value.goal
        if (done) {
            finished = true
            lockedPkg?.let { pkg ->
                viewModelScope.launch { unlocks.unlock(pkg, _ui.value.durationMin) }
            }
        }
        _ui.update { it.copy(state = s, landmarks = lm, completed = done) }
    }

    fun setExercise(e: Exercise) {
        counter = RepCounter(e); logged = 0; finished = false
        _ui.update { it.copy(exercise = e, state = RepState(0, RepPhase.UP, 180f, false), completed = false) }
    }

    fun restart() = setExercise(_ui.value.exercise)
}
