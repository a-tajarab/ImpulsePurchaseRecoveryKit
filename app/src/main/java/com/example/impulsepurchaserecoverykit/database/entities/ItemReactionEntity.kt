package com.example.impulsepurchaserecoverykit.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room database entity representing the user's sentiment reaction to a single
 * receipt item.
 *
 * When a user rates their items on the Receipt Detail screen, tapping Happy,
 * Ok, or Regret on any item creates or updates an ItemReactionEntity in the
 * `item_reactions` table. Each item can only ever have one reaction — [itemId]
 * is the primary key, so saving a new reaction for the same item always
 * replaces the previous one.
 *
 * ItemReactionEntity holds two foreign key relationships:
 * - A reference to [ItemEntity] — the specific item being reacted to
 * - A reference to [ReceiptEntity] — the receipt the item belongs to
 *
 * Both relationships use CASCADE deletion, so if either the parent receipt or
 * the parent item is deleted, the associated reaction is automatically removed,
 * keeping the database free of orphaned records.
 *
 * Indices are maintained on both [receiptId] and [itemId] to ensure fast
 * lookup when loading all reactions for a given receipt or item.
 */
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

    /**
     * The ID of the item this reaction belongs to, sourced from [ItemEntity.id].
     * Acts as the primary key — each item can only have one reaction at a time.
     * Upserting a reaction for the same item always replaces the previous value.
     */
    @PrimaryKey
    val itemId: Long,

    /**
     * The ID of the receipt this item belongs to, sourced from [ReceiptEntity.id].
     * Stored alongside [itemId] to allow efficient retrieval of all reactions
     * for a given receipt without requiring a JOIN across tables.
     */
    val receiptId: Long,

    /**
     * The user's sentiment reaction to this item, stored as an integer.
     * Possible values:
     * -  1 = Happy — the user is glad they bought this item
     * -  0 = Ok — the user feels neutral about this item
     * - -1 = Regret — the user wishes they had not bought this item
     *
     * The integer representation allows easy aggregation across all items
     * on a receipt to produce the overall mood summary banner shown on
     * the Receipt Detail screen.
     */
    val reaction: Int,

    /**
     * The timestamp (in milliseconds since epoch) at which this reaction was
     * last saved. Automatically set to the current system time on creation
     * and updated whenever the user changes their reaction for this item.
     */
    val updatedAt: Long = System.currentTimeMillis()
)