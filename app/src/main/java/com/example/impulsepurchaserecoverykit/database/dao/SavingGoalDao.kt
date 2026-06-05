package com.example.impulsepurchaserecoverykit.database.dao


import androidx.room.*
import com.example.impulsepurchaserecoverykit.database.entities.SavingGoalEntity
import kotlinx.coroutines.flow.Flow

/**
 * SavingGoalDao handles all CRUD and priority-reordering operations
 * for the user's individual saving goals.
 */
@Dao
interface SavingGoalDao {

    /** All goals ordered by priority ascending (1 = first shown). */
    @Query("SELECT * FROM saving_goals ORDER BY priority ASC")
    fun getAllGoals(): Flow<List<SavingGoalEntity>>

    /** Insert a new goal and return its generated id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingGoalEntity): Long

    /** Update an existing goal (name, targetAmount, or priority). */
    @Update
    suspend fun updateGoal(goal: SavingGoalEntity)

    /** Delete a specific goal. */
    @Delete
    suspend fun deleteGoal(goal: SavingGoalEntity)

    /**
     * How many goals currently exist — used to assign the next
     * priority value when inserting a new goal at the bottom.
     */
    @Query("SELECT COUNT(*) FROM saving_goals")
    suspend fun getGoalCount(): Int
}