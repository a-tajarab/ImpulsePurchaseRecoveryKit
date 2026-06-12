package com.example.impulsepurchaserecoverykit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.impulsepurchaserecoverykit.EmotionalResponse
import com.example.impulsepurchaserecoverykit.EmotionalResponseEngine
import com.example.impulsepurchaserecoverykit.viewmodel.ReceiptViewModel
import com.example.impulsepurchaserecoverykit.ui.theme.*
import kotlin.math.roundToInt

/**
 * Represents a single selectable mood option on the Rate Regret screen.
 *
 * Mood options are displayed as emoji+label chips in a 4-column grid.
 * The selected mood is stored as a [value] string and saved to the database
 * as the [EmotionEntity.mood] field when the user submits their regret rating.
 *
 * @property emoji The emoji displayed on the mood chip
 * @property label The short text label shown below the emoji
 * @property value The internal string value saved to the database —
 *                 for example "satisfied", "stressed", or "peer_pressure"
 */
data class MoodOption(
    val emoji: String,
    val label: String,
    val value: String
)

/**
 * The eight mood options available on the Rate Regret screen, displayed in
 * two rows of four chips. Covers the most common emotional states associated
 * with impulse purchases.
 */
private val moodOptions = listOf(
    MoodOption("😌", "No regrets",     "satisfied"),
    MoodOption("😐", "Meh",            "neutral"),
    MoodOption("😟", "Stressed",       "stressed"),
    MoodOption("😴", "Bored",          "bored"),
    MoodOption("🤩", "Excited",        "excited"),
    MoodOption("👫", "Peer pressure",  "peer_pressure"),
    MoodOption("😢", "Sad",            "sad"),
    MoodOption("😤", "Angry",          "angry")
)

