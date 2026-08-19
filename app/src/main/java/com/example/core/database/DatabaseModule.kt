package com.example.core.database

import android.content.Context
import com.example.core.database.dao.LocationPointDao
import com.example.core.database.dao.RunSessionDao
import com.example.core.database.dao.SyncQueueDao
import com.example.core.database.dao.TerritoryDao

class DatabaseModule(private val context: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    val locationPointDao: LocationPointDao by lazy {
        database.locationPointDao()
    }

    val territoryDao: TerritoryDao by lazy {
        database.territoryDao()
    }

    val battleDao: com.example.core.database.dao.BattleDao by lazy {
        database.battleDao()
    }

    val runSessionDao: RunSessionDao by lazy {
        database.runSessionDao()
    }

    val syncQueueDao: SyncQueueDao by lazy {
        database.syncQueueDao()
    }
}
