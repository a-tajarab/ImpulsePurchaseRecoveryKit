package com.example.impulsepurchaserecoverykit.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.example.impulsepurchaserecoverykit.database.entities.ItemReactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for item reaction operations.
 *
 * ItemReactionDao manages all database interactions for the per-item sentiment
 * reactions that users assign on the Receipt Detail screen. Each reaction records
 * whether the user felt Happy, Ok, or Regret about an individual item on a receipt.
 *
 * Because users can update their reactions multiple times, most write operations
 * use REPLACE or Upsert strategies to avoid duplicate entries. Flow-returning
 * queries keep the UI in sync automatically as reactions are saved.
 */
@Dao
interface ItemReactionDao {

    /**
     * Inserts or replaces a single item reaction in the database.
     *
     * If a reaction already exists for the given item, it is replaced with
     * the new value. Used when the user taps a reaction button on a single
     * item in the Receipt Detail screen.
     *
     * @param reactions The [ItemReactionEntity] to insert or replace
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReaction(reactions: ItemReactionEntity)

    /**
     * Inserts or replaces a batch of item reactions in a single transaction.
     *
     * Used when saving reactions for all items on a receipt at once,
     * for example when the user leaves the Receipt Detail screen and
     * all pending reactions are flushed to the database together.
     *
     * @param reactions The list of [ItemReactionEntity] entries to insert or replace
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReactions(reactions: List<ItemReactionEntity>)

    /**
     * Observes all item reactions for a specific receipt as a reactive stream.
     *
     * Returns a [Flow] that emits a new list whenever any reaction for the
     * given receipt changes. Used on the Receipt Detail screen to keep the
     * live mood summary banner and item reaction buttons in sync with the
     * database in real time.
     *
     * @param receiptId The ID of the receipt whose item reactions should be observed
     * @return A [Flow] emitting the current list of [ItemReactionEntity] for that receipt
     */
    @Query("SELECT * FROM item_reactions WHERE receiptId = :receiptId")
    fun getReactionsForReceipt(receiptId: Long): Flow<List<ItemReactionEntity>>

    /**
     * Retrieves all item reactions for a specific receipt as a one-shot result.
     *
     * Unlike [getReactionsForReceipt], this is a suspend function that returns
     * the current state once without observing future changes. Used when a
     * single snapshot of reactions is needed, such as when pre-populating
     * the reaction buttons when a receipt is first opened.
     *
     * @param receiptId The ID of the receipt whose reactions should be retrieved
     * @return A list of [ItemReactionEntity] entries for that receipt
     */
    @Query("SELECT * FROM item_reactions WHERE receiptId = :receiptId")
    suspend fun getReactionsForReceiptOnce(receiptId: Long): List<ItemReactionEntity>

    /**
     * Retrieves the reaction for a single item by its ID.
     *
     * Used to check whether a specific item already has a saved reaction
     * before deciding whether to insert or update. Returns null if the
     * user has not yet reacted to that item.
     *
     * @param itemId The ID of the item to look up
     * @return The [ItemReactionEntity] for that item, or null if none exists
     */
    @Query("SELECT * FROM item_reactions WHERE itemId = :itemId LIMIT 1 ")
    suspend fun getReactionForItem(itemId: Long): ItemReactionEntity?

    /**
     * Observes all item reactions for a specific receipt.
     *
     * Functionally equivalent to [getReactionsForReceipt] — both return a
     * reactive [Flow] of reactions for the given receipt. This overload exists
     * to support observation contexts where a named distinction between
     * one-shot reads and live observation improves readability.
     *
     * @param receiptId The ID of the receipt to observe
     * @return A [Flow] emitting the current list of [ItemReactionEntity] for that receipt
     */
    @Query("SELECT * FROM item_reactions WHERE receiptId = :receiptId")
    fun observeForReceipt(receiptId: Long): Flow<List<ItemReactionEntity>>

    /**
     * Inserts or updates a batch of item reactions using Room's Upsert strategy.
     *
     * Preferred over [upsertReactions] for bulk writes as Room's @Upsert annotation
     * handles conflict resolution more efficiently than @Insert with REPLACE
     * for large batches. Used when syncing all item reactions for a receipt
     * after the user finishes rating their items.
     *
     * @param rows The list of [ItemReactionEntity] entries to upsert
     */
    @Upsert
    suspend fun upsertAll(rows: List<ItemReactionEntity>)

    /**
     * Deletes all item reactions for a receipt except those whose item IDs
     * are in the provided keep list.
     *
     * Used after editing a receipt to clean up reactions for items that have
     * been removed. Only reactions belonging to items that still exist on the
     * receipt are preserved, preventing orphaned reaction records in the database.
     *
     * @param receiptId The ID of the receipt to clean up
     * @param keepIds The list of item IDs whose reactions should be kept
     */
    @Query("DELETE FROM item_reactions WHERE receiptId = :receiptId AND itemId NOT IN (:keepIds)")
    suspend fun deleteNotIn(receiptId: Long, keepIds: List<Long>)
}