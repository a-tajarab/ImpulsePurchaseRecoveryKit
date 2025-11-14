package com.example.impulsepurchaserecoverykit

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

class ReceiptScanner (private val context: Context) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Scans a receipt image and extracts text using ML Kit OCR
     *      * @param imageUri The URI of the receipt image
     *      * @return The extracted text, or null if scanning fails
     */
    suspend fun scanReceipt(imageUri: Uri): String? {
        return try {
            // Load the image
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            // Process with ML Kit
            val result = recognizer.process(inputImage).await()
            val extractedText = result.text

            // Log the raw output
            Log.d("OCR_RAW", "Extracted text:\n$extractedText")
            Log.d(
                "OCR_SUCCESS",
                "Successfully scanned receipt with ${extractedText.length} characters"
            )

            extractedText

        } catch (e: Exception) {
            Log.e("OCR_ERROR", "Failed to scan receipt", e)
            null
        }
    }

    /**
     * Scans directly from a bitmap (useful for camera captures)
     */
    suspend fun scanReceipt(bitmap: Bitmap): String? {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(inputImage).await()
            val extractedText = result.text

            Log.d("OCR_RAW", "Extracted text:\n$extractedText")
            Log.d(
                "OCR_SUCCESS",
                "Successfully scanned receipt with ${extractedText.length} characters"
            )

            extractedText

        } catch (e: Exception) {
            Log.e("OCR_ERROR", "Failed to scan receipt", e)
            null
        }
    }
}