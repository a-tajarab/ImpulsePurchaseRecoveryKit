package com.example.impulsepurchaserecoverykit.database.dao

import androidx.room.*
import com.example.impulsepurchaserecoverykit.database.entities.ItemEntity
import kotlinx.coroutines.flow.Flow
import com.example.impulsepurchaserecoverykit.database.models.CategorySpend
import com.example.impulsepurchaserecoverykit.database.models.CategoryCount


@Dao
interface ItemDao {

    @Insert
    suspend fun insertItems(items: List<ItemEntity>)

    @Insert
    suspend fun insertItem(item: ItemEntity): Long

    @Query("SELECT * FROM items WHERE receiptId = :receiptId")
    suspend fun getItemsForReceipt(receiptId: Long): List<ItemEntity>

    @Query("SELECT * FROM items WHERE receiptId = :receiptId")
    fun getItemsForReceiptFlow(receiptId: Long): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE category = :category")
    fun getItemsByCategory(category: String): Flow<List<ItemEntity>>

    @Query("SELECT SUM(price * quantity) FROM items WHERE receiptId = :receiptId")
    suspend fun getTotalForReceipt(receiptId: Long): Double?

    //@Query("SELECT category, COUNT(*) as count FROM items GROUP BY category ORDER BY count DESC")
    //suspend fun getCategoryStats(): List<CategoryStats>

    @Query("""
    SELECT category AS category,
           SUM(price * quantity) AS total
    FROM items
    WHERE category NOT IN ('stopKeywords', 'other')
    GROUP BY category
    ORDER BY total DESC
""")
    fun getSpendByCategory(): Flow<List<CategorySpend>>

    @Query("""
    SELECT category AS category,
           COUNT(*) AS count
    FROM items
    WHERE category NOT IN ('stopKeywords', 'other')
    GROUP BY category
    ORDER BY count DESC
""")
    fun getItemCountByCategory(): Flow<List<CategoryCount>>

    @Query("""
    UPDATE items 
    SET name = :name, price = :price, quantity = :quantity 
    WHERE id = :itemId
""")
    suspend fun updateItem(itemId: Long, name: String, price: Double, quantity: Int)

}