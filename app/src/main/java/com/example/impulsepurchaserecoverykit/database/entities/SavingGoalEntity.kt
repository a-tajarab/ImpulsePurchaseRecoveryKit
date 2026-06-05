package com.example.impulsepurchaserecoverykit.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * SavingGoalEntity represents a single item the user is saving towards.
 * Users can have as many saving goals as they wish.
 * [priority] is 1-indexed — 1 = highest priority. Goals are displayed
 * and estimated in ascending priority order.
 */
@Entity(tableName = "saving_goals")
data class SavingGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Display name of the goal — e.g. "New trainers", "Holiday", "PS5" */
    val name: String,

    /** Target amount the user wants to save in £ */
    val targetAmount: Double,

    /**
     * Priority order: 1 = most important.
     * When two goals are swapped, their priority values are exchanged.
     * Gaps in sequence are fine — display is always ORDER BY priority ASC.
     */
    val priority: Int,

    val createdAt: Long = System.currentTimeMillis()
)