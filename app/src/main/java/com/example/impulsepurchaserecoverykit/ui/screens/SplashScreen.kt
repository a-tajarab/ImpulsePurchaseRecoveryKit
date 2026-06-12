package com.example.impulsepurchaserecoverykit.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.impulsepurchaserecoverykit.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Animated splash screen displayed briefly when the app launches.
 *
 * Fills the screen with the primary midnight navy colour and runs a staggered
 * entrance animation sequence before calling [onSplashComplete] to navigate
 * to the next screen. The total duration from launch to navigation is
 * approximately 2.2 seconds.
 *
 * The animation sequence runs as follows:
 * 1. Logo icon + app name — fade in (600ms) and scale up from 0.7x (700ms), both in parallel
 * 2. Tagline "Spend less. Feel more." — fades in after 400ms (500ms duration)
 * 3. KIRA badge — fades in after a further 300ms (400ms duration)
 * 4. Hold — 1000ms pause so the user can read the content
 * 5. Navigate — [onSplashComplete] is called to move to onboarding or home
 *
 * All four animation values use [Animatable] so they can be driven sequentially
 * within the same coroutine using delay between stages, without needing a
 * complex animation state machine.
 *
 * @param onSplashComplete Callback invoked at the end of the animation sequence —
 *                         navigates to [OnboardingScreen] on first launch or
 *                         [HomeScreen] for returning users
 */
@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {

    // Four independent animation targets driven sequentially in a single LaunchedEffect
    val logoAlpha    = remember { Animatable(0f) }   // logo icon + app name opacity
    val logoScale    = remember { Animatable(0.7f) }  // logo icon scale — enters slightly small
    val taglineAlpha = remember { Animatable(0f) }    // tagline opacity
    val badgeAlpha   = remember { Animatable(0f) }    // KIRA badge opacity

    LaunchedEffect(Unit) {
        // Logo fade-in and scale-up run in parallel — launch creates a child coroutine
        // for the alpha so both animations progress simultaneously
        launch { logoAlpha.animateTo(1f, animationSpec = tween(600)) }
        logoScale.animateTo(1f, animationSpec = tween(700))

        delay(400) // short pause before tagline appears
        taglineAlpha.animateTo(1f, animationSpec = tween(500))

        delay(300) // short pause before KIRA badge appears
        badgeAlpha.animateTo(1f, animationSpec = tween(400))

        delay(1000) // hold so the user can read the content
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Receipt emoji logo — scales and fades in simultaneously
            Box(
                modifier = Modifier
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
                    .size(100.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🧾", fontSize = 52.sp, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(24.dp))

            // App name — shares logoAlpha so it fades in alongside the icon
            Text(
                text = "Impulse Purchase\nRecovery Kit",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(logoAlpha.value)
            )

            Spacer(Modifier.height(6.dp))

            // Tagline — delayed fade-in after the logo animation completes
            Text(
                text = "Spend less. Feel more.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(taglineAlpha.value)
            )

            Spacer(Modifier.height(32.dp))

            // KIRA badge — pill row with the K avatar and "KIRA is ready" label
            // The last element to appear, signalling the AI coach is initialised
            Box(
                modifier = Modifier
                    .alpha(badgeAlpha.value)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mini KIRA "K" avatar — matches the avatar used in SuggestionBotScreen
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Teal500),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("K", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                    Text(
                        text = "KIRA is ready",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Version tag — anchored to bottom centre, fades in with the tagline
        Text(
            text = "v1.0",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .alpha(taglineAlpha.value)
        )
    }
}