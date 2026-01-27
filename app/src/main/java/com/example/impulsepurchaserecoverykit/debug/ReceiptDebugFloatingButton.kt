package com.example.impulsepurchaserecoverykit.debug

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ReceiptDebugFloatingButton() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        Button(
            modifier = Modifier.padding(16.dp),
            enabled = !running,
            onClick = {
                scope.launch {
                    running = true
                    val activity = context as? ComponentActivity
                    if (activity == null) {
                        Toast.makeText(context, "FAILED: Not an Activity context", Toast.LENGTH_LONG).show()
                        running = false
                        return@launch
                    }

                    try {
                        val receipts = ReceiptGenerator.generateBatch(30)

                        receipts.forEachIndexed { i, r ->
                            // ComposeView rendering MUST be on Main thread
                            val bmp = renderReceiptToBitmap(r)

                            // Saving to MediaStore should be IO
                            withContext(Dispatchers.IO) {
                                saveBitmapToGallery(
                                    context,
                                    bmp,
                                    "receipt_${i + 1}_${sanitizeFilePart(r.storeName)}"
                                )
                            }
                        }

                        Toast.makeText(
                            context,
                            "Saved 30 receipts to Pictures/ReceiptTestData",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (t: Throwable) {
                        Toast.makeText(context, "FAILED: ${t.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        running = false
                    }
                }
            }
        ) {
            Text(if (running) "Generating..." else "Generate 30 Test Receipts")
        }
    }
}

private fun sanitizeFilePart(s: String): String =
    s.replace(Regex("""[^A-Za-z0-9_-]"""), "_")