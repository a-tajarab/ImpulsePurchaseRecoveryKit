package com.example.impulsepurchaserecoverykit

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
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
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    private lateinit var receiptScanner: ReceiptScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Initiliaze the scanner
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

            val extractedText = receiptScanner.scanReceipt(imageUri)
            if (extractedText != null){
                Toast.makeText(
                    this@MainActivity,
                    "Scan successful! Check Logcat for results. \nFound ${extractedText.length} characters",
                    Toast.LENGTH_LONG
                ).show()
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
            result.data?.data?.let { uri ->
                onScanReceipt(uri)
            }
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
                text = "Week 1: OCR Test",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (hasPermission) {
                        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
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
                    .fillMaxSize(0.7f)
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
