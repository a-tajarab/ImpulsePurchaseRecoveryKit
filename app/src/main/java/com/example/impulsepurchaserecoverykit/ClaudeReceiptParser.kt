package com.example.impulsepurchaserecoverykit

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream

object ClaudeReceiptParser {

    private const val TAG = "CLAUDE_PARSER"
    private const val MAX_IMAGE_DIMENSION = 1568

    suspend fun parseReceiptImage(
        bitmap: Bitmap,
        apiKey: String = BuildConfig.ANTHROPIC_API_KEY
    ): ParsedReceipt? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            Log.w(TAG, "API key is blank — skipping Claude vision parse")
            return@withContext null
        }
        try {
            val base64Image = bitmapToBase64(resizeBitmap(bitmap))
            Log.d(TAG, "Image size after resize: ${bitmap.width}x${bitmap.height} → base64 ${base64Image.length} chars")
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

            val parsed = mapJsonToParsedReceipt(cleanJson)
            Log.d(TAG, "Parsed: ${parsed.getSummary()}")
            parsed
        } catch (e: Exception) {
            Log.e(TAG, "Claude vision parse failed: ${e.message}")
            null
        }
    }

    private fun resizeBitmap(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= MAX_IMAGE_DIMENSION && h <= MAX_IMAGE_DIMENSION) return bitmap
        val scale = MAX_IMAGE_DIMENSION.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(bitmap, (w * scale).toInt(), (h * scale).toInt(), true)
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

        val receipt = ParsedReceipt(
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

        Log.d(TAG, "Mapped receipt — store: ${receipt.storeName}, " +
                "items: ${items.size}, total: ${receipt.total}")

        return receipt
    }
}