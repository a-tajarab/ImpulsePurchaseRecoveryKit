package com.example.impulsepurchaserecoverykit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.impulsepurchaserecoverykit.viewmodel.ChatMessage
import com.example.impulsepurchaserecoverykit.viewmodel.SuggestionBotViewModel
import com.example.impulsepurchaserecoverykit.ui.theme.*
import kotlinx.coroutines.launch

private val moodOptions = listOf(
    "Stressed" to "Stressed shoppers spend 34% more on impulse. Want to talk about it?",
    "Bored" to "Boredom buying is one of the top impulse triggers. Let's find a better outlet!",
    "Happy" to "Great mood! Just make sure happiness doesn't lead to celebratory splurging.",
    "Tired" to "Tired decision-making leads to regret. Maybe hold off on any big purchases today.",
    "Anxious" to "Retail therapy feels good short-term but often adds financial stress. I'm here to help.",
    "Fine" to "Good to hear! Let's take a look at your spending and keep it that way."
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionBotScreen(
    paddingValues: PaddingValues,
    botViewModel: SuggestionBotViewModel = viewModel()
) {
    val messages by botViewModel.messages.collectAsState()
    val isLoading by botViewModel.isLoading.collectAsState()
    val error by botViewModel.error.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedMood by remember { mutableStateOf<String?>(null) }
    var moodResponseShown by remember { mutableStateOf(false) }

    // Auto scroll to bottom when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            botViewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            //Kira's header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(
                        top = paddingValues.calculateTopPadding() + 16.dp,
                        bottom = 16.dp,
                        start = 20.dp,
                        end = 20.dp
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    //Kira's Avatar
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "K", fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Column {
                        Text(
                            "KIRA",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF7CF00))
                            )
                            Text(
                                " Your spending coach 🤖",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            //Input bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .imePadding()
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .padding(bottom = paddingValues.calculateBottomPadding()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    "Ask KIRA anything...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            maxLines = 3,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Teal700,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (inputText.isNotBlank() && !isLoading) {
                                        botViewModel.sendMessage(inputText.trim())
                                        inputText = ""
                                        scope.launch {
                                            listState.animateScrollToItem(messages.size)
                                        }
                                    }
                                }
                            )
                        )
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (inputText.isNotBlank() && !isLoading) Terra500
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = {
                                    if (inputText.isNotBlank() && !isLoading) {
                                        botViewModel.sendMessage(inputText.trim())
                                        inputText = ""
                                        scope.launch {
                                            listState.animateScrollToItem(messages.size)
                                        }
                                    }
                                },
                                enabled = inputText.isNotBlank() && !isLoading
                            ) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = if (inputText.isNotBlank() && !isLoading)
                                        Color.White
                                    else MaterialTheme.colorScheme.onSurfaceVariant
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            //First Kira message + mood check-in
            items(messages.take(1)) { message ->
                KiraMessageBubble(message = message)
            }

            if (messages.isNotEmpty() && selectedMood == null){
                item(key = "mood-checkin") {
                    MoodCheckInCard(
                        onMoodSelected = { mood ->
                            selectedMood = mood
                        }
                    )
                }
            }

            //Mood response bubble
            if (selectedMood != null && !moodResponseShown ) {
                item(key = "mood_response") {
                    val response = moodOptions
                        .firstOrNull { it.first == selectedMood }?.second ?: ""
                    LaunchedEffect(selectedMood) {
                        moodResponseShown = true
                    }
                    MoodResponseBubble(mood = selectedMood!!, response = response)
                }
            }

            //Rest of messages
            if (selectedMood != null) {
                items(
                    items = messages.drop(1),
                    key = { "msg_${it.hashCode()}" }
                ) { message ->
                    KiraMessageBubble(message = message)
                }
                if (messages.size == 1) {
                    item(key = "quick_suggestions") {
                        KiraQuickSuggestionsCard(
                            onSuggestionClick = { suggestion ->
                                botViewModel.sendMessage(suggestion)
                                scope.launch {
                                    listState.animateScrollToItem(messages.size + 1)
                                }
                            }
                        )
                    }
                }
            }

            if (isLoading) {
                item(key = "typing") {
                    KiraTypingIndicator()
                }
            }
        }
    }
}

@Composable
private fun MoodCheckInCard(onMoodSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 38.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Mood prompt label
        Text(
            text = "How are you feeling today?",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "This helps me give you better advice",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )

        // Mood chips grid
        val moods = moodOptions.map { it.first }
        val rows = moods.chunked(3)
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { mood ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                width = 1.5.dp,
                                color = Teal700,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { onMoodSelected(mood) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = mood,
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


@Composable
private fun MoodResponseBubble(mood: String, response: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .padding(end = 8.dp)
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Teal700),
            contentAlignment = Alignment.Center
        ) {
            Text("K", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Mood acknowledgement pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Terra50)
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "Feeling $mood — got it",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Terra700
                )
            }
            // Insight bubble
            Box(
                modifier = Modifier
                    .widthIn(max = 270.dp)
                    .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = response,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
@Composable
private fun KiraMessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Teal700),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "K",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }

        //Message Bubble
        Box(
            modifier = Modifier
                .widthIn(max = 270.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isUser) Teal700
                    else
                        MaterialTheme.colorScheme.surface
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.content,
                color = if (isUser) Color.White
                else
                    MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp
            )
        }
        //Avatar on the right
        if (isUser) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "U",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun KiraTypingIndicator() {
    Row(
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .padding(end = 8.dp)
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Teal700),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "K",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp,
                    bottomStart = 4.dp, bottomEnd = 16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3){
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Teal500)
                    )
                }
            }
        }
    }
}


@Composable
private fun KiraQuickSuggestionsCard(onSuggestionClick: (String) -> Unit) {
    val suggestions = listOf(
        " How am I doing this month?" to "💰",
        " Should I buy this?" to "🛍️",
        " What do I regret most?" to "😬",
        " Help me save money" to "💡"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 38.dp), // indent to align with bot bubbles
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Quick questions — tap one to get started:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(2.dp))

        suggestions.forEach { (question, emoji) ->
            Card(
                onClick = { onSuggestionClick(question) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(emoji, fontSize = 16.sp)
                    Text(
                        text = question,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Or type your own question below",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}