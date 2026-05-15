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

class MainActivity : ComponentActivity() {
    private val viewModel: ReceiptViewModel by viewModels()

    private val prefs by lazy {
        getSharedPreferences("iprk_prefs", MODE_PRIVATE)
    }

    private val hasAcceptedPrivacy: Boolean
        get() = prefs.getBoolean("claude_privacy_accepted", false)

    private fun markPrivacyAccepted() {
        prefs.edit().putBoolean("claude_privacy_accepted", true).apply()
    }

    private var pendingReceiptUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ImpulsePurchaseRecoveryKitTheme {
                var showDialog by remember { mutableStateOf(false) }
                var isScanning by remember { mutableStateOf(false) }
                var scanningMessage by remember { mutableStateOf("Reading your receipt...")}

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
                                        onLoadingStart =  {
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
                        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Main){
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
    private fun navigateToOcrFailure(reason: OcrFailureReason) {
        viewModel.setOcrFailureReason(reason)
    }
}


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

                // Dynamic status message
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Reassurance note
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
