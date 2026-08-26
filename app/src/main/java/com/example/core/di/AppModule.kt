package com.example.core.di

import android.content.Context
import com.example.core.database.DatabaseModule
import com.example.core.location.DefaultLocationClient
import com.example.core.location.LocationClient
import com.example.core.location.LocationManager
import com.example.core.location.LocationPermissionManager
import com.example.core.network.NetworkModule
import com.example.core.security.SecureStorage
import com.example.data.repository.AuthRepositoryImpl
import com.example.data.repository.CustomizationRepositoryImpl
import com.example.data.repository.LocationRepositoryImpl
import com.example.domain.model.HealthState
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.CustomizationRepository
import com.example.domain.repository.HealthRepository
import com.example.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

class AppModule(private val context: Context) {

    init {
        NetworkModule.authTokenProvider = { secureStorage.accessToken }
    }

    val secureStorage: SecureStorage by lazy {
        SecureStorage(context)
    }

    val databaseModule: DatabaseModule by lazy {
        DatabaseModule(context)
    }

    val apiService by lazy {
        NetworkModule.apiService
    }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(
            apiService = apiService,
            secureStorage = secureStorage,
            moshi = NetworkModule.moshi
        )
    }

    val customizationRepository: CustomizationRepository by lazy {
        CustomizationRepositoryImpl(
            apiService = apiService,
            secureStorage = secureStorage,
            moshi = NetworkModule.moshi
        )
    }

    val locationClient: LocationClient by lazy {
        DefaultLocationClient(context)
    }

    val locationPermissionManager: LocationPermissionManager by lazy {
        LocationPermissionManager(context)
    }

    val locationRepository: LocationRepository by lazy {
        LocationRepositoryImpl(
            locationPointDao = databaseModule.locationPointDao,
            runSessionDao = databaseModule.runSessionDao,
            appDatabase = databaseModule.database
        )
    }

    val networkMonitor: com.example.core.network.NetworkMonitor by lazy {
        com.example.core.network.LiveNetworkMonitor(context)
    }

    val locationManager: LocationManager by lazy {
        LocationManager(
            context = context,
            locationClient = locationClient,
            permissionManager = locationPermissionManager,
            locationRepository = locationRepository,
            networkMonitor = networkMonitor
        )
    }

    val territoryExpansionEngine: com.example.core.territory.TerritoryExpansionEngine by lazy {
        com.example.core.territory.TerritoryExpansionEngine()
    }

    val supabaseSyncService: com.example.core.supabase.SupabaseSyncService by lazy {
        com.example.core.supabase.SupabaseSyncService(databaseModule.territoryDao)
    }

    val territoryRepository: com.example.domain.repository.TerritoryRepository by lazy {
        com.example.data.repository.TerritoryRepositoryImpl(
            territoryDao = databaseModule.territoryDao,
            authRepository = authRepository,
            expansionEngine = territoryExpansionEngine,
            supabaseSyncService = supabaseSyncService
        )
    }

    val notificationRepository: com.example.domain.repository.NotificationRepository by lazy {
        com.example.data.repository.NotificationRepositoryImpl()
    }

    val realtimeClient: com.example.data.realtime.RealtimeClient by lazy {
        com.example.data.realtime.RealtimeClient()
    }

    val competitiveRepository: com.example.domain.repository.CompetitiveRepository by lazy {
        com.example.data.repository.CompetitiveRepositoryImpl(
            authRepository = authRepository,
            supabaseSyncService = supabaseSyncService
        )
    }

    val battleRepository: com.example.domain.repository.BattleRepository by lazy {
        com.example.data.repository.BattleRepositoryImpl(
            battleDao = databaseModule.battleDao,
            territoryDao = databaseModule.territoryDao,
            database = databaseModule.database,
            authRepository = authRepository,
            competitiveRepository = competitiveRepository,
            notificationRepository = notificationRepository
        )
    }

    val healthRepository: HealthRepository by lazy {
        object : HealthRepository {
            private val _healthState = MutableStateFlow<HealthState>(HealthState.Idle)
            override val healthState: Flow<HealthState> = _healthState.asStateFlow()

            override suspend fun checkHealth(): HealthState {
                _healthState.value = HealthState.Loading
                return try {
                    val response = apiService.checkHealth()
                    if (response.isSuccessful && response.body()?.data != null) {
                        val state = HealthState.Success(response.body()!!.data.status)
                        _healthState.value = state
                        state
                    } else {
                        val state = HealthState.Error("Server returned code ${response.code()}")
                        _healthState.value = state
                        state
                    }
                } catch (e: IOException) {
                    val state = HealthState.Error("Offline / Host unreachable: ${e.message}")
                    _healthState.value = state
                    state
                } catch (e: Exception) {
                    val state = HealthState.Error("Error: ${e.message}")
                    _healthState.value = state
                    state
                }
            }
        }
    }
}
