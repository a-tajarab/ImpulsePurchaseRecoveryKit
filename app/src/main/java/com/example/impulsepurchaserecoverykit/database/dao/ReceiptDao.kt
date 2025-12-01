package com.example.impulsepurchaserecoverykit.database.dao

import androidx.room.*
import com.example.impulsepurchaserecoverykit.database.entities.ReceiptEntity
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
}