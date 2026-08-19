package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.core.database.dao.LocationPointDao
import com.example.core.database.dao.RunSessionDao
import com.example.core.database.dao.SyncQueueDao
import com.example.core.database.dao.TerritoryDao
import com.example.core.database.dao.BattleDao
import com.example.core.database.entity.BattleEntity
import com.example.core.database.entity.LocationPointEntity
import com.example.core.database.entity.RunSessionEntity
import com.example.core.database.entity.SyncQueueEntity
import com.example.core.database.entity.TerritoryEntity

@Database(
    entities = [
        LocationPointEntity::class,
        TerritoryEntity::class,
        RunSessionEntity::class,
        SyncQueueEntity::class,
        BattleEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationPointDao(): LocationPointDao
    abstract fun territoryDao(): TerritoryDao
    abstract fun runSessionDao(): RunSessionDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun battleDao(): BattleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "run2capture_local.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
