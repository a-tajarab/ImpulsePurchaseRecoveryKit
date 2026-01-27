package com.example.impulsepurchaserecoverykit.database

import com.example.impulsepurchaserecoverykit.ParsedReceipt
import com.example.impulsepurchaserecoverykit.database.entities.ItemEntity
import com.example.impulsepurchaserecoverykit.database.entities.ReceiptEntity
import com.example.impulsepurchaserecoverykit.database.models.CategorySpend
import com.example.impulsepurchaserecoverykit.database.models.CategoryCount
import com.example.impulsepurchaserecoverykit.database.models.WeeklySpend
import com.example.impulsepurchaserecoverykit.database.models.WeeklyRegret

import kotlinx.coroutines.flow.Flow

class ReceiptRepository(private val database: AppDatabase) {

    private val receiptDao = database.receiptDao()
    private val itemDao = database.itemDao()
    private val emotionDao = database.emotionDao()

    // ========== Receipt Operations ==========

    /**
     * Save a parsed receipt to the database
     */
    suspend fun saveReceipt(parsedReceipt: ParsedReceipt, imageUri: String?): Long {
        // Create receipt entity
        val receiptEntity = ReceiptEntity(
            storeName = parsedReceipt.storeName,
            purchaseDate = parsedReceipt.purchaseDate,
            totalAmount = parsedReceipt.total,
            subtotal = parsedReceipt.subtotal,
            tax = parsedReceipt.tax,
            rawOcrText = parsedReceipt.rawText,
            imageUri = imageUri
        )

        // Insert receipt and get its ID
        val receiptId = receiptDao.insertReceipt(receiptEntity)

        // Create item entities linked to this receipt
        val itemEntities = parsedReceipt.items.map { parsedItem ->
            ItemEntity(
                receiptId = receiptId,
                name = parsedItem.name,
                price = parsedItem.price,
                quantity = parsedItem.quantity,
                category = parsedItem.category
            )
        }

        // Insert all items
        if (itemEntities.isNotEmpty()) {
            itemDao.insertItems(itemEntities)
        }

        return receiptId
    }

    /**
     * Get all receipts
     */
    fun getAllReceipts(): Flow<List<ReceiptEntity>> {
        return receiptDao.getAllReceipts()
    }

    /**
     * Get a specific receipt by ID
     */
    suspend fun getReceiptById(receiptId: Long): ReceiptEntity? {
        return receiptDao.getReceiptById(receiptId)
    }

    /**
     * Update regret score for a receipt
     */
    suspend fun updateRegretScore(receiptId: Long, regretScore: Int, note: String?) {
        val receipt = receiptDao.getReceiptById(receiptId)
        receipt?.let {
            val updated = it.copy(
                regretScore = regretScore,
                emotionalNote = note,
                updatedAt = System.currentTimeMillis()
            )
            receiptDao.updateReceipt(updated)
        }
    }

    /**
     * Delete a receipt (items will be deleted automatically via CASCADE)
     */
    suspend fun deleteReceipt(receipt: ReceiptEntity) {
        receiptDao.deleteReceipt(receipt)
    }

    // ========== Item Operations ==========

    /**
     * Get all items for a specific receipt
     */
    suspend fun getItemsForReceipt(receiptId: Long): List<ItemEntity> {
        return itemDao.getItemsForReceipt(receiptId)
    }

    /**
     * Get items by category
     */
    fun getItemsByCategory(category: String): Flow<List<ItemEntity>> {
        return itemDao.getItemsByCategory(category)
    }

    // ========== Statistics ==========

    /**
     * Get total number of receipts
     */
    suspend fun getReceiptCount(): Int {
        return receiptDao.getReceiptCount()
    }

    /**
     * Get average regret score
     */
    suspend fun getAverageRegretScore(): Double? {
        return emotionDao.getAverageRegretScore()
    }

    /**
     * Get high regret purchases
     */
    fun getHighRegretReceipts(minScore: Int = 7): Flow<List<ReceiptEntity>> {
        return receiptDao.getHighRegretReceipts(minScore)
    }

    fun getRecentReceipts(limit: Int = 5): Flow<List<com.example.impulsepurchaserecoverykit.database.entities.ReceiptEntity>> {
        return receiptDao.getRecentReceipts(limit)
    }

    fun getItemsForReceiptFlow(receiptId: Long): Flow<List<com.example.impulsepurchaserecoverykit.database.entities.ItemEntity>> {
        return itemDao.getItemsForReceiptFlow(receiptId)
    }

    fun getSpendByCategory(): Flow<List<CategorySpend>> {
        return itemDao.getSpendByCategory()
    }

    fun getItemCountByCategory(): Flow<List<CategoryCount>> {
        return itemDao.getItemCountByCategory()
    }

    fun getTotalSpend(): Flow<Double?> {
        return receiptDao.getTotalSpend()
    }

    fun getTopRegretReceipts(limit: Int = 3): Flow<List<ReceiptEntity>> {
        return receiptDao.getTopRegretReceipts(limit)
    }

    fun getWeeklySpend(): Flow<List<WeeklySpend>> {
        return receiptDao.getWeeklySpend()
    }

    fun getWeeklyAverageRegret(): Flow<List<WeeklyRegret>> {
        return receiptDao.getWeeklyAverageRegret()
    }



}
