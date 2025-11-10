package mobappdev.example.nback_cimpl.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mobappdev.example.nback_cimpl.ui.viewmodels.GameState

@Composable
fun VisualGrid(gameState: GameState, gridSize: androidx.compose.ui.unit.Dp) {
    val boxSize = (gridSize / 3) - 4.dp // 3x3 grid with spacing

    Column(
        modifier = Modifier.size(gridSize),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in 0 until 3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (col in 0 until 3) {
                    val index = row * 3 + col
                    val isActive = index == gameState.eventValue - 1
                    val boxColor = when {
                        isActive && gameState.wrongGuess -> MaterialTheme.colorScheme.error
                        isActive -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }

                    Box(
                        modifier = Modifier
                            .size(boxSize)
                            .background(color = boxColor, shape = MaterialTheme.shapes.medium)
                    )
                }
            }
        }
    }
}