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

/**
 * Single source of truth for all data operations in the app.
 *
 * ReceiptRepository sits between the ViewModels and the Room database,
 * providing a clean API that abstracts away the individual DAOs. ViewModels
 * never access DAOs directly — all database reads and writes go through
 * this class.
 *
 * The repository coordinates operations that span multiple DAOs, such as
 * saving a receipt and its items together, or updating sentiment scores
 * across both item reactions and the parent receipt in a single transaction.
 *
 * All Flow-returning methods are reactive — the UI automatically updates
 * whenever the underlying data changes. Suspend functions are one-shot
 * operations that run on the calling coroutine.
 *
 * @param database The [AppDatabase] instance used to access all DAOs
 */
class ReceiptRepository(private val database: AppDatabase) {

    private val receiptDao = database.receiptDao()
    private val itemDao = database.itemDao()
    private val emotionDao = database.emotionDao()
    private val itemReactionDao = database.itemReactionDao()
    private val goalDao = database.goalDao()
    private val savingGoalDao = database.savingGoalDao()

    // ── Monthly Budget Goal ───────────────────────────────────────────────

    /**
     * Observes the user's monthly budget goal as a reactive stream.
     *
     * @return A [Flow] emitting the current [GoalEntity], or null if no
     *         budget has been set yet
     */
    fun getGoal(): Flow<GoalEntity?> = goalDao.getGoal()

    /**
     * Inserts or updates the user's monthly budget goal.
     * If a goal already exists it is replaced with the new values.
     *
     * @param goal The [GoalEntity] to save
     */
    suspend fun upsertGoal(goal: GoalEntity) = goalDao.upsertGoal(goal)

    /**
     * Deletes the user's monthly budget goal entirely.
     * Called when the user removes their budget on the Spending Goal screen.
     */
    suspend fun deleteGoal() = goalDao.deleteGoal()

    // ── Receipt Operations ────────────────────────────────────────────────

    /**
     * Saves a fully parsed receipt and all its line items to the database
     * in a single coordinated operation.
     *
     * The impulse score is calculated first using [ImpulseScorer], then the
     * [ReceiptEntity] is inserted and its auto-generated ID is used to link
     * all [ItemEntity] entries to the correct parent receipt. The two inserts
     * are separate operations — Room does not automatically wrap them in a
     * transaction here, so [saveItemReactions] uses [withTransaction] instead
     * when atomicity is required.
     *
     * @param parsedReceipt The [ParsedReceipt] returned by [ClaudeReceiptParser]
     *                      containing store name, date, items and totals
     * @param imageUri The URI of the scanned receipt image, stored so the
     *                 image can be displayed on the Receipt Detail screen.
     *                 Null for manually entered receipts.
     * @return The auto-generated database ID of the newly inserted receipt
     */
    suspend fun saveReceipt(parsedReceipt: ParsedReceipt, imageUri: String?): Long {
        val impulse = ImpulseScorer.score(parsedReceipt)

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

        val receiptId = receiptDao.insertReceipt(receiptEntity)

        val itemEntities = parsedReceipt.items.map { parsedItem ->
            ItemEntity(
                receiptId = receiptId,
                name = parsedItem.name,
                price = parsedItem.price,
                quantity = parsedItem.quantity,
                category = parsedItem.category
            )
        }

        if (itemEntities.isNotEmpty()) {
            itemDao.insertItems(itemEntities)
        }

        return receiptId
    }

    /**
     * Observes all receipts in the database ordered by date, as a reactive stream.
     *
     * @return A [Flow] emitting the full list of [ReceiptEntity] records,
     *         updated automatically whenever receipts are added, edited, or deleted
     */
    fun getAllReceipts(): Flow<List<ReceiptEntity>> = receiptDao.getAllReceipts()

    /**
     * Observes a single receipt by its ID as a reactive stream.
     *
     * Used on the Receipt Detail screen so the UI updates automatically
     * if the receipt is edited elsewhere while the screen is open.
     *
     * @param receiptId The ID of the receipt to observe
     * @return A [Flow] emitting the [ReceiptEntity], or null if it has been deleted
     */
    fun getReceiptByIdFlow(receiptId: Long): Flow<ReceiptEntity?> =
        receiptDao.getReceiptByIdFlow(receiptId)

