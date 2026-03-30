package com.example.impulsepurchaserecoverykit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import com.example.impulsepurchaserecoverykit.EmotionalResponseEngine
import com.example.impulsepurchaserecoverykit.EmotionalResponse
import com.example.impulsepurchaserecoverykit.viewmodel.ReceiptViewModel
import kotlin.math.roundToInt

data class MoodOption(
    val emoji: String,
    val label: String,
    val value: String
)

private val moodOptions = listOf(
    MoodOption("😌", "No regrets", "satisfied"),
    MoodOption("😐", "Meh", "neutral"),
    MoodOption("😟", "Stressed", "stressed"),
    MoodOption("😴", "Bored", "bored"),
    MoodOption("🤩", "Excited", "excited"),
    MoodOption("👫", "Peer pressure", "peer_pressure"),
    MoodOption("😢", "Sad", "sad"),
    MoodOption("😤", "Angry", "angry")
)
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
    var selectedMood by remember {mutableStateOf<MoodOption?>(null)}

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

    var purchaseTime by remember{
        mutableStateOf(receipt?.purchaseTime ?: "")
    }
    val timeWasParsed = remember(receipt){
        receipt?.purchaseTime != null
    }

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
        modifier = Modifier
            .padding(paddingValues)
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("How do you feel about this purchase?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)

        // Emoji feedback as score changes
        val feedbackEmoji = when (score) {
            1, 2 -> "😌 No regrets at all"
            3, 4 -> "🤔 Slightly unsure"
            5, 6 -> "😐 Mixed feelings"
            7, 8 -> "😬 Kind of regret it"
            9, 10 -> "😩 Major regret"
            else -> ""
        }

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ){
                Text(
                    text = "$score",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (score) {
                        in 1..3 -> MaterialTheme.colorScheme.primary
                        in 4..6 -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
                Text(
                    text = "out of 10",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = feedbackEmoji,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        val sliderColour = when (score) {
            in 1..3 -> Color(0xFF4CAF50)
            in 4..6 -> Color(0xFFFFC107)
            else -> Color(0xFFF44336)
        }

        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            valueRange = 1f..10f,
            steps = 8,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = sliderColour,
                activeTrackColor = sliderColour,
                inactiveTrackColor = sliderColour.copy(alpha = 0.2f)
            )
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ){
            Text("1 - No regret",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("10 - Total regret",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Mood selector
        Text(
            "What were you feeling when you bought this?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        Text(
            "Select all that apply - this helps spot your patterns",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // This is a mood chips which is wrapped in a grid 2 row of 4
        val chunkedMoods = moodOptions.chunked(4)
        chunkedMoods.forEach { rowMoods ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowMoods.forEach { mood ->
                    val isSelected = selectedMood == mood
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedMood = if (isSelected) null else mood
                        },
                        label = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ){
                                Text(
                                    text = mood.emoji,
                                    fontSize = 20.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = mood.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        if (!timeWasParsed){
            HorizontalDivider()

            Text(
                "When did you make this purchase?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Text(
                "We couldn't find a time on your receipt — enter it if you remember.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = purchaseTime,
                onValueChange = { input ->
                    // Only allow digits and colon, max 5 chars (HH:MM)
                    if (input.length <= 5 && input.all { it.isDigit() || it == ':' }) {
                        purchaseTime = input
                    }
                },
                label = { Text("Purchase time (optional)") },
                placeholder = { Text("e.g. 14:30") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                supportingText = {
                    Text("Use 24-hour format — e.g. 09:00 or 21:45")
                }
            )
        }

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("What triggered this purchase? (optional)") },
            placeholder = {Text("e.g. I was stressed after work...")},
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )


        //Save Button
        Button(
            onClick = {
                if (!timeWasParsed && purchaseTime.isNotBlank()){
                    viewModel.updatePurchaseTime(receiptId, purchaseTime)
                }
                val moodValue = selectedMood?.value ?:
                when {
                        score >= 8 -> "regretful"
                        score >= 5 -> "neutral"
                        else -> "satisfied"
                    }
                viewModel.addEmotionCheckIn(
                    receiptId = receiptId,
                    regretScore = score,
                    mood = moodValue,
                    notes = note.ifBlank { null}
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
            Text("Save", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
    }
}