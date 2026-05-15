package com.example.impulsepurchaserecoverykit

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.resume

object ClaudeReceiptParser {

    private const val TAG = "CLAUDE_PARSER"
    private const val MAX_IMAGE_DIMENSION = 1568

    suspend fun parseReceiptImage(
        bitmap: Bitmap,
        apiKey: String = BuildConfig.ANTHROPIC_API_KEY,
        onFallback: (() -> Unit) ? = null
    ): ParsedReceipt? = withContext(Dispatchers.IO) {
        if (apiKey.isNotBlank()) {
            try {
                Log.d(TAG, "Attempting Claude Vision parse...")
                val result = parseWithClaude(bitmap, apiKey)
                if (result != null && result.total != null) {
                    Log.d(TAG, "Claude parse succeeded: ${result.getSummary()}")
                    return@withContext result
                }
                Log.w(TAG, "Claude returned empty or invalid result - trying fallback")
            } catch (e: Exception) {
                Log.w(TAG, "Claude parse failed (${e.message}) - trying local fallbacl")
            }
        } else {
            Log.w(TAG, "API key is blank - skipping Claude, using the local parser")
        }

        return@withContext try {

            Log.d(TAG, "Starting local ML Kit fallback parse...")
            onFallback?.invoke()
            parseWithLocalOcr(bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Local fallback also failed: ${e.message}")
            null
        }
    }



    /**
     * Parses the receipt using Claude Vision API.
     * Returns null if the response is empty or cannot be mapped.
     */
        private suspend fun parseWithClaude(bitmap: Bitmap, apiKey: String): ParsedReceipt?{
            val base64Image = bitmapToBase64(resizeBitmap(bitmap))
            Log.d(TAG, " Image converted - base64 length: ${base64Image.length} chars")

            val rawResponse = AnthropicApiClient.parseReceipt(
                apiKey = apiKey,
                imageBase64 = base64Image,
                mediaType = "image/jpeg"
            )
            Log.d(TAG, "Raw Claude response:\n$rawResponse")

            val cleanJson = rawResponse
                .replace("```json", "")
                .replace("```", "")
                .trim()

            return mapJsonToParsedReceipt(cleanJson)
    }

    /**
     * Parses the receipt using ML Kit OCR on-device followed by
     * the local ReceiptParser. This is the offline fallback path.
     * No data leaves the device in this path.
     */
    private suspend fun parseWithLocalOcr(bitmap: Bitmap): ParsedReceipt? {
        val rawText = runMlKitOcr(bitmap)
        if (rawText.isBlank()) {
            Log.w(TAG, "ML Kit returned no text")
            return null
        }
        Log.d(TAG, "ML Kit extracted ${rawText.length} chars of text")
        return ReceiptParser().parseReceipt(rawText)
    }


    /**
     * Runs ML Kit text recognition on the bitmap and returns the
     * extracted raw text as a String.
     *
     * Uses suspendCancellableCoroutine to bridge ML Kit's callback
     * based API into a Kotlin coroutine suspend function.
     */
    private suspend fun runMlKitOcr(bitmap: Bitmap): String =
        suspendCancellableCoroutine { continuation ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)

            recognizer.process(image)
                .addOnSuccessListener { result ->
                    val text = result.text
                    Log.d(TAG, "ML Kit OCR succeeded — ${text.length} chars")
                    continuation.resume(text)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "ML Kit OCR failed: ${e.message}")
                    continuation.resumeWithException(e)
                }
        }

    private fun resizeBitmap(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= MAX_IMAGE_DIMENSION && h <= MAX_IMAGE_DIMENSION) return bitmap
        val scale = MAX_IMAGE_DIMENSION.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(bitmap,
            (w * scale).toInt(),
            (h * scale).toInt(),
            true)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Maps the Claude JSON response to a ParsedReceipt object.
     *
     * Uses optString and optDouble throughout so that missing fields
     * return null rather than throwing exceptions — making the parser
     * resilient to partial responses.
     *
     * After mapping, runs the ImpulseScorer to calculate the impulse
     * risk score and label for the parsed receipt.
     *
     * @param jsonString The cleaned JSON string returned by Claude
     * @return A fully populated ParsedReceipt with impulse scoring applied
     */
    private fun mapJsonToParsedReceipt(jsonString: String): ParsedReceipt {
        val json = JSONObject(jsonString)

        // Parse items array
        val items = mutableListOf<ParsedItem>()
        val itemsArray = json.optJSONArray("items")
        if (itemsArray != null) {
            for (i in 0 until itemsArray.length()) {
                val item = itemsArray.getJSONObject(i)
                val name = item.optString("name", "").ifBlank { null } ?: continue
                val price = item.optDouble("price", 0.0)
                val quantity = item.optInt("quantity", 1)
                val category = item.optString("category", "other")
                    .ifBlank { "other" }

                items.add(
                    ParsedItem(
                        name = name,
                        price = price,
                        quantity = quantity,
                        category = category
                    )
                )
            }
        }

        return ParsedReceipt(
            storeName = json.optString("storeName").ifEmpty { null },
            purchaseDate = json.optString("purchaseDate").ifEmpty { null },
            purchaseTime = json.optString("purchaseTime").ifEmpty { null },
            items = items,
            subtotal = if (json.isNull("subtotal")) null
            else json.optDouble("subtotal").takeIf { !it.isNaN() },
            tax = if (json.isNull("tax")) null
            else json.optDouble("tax").takeIf { !it.isNaN() },
            shipping = if (json.isNull("shipping")) null
            else json.optDouble("shipping").takeIf { !it.isNaN() },
            total = if (json.isNull("total")) null
            else json.optDouble("total").takeIf { !it.isNaN() },
            rawText = jsonString
        )
    }
}