package com.example.impulsepurchaserecoverykit.database.dao

import androidx.room.*
import com.example.impulsepurchaserecoverykit.database.entities.ReceiptEntity
import com.example.impulsepurchaserecoverykit.database.entities.ItemEntity
import com.example.impulsepurchaserecoverykit.database.models.WeeklySpend
import com.example.impulsepurchaserecoverykit.database.models.WeeklyRegret
import kotlinx.coroutines.flow.Flow

/**
 * ReceiptDao is the Data Access Object for the receipts table
 */
@Dao
interface ReceiptDao {

    /**
     * Inserts a new receipt into the receipts table
     * Returns the auto-generated row ID of the newly inserted receipt which
     * can be used to navigate to the receipt detail screen
     */
    @Insert
    suspend fun insertReceipt(receipt: ReceiptEntity): Long

    /**
     * Updates an existing receipt record in the receipts table
     * Matches the record by the receipts key ID field, it is used by the inline
     * edit mode on the Receipt Detail Screen
     */
    @Update
    suspend fun updateReceipt(receipt: ReceiptEntity)

    /**
     * Deletes a receipt record from the receipts table
     * Triggers deletion of associated items, emotion check-ins, and item reactions
     * through the foreign key
     */
    @Delete
    suspend fun deleteReceipt(receipt: ReceiptEntity)

    /**
     * Returns all receipts from the database that is ordered by the most recently
     * created. Throws out a list whenever the receipt table changes
     */
    @Query("SELECT * FROM receipts ORDER BY createdAt DESC")
    fun getAllReceipts(): Flow<List<ReceiptEntity>>

    /**
     * Returns a single receipt by its unique ID as one-shot function
     * It is used when navigating to a receipt detail screen by ID
     */
    @Query("SELECT * FROM receipts WHERE id = :receiptId")
    suspend fun getReceiptById(receiptId: Long): ReceiptEntity?

    /**
     * Returns all receipts whose purchase date falls in a specified date range
     * It is ordered by the most recently created in the range
     */
    @Query("SELECT * FROM receipts WHERE purchaseDate BETWEEN :startDate AND :endDate ORDER BY createdAt DESC")
    fun getReceiptsInDateRange(startDate: String, endDate: String): Flow<List<ReceiptEntity>>

    /**
     * Returns all receipt where the user's regret score meets or exceed a minimum
     * threshold, ordered by highest regret score first
     * It powers the emotional response engine which triggers a supportive intervention
     * when a high regret score is recorded
     */
    @Query("SELECT * FROM receipts WHERE regretScore >= :minScore ORDER BY regretScore DESC")
    fun getHighRegretReceipts(minScore: Int): Flow<List<ReceiptEntity>>

    /**
     * Returns the total number of receipts which is stored in the database
     * It is used to populate the Receipts stat card on the Home screen
     */
    @Query("SELECT COUNT(*) FROM receipts")
    suspend fun getReceiptCount(): Int

