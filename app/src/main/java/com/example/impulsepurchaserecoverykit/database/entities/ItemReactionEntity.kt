package com.example.impulsepurchaserecoverykit.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "item_reactions",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ReceiptEntity::class,
            parentColumns = ["id"],
            childColumns = ["receiptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("receiptId"),
        Index("itemId")
    ]
)
data class ItemReactionEntity(
    @PrimaryKey
    val itemId: Long,              // one reaction per item (simple!)
    val receiptId: Long,
    val reaction: Int,             // 1 = positive, 0 = neutral, -1 = negative
    val updatedAt: Long = System.currentTimeMillis()
)