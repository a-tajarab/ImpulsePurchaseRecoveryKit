package com.example.impulsepurchaserecoverykit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.impulsepurchaserecoverykit.viewmodel.ReceiptViewModel
import kotlin.math.roundToInt

@Composable
fun RegretScoreScreen(
    paddingValues: PaddingValues,
    receiptId: Long,
    viewModel: ReceiptViewModel,
    onDone: () -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(5f) }
    var note by remember { mutableStateOf("") }

    val score = sliderValue.roundToInt().coerceIn(1, 10)

    Column(
        Modifier
            .padding(paddingValues)
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Regret Score", style = MaterialTheme.typography.headlineSmall)
        Text("How do you feel about this purchase?")

        Text("Score: $score / 10", style = MaterialTheme.typography.titleMedium)

        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            valueRange = 1f..10f,
            steps = 8
        )

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Optional note (what triggered the purchase?)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Button(
            onClick = {
                viewModel.updateRegretScore(receiptId, score, note) {
                    onDone()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save")
        }
    }
}