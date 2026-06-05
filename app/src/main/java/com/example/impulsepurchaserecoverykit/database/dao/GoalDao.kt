package com.example.impulsepurchaserecoverykit.database.dao

import androidx.room.*
import com.example.impulsepurchaserecoverykit.database.entities.GoalEntity
import kotlinx.coroutines.flow.Flow

/**
 * GoalDao handles all database operations for the spending goal.
 */
@Dao
interface GoalDao {

    /** Returns the current goal as a reactive Flow */
    @Query("SELECT * FROM spending_goals WHERE id = 1 LIMIT 1")
    fun getGoal(): Flow<GoalEntity?>

    /** Returns the current goal as a one-shot suspend */
    @Query("SELECT * FROM spending_goals WHERE id = 1 LIMIT 1")
    suspend fun getGoalOnce(): GoalEntity?

    /** Inserts or replaces the goal — always single row */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(goal: GoalEntity)

    /** Deletes the goal */
    @Query("DELETE FROM spending_goals WHERE id = 1")
    suspend fun deleteGoal()
}