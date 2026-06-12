package com.example.impulsepurchaserecoverykit.database.dao

import androidx.room.*
import com.example.impulsepurchaserecoverykit.database.entities.ItemEntity
import kotlinx.coroutines.flow.Flow
import com.example.impulsepurchaserecoverykit.database.models.CategorySpend
import com.example.impulsepurchaserecoverykit.database.models.CategoryCount

/**
 * Data Access Object (DAO) for item-level database operations.
 *
 * ItemDao manages all interactions with the `items` table, which stores the
 * individual line items parsed from each receipt. It provides methods for
 * inserting, querying, and updating items, as well as aggregated queries
 * that power the category breakdown charts on the Stats screen.
 *
 * Items are always linked to a parent receipt via [ItemEntity.receiptId].
 * Queries that return a [Flow] keep the UI reactively updated whenever
 * the underlying item data changes in the database.
 */
@Dao
interface ItemDao {

    /**
     * Inserts a batch of items into the database in a single transaction.
     *
     * Called after a receipt is parsed — all items extracted from the receipt
     * are saved together at once rather than one by one, which is more
     * efficient and ensures the items are either all saved or none are.
     *
     * @param items The list of [ItemEntity] entries to insert
     */
    @Insert
    suspend fun insertItems(items: List<ItemEntity>)

    /**
     * Inserts a single item into the database.
     *
     * Used when adding an individual item, for example during manual entry
     * or when the user adds a single item to an existing receipt.
     *
     * @param item The [ItemEntity] to insert
     * @return The auto-generated row ID of the newly inserted item
     */
    @Insert
    suspend fun insertItem(item: ItemEntity): Long

    /**
     * Retrieves all items belonging to a specific receipt as a one-shot result.
     *
     * Returns the current list of items once without observing future changes.
     * Used when a single snapshot is needed, such as when calculating the
     * receipt total or pre-populating an edit form.
     *
     * @param receiptId The ID of the receipt whose items should be retrieved
     * @return A list of [ItemEntity] entries for that receipt
     */
    @Query("SELECT * FROM items WHERE receiptId = :receiptId")
    suspend fun getItemsForReceipt(receiptId: Long): List<ItemEntity>

    /**
     * Observes all items belonging to a specific receipt as a reactive stream.
     *
     * Returns a [Flow] that emits a new list whenever items for the given
     * receipt are inserted, updated, or deleted. Used on the Receipt Detail
     * screen to keep the item list and mood summary banner in sync with
     * the database in real time.
     *
     * @param receiptId The ID of the receipt whose items should be observed
     * @return A [Flow] emitting the current list of [ItemEntity] for that receipt
     */
    @Query("SELECT * FROM items WHERE receiptId = :receiptId")
    fun getItemsForReceiptFlow(receiptId: Long): Flow<List<ItemEntity>>

    /**
     * Observes all items belonging to a specific spending category.
     *
     * Returns a [Flow] that updates automatically as items are added or
     * recategorised. Used to filter and display items by category on the
     * Stats screen.
     *
     * @param category The category name to filter by, for example "tops" or "beverage"
     * @return A [Flow] emitting all [ItemEntity] entries in that category
     */
    @Query("SELECT * FROM items WHERE category = :category")
    fun getItemsByCategory(category: String): Flow<List<ItemEntity>>

    /**
     * Calculates the total value of all items on a specific receipt.
     *
     * Multiplies price by quantity for each item and sums the results,
     * giving the correct total even when items have quantities greater than 1.
     * Returns null if the receipt has no items.
     *
     * @param receiptId The ID of the receipt to calculate the total for
     * @return The sum of (price × quantity) for all items on the receipt,
     *         or null if no items exist for that receipt
     */
    @Query("SELECT SUM(price * quantity) FROM items WHERE receiptId = :receiptId")
    suspend fun getTotalForReceipt(receiptId: Long): Double?

    /**
     * Observes total spending grouped by category, excluding generic categories.
     *
     * Aggregates all item spend by category (price × quantity), filters out
     * the "other" and "stopKeywords" catch-all categories so only meaningful
     * category names appear in the results, and orders by highest spend first.
     *
     * Used to populate the category breakdown bar chart on the Categories tab
     * of the Stats screen.
     *
     * @return A [Flow] emitting a list of [CategorySpend] entries ordered by
     *         total spend descending, updated automatically as items change
     */
    @Query("""
        SELECT category AS category,
               SUM(price * quantity) AS total
        FROM items
        WHERE category NOT IN ('stopKeywords', 'other')
        GROUP BY category
        ORDER BY total DESC
    """)
    fun getSpendByCategory(): Flow<List<CategorySpend>>

    /**
     * Observes the number of items purchased in each category, excluding generic categories.
     *
     * Groups all items by category and counts the entries in each group,
     * filtering out "other" and "stopKeywords". Ordered by highest count first
     * so the most frequently purchased category appears at the top.
     *
     * Used alongside [getSpendByCategory] to provide frequency context on the
     * Stats screen — showing not just how much was spent in a category but
     * how many individual items were purchased there.
     *
     * @return A [Flow] emitting a list of [CategoryCount] entries ordered by
     *         item count descending, updated automatically as items change
     */
    @Query("""
        SELECT category AS category,
               COUNT(*) AS count
        FROM items
        WHERE category NOT IN ('stopKeywords', 'other')
        GROUP BY category
        ORDER BY count DESC
    """)
    fun getItemCountByCategory(): Flow<List<CategoryCount>>

    /**
     * Updates the name, price, and quantity of an existing item by its ID.
     *
     * Called when the user edits an item on the Edit Receipt screen. Only
     * the three editable fields are updated — the item's category and
     * receipt association remain unchanged.
     *
     * @param itemId The ID of the item to update
     * @param name The updated item name
     * @param price The updated price per unit in GBP
     * @param quantity The updated quantity purchased
     */
    @Query("""
        UPDATE items 
        SET name = :name, price = :price, quantity = :quantity 
        WHERE id = :itemId
    """)
    suspend fun updateItem(itemId: Long, name: String, price: Double, quantity: Int)
}