package com.example.impulsepurchaserecoverykit.database.dao

import androidx.room.*
import com.example.impulsepurchaserecoverykit.database.entities.EmotionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for emotion check-in operations.
 *
 * EmotionDao handles all database interactions related to the user's emotional
 * responses to their purchases. Every time a user rates their regret or logs
 * how they were feeling during a shopping trip, those entries are stored and
 * retrieved through this DAO.
 *
 * All Flow-returning queries are observed reactively — the UI automatically
 * updates whenever the underlying emotion data changes in the database.
 */
@Dao
interface EmotionDao {

    /**
     * Inserts a new emotion check-in entry into the database.
     *
     * Called after the user completes the Rate Regret screen, saving their
     * regret score, mood selection, and any personal reflection note they
     * have added for a given receipt.
     *
     * @param emotion The [EmotionEntity] to insert, linked to a specific receipt
     * @return The auto-generated row ID of the newly inserted entry
     */
    @Insert
    suspend fun insertEmotion(emotion: EmotionEntity): Long

    /**
     * Retrieves all emotion check-ins associated with a specific receipt,
     * ordered from most recent to oldest.
     *
     * Used on the Receipt Detail screen to display the user's emotional
     * history for a particular purchase. Returns a [Flow] so the UI
     * updates automatically if the user re-rates the receipt.
     *
     * @param receiptId The ID of the receipt whose emotions should be retrieved
     * @return A [Flow] emitting a list of [EmotionEntity] entries for that receipt
     */
    @Query("SELECT * FROM emotion_checkins WHERE receiptId = :receiptId ORDER BY checkInTime DESC")
    fun getEmotionsForReceipt(receiptId: Long): Flow<List<EmotionEntity>>

    /**
     * Calculates the average regret score across all emotion check-ins
     * ever recorded by the user.
     *
     * Used on the Home screen and Stats screen to display the user's
     * overall average regret score. Returns null if no receipts have
     * been rated yet.
     *
     * @return The average regret score as a [Double], or null if no ratings exist
     */
    @Query("SELECT AVG(regretScore) FROM emotion_checkins")
    suspend fun getAverageRegretScore(): Double?

    /**
     * Retrieves the 10 most recent emotion check-ins across all receipts,
     * ordered from most recent to oldest.
     *
     * Used on the Stats screen to display the user's latest emotional
     * responses and identify short-term trends in their spending behaviour.
     * Returns a [Flow] so the list updates automatically as new entries are added.
     *
     * @return A [Flow] emitting the 10 most recent [EmotionEntity] entries
     */
    @Query("SELECT * FROM emotion_checkins ORDER BY checkInTime DESC LIMIT 10")
    fun getRecentEmotions(): Flow<List<EmotionEntity>>
}