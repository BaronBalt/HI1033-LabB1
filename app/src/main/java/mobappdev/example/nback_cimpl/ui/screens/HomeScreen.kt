package mobappdev.example.nback_cimpl.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mobappdev.example.nback_cimpl.R
import mobappdev.example.nback_cimpl.ui.viewmodels.FakeVM
import mobappdev.example.nback_cimpl.ui.viewmodels.GameType
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel
import mobappdev.example.nback_cimpl.ui.viewmodels.NO_EVENT

/**
 * This is the Home screen composable
 *
 * Currently this screen shows the saved highscore
 * It also contains a button which can be used to show that the C-integration works
 * Furthermore it contains two buttons that you can use to start a game
 *
 * Date: 25-08-2023
 * Version: Version 1.0
 * Author: Yeetivity
 *
 */

@Composable
fun HomeScreen(vm: GameViewModel) {
    val highscore by vm.highscore.collectAsState()
    val score by vm.score.collectAsState()
    val gameState by vm.gameState.collectAsState()

    val scope = rememberCoroutineScope()

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ---------- TOP SECTION ----------
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = if (gameState.eventValue == NO_EVENT) "Highscore: $highscore" else "Score: $score",
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsButtons(vm = vm, gameState = gameState)
            }

            // ---------- MIDDLE SECTION ----------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Centered grid container
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .fillMaxHeight(0.6f),
                        contentAlignment = Alignment.Center
                    ) {
                        val gridSize = maxWidth.coerceAtMost(maxHeight)

                        // Display the correct version of the game or start game button
                        when {
                            gameState.eventValue == NO_EVENT -> {
                                Button(
                                    onClick = vm::startGame,
                                    enabled = gameState.gameType != GameType.NoSelection
                                ) {
                                    Text("Start Game")
                                }
                            }

                            gameState.gameType == GameType.Visual ->
                                VisualGrid(gameState = gameState, gridSize = gridSize)

                            gameState.gameType == GameType.Audio -> {
                                val letter = if (gameState.eventValue > 0) ('A' + (gameState.eventValue - 1)).toString() else ""
                                Text(
                                    text = letter,
                                    style = MaterialTheme.typography.displayLarge,
                                    textAlign = TextAlign.Center
                                )
                            }

                            gameState.gameType == GameType.AudioVisual -> {
                                // Todo: Implement AudioVisual version of game
                            }

                        }
                    }

                    if (gameState.eventValue != NO_EVENT) {
                        Button(
                            onClick = vm::checkMatch,
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(56.dp)
                        ) {
                            Text(
                                text = "Match!",
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }


            // ---------- BOTTOM SECTION ----------
            if (gameState.eventValue == NO_EVENT) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "Select Game Type",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isAudioSelected = gameState.gameType == GameType.Audio || gameState.gameType == GameType.AudioVisual
                        val isVisualSelected = gameState.gameType == GameType.Visual || gameState.gameType == GameType.AudioVisual

                        Button(
                            onClick = {
                                val newAudioSelected = !isAudioSelected
                                val newType = when {
                                    newAudioSelected && isVisualSelected -> GameType.AudioVisual
                                    newAudioSelected -> GameType.Audio
                                    isVisualSelected -> GameType.Visual
                                    else -> GameType.NoSelection
                                }
                                scope.launch { vm.setGameType(newType) }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAudioSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.sound_on),
                                contentDescription = "Audio",
                                modifier = Modifier.height(48.dp).aspectRatio(3f / 2f)
                            )
                        }

                        Button(
                            onClick = {
                                val newVisualSelected = !isVisualSelected
                                val newType = when {
                                    isAudioSelected && newVisualSelected -> GameType.AudioVisual
                                    isAudioSelected -> GameType.Audio
                                    newVisualSelected -> GameType.Visual
                                    else -> GameType.NoSelection
                                }
                                scope.launch { vm.setGameType(newType) }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isVisualSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.visual),
                                contentDescription = "Visual",
                                modifier = Modifier.height(48.dp).aspectRatio(3f / 2f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    Surface {
        HomeScreen(FakeVM())
    }
}
