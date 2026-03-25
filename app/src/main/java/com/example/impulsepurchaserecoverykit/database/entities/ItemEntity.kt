package com.example.impulsepurchaserecoverykit.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "items",
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
data classItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val receiptId: Long,
    val name: String,
    val price: Double,
    val quantity: Int = 1,
    val category: String = "other"
)
