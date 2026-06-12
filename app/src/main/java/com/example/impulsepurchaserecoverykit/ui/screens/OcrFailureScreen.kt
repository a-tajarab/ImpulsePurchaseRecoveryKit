package com.example.impulsepurchaserecoverykit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Describes the reason why OCR or receipt parsing failed.
 *
 * Used to determine which error message, emoji, and action buttons are shown
 * on the [OcrFailureScreen]. Each value represents a distinct stage of the
 * scanning pipeline where failure can occur.
 */
enum class OcrFailureReason {

    /**
     * ML Kit OCR ran successfully but returned no text from the image.
     * Typically caused by a blurry photo, poor lighting, or an image
     * that does not contain a receipt.
     */
    NO_TEXT_FOUND,

    /**
     * OCR successfully extracted text but the Claude AI parser could not
     * identify any individual line items within it.
     * Typically caused by an unusual or non-standard receipt format.
     */
    NO_ITEMS_PARSED,

    /**
     * Items were parsed successfully but key fields such as the store name
     * or total amount are missing from the result.
     * The receipt is saved to the database with whatever data was found,
     * and the user is directed to edit it from the Receipts screen.
     */
    INCOMPLETE_RECEIPT
}

/**
 * Error screen displayed when the receipt scanning or parsing pipeline fails.
 *
 * Shows a contextual error message, emoji, and explanation tailored to the
 * specific [OcrFailureReason] — so the user understands exactly what went
 * wrong and what they can do about it. A tips card with photography advice
 * is shown for all failure types to help the user avoid the same issue
 * on their next scan.
 *
 * Three action buttons are offered depending on the failure reason:
 * - **Try another image** — always shown, navigates back to [ScanScreen]
 * - **Enter details manually** — shown for [OcrFailureReason.NO_TEXT_FOUND]
 *   and [OcrFailureReason.NO_ITEMS_PARSED] only, as [OcrFailureReason.INCOMPLETE_RECEIPT]
 *   has already saved a partial receipt that the user can edit directly
 * - **Go back to home** — always shown, dismisses the error and returns to [HomeScreen]
 *
 * @param paddingValues Padding applied by the parent [Scaffold] to avoid
 *                      overlap with system bars and the bottom navigation bar
 * @param reason The [OcrFailureReason] that caused the scan to fail,
 *               used to select the appropriate message and button set
 * @param onTryAgain Callback invoked when the user taps "Try another image" —
 *                   navigates back to [ScanScreen] to select a new photo
 * @param onManualEntry Callback invoked when the user taps "Enter details manually" —
 *                      navigates to [ManualEntryScreen] to log the purchase by hand
 * @param onDismiss Callback invoked when the user taps "Go back to home" —
 *                  dismisses the error screen and returns to [HomeScreen]
 */
@Composable
fun OcrFailureScreen(
    paddingValues: PaddingValues,
    reason: OcrFailureReason,
    onTryAgain: () -> Unit,
    onManualEntry: () -> Unit,
    onDismiss: () -> Unit
) {
    // Select the emoji, title and message based on the specific failure reason
    val (emoji, title, message) = when (reason) {
        OcrFailureReason.NO_TEXT_FOUND -> Triple(
            "📷",
            "Couldn't read this image",
            "The scanner didn't find any text. This can happen with blurry photos, " +
                    "low lighting, or images that aren't receipts. Try a clearer photo taken " +
                    "straight on with good lighting."
        )
        OcrFailureReason.NO_ITEMS_PARSED -> Triple(
            "🧾",
            "Receipt scanned but no items found",
            "The scanner read the receipt but couldn't identify individual items. " +
                    "This sometimes happens with unusual receipt formats. You can try " +
                    "another photo or add the purchase details manually."
        )
        OcrFailureReason.INCOMPLETE_RECEIPT -> Triple(
            "⚠️",
            "Receipt looks incomplete",
            "The scan worked but some details like the store name or total are " +
                    "missing. The receipt has been saved with what was found — you can " +
                    "view and edit it from your Receipts screen."
        )
    }

    Column(
        modifier = Modifier
            .padding(paddingValues)
            .padding(32.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = emoji, fontSize = 72.sp, textAlign = TextAlign.Center)

        Spacer(Modifier.height(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )

        Spacer(Modifier.height(32.dp))

        // Tips card — shown for all failure reasons to help improve future scans
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "📸 Tips for better scans",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "• Make sure the receipt is flat and fully visible",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "• Use good lighting — avoid shadows",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "• Hold the camera directly above the receipt",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "• Make sure the image is in focus",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // Primary action — always available regardless of failure reason
        Button(onClick = onTryAgain, modifier = Modifier.fillMaxWidth()) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Try another image")
        }

        Spacer(Modifier.height(12.dp))

        // Manual entry — only shown when no receipt was saved at all.
        // Hidden for INCOMPLETE_RECEIPT because a partial record already exists
        // in the database and the user should edit that rather than create a duplicate.
        if (reason != OcrFailureReason.INCOMPLETE_RECEIPT) {
            OutlinedButton(onClick = onManualEntry, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Enter details manually")
            }

            Spacer(Modifier.height(12.dp))
        }

        // Dismiss — always available, returns to home without taking any action
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Go back to home")
        }
    }
}