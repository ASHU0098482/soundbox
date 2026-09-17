package com.ashupaybox.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ashupaybox.data.local.dao.ProcessedEventDao
import com.ashupaybox.data.local.dao.TransactionDao
import com.ashupaybox.data.local.entity.ProcessedEventEntity
import com.ashupaybox.data.local.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        ProcessedEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PayBoxDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun processedEventDao(): ProcessedEventDao

    companion object {
        @Volatile
        private var INSTANCE: PayBoxDatabase? = null

        fun getInstance(context: Context): PayBoxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PayBoxDatabase::class.java,
                    "ashu_paybox.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
