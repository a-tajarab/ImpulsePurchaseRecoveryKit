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

enum class OcrFailureReason {
    NO_TEXT_FOUND,       // OCR returned empty
    NO_ITEMS_PARSED,     // OCR worked but parser found no items
    INCOMPLETE_RECEIPT   // Items found but missing total/store
}

@Composable
fun OcrFailureScreen(
    paddingValues: PaddingValues,
    reason: OcrFailureReason,
    onTryAgain: () -> Unit,
    onManualEntry: () -> Unit,
    onDismiss: () -> Unit
) {
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
        Text(
            text = emoji,
            fontSize = 72.sp,
            textAlign = TextAlign.Center
        )

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

        // Tips card
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

        // Action buttons
        Button(
            onClick = onTryAgain,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Try another image")
        }

        Spacer(Modifier.height(12.dp))

        if (reason != OcrFailureReason.INCOMPLETE_RECEIPT) {
            OutlinedButton(
                onClick = onManualEntry,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Enter details manually")
            }

            Spacer(Modifier.height(12.dp))
        }

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Go back to home")
        }
    }
}