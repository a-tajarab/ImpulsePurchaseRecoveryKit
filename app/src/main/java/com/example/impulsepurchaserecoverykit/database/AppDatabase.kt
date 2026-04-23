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

/**
 * AppDatabase is the main Room database class for the IPRK
 *
 * This class serves as single entry point for all database in the
 * application. It registers all the entity classes that correspond to
 * the database tables, and defines the database version and provides
 * access to all the Data Access Objects through the abstract functions
 */
@Database(
    entities = [
        ReceiptEntity::class, //primary receipt table
        ItemEntity::class,  //individual items extracted from each receipt
        EmotionEntity::class,  //Emotional check in records linked to receipts
        ItemReactionEntity::class  // Item level positive, neutral, negative reactions
    ],
    version =  7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Provide access to all receipt database operations
     * It is used throughout the application for inserting, updating, deleting
     * and querying receipt records including spending totals and regret scores
     */
    abstract fun receiptDao(): ReceiptDao

    /**
     * Provides access to all item-related database operations
     * It is used to insert, retrieve and delete individual items that is associated
     * with a scanned receipt
     */
    abstract fun itemDao(): ItemDao

    /**
     * Provides access to all emotion check in database operations
     * It is used to store and retrieve emotional states that is recorded
     * by the user at the point of logging a purchase
     */
    abstract fun emotionDao(): EmotionDao

    /**
     * Provide access to all item reaction database operations
     * It is used to store and retrieve the positive, neutral or negative
     * reactions the user assigns to individual items in a receipt
     * These reactions are used to calculate the overall user sentiment score
     */
    abstract fun itemReactionDao(): ItemReactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the singleton instance of AppDatabase, making it if
         * it does not already exist
         *
         * If two threads attempt to access the database at the same time
         * before it has been created, only one instance will be built and
         * shared between them
         *
         *
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext, // used to prevent memory leaks from Activity contexts
                    AppDatabase::class.java,
                    "impulse_recovery_database" //name of database file
                )
                    .fallbackToDestructiveMigration() //rebuilds the database cleanly when the schema version is incremented
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}