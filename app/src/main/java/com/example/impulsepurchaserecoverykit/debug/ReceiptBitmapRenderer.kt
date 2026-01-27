package com.example.impulsepurchaserecoverykit.debug

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

fun renderReceiptToBitmap(receipt: ReceiptData): Bitmap {
    val widthPx = 360
    val padding = 16f

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 18f
        typeface = Typeface.MONOSPACE
    }

    val lineHeight = (paint.fontMetrics.bottom - paint.fontMetrics.top) + 6f

    val lines = buildList {
        add(receipt.storeName)
        addAll(receipt.addressLines)
        receipt.phone?.let { add(it) }
        add(receipt.dateTime)
        add("--------------------------------")

        receipt.items.forEach { item ->
            val left = item.name.take(22).padEnd(22, ' ')
            val right = "£%.2f".format(item.price).padStart(7, ' ')
            add(left + right)
        }

        add("--------------------------------")
        add(("SubTotal").padEnd(22, ' ') + ("£%.2f".format(receipt.subtotal)).padStart(7, ' '))
        add(("Tax").padEnd(22, ' ') + ("£%.2f".format(receipt.tax)).padStart(7, ' '))
        add(("TOTAL").padEnd(22, ' ') + ("£%.2f".format(receipt.total)).padStart(7, ' '))
        add("")
        add("${receipt.paymentMethod}  ****${receipt.last4}")
    }

    val heightPx = (padding * 2 + lines.size * lineHeight).toInt().coerceAtLeast(200)
    val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)

    val canvas = Canvas(bmp)
    canvas.drawColor(Color.WHITE)

    var y = padding - paint.fontMetrics.top
    for (line in lines) {
        canvas.drawText(line, padding, y, paint)
        y += lineHeight
    }

    return bmp
}