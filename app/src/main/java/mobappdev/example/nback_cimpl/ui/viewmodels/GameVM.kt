package mobappdev.example.nback_cimpl.ui.viewmodels

import android.speech.tts.TextToSpeech
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
import java.util.Locale
import kotlin.math.absoluteValue

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
    val nBack: StateFlow<Int>
    val timeBetweenEvents: StateFlow<Int>
    val numEvents: StateFlow<Int>

    fun setGameType(gameType: GameType)
    fun startGame()
    fun checkMatch()
}

class GameVM(
    private val app: GameApplication,
    private val userPreferencesRepository: UserPreferencesRepository
) : GameViewModel, ViewModel(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null

    private val _gameState = MutableStateFlow(GameState())
    override val gameState: StateFlow<GameState> get() = _gameState.asStateFlow()

    private val _score = MutableStateFlow(0)
    override val score: StateFlow<Int> get() = _score

    private val _highscore = MutableStateFlow(0)
    override val highscore: StateFlow<Int> get() = _highscore

    private val _nBack = MutableStateFlow(2) // TODO: FIX THESE THEY DONT SAVE
    override val nBack: StateFlow<Int> get() = _nBack

    private var _timeBetweenEvents = MutableStateFlow(2)
    override val timeBetweenEvents: StateFlow<Int> get() = _timeBetweenEvents

    private var _numEvents = MutableStateFlow(10)
    override val numEvents: StateFlow<Int> get() = _numEvents

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
            .generateNBackString(numEvents.value, 9, 30, nBack.value)
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
        if (currentIndex < nBack.value || matchRegistered) return

        val current = events[currentIndex]
        val previous = events[currentIndex - nBack.value]

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

    private suspend fun runAudioGame() {
        for ((index, value) in events.withIndex()) {
            currentIndex = index
            matchRegistered = false

            val letter = ('A' + (value - 1)).toString() // Convert 1–9 to A–I
            _gameState.value = _gameState.value.copy(eventValue = value)

            // Speak the letter
            tts?.speak(letter, TextToSpeech.QUEUE_FLUSH, null, null)
            Log.d("GameVM", "Speaking letter: $letter (index $index)")

            // Wait for event duration
            delay(timeBetweenEvents.value * 1000L)
            _gameState.value = _gameState.value.copy(eventValue = 0)
            delay(timeBetweenEvents.value * 1000L)
        }

        Log.d("GameVM", "Audio game finished with score ${_score.value}")
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

            delay(timeBetweenEvents.value * 1000L) // use user-configurable time
            _gameState.value = _gameState.value.copy(eventValue = 0)
            delay(timeBetweenEvents.value * 1000L)
        }

        Log.d("GameVM", "Game finished with score ${_score.value}")
    }

    private suspend fun updateHighscore() {
        if (_score.value > _highscore.value) {
            userPreferencesRepository.saveHighScore(_score.value)
        }
    }

    suspend fun setNBack(value: Int) {
        _nBack.value = value
        userPreferencesRepository.saveNBack(_nBack.value)
    }

    suspend fun setTimeBetweenEvents(seconds: Int) {
        _timeBetweenEvents.value = seconds
        userPreferencesRepository.saveTimeBetween(_timeBetweenEvents.value)
    }

    suspend fun setNumEvents(count: Int) {
        _numEvents.value = count
        userPreferencesRepository.saveEvents(_numEvents.value)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as GameApplication)
                GameVM(
                    app = application,
                    userPreferencesRepository = application.userPreferencesRespository
                )
            }
        }
    }


    init {
        tts = TextToSpeech(app.applicationContext, this)

        viewModelScope.launch {
            userPreferencesRepository.highscore.collect {
                _highscore.value = it
            }
            userPreferencesRepository.nBack.collect {
                _nBack.value = it
            }
            userPreferencesRepository.timeBetween.collect {
                _timeBetweenEvents.value = it
            }
            userPreferencesRepository.events.collect {
                _numEvents.value = it
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
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
    override val nBack = MutableStateFlow(2).asStateFlow()
    override val timeBetweenEvents = MutableStateFlow(2).asStateFlow()
    override val numEvents = MutableStateFlow(10).asStateFlow()

    override fun setGameType(gameType: GameType) {}
    override fun startGame() {}
    override fun checkMatch() {}
}