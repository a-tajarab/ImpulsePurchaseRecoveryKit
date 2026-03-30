package com.example.impulsepurchaserecoverykit.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.impulsepurchaserecoverykit.database.dao.EmotionDao
import com.example.impulsepurchaserecoverykit.database.dao.ItemDao
import com.example.impulsepurchaserecoverykit.database.dao.ItemReactionDao
import com.example.impulsepurchaserecoverykit.database.dao.ReceiptDao
import com.example.impulsepurchaserecoverykit.database.entities.EmotionEntity
import com.example.impulsepurchaserecoverykit.database.entities.ItemEntity
import com.example.impulsepurchaserecoverykit.database.entities.ReceiptEntity
import com.example.impulsepurchaserecoverykit.database.entities.ItemReactionEntity

@Database(
    entities = [
        ReceiptEntity::class,
        ItemEntity::class,
        EmotionEntity::class,
        ItemReactionEntity::class
    ],
    version =  5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun receiptDao(): ReceiptDao
    abstract fun itemDao(): ItemDao
    abstract fun emotionDao(): EmotionDao
    abstract fun itemReactionDao(): ItemReactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "impulse_recovery_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}