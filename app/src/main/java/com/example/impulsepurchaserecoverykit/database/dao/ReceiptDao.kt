package com.example.impulsepurchaserecoverykit.database.dao

import androidx.room.*
import com.example.impulsepurchaserecoverykit.database.entities.ReceiptEntity
import com.example.impulsepurchaserecoverykit.database.models.WeeklySpend
import com.example.impulsepurchaserecoverykit.database.models.WeeklyRegret
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {

    @Insert
    suspend fun insertReceipt(receipt: ReceiptEntity): Long

    @Update
    suspend fun updateReceipt(receipt: ReceiptEntity)

    @Delete
    suspend fun deleteReceipt(receipt: ReceiptEntity)

    @Query("SELECT * FROM receipts ORDER BY createdAt DESC")
    fun getAllReceipts(): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE id = :receiptId")
    suspend fun getReceiptById(receiptId: Long): ReceiptEntity?

    @Query("SELECT * FROM receipts WHERE purchaseDate BETWEEN :startDate AND :endDate ORDER BY createdAt DESC")
    fun getReceiptsInDateRange(startDate: String, endDate: String): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE regretScore >= :minScore ORDER BY regretScore DESC")
    fun getHighRegretReceipts(minScore: Int): Flow<List<ReceiptEntity>>

    @Query("SELECT COUNT(*) FROM receipts")
    suspend fun getReceiptCount(): Int

    @Query("SELECT * FROM receipts ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentReceipts(limit: Int): Flow<List<ReceiptEntity>>

    //@Query("SELECT * FROM receipts WHERE id = :receiptId LIMIT 1")
    //fun getReceiptByIdFlow(receiptId: Long): Flow<ReceiptEntity?>

    @Query("SELECT SUM(totalAmount) FROM receipts")
    fun getTotalSpend(): Flow<Double?>

    @Query("""
    SELECT * FROM receipts
    WHERE regretScore IS NOT NULL
    ORDER BY regretScore DESC, createdAt DESC
    LIMIT :limit
""")
    fun getTopRegretReceipts(limit: Int): Flow<List<ReceiptEntity>>

    @Query("""
    SELECT (createdAt / 604800000) * 604800000 AS weekStart,
           SUM(COALESCE(totalAmount, 0)) AS total
    FROM receipts
    GROUP BY weekStart
    ORDER BY weekStart ASC
""")
    fun getWeeklySpend(): Flow<List<WeeklySpend>>

    @Query("""
    SELECT (createdAt / 604800000) * 604800000 AS weekStart,
           AVG(regretScore) AS avgRegret
    FROM receipts
    WHERE regretScore IS NOT NULL
    GROUP BY weekStart
    ORDER BY weekStart ASC
""")
    fun getWeeklyAverageRegret(): Flow<List<WeeklyRegret>>

    @Query("""
        UPDATE receipts
        SET userSentimentScore = :score,
            userSentimentLabel = :label
        WHERE id = :receiptId
    """)
    suspend fun updateUserSentiment(receiptId: Long, score: Double?, label: String?)

}