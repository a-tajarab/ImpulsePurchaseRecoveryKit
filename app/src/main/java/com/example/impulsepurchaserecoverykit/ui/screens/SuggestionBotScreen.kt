package com.example.impulsepurchaserecoverykit.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.impulsepurchaserecoverykit.viewmodel.ChatMessage
import com.example.impulsepurchaserecoverykit.viewmodel.SuggestionBotViewModel
import com.example.impulsepurchaserecoverykit.ui.theme.*
import kotlinx.coroutines.launch

// Each mood: label, emoji (stored but not displayed), response
private data class KiraMoodOption(val label: String, val emoji: String, val response: String)

private val moodOptions = listOf(
    KiraMoodOption("Stressed",  "😰", "Stressed shoppers spend 34% more on impulse. Want to talk about it?"),
    KiraMoodOption("Bored",     "😑", "Boredom buying is one of the top impulse triggers. Let's find a better outlet!"),
    KiraMoodOption("Happy",     "😊", "Great mood! Just make sure happiness doesn't lead to celebratory splurging."),
    KiraMoodOption("Tired",     "😴", "Tired decision-making leads to regret. Maybe hold off on any big purchases today."),
    KiraMoodOption("Anxious",   "😟", "Retail therapy feels good short-term but often adds financial stress. I'm here to help."),
    KiraMoodOption("Fine",      "😌", "Good to hear! Let's take a look at your spending and keep it that way.")
)

