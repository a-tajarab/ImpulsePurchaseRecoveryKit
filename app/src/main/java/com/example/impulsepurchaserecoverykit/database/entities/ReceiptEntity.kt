package com.example.impulsepurchaserecoverykit.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ReceiptEntity defines the structure of the 'receipts' table in the Room database
 *
 * Its the primary data entity of the Impulse Purchase Recovery Kit and stores both
 * financial data extracted by the OCR receipt parser and the emotional and behavioural
 * data that is recorded by the user following each purchase
 */
@Entity(tableName = "receipts")
data class ReceiptEntity(
    /**
     * Auto-generated unique identifier for each receipt entry
     * Its the first key for all the database relationships
     */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * The store name extracted by the parser, null if the parser can not
     * identify a store name. Users can manually correct this through edit mode
     */
    val storeName: String?,
    /**
     * The purchase date extracted from the receipt by OCR parser,
     * It is stored as string, to preserve the original format returned by OCR
     * which varies between formats. Null if no date pattern was matched through parsing.
     */
    val purchaseDate: String?,
    /**
     * The purchase time extracted from the receipt by the OCR parser,
     * null if no total value can be identified through parsing
     */
    val purchaseTime: String? = null,
    /**
     * The total amount extracted from the receipt
     * Null if no value can be identified through parsing
     */
    val totalAmount: Double?,
    /**
     * The subtotal amount before tax, taken from the receipt
     * Null if no subtotal was present or can be taken from receipt
     */
    val subtotal: Double?,
    /**
     * The tax or VAT amount extracted from the receipt
     * Null if no tax value is present or found through the parsing
     */
    val tax: Double?,

    /**
     * The shipping or delivery cost extracted from the receipt
     * Null if there is no shipping cost
     * Only present on online order receipts
     */
    val shipping: Double? = null,
    /**
     * The raw unprocessed text returned by Google ML Kit OCR scanning
     * Stored for debugging purposes and to allow re-parsing if the parser logic
     * is updated without needing the user to rescan the receipts
     */
    val rawOcrText: String,
    /**
     * The URI image of the image that is captured by the camera or selected
     * from the gallery. It is null if there is no image with the receipt entry
     */
    val imageUri: String?,

    /**
     * The automatic computed impulse risk score for this receipt
     * Ranges from 0 to 100 where higher values means higher impulse
     * Computed by the scoring engine based on the store type, time, category,
     * and spending patterns. It is null until computed.
     */
    val impulseScore: Int? = null,          // 0..100
    /**
     * The impulse risk labelled based on the impulse score
     * RED, YELLOW AND GREEN to indicate the danger zone
     * Null until the impulse score has been computed
     */
    val impulseLabel: String? = null,       // "LOW" | "MEDIUM" | "HIGH"
    /**
     * A JSON array string contains the reasons used to compute impulse score
     * Stored as a serialised JSON string
     * Null until the impulse scoring engine has processed this receipt
     */
    val impulseReasonsJson: String? = null, // JSON array of strings

    /**
     * The user's regret score for this receipt on a scale of 1 - 10
     * Null until the user has rated the purchase
     */
    // Regret score
    val regretScore: Int? = null,

    /**
     * An optional emotional note for the user along with regret score
     * Allows users to capture context behind a purchase in their own words
     * Null if the user did not provide a note
     */
    // Emotional note
    val emotionalNote: String? = null,
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    /**
     * A numerical sentiment score derived from the user's item level reaction
     * Calculated from the ratio of positive, neutral, and negative reactions to each item
     * in the receipt
     * Null until the user has rated at least one item from the receipt
     */
    val userSentimentScore: Int? = null,
    /**
     * A sentiment label derived from the UserSentimentScore
     * Represents overall emotional outcome of the purchase based on item-level
     * reactions. Null until sentiment has been calculated
     */
    val userSentimentLabel: String? = null
)