    /**
     * Updates the regret score and optional reflection note for a receipt.
     *
     * Fetches the current receipt, applies the new values via [copy], and
     * updates the record in the database. Also sets [ReceiptEntity.updatedAt]
     * to the current timestamp.
     *
     * @param receiptId The ID of the receipt to update
     * @param regretScore The new regret score from 1 to 10
     * @param note An optional reflection note added by the user
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
     * Permanently deletes a receipt from the database.
     *
     * All associated items, emotions, and item reactions are automatically
     * removed via the CASCADE foreign key constraints defined on those tables.
     *
     * @param receipt The [ReceiptEntity] to delete
     */
    suspend fun deleteReceipt(receipt: ReceiptEntity) = receiptDao.deleteReceipt(receipt)

    // ── Item Operations ───────────────────────────────────────────────────

    /**
     * Retrieves all items for a specific receipt as a one-shot result.
     *
     * @param receiptId The ID of the receipt whose items should be retrieved
     * @return A list of [ItemEntity] entries for that receipt
     */
    suspend fun getItemsForReceipt(receiptId: Long): List<ItemEntity> =
        itemDao.getItemsForReceipt(receiptId)

    /**
     * Observes all items for a specific receipt as a reactive stream.
     *
     * @param receiptId The ID of the receipt whose items should be observed
     * @return A [Flow] emitting the current list of [ItemEntity] for that receipt
     */
    fun getItemsForReceiptFlow(receiptId: Long): Flow<List<ItemEntity>> =
        itemDao.getItemsForReceiptFlow(receiptId)

    /**
     * Observes all items belonging to a specific spending category.
     *
     * @param category The category name to filter by, for example "tops" or "beverage"
     * @return A [Flow] emitting all [ItemEntity] entries in that category
     */
    fun getItemsByCategory(category: String): Flow<List<ItemEntity>> =
        itemDao.getItemsByCategory(category)

    // ── Statistics ────────────────────────────────────────────────────────

    /**
     * Returns the total number of receipts saved in the database.
     *
     * @return The receipt count as an [Int]
     */
    suspend fun getReceiptCount(): Int = receiptDao.getReceiptCount()

    /**
     * Observes the average regret score across all rated receipts.
     *
     * @return A [Flow] emitting the average regret score as a [Double],
     *         or null if no receipts have been rated yet
     */
    fun getAverageRegretScoreFlow(): Flow<Double?> = receiptDao.getAverageRegretScoreFlow()

    /**
     * Observes all receipts with a regret score at or above the given threshold.
     *
     * Used on the Regret Stats screen to display the user's most regretted purchases.
     *
     * @param minScore The minimum regret score to include, defaults to 7
     * @return A [Flow] emitting the list of high-regret [ReceiptEntity] records
     */
    fun getHighRegretReceipts(minScore: Int = 7): Flow<List<ReceiptEntity>> =
        receiptDao.getHighRegretReceipts(minScore)

    /**
     * Observes the most recently added receipts up to the given limit.
     *
     * Used on the Home screen to display the Recent Purchases list.
     *
     * @param limit The maximum number of receipts to return, defaults to 5
     * @return A [Flow] emitting the most recent [ReceiptEntity] records
     */
    fun getRecentReceipts(limit: Int = 5): Flow<List<ReceiptEntity>> =
        receiptDao.getRecentReceipts(limit)

    /**
     * Observes total spending grouped by item category.
     *
     * Used to populate the category breakdown bar chart on the Stats screen.
     *
     * @return A [Flow] emitting a list of [CategorySpend] entries ordered by
     *         total spend descending
     */
    fun getSpendByCategory(): Flow<List<CategorySpend>> = itemDao.getSpendByCategory()

    /**
     * Observes the number of items purchased in each category.
     *
     * @return A [Flow] emitting a list of [CategoryCount] entries ordered by
     *         item count descending
     */
    fun getItemCountByCategory(): Flow<List<CategoryCount>> = itemDao.getItemCountByCategory()

