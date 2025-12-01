package com.example.impulsepurchaserecoverykit

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.impulsepurchaserecoverykit.ui.theme.ImpulsePurchaseRecoveryKitTheme
import com.example.impulsepurchaserecoverykit.viewmodel.ReceiptViewModel
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    private lateinit var receiptScanner: ReceiptScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Initialize the scanner
        receiptScanner = ReceiptScanner(this)
        setContent {
            ImpulsePurchaseRecoveryKitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ReceiptScannerScreen(
                        onScanReceipt = { uri ->
                            processReceipt(uri)

                        }
                    )
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
            //Extract the text with OCR
            val extractedText = receiptScanner.scanReceipt(imageUri)

            if (extractedText != null) {
                //Parse the text into structured data
                val parser = ReceiptParser()
                val parsedReceipt = parser.parseReceipt(extractedText)
                //Export to CSV for testing
                val csvExporter = CsvExporter(this@MainActivity)
                val csvFile = csvExporter.exportToCSV(parsedReceipt)

                val viewModel = ReceiptViewModel(application)
                viewModel.saveReceipt(parsedReceipt, imageUri.toString()) { receiptId ->
                    Log.d("DATABASE", "Receipt saved with ID: $receiptId")
                }
                //Shows the results
                if (parsedReceipt.isValid()) {
                    Toast.makeText(
                        this@MainActivity,
                        """Scan successful! Receipt is saved to the database.
                            Store: ${parsedReceipt.storeName ?: "Unknown"} 
                            Items: ${parsedReceipt.items.size} 
                            Total : $${parsedReceipt.total} 
                            Check Logcat for results.
                            """.trimIndent(),
                        Toast.LENGTH_LONG
                    ).show()

                    //This is where the results are logged in detail
                    Log.d("PARSED_RECEIPT", " == Receipt Details ==")
                    Log.d("PARSED_RECEIPT", parsedReceipt.getSummary())
                    Log.d("PARSED_RECEIPT", "\nItems:")
                    parsedReceipt.items.forEach { item ->
                        Log.d(
                            "PARSED_RECEIPT",
                            " - ${item.name} [${item.category}]: $${item.price}"
                        )
                    }
                    Log.d("PARSED_RECEIPT", "\nSaved to database with auto-generated ID")
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Scan completed but parsing failed.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Scan failed. Try another image.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

@Composable
fun ReceiptScannerScreen(onScanReceipt: (Uri) -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(false)
    }
    //Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (!isGranted) {
            Toast.makeText(
                context,
                "Permission denied. Please grant access in settings.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    //Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == ComponentActivity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                Log.d("IMAGE_PICKER", "Selected image: $uri")
                onScanReceipt(uri)
            } else {
                Log.e("IMAGE_PICKER", "URI is null")
                Toast.makeText(
                    context,
                    "Error: Could not get selected file",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Log.e("IMAGE_PICKER", "Result code: ${result.resultCode}")
            Toast.makeText(
                context,
                "Image selection cancelled",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    //checks the permission on the launch
    LaunchedEffect(Unit) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        hasPermission = context.checkSelfPermission(permission) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Impulse Purchase Recovery Kit",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = "Week 2: Receipt Parsing",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (hasPermission) {
                        val intent = Intent(Intent.ACTION_GET_CONTENT) .apply{
                            type = "image/*"
                            addCategory(Intent.CATEGORY_OPENABLE)
                        }
                        imagePickerLauncher.launch(intent)
                    } else {
                        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Manifest.permission.READ_MEDIA_IMAGES
                        } else {
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        }
                        permissionLauncher.launch(permission)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(56.dp)
            ) {
                Text(
                    text = "Scan Receipt",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Results will appear in Logcat",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}



