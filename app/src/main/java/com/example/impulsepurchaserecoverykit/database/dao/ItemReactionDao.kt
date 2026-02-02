package com.example.impulsepurchaserecoverykit.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.impulsepurchaserecoverykit.database.entities.ItemReactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemReactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReaction(reactions: ItemReactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReactions(reactions: List<ItemReactionEntity>)

    @Query("SELECT * FROM item_reactions WHERE receiptId = :receiptId")
    fun getReactionsForReceipt(receiptId: Long): Flow<List<ItemReactionEntity>>

    @Query("SELECT * FROM item_reactions WHERE receiptId = :receiptId")
    suspend fun getReactionsForReceiptOnce(receiptId: Long): List<ItemReactionEntity>

    @Query("SELECT * FROM item_reactions WHERE itemId = :itemId LIMIT 1 ")
    suspend fun getReactionForItem(itemId: Long): ItemReactionEntity?

}