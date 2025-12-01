package com.example.impulsepurchaserecoverykit.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "emotion_checkins",
    foreignKeys = [
        ForeignKey(
            entity = ReceiptEntity::class,
            parentColumns = ["id"],
            childColumns = ["receiptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("receiptId")]
)
data class EmotionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val receiptId: Long,
    val checkInTime: Long = System.currentTimeMillis(),
    val regretScore: Int, // 1-10
    val mood: String, // "regretful", "neutral", "satisfied"
    val notes: String?
)