    /**
     * Observes the user's total spend across all receipts ever logged.
     *
     * @return A [Flow] emitting the total as a [Double], or null if no receipts exist
     */
    fun getTotalSpend(): Flow<Double?> = receiptDao.getTotalSpend()

    /**
     * Observes the receipts with the highest regret scores up to the given limit.
     *
     * Used to populate the all-time top regret list on the Regret Stats screen.
     *
     * @param limit The maximum number of receipts to return, defaults to 3
     * @return A [Flow] emitting the top-regret [ReceiptEntity] records
     */
    fun getTopRegretReceipts(limit: Int = 3): Flow<List<ReceiptEntity>> =
        receiptDao.getTopRegretReceipts(limit)

    /**
     * Observes the user's total spend grouped by week.
     *
     * Used to populate the weekly spend trend line chart on the Spending Stats screen.
     *
     * @return A [Flow] emitting a list of [WeeklySpend] entries
     */
    fun getWeeklySpend(): Flow<List<WeeklySpend>> = receiptDao.getWeeklySpend()

    /**
     * Observes the user's average regret score grouped by week.
     *
     * Used to populate the weekly regret trend line chart on the Regret Stats screen.
     *
     * @return A [Flow] emitting a list of [WeeklyRegret] entries
     */
    fun getWeeklyAverageRegret(): Flow<List<WeeklyRegret>> = receiptDao.getWeeklyAverageRegret()

    /**
     * Updates the stored purchase time for a specific receipt.
     *
     * @param receiptId The ID of the receipt to update
     * @param time The new purchase time string in HH:mm format
     */
    suspend fun updatePurchaseTime(receiptId: Long, time: String) =
        receiptDao.updatePurchaseTime(receiptId, time)

    // ── Emotion Check-ins ─────────────────────────────────────────────────

    /**
     * Saves a new emotion check-in for a receipt and updates the receipt's
     * regret score and note in a single coordinated operation.
     *
     * Creates a new [EmotionEntity] linked to the receipt, then updates
     * [ReceiptEntity.regretScore] and [ReceiptEntity.emotionalNote] so the
     * latest regret rating is always accessible directly on the receipt
     * without needing to join the emotion_checkins table.
     *
     * @param receiptId The ID of the receipt being rated
     * @param regretScore The regret score from 1 to 10
     * @param mood The selected mood string, for example "excited" or "stressed"
     * @param notes An optional personal reflection note
     */
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

    // ── Item Reactions ────────────────────────────────────────────────────

    /**
     * Saves or updates the sentiment reaction for a single item.
     *
     * @param receiptId The ID of the receipt this item belongs to
     * @param itemId The ID of the item being reacted to
     * @param reaction The reaction value: 1 = Happy, 0 = Ok, -1 = Regret
     */
    suspend fun setItemReaction(receiptId: Long, itemId: Long, reaction: Int) {
        itemReactionDao.upsertReaction(
            ItemReactionEntity(itemId = itemId, receiptId = receiptId, reaction = reaction)
        )
    }

    /**
     * Observes all item reactions for a specific receipt as a reactive stream.
     *
     * @param receiptId The ID of the receipt whose item reactions should be observed
     * @return A [Flow] emitting the current list of [ItemReactionEntity] for that receipt
     */
    fun getItemReactionsForReceipt(receiptId: Long): Flow<List<ItemReactionEntity>> =
        itemReactionDao.getReactionsForReceipt(receiptId)

    /**
     * Calculates the user's overall sentiment score and label from a list
     * of item reactions.
     *
     * Averages the raw reaction values (-1, 0, 1), scales the result to a
     * 0–100 percentage, then maps it to a human-readable label:
     * - GOOD — 67% or above (mostly Happy reactions)
     * - MIXED — between 33% and 67%
     * - BAD — 33% or below (mostly Regret reactions)
     *
     * @param reactions The list of [ItemReactionEntity] entries to aggregate
     * @return A [Pair] of the sentiment percentage (0.0–100.0) and label string,
     *         or null to null if the reactions list is empty
     */
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

