package com.example.impulsepurchaserecoverykit.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val storeName: String?,
    val purchaseDate: String?,
    val purchaseTime: String? = null,
    val totalAmount: Double?,
    val subtotal: Double?,
    val tax: Double?,

    val shipping: Double? = null,
    val rawOcrText: String,
    val imageUri: String?,

    // NEW: impulse scoring (computed automatically)
    val impulseScore: Int? = null,          // 0..100
    val impulseLabel: String? = null,       // "LOW" | "MEDIUM" | "HIGH"
    val impulseReasonsJson: String? = null, // JSON array of strings

    // Regret score (1-10, null until user rates)
    val regretScore: Int? = null,

    // Emotional note
    val emotionalNote: String? = null,

    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    val userSentimentScore: Int? = null,
    val userSentimentLabel: String? = null
)
