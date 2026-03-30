package com.example.impulsepurchaserecoverykit.database

import androidx.room.withTransaction
import com.example.impulsepurchaserecoverykit.ImpulseScorer
import com.example.impulsepurchaserecoverykit.ParsedReceipt
import com.example.impulsepurchaserecoverykit.database.dao.ItemReactionDao
import com.example.impulsepurchaserecoverykit.database.entities.EmotionEntity
import com.example.impulsepurchaserecoverykit.database.entities.ItemEntity
import com.example.impulsepurchaserecoverykit.database.entities.ReceiptEntity
import com.example.impulsepurchaserecoverykit.database.models.CategorySpend
import com.example.impulsepurchaserecoverykit.database.models.CategoryCount
import com.example.impulsepurchaserecoverykit.database.models.WeeklySpend
import com.example.impulsepurchaserecoverykit.database.models.WeeklyRegret
import com.example.impulsepurchaserecoverykit.database.entities.ItemReactionEntity

import kotlinx.coroutines.flow.Flow

class ReceiptRepository(private val database: AppDatabase) {

    private val receiptDao = database.receiptDao()
    private val itemDao = database.itemDao()
    private val emotionDao = database.emotionDao()
    private val itemReactionDao = database.itemReactionDao()


    // ========== Receipt Operations ==========

    /**
     * Save a parsed receipt to the database
     */
    suspend fun saveReceipt(parsedReceipt: ParsedReceipt, imageUri: String?): Long {
        val impulse = ImpulseScorer.score(parsedReceipt)

        // Create receipt entity
        val receiptEntity = ReceiptEntity(
            storeName = parsedReceipt.storeName,
            purchaseDate = parsedReceipt.purchaseDate,
            purchaseTime = parsedReceipt.purchaseTime,
            totalAmount = parsedReceipt.total,
            subtotal = parsedReceipt.subtotal,
            tax = parsedReceipt.tax,
            rawOcrText = parsedReceipt.rawText,
            imageUri = imageUri,

            impulseScore = impulse.score,
            impulseLabel = impulse.label.name,
            impulseReasonsJson = impulse.reasonsJson()
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
    fun getAverageRegretScoreFlow(): Flow<Double?> {
        return receiptDao.getAverageRegretScoreFlow()
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

    suspend fun updatePurchaseTime(receiptId: Long, time: String){
        receiptDao.updatePurchaseTime(receiptId, time)
    }


    suspend fun addEmotionCheckIn(
        receiptId: Long,
        regretScore: Int,
        mood: String,
        notes: String?
    ) {
        emotionDao.insertEmotion(
            EmotionEntity(
                receiptId = receiptId,
                regretScore = regretScore,
                mood = mood,
                notes = notes
            )
        )

        val receipt = receiptDao.getReceiptById(receiptId) ?: return
        val updated = receipt.copy(
            regretScore = regretScore,
            emotionalNote = notes,
            updatedAt = System.currentTimeMillis()
        )
        receiptDao.updateReceipt(updated)
    }

    suspend fun setItemReaction(receiptId: Long, itemId: Long, reaction: Int) {
        itemReactionDao.upsertReaction(
            ItemReactionEntity(
                itemId = itemId,
                receiptId = receiptId,
                reaction = reaction
            )
        )
    }

    fun getItemReactionsForReceipt(receiptId: Long): Flow<List<ItemReactionEntity>> {
        return itemReactionDao.getReactionsForReceipt(receiptId)
    }


    private fun calcUserSentiment(reactions: List<ItemReactionEntity>): Pair<Double?, String?> {
        if (reactions.isEmpty()) return null to null

        val avg = reactions.map { it.reaction }.average()
        val percent = (((avg + 1.0) / 2.0) * 100.0).coerceIn(0.0, 100.0)

        val label = when {
            percent >= 67.0 -> "GOOD"
            percent <= 33.0 -> "BAD"
            else -> "MIXED"
        }
        return percent to label
    }

    suspend fun saveItemReactions(receiptId: Long, draft: Map<Long, Int>) {
        database.withTransaction {
            val rows = draft.map { (itemId, reaction) ->
                ItemReactionEntity(
                    itemId = itemId,
                    receiptId = receiptId,
                    reaction = reaction
                )
            }
            itemReactionDao.upsertAll(rows)
            itemReactionDao.deleteNotIn(receiptId, draft.keys.toList())

            val (score, label) = calcUserSentiment(rows)
            receiptDao.updateUserSentiment(receiptId, score, label)

        }
    }
}

