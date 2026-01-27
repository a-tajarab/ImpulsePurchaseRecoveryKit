package com.example.impulsepurchaserecoverykit.debug

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun ReceiptDebugScreen() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    Button(onClick = {
        if (activity == null) {
            Toast.makeText(context, "FAILED: Activity is null", Toast.LENGTH_LONG).show()
            return@Button
        }

        val receipts = ReceiptGenerator.generateBatch(30)
        receipts.forEachIndexed { i, r ->
            val bmp = renderReceiptToBitmap(activity, r)
            saveBitmapToGallery(context, bmp, "receipt_${i + 1}_${r.storeName}")
        }
        Toast.makeText(context, "Saved 30 receipts to Pictures/ReceiptTestData", Toast.LENGTH_LONG).show()
    }) {
        Text("Generate 30 Test Receipts")
    }
}