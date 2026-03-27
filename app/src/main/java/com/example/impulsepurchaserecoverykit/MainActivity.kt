package com.example.impulsepurchaserecoverykit

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.impulsepurchaserecoverykit.debug.ReceiptDebugScreen
import com.example.impulsepurchaserecoverykit.ui.theme.ImpulsePurchaseRecoveryKitTheme
import com.example.impulsepurchaserecoverykit.viewmodel.ReceiptViewModel
import com.example.impulsepurchaserecoverykit.ui.AppRoot
import com.example.impulsepurchaserecoverykit.ui.screens.OcrFailureReason
import com.example.impulsepurchaserecoverykit.ui.screens.OcrFailureScreen
import com.google.firebase.encoders.json.BuildConfig
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var receiptScanner: ReceiptScanner
    private val viewModel: ReceiptViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Initialize the scanner
        receiptScanner = ReceiptScanner(this)
        setContent {
            ImpulsePurchaseRecoveryKitTheme {
                androidx.compose.foundation.layout.Box {
                    AppRoot(
                        viewModel = viewModel,
                        onScanReceiptPicked = { uri -> processReceipt(uri) }

                    )
                    if (BuildConfig.DEBUG) {
                        com.example.impulsepurchaserecoverykit.debug.ReceiptDebugFloatingButton()
                    }
                }
            }
        }
    }

    private fun processReceipt(imageUri: Uri) {
        lifecycleScope.launch {
            Toast.makeText(
                this@MainActivity,
                "Scanning receipt...",
                Toast.LENGTH_SHORT
            ).show()

            //Step 1 - run OCR
            val extractedText = receiptScanner.scanReceipt(imageUri)
            if (extractedText.isNullOrBlank()) {
                navigateToOcrFailure(OcrFailureReason.NO_TEXT_FOUND)
                return@launch
            }
            //Step 2 - parse the text
            val parser = ReceiptParser()
            val parsedReceipt = parser.parseReceipt(extractedText)

            //Step 3 - check what we got
            if (parsedReceipt.items.isEmpty()) {
                navigateToOcrFailure(OcrFailureReason.NO_ITEMS_PARSED)
                return@launch
            }

            //Step 4 - save to database
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