// Gradient used for the KIRA avatar and header
private val kiraGradient = listOf(Teal900, Teal700)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionBotScreen(
    paddingValues: PaddingValues,
    botViewModel: SuggestionBotViewModel = viewModel()
) {
    val messages    by botViewModel.messages.collectAsState()
    val isLoading   by botViewModel.isLoading.collectAsState()
    val error       by botViewModel.error.collectAsState()

    var inputText          by remember { mutableStateOf("") }
    val listState          = rememberLazyListState()
    val scope              = rememberCoroutineScope()
    val snackbarHostState  = remember { SnackbarHostState() }
    var selectedMood       by remember { mutableStateOf<String?>(null) }
    var moodResponseShown  by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }
    LaunchedEffect(error) {
        error?.let { snackbarHostState.showSnackbar(it); botViewModel.clearError() }
    }

    // Pulse animation for the online dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // ── KIRA Header ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(kiraGradient))
                    .padding(
                        top  = paddingValues.calculateTopPadding() + 16.dp,
                        bottom = 20.dp,
                        start = 20.dp,
                        end   = 20.dp
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Gradient avatar with shadow
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(Teal500, Teal900))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("K", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "KIRA",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Pulsing online dot
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50).copy(alpha = pulseAlpha))
                            )
                            Text(
                                "Spending Intelligence · Online",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.75f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // ── Input Bar ─────────────────────────────────────────────────
            val canSend = inputText.isNotBlank() && !isLoading

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .imePadding()
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .padding(bottom = paddingValues.calculateBottomPadding()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Pill-shaped text field
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                "Ask KIRA anything...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        maxLines = 3,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = Teal700,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (canSend) {
                                botViewModel.sendMessage(inputText.trim()); inputText = ""
                                scope.launch { listState.animateScrollToItem(messages.size) }
                            }
                        })
                    )

                    // Send button — terracotta when active, muted when empty
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (canSend) Brush.linearGradient(listOf(Terra500, Terra700)) else Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                if (canSend) {
                                    botViewModel.sendMessage(inputText.trim()); inputText = ""
                                    scope.launch { listState.animateScrollToItem(messages.size) }
                                }
                            },
                            enabled = canSend
                        ) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = "Send",
                                tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // First KIRA message
            items(messages.take(1)) { message -> KiraMessageBubble(message = message) }

            // Mood check-in (shown after first message, before mood is selected)
            if (messages.isNotEmpty() && selectedMood == null) {
                item(key = "mood-checkin") {
                    MoodCheckInCard(onMoodSelected = { selectedMood = it })
                }
            }

            // Mood response
            if (selectedMood != null && !moodResponseShown) {
                item(key = "mood_response") {
                    val option = moodOptions.firstOrNull { it.label == selectedMood }
                    LaunchedEffect(selectedMood) { moodResponseShown = true }
                    if (option != null) MoodResponseBubble(option = option)
                }
            }

            // Rest of conversation
            if (selectedMood != null) {
                items(messages.drop(1), key = { "msg_${it.hashCode()}" }) { message ->
                    KiraMessageBubble(message = message)
                }
                if (messages.size == 1) {
                    item(key = "quick_suggestions") {
                        KiraQuickSuggestionsCard(onSuggestionClick = { suggestion ->
                            botViewModel.sendMessage(suggestion)
                            scope.launch { listState.animateScrollToItem(messages.size + 1) }
                        })
                    }
                }
            }

            if (isLoading) {
                item(key = "typing") { KiraTypingIndicator() }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Mood Check-In Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MoodCheckInCard(onMoodSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 46.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "How are you feeling today?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "This helps me give better advice",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 2 rows of 3 mood chips
        val rows = moodOptions.chunked(3)
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { option ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.5.dp, Teal700.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                            .clickable { onMoodSelected(option.label) }
                            .background(Teal700.copy(alpha = 0.05f))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            option.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Teal700
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Mood Response Bubble
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MoodResponseBubble(option: KiraMoodOption) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        KiraAvatar(size = 32)
        Spacer(Modifier.width(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Mood acknowledgement pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Terra500.copy(alpha = 0.12f))
                    .border(1.dp, Terra500.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    "Feeling ${option.label} — got it",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Terra700
                )
            }
            // Response bubble
            KiraBubbleBox {
                Text(option.response, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, lineHeight = 20.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Message Bubble
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun KiraMessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            KiraAvatar(size = 32)
            Spacer(Modifier.width(8.dp))
        }

        if (isUser) {
            // User bubble — teal gradient
            Box(
                modifier = Modifier
                    .widthIn(max = 270.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp))
                    .background(Brush.linearGradient(listOf(Teal700, Teal900)))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(message.content, color = Color.White, style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp)
            }
            Spacer(Modifier.width(8.dp))
            // User avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("U", fontSize = 14.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        } else {
            // KIRA bubble — surface with left accent border
            KiraBubbleBox {
                Text(message.content, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Animated Typing Indicator
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun KiraTypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    // Three dots with staggered bounce — each animates Y offset with delay
    val offsets = listOf(0, 180, 360).map { delay ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue  = -6f,
            animationSpec = infiniteRepeatable(
                animation  = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(delay)
            ),
            label = "dot_$delay"
        ).value
    }

    Row(verticalAlignment = Alignment.Bottom) {
        KiraAvatar(size = 32)
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, Teal700.copy(alpha = 0.15f), RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                offsets.forEach { offsetY ->
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .graphicsLayer { translationY = offsetY }
                            .clip(CircleShape)
                            .background(Teal700.copy(alpha = 0.7f))
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Quick Suggestions Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun KiraQuickSuggestionsCard(onSuggestionClick: (String) -> Unit) {
    val suggestions = listOf(
        "How am I doing this month?",
        "Should I buy this?",
        "What do I regret most?",
        "Help me save money"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 46.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Quick questions — tap to get started:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(2.dp))

        suggestions.forEach { question ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    .clickable { onSuggestionClick(question) }
                    .padding(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Teal left accent strip
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(52.dp)
                        .background(
                            Brush.verticalGradient(listOf(Teal700.copy(alpha = 0.6f), Teal700.copy(alpha = 0.3f))),
                            RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)
                        )
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        question,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(2.dp))
        Text(
            "Or type your own question below ↓",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared helpers
// ─────────────────────────────────────────────────────────────────────────────

/** Reusable KIRA "K" avatar square */
@Composable
private fun KiraAvatar(size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size * 0.28f).dp))
            .background(Brush.linearGradient(kiraGradient)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "K",
            fontSize = (size * 0.46f).sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
    }
}

/** KIRA message bubble surface — white card with subtle teal left border */
@Composable
private fun KiraBubbleBox(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .widthIn(max = 270.dp)
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
            )
    ) {
        // Teal left accent bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(Brush.verticalGradient(listOf(Teal700.copy(alpha = 0.7f), Teal700.copy(alpha = 0.2f))))
        )
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            content()
        }
    }
}