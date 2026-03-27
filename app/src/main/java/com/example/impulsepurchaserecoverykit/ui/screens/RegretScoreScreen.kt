package com.example.impulsepurchaserecoverykit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.impulsepurchaserecoverykit.EmotionalResponseEngine
import com.example.impulsepurchaserecoverykit.viewmodel.ReceiptViewModel
import kotlin.math.roundToInt

@Composable
fun RegretScoreScreen(
    paddingValues: PaddingValues,
    receiptId: Long,
    viewModel: ReceiptViewModel,
    onDone: () -> Unit,
    onViewStats: () -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(5f) }
    var note by remember { mutableStateOf("") }

    // Emotional response dialog state
    var showEmotionalResponse by remember { mutableStateOf(false)}
    var emotionalResponse by remember {
        mutableStateOf(
            com.example.impulsepurchaserecoverykit.EmotionalResponse(
                "", "", "", ""
            )
        )
    }
    // Fetch the receipt so we have store name + impulse label for the engine
    val receipts by viewModel.getAllReceipts().collectAsState(initial = emptyList())
    val receipt = remember(receipts) {receipts.firstOrNull {it.id == receiptId} }

    val score = sliderValue.roundToInt().coerceIn(1, 10)

    // Show dialog if triggered
    if (showEmotionalResponse){
        EmotionalResponseDialog(
            response = emotionalResponse,
            onDismiss = {
                showEmotionalResponse = false
                onDone()
            },
            onViewStats = {
                showEmotionalResponse = false
                onViewStats()
            }
        )
    }

    Column(
        Modifier
            .padding(paddingValues)
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("How do you feel about this purchase?",
            style = MaterialTheme.typography.headlineSmall)

        // Emoji feedback as score changes
        val feedbackEmoji = when (score) {
            1, 2 -> "😌 No regrets"
            3, 4 -> "🤔 Slightly unsure"
            5, 6 -> "😐 Mixed feelings"
            7, 8 -> "😬 Kind of regret it"
            9, 10 -> "😩 Major regret"
            else -> ""
        }
        Text(feedbackEmoji, style = MaterialTheme.typography.titleMedium)

        Text("Score: $score / 10", style = MaterialTheme.typography.titleMedium)

        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            valueRange = 1f..10f,
            steps = 8
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ){
            Text("1 - No regret", style = MaterialTheme.typography.labelSmall)
            Text("10 - Total regret", style = MaterialTheme.typography.labelSmall)
        }

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("What triggered this purchase? (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Button(
            onClick = {
                viewModel.addEmotionCheckIn(
                    receiptId,
                    score,
                    mood = when {
                        score >= 8 -> "regretful"
                        score >= 5 -> "neutral"
                        else -> "satisfied"
                    },
                    notes = note.ifBlank { null }
                ) {
                    if (score >= 8){
                        emotionalResponse = EmotionalResponseEngine.getResponse(
                            regretScore = score,
                            storeName = receipt?.storeName,
                            impulseLabel = receipt?.impulseLabel,
                            emotionalNote = note.ifBlank { null }
                        )
                        showEmotionalResponse = true
                    } else {
                        onDone()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save")
        }
    }
}