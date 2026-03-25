package com.example.impulsepurchaserecoverykit.debug

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.runtime.CompositionLocalProvider
import androidx.activity.ComponentActivity

fun renderReceiptToBitmap(activity: ComponentActivity, receipt: ReceiptData): Bitmap {
    // Measure/layout at a fixed size for consistent OCR
    val width = 360
    val height = 640

    val root = activity.findViewById<ViewGroup>(android.R.id.content)

    val host = FrameLayout(activity).apply{
        visibility = View.INVISIBLE
        layoutParams = ViewGroup.LayoutParams(width, height)
    }

    val composeView = ComposeView(activity).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent { ReceiptComposable(receipt) }
        layoutParams = ViewGroup.LayoutParams(width, height)
    }
    root.addView(host)
    root.addView(composeView)


    val wSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
    val hSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
    composeView.measure(wSpec, hSpec)
    composeView.layout(0,0, width, height)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    composeView.draw(canvas)

    root.removeView(host)
    return bitmap
}

fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileName: String): android.net.Uri? {
    val resolver = context.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ReceiptTestData")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val uri =
        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return null
    resolver.openOutputStream(uri)?.use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        contentValues.clear()
        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)
    }
    return uri
}