    /**
     * Saves all pending item reactions for a receipt in a single atomic transaction
     * and updates the parent receipt's overall sentiment score.
     *
     * Runs inside [withTransaction] to guarantee that the upsert, the cleanup
     * of removed items, and the sentiment update either all succeed or all
     * roll back together — preventing the database from being left in a
     * partially updated state.
     *
     * @param receiptId The ID of the receipt whose reactions are being saved
     * @param draft A map of item ID to reaction value (1 = Happy, 0 = Ok, -1 = Regret)
     *              representing the current state of the reaction buttons on screen
     */
    suspend fun saveItemReactions(receiptId: Long, draft: Map<Long, Int>) {
        database.withTransaction {
            val rows = draft.map { (itemId, reaction) ->
                ItemReactionEntity(itemId = itemId, receiptId = receiptId, reaction = reaction)
            }
            itemReactionDao.upsertAll(rows)
            // Remove reactions for any items that were deleted since the last save
            itemReactionDao.deleteNotIn(receiptId, draft.keys.toList())
            val (score, label) = calcUserSentiment(rows)
            receiptDao.updateUserSentiment(receiptId, score, label)
        }
    }

    // ── Receipt Editing ───────────────────────────────────────────────────

    /**
     * Updates the editable header fields of a receipt.
     *
     * Called from the Edit Receipt screen when the user modifies the store
     * name, date, time, or financial totals. Sets [ReceiptEntity.updatedAt]
     * to the current timestamp automatically.
     *
     * @param receiptId The ID of the receipt to update
     * @param storeName The updated store or website name
     * @param purchaseDate The updated purchase date string
     * @param purchaseTime The updated purchase time in HH:mm format
     * @param totalAmount The updated total amount in GBP
     * @param subtotal The updated subtotal before tax
     * @param tax The updated tax amount
     */
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

    /**
     * Updates the name, price, and quantity of an existing item.
     *
     * @param itemId The ID of the item to update
     * @param name The updated item name
     * @param price The updated price per unit in GBP
     * @param quantity The updated quantity purchased
     */
    suspend fun updateItem(itemId: Long, name: String, price: Double, quantity: Int) =
        itemDao.updateItem(itemId, name, price, quantity)

    /**
     * Adds a new item to an existing receipt.
     *
     * The item's category is automatically determined by [categorizeByName]
     * using keyword matching on the item name.
     *
     * @param receiptId The ID of the receipt to add the item to
     * @param name The item name
     * @param price The price per unit in GBP
     * @param quantity The quantity purchased
     */
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

    /**
     * Determines a spending category for an item by matching its name
     * against lists of known keywords.
     *
     * Categories checked in order: dairy, produce, beverage, meat, bakery,
     * tops, bottoms, outerwear, shoes, accessories, bags, toiletries, homeware.
     * Defaults to "other" if no keywords match.
     *
     * @param name The item name to categorise, matched case-insensitively
     * @return The matched category string, or "other" if no match is found
     */
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

    /** Extension helper that returns true if the string contains any of the given keywords. */
    private fun String.containsAny(vararg keywords: String) = keywords.any { this.contains(it) }

    // ── Monthly Analytics ─────────────────────────────────────────────────

    /**
     * Observes all items purchased in a specific month and year.
     *
     * @param year The four-digit year, for example 2026
     * @param month The month number from 1 (January) to 12 (December)
     * @return A [Flow] emitting all [ItemEntity] records for that month
     */
    fun getItemsForMonth(year: Int, month: Int): Flow<List<ItemEntity>> =
        receiptDao.getItemsForMonth(year, month)

    /**
     * Observes the user's total spend for a specific month and year.
     *
     * Used on the Spending Goal screen to show how much of the monthly
     * budget has been used so far.
     *
     * @param year The four-digit year
     * @param month The month number from 1 to 12
     * @return A [Flow] emitting the monthly total as a [Double], or null if
     *         no receipts exist for that month
     */
    fun getMonthlySpend(year: Int, month: Int): Flow<Double?> =
        receiptDao.getMonthlySpend(year, month)

