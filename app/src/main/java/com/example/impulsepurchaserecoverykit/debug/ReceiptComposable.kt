package com.example.impulsepurchaserecoverykit.debug
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

    @Composable
    fun ReceiptComposable(r: ReceiptData) {
        val mono = FontFamily.Monospace

        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(16.dp)
                .width(320.dp)
        ) {
            Text(r.storeName, fontFamily = mono, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))

            r.addressLines.forEach { Text(it, fontFamily = mono) }
            r.phone?.let { Text(it, fontFamily = mono) }

            Spacer(Modifier.height(8.dp))
            Text(r.dateTime, fontFamily = mono)
            Divider(Modifier.padding(vertical = 8.dp))

            r.items.forEach { item ->
                Row(Modifier.fillMaxWidth()) {
                    Text(item.name, fontFamily = mono, modifier = Modifier.weight(1f))
                    Text("£%.2f".format(item.price), fontFamily = mono)
                }
            }

            Divider(Modifier.padding(vertical = 8.dp))

            Row(Modifier.fillMaxWidth()) {
                Text("SubTotal", fontFamily = mono, modifier = Modifier.weight(1f))
                Text("£%.2f".format(r.subtotal), fontFamily = mono)
            }
            Row(Modifier.fillMaxWidth()){
                Text("Tax", fontFamily = mono, modifier = Modifier.weight(1f))
                Text("£%.2f".format(r.tax), fontFamily = mono)
            }
            Row(Modifier.fillMaxWidth()) {
                Text("TOTAL", fontFamily = mono, fontSize = 18.sp, modifier = Modifier.weight(1f))
                Text("£%.2f".format(r.total), fontFamily = mono, fontSize = 18.sp)
            }

            Spacer(Modifier.height(10.dp))
            Text("${r.paymentMethod}  ****${r.last4}", fontFamily = mono)
        }
    }
