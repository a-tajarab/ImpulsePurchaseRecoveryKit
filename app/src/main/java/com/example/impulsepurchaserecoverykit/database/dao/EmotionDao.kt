package com.example.impulsepurchaserecoverykit.database.dao

import androidx.room.*
import com.example.impulsepurchaserecoverykit.database.entities.EmotionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmotionDao {

    @Insert
    suspend fun insertEmotion(emotion: EmotionEntity): Long

    @Query("SELECT * FROM emotion_checkins WHERE receiptId = :receiptId ORDER BY checkInTime DESC")
    fun getEmotionsForReceipt(receiptId: Long): Flow<List<EmotionEntity>>

    @Query("SELECT AVG(regretScore) FROM emotion_checkins")
    suspend fun getAverageRegretScore(): Double?

    @Query("SELECT * FROM emotion_checkins ORDER BY checkInTime DESC LIMIT 10")
    fun getRecentEmotions(): Flow<List<EmotionEntity>>
}