package mobappdev.example.nback_cimpl.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mobappdev.example.nback_cimpl.ui.viewmodels.GameState
import mobappdev.example.nback_cimpl.ui.viewmodels.GameVM
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel
import mobappdev.example.nback_cimpl.ui.viewmodels.NO_EVENT

@Composable
fun SettingsButtons(vm: GameViewModel, gameState: GameState) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val showNDialog = remember { mutableStateOf(false) }
        val showTimeDialog = remember { mutableStateOf(false) }
        val showEventDialog = remember { mutableStateOf(false) }

        val nInput = rememberSaveable { mutableStateOf(vm.nBack.value.toString()) }
        val timeInput = rememberSaveable { mutableStateOf(vm.timeBetweenEvents.value.toString()) }
        val eventInput = rememberSaveable { mutableStateOf(vm.numEvents.value.toString()) }

        val scope = rememberCoroutineScope()

        // N button
        Button(
            onClick = { showNDialog.value = true },
            enabled = gameState.eventValue == NO_EVENT
        ) {
            Text("N = ${vm.nBack.value}")
        }

        // Time button
        Button(
            onClick = { showTimeDialog.value = true },
            enabled = gameState.eventValue == NO_EVENT
        ) {
            Text("Time = ${vm.timeBetweenEvents.value}s")
        }

        // Event count button
        Button(
            onClick = { showEventDialog.value = true },
            enabled = gameState.eventValue == NO_EVENT
        ) {
            Text("Events = ${vm.numEvents.value}")
        }

        // --- Dialog for N-back value ---
        if (showNDialog.value) {
            AlertDialog(
                onDismissRequest = { showNDialog.value = false },
                title = { Text("Set N-back value") },
                text = {
                    OutlinedTextField(
                        value = nInput.value,
                        onValueChange = { nInput.value = it },
                        label = { Text("N") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val n = nInput.value.toIntOrNull()
                        if (n != null && n > 0 && vm is GameVM) {
                            scope.launch {
                                vm.setNBack(n)
                                showNDialog.value = false
                            }
                        }
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showNDialog.value = false }) { Text("Cancel") }
                }
            )
        }

        // --- Dialog for Time Between Events ---
        if (showTimeDialog.value) {
            AlertDialog(
                onDismissRequest = { showTimeDialog.value = false },
                title = { Text("Set Time Between Events (s)") },
                text = {
                    OutlinedTextField(
                        value = timeInput.value,
                        onValueChange = { timeInput.value = it },
                        label = { Text("Seconds") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val t = timeInput.value.toIntOrNull()
                        if (t != null && t > 0 && vm is GameVM) {
                            scope.launch {
                                vm.setTimeBetweenEvents(t)
                                showTimeDialog.value = false
                            }
                        }
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showTimeDialog.value = false }) { Text("Cancel") }
                }
            )
        }

        // --- Dialog for Number of Events ---
        if (showEventDialog.value) {
            AlertDialog(
                onDismissRequest = { showEventDialog.value = false },
                title = { Text("Set Number of Events") },
                text = {
                    OutlinedTextField(
                        value = eventInput.value,
                        onValueChange = { eventInput.value = it },
                        label = { Text("Count") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val e = eventInput.value.toIntOrNull()
                        if (e != null && e > 0 && vm is GameVM) {
                            scope.launch {
                                vm.setNumEvents(e)
                                showEventDialog.value = false
                            }
                        }
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showEventDialog.value = false }) { Text("Cancel") }
                }
            )
        }
    }
}