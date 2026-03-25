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
import com.example.impulsepurchaserecoverykit.debug.ReceiptDebugScreen
import com.example.impulsepurchaserecoverykit.ui.theme.ImpulsePurchaseRecoveryKitTheme
import com.example.impulsepurchaserecoverykit.viewmodel.ReceiptViewModel
import com.example.impulsepurchaserecoverykit.ui.AppRoot
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
    private fun processReceipt(imageUri: Uri){
        lifecycleScope.launch {
            Toast.makeText(
                this@MainActivity,
                "Scanning receipt...",
                Toast.LENGTH_SHORT
            ).show()
            Log.d("SCAN_URI", "Scanning URI: $imageUri")
            val extractedText = receiptScanner.scanReceipt(imageUri)
            if (extractedText == null) {
                Toast.makeText(
                    this@MainActivity,
                    "Scan failed. Try another image.",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }
            val parser = ReceiptParser()
            val parsedReceipt = parser.parseReceipt(extractedText)

            viewModel.saveReceipt(parsedReceipt, imageUri.toString()) { receiptId ->
                Log.d("DATABASE", "Receipt saved with ID: $receiptId")
            }

            //Shows the results
            if (parsedReceipt.isValid()) {
                Toast.makeText(
                    this@MainActivity,
                    "Saved! Store: ${parsedReceipt.storeName ?: "Unknown"} • Items: ${parsedReceipt.items.size} • Total: £${parsedReceipt.total}",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Saved, but parsing looks incomplete.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}