    /**
     * Calculates the total amount spent on HIGH impulse purchases in a
     * specific month.
     *
     * Fetches all receipts, filters to the given month using [isInMonth],
     * then sums the totals of receipts labelled HIGH by the impulse scorer.
     * Used on the Spending Goal screen to show the "High impulse buys" figure
     * in the Mindful Spending card.
     *
     * @param year The four-digit year
     * @param month The month number from 1 to 12
     * @return The total amount spent on HIGH impulse purchases in GBP
     */
    suspend fun calculateMonthlySavings(year: Int, month: Int): Double {
        val allReceipts = receiptDao.getAllReceipts().first()
        val monthReceipts = allReceipts.filter { receipt ->
            receipt.purchaseDate?.let { date -> isInMonth(date, year, month) } ?: false
        }
        return monthReceipts
            .filter { it.impulseLabel == "HIGH" }
            .sumOf { it.totalAmount ?: 0.0 }
    }

    /**
     * Checks whether a date string falls within the given month and year.
     *
     * Supports two date formats:
     * - DD/MM/YYYY — for example "03/06/2026"
     * - D/M/YYYY — for example "3/6/2026"
     *
     * Returns false if the date string does not match either format or
     * if parsing fails for any reason.
     *
     * @param date The date string to check
     * @param year The four-digit year to match
     * @param month The month number from 1 to 12 to match
     * @return True if the date falls within the specified month and year
     */
    private fun isInMonth(date: String, year: Int, month: Int): Boolean {
        return try {
            when {
                date.matches(Regex("""\d{2}/\d{2}/\d{4}""")) -> {
                    val m = date.substring(3, 5).toInt()
                    val y = date.substring(6, 10).toInt()
                    m == month && y == year
                }
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

    // ── Saving Goals ──────────────────────────────────────────────────────

    /**
     * Observes all saving goals ordered by priority.
     *
     * @return A [Flow] emitting the current list of [SavingGoalEntity] records
     */
    fun getSavingGoals(): Flow<List<SavingGoalEntity>> = savingGoalDao.getAllGoals()

    /**
     * Adds a new saving goal with the next available priority.
     *
     * The priority is set to the current goal count plus one, so new goals
     * are always appended to the bottom of the priority list.
     *
     * @param name The name of the saving goal, for example "Holiday" or "Bike"
     * @param targetAmount The target savings amount in GBP
     */
    suspend fun addSavingGoal(name: String, targetAmount: Double) {
        val nextPriority = savingGoalDao.getGoalCount() + 1
        savingGoalDao.insertGoal(
            SavingGoalEntity(name = name, targetAmount = targetAmount, priority = nextPriority)
        )
    }

    /**
     * Updates an existing saving goal's name or target amount.
     *
     * @param goal The [SavingGoalEntity] with updated values to save
     */
    suspend fun updateSavingGoal(goal: SavingGoalEntity) = savingGoalDao.updateGoal(goal)

    /**
     * Permanently deletes a saving goal from the database.
     *
     * @param goal The [SavingGoalEntity] to delete
     */
    suspend fun deleteSavingGoal(goal: SavingGoalEntity) = savingGoalDao.deleteGoal(goal)

    /**
     * Swaps the priority values of two saving goals so one moves up or down
     * in the ordered list.
     *
     * Called by the ViewModel when the user taps the ↑ or ↓ priority buttons
     * on the Spending Goal screen. The two updates are performed sequentially
     * rather than in a transaction — the priority values are simple integers
     * and a partial swap is recoverable by tapping again.
     *
     * @param a The first [SavingGoalEntity] to swap
     * @param b The second [SavingGoalEntity] to swap, adjacent to [a] in the list
     */
    suspend fun swapSavingGoalPriorities(a: SavingGoalEntity, b: SavingGoalEntity) {
        savingGoalDao.updateGoal(a.copy(priority = b.priority))
        savingGoalDao.updateGoal(b.copy(priority = a.priority))
    }
}