/**
 * Screen for rating the user's regret level for a specific receipt.
 *
 * Presents a structured emotional check-in after the user scans a receipt,
 * consisting of four input sections:
 *
 * **Regret slider** — a 1–10 slider with a large score display and a colour-coded
 * label. The slider colour shifts from green (low regret) to amber (mixed) to
 * red (high regret) as the score increases.
 *
 * **Mood chips** — eight emoji chips arranged in a 4×2 grid. The user selects
 * the emotion that best describes how they felt when they made the purchase.
 * Tapping the selected chip deselects it.
 *
 * **Purchase time field** — only shown when the receipt did not include a
 * parseable time. The [EmotionalResponseEngine] and [ImpulseScorer] use purchase
 * time to factor in time-of-day patterns, so capturing it here is important
 * for accuracy.
 *
 * **Reflection note** — an optional free-text field for the user to add context
 * about why they made the purchase. Stored as [EmotionEntity.notes].
 *
 * **Contextual tip card** — a static tip below the note field that adapts its
 * message based on the current slider score.
 *
 * On save:
 * - The purchase time is written to the receipt if it was not already present
 * - [ReceiptViewModel.addEmotionCheckIn] saves the score, mood, and note
 * - If the score is 8 or above, [EmotionalResponseEngine] generates a
 *   compassionate response and [EmotionalResponseDialog] is shown before
 *   navigating away. Scores below 8 navigate directly via [onDone].
 *
 * The user can also skip the rating entirely using the Skip button, which
 * calls [onDone] without saving anything.
 *
 * @param paddingValues Padding applied by the parent [Scaffold]
 * @param receiptId The ID of the receipt being rated
 * @param viewModel The shared [ReceiptViewModel] used to save the emotion check-in
 * @param onDone Callback invoked when the rating is saved or skipped —
 *               navigates back to the Receipt Detail screen
 * @param onViewStats Callback invoked from the [EmotionalResponseDialog] when
 *                    the user taps "Show my stats" — navigates to the Stats screen
 */
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
    var selectedMood by remember { mutableStateOf<MoodOption?>(null) }

    // Emotional response dialog state — populated by EmotionalResponseEngine on high regret scores
    var showEmotionalResponse by remember { mutableStateOf(false) }
    var emotionalResponse by remember {
        mutableStateOf(EmotionalResponse("", "", "", ""))
    }

    // Observe the receipt to display the store name in the header and check for purchase time
    val receipt by viewModel.getReceiptByIdFlow(receiptId).collectAsState(initial = null)

    // Pre-populate purchase time if the receipt already has one from OCR parsing
    var purchaseTime by remember { mutableStateOf(receipt?.purchaseTime ?: "") }

    // True when the receipt already has a purchase time — hides the time input field
    val timeWasParsed = remember(receipt) { receipt?.purchaseTime != null }

    // Round the slider float to an integer score clamped to 1–10
    val score = sliderValue.roundToInt().coerceIn(1, 10)

    // Human-readable label matching the score range shown below the large score number
    val regretLabel = when {
        score <= 2 -> "No regret at all"
        score <= 4 -> "A little unsure"
        score <= 6 -> "Mixed feelings"
        score <= 8 -> "Quite regretful"
        else       -> "Major regret"
    }

    // Slider and score colour — shifts from green to amber to red as regret increases
    val regretColor = when {
        score <= 3 -> Success700
        score <= 6 -> Warning700
        else       -> Error700
    }

    // Show the emotional response dialog for high-regret scores (8+)
    if (showEmotionalResponse) {
        EmotionalResponseDialog(
            response = emotionalResponse,
            onDismiss = { showEmotionalResponse = false; onDone() },
            onViewStats = { showEmotionalResponse = false; onViewStats() }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ── Midnight navy header ──────────────────────────────────────────
        // Title personalised with the store name if available
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(
                    top = paddingValues.calculateTopPadding() + 16.dp,
                    bottom = 20.dp, start = 20.dp, end = 20.dp
                )
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Rate your regret",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = receipt?.storeName?.let { "How do you feel about $it?" }
                        ?: "Be honest with yourself. This is a safe space.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = paddingValues.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Regret slider card ────────────────────────────────────────
            // Large score number + label + slider + scale endpoints
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Large score number — colour shifts with regretColor
                    Text(
                        text = score.toString(),
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Black,
                        color = regretColor,
                        lineHeight = 72.sp
                    )
                    Text(
                        text = regretLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    // 9 steps = 10 positions (1–10 inclusive)
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        valueRange = 1f..10f,
                        steps = 8,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = regretColor,
                            activeTrackColor = regretColor,
                            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    // Scale endpoint labels
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("1 — No regret", style = MaterialTheme.typography.labelSmall, color = Success700)
                        Text("10 — Total regret", style = MaterialTheme.typography.labelSmall, color = Error700)
                    }
                }
            }

            // ── Mood chip grid ────────────────────────────────────────────
            // 8 options in two rows of 4 — selected chip fills with Teal700
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("What were you feeling?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Select the mood that best fits this purchase", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    // Chunk into rows of 4 for the grid layout
                    val chunkedMoods = moodOptions.chunked(4)
                    chunkedMoods.forEach { rowMoods ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowMoods.forEach { mood ->
                                val isSelected = selectedMood == mood
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Teal700 else MaterialTheme.colorScheme.surfaceVariant)
                                        .border(
                                            width = if (isSelected) 0.dp else 1.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        // Tapping the selected chip deselects it
                                        .clickable { selectedMood = if (isSelected) null else mood }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(text = mood.emoji, fontSize = 22.sp, textAlign = TextAlign.Center)
                                        Text(
                                            text = mood.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            textAlign = TextAlign.Center,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Purchase time field — only shown when OCR did not find a time ──
            // Purchase time influences the impulse score, so capturing it here
            // improves scoring accuracy for manually entered or poorly scanned receipts
            if (!timeWasParsed) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("When did you buy this?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("We couldn't find a time on your receipt - enter it if you remember.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        // Validates HH:MM format on each keystroke — digits and colons only, max 5 chars
                        OutlinedTextField(
                            value = purchaseTime,
                            onValueChange = { input ->
                                if (input.length <= 5 && input.all { it.isDigit() || it == ':' })
                                    purchaseTime = input
                            },
                            label = { Text("Purchase time (optional)") },
                            placeholder = { Text("e.g. 14:30") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp),
                            supportingText = { Text("24-hour format") }
                        )
                    }
                }
            }

            // ── Reflection note ───────────────────────────────────────────
            // Optional free-text for the user to capture context about the purchase
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Anything to add? (optional)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = {
                            Text(
                                "e.g. I was stressed after work and just clicked buy...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp),
                        shape = RoundedCornerShape(10.dp),
                        maxLines = 4
                    )
                }
            }

            // ── Contextual tip card ───────────────────────────────────────
            // Message adapts based on the current slider score — three tiers:
            // low regret (encouragement), medium (normalising mixed feelings), high (24-hour rule)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Teal50),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Text("💡", fontSize = 18.sp)
                    Text(
                        text = when {
                            score <= 3 -> "Low regret purchases suggest you're spending intentionally. Keep it up!"
                            score <= 6 -> "Mixed feelings are normal. Tracking them helps you spot patterns over time."
                            else       -> "High regret is a signal worth noting. Next time, try waiting 24 hours before buying."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Teal700,
                        lineHeight = 18.sp
                    )
                }
            }

            // ── Save button ───────────────────────────────────────────────
            // Saves purchase time (if newly entered), emotion check-in, and triggers
            // EmotionalResponseDialog for scores >= 8 before navigating away
            Button(
                onClick = {
                    // Write purchase time if the user filled it in and it wasn't in the receipt
                    if (!timeWasParsed && purchaseTime.isNotBlank()) {
                        viewModel.updatePurchaseTime(receiptId, purchaseTime)
                    }

                    // Fall back to a derived mood string if the user did not select a chip
                    val moodValue = selectedMood?.value ?: when {
                        score >= 8 -> "regretful"
                        score >= 5 -> "neutral"
                        else       -> "satisfied"
                    }

                    viewModel.addEmotionCheckIn(
                        receiptId = receiptId,
                        regretScore = score,
                        mood = moodValue,
                        notes = note.ifBlank { null }
                    ) {
                        if (score >= 8) {
                            // High regret — show the EmotionalResponseDialog before navigating
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
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal700, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Save regret score", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            // Skip — navigates away without saving any rating
            TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text("Skip for now", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}