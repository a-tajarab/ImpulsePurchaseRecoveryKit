package com.example.impulsepurchaserecoverykit.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * GoalEntity stores the user's monthly spending goal.
 * Resets automatically on the 1st of each month.
 * Tracks both the budget limit and how much has been saved
 * by avoiding impulse purchases.
 */
@Entity(tableName = "spending_goals")
data class GoalEntity(
    @PrimaryKey
    val id: Int = 1, // Single row — always update not insert

    /** Monthly spending limit set by the user in £ */
    val monthlyLimit: Double,

    /** The month this goal applies to (1-12) */
    val goalMonth: Int,

    /** The year this goal applies to */
    val goalYear: Int,

    /** Optional name of what the user is saving towards */
    val savingFor: String? = null,

    /** Target savings amount for the thing they are saving for */
    val savingTarget: Double? = null,

    /** Timestamp when goal was created */
    val createdAt: Long = System.currentTimeMillis()
)