    /**
     * Returns the most recently created receipts to a specified limit
     * It is used to display the recent purchase section on the Home screen
     */
    @Query("SELECT * FROM receipts ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentReceipts(limit: Int): Flow<List<ReceiptEntity>>

    /**
     * Returns a single receipt by ID as a reactive Flow
     * This function emits a new value whenever the receipt is updated, which
     * allows the Receipt Detail Screen to reflect edits in real time without
     * the page needing to reload
     */
    @Query("SELECT * FROM receipts WHERE id = :receiptId LIMIT 1")
    fun getReceiptByIdFlow(receiptId: Long): Flow<ReceiptEntity?>

    /**
     * Returns the sum of all receipt total amounts across the entire database
     * It powers the total spend status card on the Home screen
     * Returns null if no receipts have been recorded
     */
    @Query("SELECT SUM(totalAmount) FROM receipts")
    fun getTotalSpend(): Flow<Double?>

    /**
     * Returns the receipts with the highest regret scores across all time
     * Only includes receipts where the user has recorded a regret score
     * The regret scores is ordered so that the most recently is created
     * Powers the all-time top regret section of the statistic screen
     */
    @Query("""
    SELECT * FROM receipts
    WHERE regretScore IS NOT NULL
    ORDER BY regretScore DESC, createdAt DESC
    LIMIT :limit
""")
    fun getTopRegretReceipts(limit: Int): Flow<List<ReceiptEntity>>

    /**
     * Returns weekly spending totals grouped by the week
     * This is one of the most complex queries in the application, it handles
     * multiple date formats, it extracts the year and month using the SUBSTR
     * and handles the space OCR error
     *
     * The results are grouped by a weekStart value and ordered in chronological order
     * to power the Weekly spend line chart on the statistic screen
     */
    @Query("""
    SELECT 
    COALESCE(
        CASE
            WHEN purchaseDate LIKE '__/__/____' 
                THEN CAST(SUBSTR(purchaseDate,7,4) || SUBSTR(purchaseDate,4,2) AS INTEGER)
                WHEN purchaseDate LIKE '__/__ /____'
                THEN CAST(SUBSTR(purchaseDate,8,4) || SUBSTR(purchaseDate,4,2) AS INTEGER)
                ELSE createdAt / 2592000000
            END,
            createdAt / 2592000000
        ) AS weekStart,
        SUM(COALESCE(totalAmount, 0)) AS total
    FROM receipts
    GROUP BY weekStart
    ORDER BY weekStart ASC
""")
    fun getWeeklySpend(): Flow<List<WeeklySpend>>

    /**
     * Returns the average regret score grouped by the week
     * It only includes receipts where the user has recorded regret score
     * Groups the results dividing the createdAr timestamp by the number of
     * milliseconds in week 604800000 to produce the week identifier
     *
     * This function powers the Weekly Regret Trend Line chart on the
     * statistics screen
     */
    @Query("""
    SELECT (createdAt / 604800000) * 604800000 AS weekStart,
           AVG(regretScore) AS avgRegret
    FROM receipts
    WHERE regretScore IS NOT NULL
    GROUP BY weekStart
    ORDER BY weekStart ASC
""")
    fun getWeeklyAverageRegret(): Flow<List<WeeklyRegret>>

    /**
     * Updates the user sentiment score and label for a receipt
     * It is called after the user has rated individual items in a receipt
     * and the overall sentiment has been calculated from those item reactions
     */
    @Query("""
        UPDATE receipts
        SET userSentimentScore = :score,
            userSentimentLabel = :label
        WHERE id = :receiptId
    """)
    suspend fun updateUserSentiment(receiptId: Long, score: Double?, label: String?)

    /**
     * Returns the average regret score across all rated receipts as reactive flow
     *It only includes receipts where a regret score has been recorded by the user
     * It powers the Average Regret stat card on the Home Screen and statistics
     */
    @Query("SELECT AVG(regretScore) FROM receipts WHERE regretScore IS NOT NULL")
    fun getAverageRegretScoreFlow(): Flow<Double?>

    /**
     * Updates the purchase time for a specified receipt
     * Called when the user manually corrects the purchase time through the inline
     * edit mode on the Detail screen
     */
    @Query("UPDATE receipts SET purchaseTime = :time WHERE id = :receiptId")
    suspend fun updatePurchaseTime(receiptId: Long, time: String)

    /**
     * Updates the main editable fields of a receipt record
     * Its called when the user saves changes made through the inline edit mode
     * on the receipt detail screen and updates the updateAt timestamp to record when
     * the edit was made
     */
    @Query("""
    UPDATE receipts 
    SET storeName = :storeName,
        purchaseDate = :purchaseDate,
        purchaseTime = :purchaseTime,
        totalAmount = :totalAmount,
        subtotal = :subtotal, 
        tax = :tax, 
        updatedAt = :updatedAt
    WHERE id = :receiptId
""")
    suspend fun updateReceiptDetails(
        receiptId: Long,
        storeName: String?,
        purchaseDate: String?,
        purchaseTime: String?,
        totalAmount: Double?,
        subtotal: Double?,
        tax: Double?,
        updatedAt: Long = System.currentTimeMillis()
    )

    /**
     * Returns all items belonging to the receipts purchsed in a specific month and year
     * It uses SQLite SUBSTR functions to parse the date strings that is stored in multiple
     * formats that is returned by the OCR scanner. It joins the items table with the
     * receipts table to filter by the purchase date.
     *
     * The function powers the category visualisation on the statistic screen
     */

    @Query("""
    SELECT i.* FROM items i
    INNER JOIN receipts r ON i.receiptId = r.id
    WHERE 
        (r.purchaseDate LIKE '__/__/____' 
         AND CAST(SUBSTR(r.purchaseDate,4,2) AS INTEGER) = :month
         AND CAST(SUBSTR(r.purchaseDate,7,4) AS INTEGER) = :year)
    OR
        (r.purchaseDate LIKE '__ ___ ____'
         AND CAST(SUBSTR(r.purchaseDate,8,4) AS INTEGER) = :year)
""")
    fun getItemsForMonth(year: Int, month: Int): Flow<List<ItemEntity>>

    /**
     * Returns the total amount spent across all receipts in a specific month
     * and year. Uses SQLite SUBSTR functions to parse date strings in multiple OCR formats.
     * COALESCE makes sure that the null total amount value is treated as 0
     * rather than causing the sum to return null
     *
     * This function powers the 'this month' stat card on the Home screen.
     */
    @Query("""
    SELECT SUM(COALESCE(totalAmount, 0))
    FROM receipts
    WHERE 
        (purchaseDate LIKE '__/__/____'
         AND CAST(SUBSTR(purchaseDate, 4, 2) AS INTEGER) = :month
         AND CAST(SUBSTR(purchaseDate, 7, 4) AS INTEGER) = :year)
    OR
        (purchaseDate LIKE '__ ___ ____'
         AND CAST(SUBSTR(purchaseDate, 8, 4) AS INTEGER) = :year)
""")
    fun getMonthlySpend(year: Int, month: Int): Flow<Double?>

}