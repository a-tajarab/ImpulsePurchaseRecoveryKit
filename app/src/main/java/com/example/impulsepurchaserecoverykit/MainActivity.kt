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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: ReceiptViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ImpulsePurchaseRecoveryKitTheme {
                androidx.compose.foundation.layout.Box {
                    AppRoot(
                        viewModel = viewModel,
                        onScanReceiptPicked = { uri -> processReceipt(uri) }

                    )
                }
            }
        }
    }

    private fun processReceipt(imageUri: Uri) {
        lifecycleScope.launch {
            Toast.makeText(this@MainActivity, "Scanning receipt...", Toast.LENGTH_SHORT).show()

            val bitmap = contentResolver.openInputStream(imageUri)?.use {
                BitmapFactory.decodeStream(it)
            }
            if (bitmap == null) {
                navigateToOcrFailure(OcrFailureReason.NO_TEXT_FOUND)
                return@launch
            }

            val parsedReceipt = ClaudeReceiptParser.parseReceiptImage(bitmap)

            if (parsedReceipt == null || parsedReceipt.items.isEmpty()) {
                navigateToOcrFailure(OcrFailureReason.NO_ITEMS_PARSED)
                return@launch
            }

            viewModel.saveReceipt(parsedReceipt, imageUri.toString()) { receiptId ->
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
        }
    }


    private fun navigateToOcrFailure(reason: OcrFailureReason) {
        viewModel.setOcrFailureReason(reason)
    }
}
