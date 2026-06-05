package com.example.impulsepurchaserecoverykit.database

import androidx.room.withTransaction
import com.example.impulsepurchaserecoverykit.ImpulseScorer
import com.example.impulsepurchaserecoverykit.ParsedReceipt
import com.example.impulsepurchaserecoverykit.database.dao.ItemReactionDao
import com.example.impulsepurchaserecoverykit.database.entities.EmotionEntity
import com.example.impulsepurchaserecoverykit.database.entities.GoalEntity
import com.example.impulsepurchaserecoverykit.database.entities.ItemEntity
import com.example.impulsepurchaserecoverykit.database.entities.ReceiptEntity
import com.example.impulsepurchaserecoverykit.database.models.CategorySpend
import com.example.impulsepurchaserecoverykit.database.models.CategoryCount
import com.example.impulsepurchaserecoverykit.database.models.WeeklySpend
import com.example.impulsepurchaserecoverykit.database.models.WeeklyRegret
import com.example.impulsepurchaserecoverykit.database.entities.ItemReactionEntity
import com.example.impulsepurchaserecoverykit.database.entities.SavingGoalEntity

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ReceiptRepository(private val database: AppDatabase) {

    private val receiptDao = database.receiptDao()
    private val itemDao = database.itemDao()
    private val emotionDao = database.emotionDao()
    private val itemReactionDao = database.itemReactionDao()
    private val goalDao = database.goalDao()
    private val savingGoalDao = database.savingGoalDao()

    fun getGoal(): Flow<GoalEntity?> = goalDao.getGoal()
    suspend fun upsertGoal(goal: GoalEntity) = goalDao.upsertGoal(goal)

    suspend fun deleteGoal() = goalDao.deleteGoal()


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
            shipping = parsedReceipt.shipping,
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
    fun getReceiptByIdFlow(receiptId: Long): Flow<ReceiptEntity?> {
        return receiptDao.getReceiptByIdFlow(receiptId)
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

    suspend fun updateReceiptDetails(
        receiptId: Long,
        storeName: String?,
        purchaseDate: String?,
        purchaseTime: String?,
        totalAmount: Double?,
        subtotal: Double?,
        tax: Double?
    ) {
        receiptDao.updateReceiptDetails(
            receiptId = receiptId,
            storeName = storeName,
            purchaseDate = purchaseDate,
            purchaseTime = purchaseTime,
            totalAmount = totalAmount,
            updatedAt = System.currentTimeMillis(),
            subtotal = subtotal,
            tax = tax
        )
    }

    suspend fun updateItem(itemId: Long, name: String, price: Double, quantity: Int) {
        itemDao.updateItem(itemId, name, price, quantity)
    }

    suspend fun addItemToReceipt(receiptId: Long, name: String, price: Double, quantity: Int) {
        val item = ItemEntity(
            receiptId = receiptId,
            name = name,
            price = price,
            quantity = quantity,
            category = categorizeByName(name)
        )
        itemDao.insertItem(item)
    }

    private fun categorizeByName(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.containsAny("milk", "cheese", "yogurt", "butter", "cream", "dairy") -> "dairy"
            lower.containsAny("fruit", "vegetable", "veg", "salad", "lettuce", "tomato", "apple", "banana", "orange", "carrot", "onion", "potato") -> "produce"
            lower.containsAny("water", "juice", "soda", "coffee", "tea", "drink", "beverage", "beer", "wine", "coke", "pepsi", "lemonade") -> "beverage"
            lower.containsAny("chicken", "beef", "pork", "lamb", "fish", "salmon", "tuna", "shrimp", "steak", "bacon", "sausage", "meat") -> "meat"
            lower.containsAny("bread", "cake", "muffin", "croissant", "bagel", "pastry", "cookie", "donut", "biscuit", "bakery") -> "bakery"
            lower.containsAny("shirt", "top", "blouse", "tee", "jumper", "sweater", "hoodie") -> "tops"
            lower.containsAny("jeans", "trousers", "pants", "shorts", "skirt", "leggings") -> "bottoms"
            lower.containsAny("jacket", "coat", "blazer", "hoodie", "cardigan", "outerwear") -> "outerwear"
            lower.containsAny("shoe", "boot", "sneaker", "trainer", "heel", "sandal", "loafer") -> "shoes"
            lower.containsAny("necklace", "ring", "bracelet", "earring", "watch", "accessory") -> "accessories"
            lower.containsAny("bag", "handbag", "backpack", "purse", "wallet", "tote") -> "bags"
            lower.containsAny("shampoo", "conditioner", "soap", "toothpaste", "deodorant", "moisturiser", "perfume", "makeup", "lipstick", "mascara") -> "toiletries"
            lower.containsAny("candle", "towel", "pillow", "blanket", "cup", "mug", "plate", "bowl", "vase", "frame", "lamp") -> "homeware"
            else -> "other"
        }
    }

    private fun String.containsAny(vararg keywords: String) = keywords.any { this.contains(it) }
    fun getItemsForMonth(year: Int, month: Int): Flow<List<ItemEntity>> {
        return receiptDao.getItemsForMonth(year, month)
    }

    fun getMonthlySpend(year: Int, month: Int): Flow<Double?> =
        receiptDao.getMonthlySpend(year, month)



    /**
     * Calculates how much the user has saved this month by NOT making
     * high impulse purchases. Compares what they actually spent on LOW
     * impulse items vs what they would have spent if all purchases
     * were high impulse.
     */
    suspend fun calculateMonthlySavings(year: Int, month: Int): Double {
        // Use first() to get a single snapshot from the Flow
        val allReceipts = receiptDao.getAllReceipts().first()

        val monthReceipts = allReceipts.filter { receipt ->
            receipt.purchaseDate?.let { date ->
                isInMonth(date, year, month)
            } ?: false
        }

        // High impulse spend = money spent impulsively this month
        val highImpulseSpend = monthReceipts
            .filter { it.impulseLabel == "HIGH" }
            .sumOf { it.totalAmount ?: 0.0 }

        return highImpulseSpend
    }

    private fun isInMonth(date: String, year: Int, month: Int): Boolean {
        return try {
            when {
                // DD/MM/YYYY format
                date.matches(Regex("""\d{2}/\d{2}/\d{4}""")) -> {
                    val m = date.substring(3, 5).toInt()
                    val y = date.substring(6, 10).toInt()
                    m == month && y == year
                }
                // D/M/YYYY format
                date.matches(Regex("""\d{1,2}/\d{1,2}/\d{4}""")) -> {
                    val parts = date.split("/")
                    parts[1].toInt() == month && parts[2].toInt() == year
                }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }



    fun getSavingGoals(): Flow<List<SavingGoalEntity>> =
        savingGoalDao.getAllGoals()

    suspend fun addSavingGoal(name: String, targetAmount: Double) {
        val nextPriority = savingGoalDao.getGoalCount() + 1
        savingGoalDao.insertGoal(
            SavingGoalEntity(name = name, targetAmount = targetAmount, priority = nextPriority)
        )
    }

    suspend fun updateSavingGoal(goal: SavingGoalEntity) =
        savingGoalDao.updateGoal(goal)

    suspend fun deleteSavingGoal(goal: SavingGoalEntity) =
        savingGoalDao.deleteGoal(goal)

    /**
     * Swaps the priority values of two adjacent goals so one moves
     * up or down in the ordered list.
     */
    suspend fun swapSavingGoalPriorities(a: SavingGoalEntity, b: SavingGoalEntity) {
        savingGoalDao.updateGoal(a.copy(priority = b.priority))
        savingGoalDao.updateGoal(b.copy(priority = a.priority))
    }
}

