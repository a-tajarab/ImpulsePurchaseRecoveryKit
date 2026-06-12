package com.example.impulsepurchaserecoverykit

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.impulsepurchaserecoverykit.ui.theme.ImpulsePurchaseRecoveryKitTheme
import com.example.impulsepurchaserecoverykit.viewmodel.ReceiptViewModel
import com.example.impulsepurchaserecoverykit.ui.AppRoot
import com.example.impulsepurchaserecoverykit.ui.screens.OcrFailureReason
import android.graphics.BitmapFactory
import androidx.compose.runtime.mutableStateOf
import com.example.impulsepurchaserecoverykit.ui.screens.PrivacyConsentDialog
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.impulsepurchaserecoverykit.ui.theme.Teal700
import kotlinx.coroutines.processNextEventInCurrentThread

/**
 * The single Activity that hosts the entire application.
 *
 * MainActivity follows the single-Activity architecture pattern recommended
 * for Jetpack Compose apps. All screens are composable functions managed by
 * Jetpack Navigation inside [AppRoot] — MainActivity itself is responsible
 * only for:
 *
 * 1. Setting up the Compose content and applying the app theme
 * 2. Handling the receipt scan flow — receiving a selected image URI,
 *    checking privacy consent, and orchestrating OCR and AI parsing
 * 3. Showing the [PrivacyConsentDialog] on the user's first scan, and
 *    persisting their acceptance to SharedPreferences
 * 4. Displaying the [ScanningLoadingOverlay] while a receipt is being processed
 *
 * The [ReceiptViewModel] is scoped to this Activity so it survives
 * configuration changes such as screen rotation.
 */
class MainActivity : ComponentActivity() {

    /**
     * The shared ViewModel for all receipt-related state and database operations.
     * Scoped to the Activity lifecycle so state is preserved across recompositions
     * and configuration changes.
     */
    private val viewModel: ReceiptViewModel by viewModels()

    /**
     * Lazily initialised SharedPreferences used to persist the user's privacy
     * consent decision across app sessions. Stored under the key `iprk_prefs`.
     */
    private val prefs by lazy {
        getSharedPreferences("iprk_prefs", MODE_PRIVATE)
    }

    /**
     * Returns true if the user has previously accepted the privacy consent
     * dialog, meaning their receipt text may be sent to the Claude AI API
     * for parsing. Checked before every scan attempt.
     */
    private val hasAcceptedPrivacy: Boolean
        get() = prefs.getBoolean("claude_privacy_accepted", false)

    /**
     * Persists the user's privacy acceptance to SharedPreferences.
     * Called once when the user taps Accept on the [PrivacyConsentDialog].
     * After this point [hasAcceptedPrivacy] returns true for all future sessions.
     */
    private fun markPrivacyAccepted() {
        prefs.edit().putBoolean("claude_privacy_accepted", true).apply()
    }

    /**
     * Temporarily holds the URI of a receipt image selected by the user when
     * the privacy consent dialog is shown mid-flow. Once the user accepts or
     * declines, this URI is either processed or discarded and reset to null.
     */
    private var pendingReceiptUri: Uri? = null

    /**
     * Initialises the Activity and sets the Compose content tree.
     *
     * Sets up [AppRoot] as the root composable, wires the receipt scan callback,
     * and manages the overlapping UI states — the privacy consent dialog and
     * the scanning loading overlay — using local Compose state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ImpulsePurchaseRecoveryKitTheme {
                var showDialog by remember { mutableStateOf(false) }
                var isScanning by remember { mutableStateOf(false) }
                var scanningMessage by remember { mutableStateOf("Reading your receipt...") }

                androidx.compose.foundation.layout.Box {
                    AppRoot(
                        viewModel = viewModel,
                        onScanReceiptPicked = { uri ->
                            if (!hasAcceptedPrivacy) {
                                pendingReceiptUri = uri
                                showDialog = true
                            } else {
                                processReceipt(uri,
                                    onLoadingStart = {
                                        isScanning = true
                                        scanningMessage = "Reading your receipt..."
                                    },
                                    onLoadingUpdate = { message ->
                                        scanningMessage = message
                                    },
                                    onLoadingEnd = {
                                        isScanning = false
                                    }
                                )
                            }
                        }
                    )
                    if (showDialog) {
                        PrivacyConsentDialog(
                            onAccept = {
                                markPrivacyAccepted()
                                showDialog = false
                                pendingReceiptUri?.let { uri ->
                                    processReceipt(
                                        imageUri = uri,
                                        onLoadingStart = {
                                            isScanning = true
                                            scanningMessage = "Reading your receipt..."
                                        },
                                        onLoadingUpdate = { message ->
                                            scanningMessage = message
                                        },
                                        onLoadingEnd = {
                                            isScanning = false
                                        }
                                    )
                                }
                                pendingReceiptUri = null
                            },
                            onDecline = {
                                showDialog = false
                                pendingReceiptUri = null
                            }
                        )
                    }

                    if (isScanning) {
                        ScanningLoadingOverlay(message = scanningMessage)
                    }
                }
            }
        }
    }

    /**
     * Entry point for the receipt scanning flow.
     *
     * Checks whether the user has accepted the privacy consent before
     * proceeding. If consent has not been given, the URI is stored in
     * [pendingReceiptUri] and the function returns early — the actual
     * scan will be triggered once the user accepts the dialog. If consent
     * has already been given, delegates immediately to [executeReceiptScan].
     *
     * @param imageUri The URI of the receipt image selected by the user
     * @param onLoadingStart Called when the scan begins, used to show the loading overlay
     * @param onLoadingUpdate Called with a status message as each scan stage completes
     * @param onLoadingEnd Called when the scan finishes (success or failure) to hide the overlay
     */
    private fun processReceipt(
        imageUri: Uri,
        onLoadingStart: () -> Unit,
        onLoadingUpdate: (String) -> Unit,
        onLoadingEnd: () -> Unit
    ) {
        if (!hasAcceptedPrivacy) {
            pendingReceiptUri = imageUri
            return
        }
        executeReceiptScan(imageUri, onLoadingStart, onLoadingUpdate, onLoadingEnd)
    }

