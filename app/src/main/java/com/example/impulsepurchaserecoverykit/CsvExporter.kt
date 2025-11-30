package com.example.impulsepurchaserecoverykit
import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
class CsvExporter(private val context: Context) {
    /**
     * Export a parsed receipt to CSV file
     */
    fun exportToCSV(receipt: ParsedReceipt): File? {
        return try {
            // Create filename with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "receipt_$timestamp.csv"

            // Get the Downloads directory
            val file = File(context.getExternalFilesDir(null), filename)

            FileWriter(file).use { writer ->
                // Write header
                writer.append("Store,Date,Item,Category,Price,Quantity,Total\n")

                // Write each item
                for (item in receipt.items) {
                    writer.append("${csvEscape(receipt.storeName ?: "Unknown")},")
                    writer.append("${csvEscape(receipt.purchaseDate ?: "Unknown")},")
                    writer.append("${csvEscape(item.name)},")
                    writer.append("${csvEscape(item.category)},")
                    writer.append("${item.price},")
                    writer.append("${item.quantity},")
                    writer.append("${item.price * item.quantity}\n")
                }

                // Write total row
                writer.append("\n")
                writer.append(",,,Subtotal,,${receipt.subtotal ?: ""}\n")
                writer.append(",,,Tax,,${receipt.tax ?: ""}\n")
                writer.append(",,,Total,,${receipt.total ?: ""}\n")
            }

            Log.d("CSV_EXPORT", "Exported to: ${file.absolutePath}")
            file

        } catch (e: Exception) {
            Log.e("CSV_EXPORT", "Failed to export CSV", e)
            null
        }
    }

    /**
     * Escape special characters for CSV format
     */
    private fun csvEscape(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    /**
     * Export raw OCR text for analysis
     */
    fun exportRawOcrText(ocrText: String, receiptId: String = "unknown"): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "ocr_raw_${receiptId}_$timestamp.txt"

            val file = File(context.getExternalFilesDir(null), filename)
            file.writeText(ocrText)

            Log.d("CSV_EXPORT", "Exported raw OCR to: ${file.absolutePath}")
            file

        } catch (e: Exception) {
            Log.e("CSV_EXPORT", "Failed to export raw OCR", e)
            null
        }
    }
}

