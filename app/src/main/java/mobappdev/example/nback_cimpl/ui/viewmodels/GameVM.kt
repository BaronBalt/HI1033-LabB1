package mobappdev.example.nback_cimpl.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mobappdev.example.nback_cimpl.GameApplication
import mobappdev.example.nback_cimpl.NBackHelper
import mobappdev.example.nback_cimpl.data.UserPreferencesRepository

/**
 * This is the GameViewModel.
 *
 * It is good practice to first make an interface, which acts as the blueprint
 * for your implementation. With this interface we can create fake versions
 * of the viewmodel, which we can use to test other parts of our app that depend on the VM.
 *
 * Our viewmodel itself has functions to start a game, to specify a gametype,
 * and to check if we are having a match
 *
 * Date: 25-08-2023
 * Version: Version 1.0
 * Author: Yeetivity
 *
 */

interface GameViewModel {
    val gameState: StateFlow<GameState>
    val score: StateFlow<Int>
    val highscore: StateFlow<Int>
    val nBack: Int
    val timeBetweenEvents: Long
    val numEvents: Int

    fun setGameType(gameType: GameType)
    fun startGame()
    fun checkMatch()
}

class GameVM(
    private val userPreferencesRepository: UserPreferencesRepository
) : GameViewModel, ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    override val gameState: StateFlow<GameState> get() = _gameState.asStateFlow()

    private val _score = MutableStateFlow(0)
    override val score: StateFlow<Int> get() = _score

    private val _highscore = MutableStateFlow(0)
    override val highscore: StateFlow<Int> get() = _highscore

    private var _nBack = 2
    override val nBack: Int get() = _nBack

    private val _timeBetweenEvents = MutableStateFlow(1L) // seconds
    override val timeBetweenEvents: Long get() = _timeBetweenEvents.value

    private val _numEvents = MutableStateFlow(10)
    override val numEvents: Int get() = _numEvents.value

    private var job: Job? = null
    private val nBackHelper = NBackHelper()
    private var events = emptyArray<Int>()
    private var currentIndex = 0
    private var matchRegistered = false

    override fun setGameType(gameType: GameType) {
        _gameState.value = _gameState.value.copy(gameType = gameType)
    }

    override fun startGame() {
        job?.cancel()
        _score.value = 0
        currentIndex = 0
        matchRegistered = false

        events = nBackHelper
            .generateNBackString(numEvents, 9, 30, nBack)
            .toList()
            .toTypedArray()

        Log.d("GameVM", "Generated sequence: ${events.contentToString()}")

        job = viewModelScope.launch {
            when (gameState.value.gameType) {
                GameType.Audio -> runAudioGame()
                GameType.AudioVisual -> runAudioVisualGame()
                GameType.Visual -> runVisualGame(events)
                GameType.NoSelection -> throw IllegalStateException("No Game Type selected")
            }
            updateHighscore()
            _gameState.value = _gameState.value.copy(eventValue = NO_EVENT)
        }
    }

    override fun checkMatch() {
        if (currentIndex < nBack || matchRegistered) return

        val current = events[currentIndex]
        val previous = events[currentIndex - nBack]

        if (current == previous) {
            _score.value++
            Log.d("GameVM", "Match detected! Score = ${_score.value}")
        } else {
            _score.value--
            Log.d("GameVM", "No match.")
            _gameState.value = _gameState.value.copy(wrongGuess = true)
            viewModelScope.launch {
                delay(500)
                _gameState.value = _gameState.value.copy(wrongGuess = false)
            }
        }

        matchRegistered = true
    }

    private fun runAudioGame() {
        // Todo: Make work for Basic grade
    }

    private fun runAudioVisualGame() {
        // Todo: Make work for Higher grade
    }

    private suspend fun runVisualGame(events: Array<Int>) {
        for ((index, value) in events.withIndex()) {
            currentIndex = index
            matchRegistered = false

            _gameState.value = _gameState.value.copy(eventValue = value)
            Log.d("GameVM", "Showing event $value (index $index)")

            delay(timeBetweenEvents * 1000L) // use user-configurable time
            _gameState.value = _gameState.value.copy(eventValue = 0)
            delay(timeBetweenEvents * 1000L)
        }

        Log.d("GameVM", "Game finished with score ${_score.value}")
    }

    private suspend fun updateHighscore() {
        if (_score.value > _highscore.value) {
            userPreferencesRepository.saveHighScore(_score.value)
        }
    }

    fun setNBack(value: Int) {
        _nBack = value
    }

    fun setTimeBetweenEvents(seconds: Long) {
        _timeBetweenEvents.value = seconds
    }

    fun setNumEvents(count: Int) {
        _numEvents.value = count
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as GameApplication)
                GameVM(application.userPreferencesRespository)
            }
        }
    }

    init {
        viewModelScope.launch {
            userPreferencesRepository.highscore.collect {
                _highscore.value = it
            }
        }
    }
}

enum class GameType {
    NoSelection,
    Audio,
    Visual,
    AudioVisual
}

const val NO_EVENT = -1

data class GameState(
    val gameType: GameType = GameType.Visual,
    val eventValue: Int = NO_EVENT,
    val wrongGuess: Boolean = false
)

class FakeVM : GameViewModel {
    override val gameState = MutableStateFlow(GameState()).asStateFlow()
    override val score = MutableStateFlow(2).asStateFlow()
    override val highscore = MutableStateFlow(42).asStateFlow()
    override val nBack = 2
    override val timeBetweenEvents = 1L
    override val numEvents = 10

    override fun setGameType(gameType: GameType) {}
    override fun startGame() {}
    override fun checkMatch() {}
}