    /**
     * Executes the full receipt scanning pipeline in a coroutine on the
     * lifecycle scope.
     *
     * The pipeline runs through the following stages, updating the loading
     * overlay message at each step:
     * 1. Decode the selected image URI into a [Bitmap]
     * 2. Send the bitmap to [ClaudeReceiptParser] for AI-powered text extraction
     *    and structured data parsing — falling back to on-device OCR if the
     *    Claude API is unavailable
     * 3. Validate that the parsed result contains at least one item
     * 4. Save the parsed receipt to the Room database via [ReceiptViewModel]
     * 5. Navigate to [OcrFailureReason] screen if any stage fails, or show a
     *    success Toast if the receipt is saved successfully
     *
     * @param imageUri The URI of the receipt image to process
     * @param onLoadingStart Called immediately to show the loading overlay
     * @param onLoadingUpdate Called with a status string at each pipeline stage
     * @param onLoadingEnd Called when the pipeline completes to hide the overlay
     */
    private fun executeReceiptScan(
        imageUri: Uri,
        onLoadingStart: () -> Unit,
        onLoadingUpdate: (String) -> Unit,
        onLoadingEnd: () -> Unit
    ) {
        lifecycleScope.launch {
            try {
                onLoadingStart()
                onLoadingUpdate("Loading your image...")

                val bitmap = contentResolver.openInputStream(imageUri)?.use {
                    BitmapFactory.decodeStream(it)
                }
                if (bitmap == null) {
                    onLoadingEnd()
                    navigateToOcrFailure(OcrFailureReason.NO_TEXT_FOUND)
                    return@launch
                }
                onLoadingUpdate("Sending to AI for analysis...")

                val parsedReceipt = ClaudeReceiptParser.parseReceiptImage(
                    bitmap = bitmap,
                    onFallback = {
                        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                            onLoadingUpdate("AI unavailable - using on-device scanner... ")
                        }
                    })

                if (parsedReceipt == null || parsedReceipt.items.isEmpty()) {
                    onLoadingEnd()
                    navigateToOcrFailure(OcrFailureReason.NO_ITEMS_PARSED)
                    return@launch
                }

                onLoadingUpdate("Saving your receipt...")
                viewModel.saveReceipt(parsedReceipt, imageUri.toString()) { receiptId ->
                    onLoadingEnd()
                    Log.d("DATABASE", "Receipt saved with ID: $receiptId")
                    if (!parsedReceipt.isValid()) {
                        navigateToOcrFailure(OcrFailureReason.INCOMPLETE_RECEIPT)
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "✅ Saved! ${parsedReceipt.storeName ?: "Receipt"} • " +
                                    "${parsedReceipt.items.size} items • £${parsedReceipt.total}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                onLoadingEnd()
                Log.e("SCAN", "Scanning failed", e)
                navigateToOcrFailure(OcrFailureReason.NO_TEXT_FOUND)
            }
        }
    }

    /**
     * Signals to the [ReceiptViewModel] that OCR or parsing has failed,
     * triggering navigation to the OCR failure screen.
     *
     * The failure reason is stored in the ViewModel rather than passed
     * directly through the navigation graph so that the failure screen
     * can retrieve it reactively without relying on navigation arguments.
     *
     * @param reason The [OcrFailureReason] describing why the scan failed
     */
    private fun navigateToOcrFailure(reason: OcrFailureReason) {
        viewModel.setOcrFailureReason(reason)
    }
}

/**
 * A full-screen loading overlay displayed while a receipt is being scanned
 * and processed by the AI.
 *
 * Rendered as a semi-transparent black scrim over the entire app with a
 * centred card containing a spinner, KIRA branding, a dynamic status message,
 * and a reassurance note telling the user how long the process typically takes.
 *
 * The overlay is shown as soon as a receipt image is selected and hidden once
 * the pipeline completes — whether successfully or with an error.
 *
 * @param message The current status message to display below the KIRA branding,
 *                updated at each stage of the scanning pipeline
 */
@Composable
private fun ScanningLoadingOverlay(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Teal spinning indicator
                CircularProgressIndicator(
                    color = Teal700,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(52.dp)
                )

                // KIRA branding
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                color = Teal700,
                                shape = RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "K",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "KIRA is reading your receipt",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Dynamic status message updated at each pipeline stage
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Reassurance note so the user knows the delay is expected
                Text(
                    text = "This usually takes 3–5 seconds",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}