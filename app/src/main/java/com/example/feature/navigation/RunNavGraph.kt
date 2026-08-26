package com.example.feature.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.core.di.AppModule
import com.example.feature.auth.AuthViewModel
import com.example.feature.auth.LoginScreen
import com.example.feature.auth.RegisterScreen
import com.example.feature.customization.FlagCreatorScreen
import com.example.feature.customization.FlagCreatorViewModel
import com.example.feature.identity.PlayerIdentityScreen
import com.example.feature.map.WorldMapScreen
import com.example.feature.map.WorldMapViewModel
import com.example.feature.onboarding.OnboardingScreen
import com.example.feature.splash.SplashScreen

object RunDestinations {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAP = "map"
    const val IDENTITY = "identity"
    const val CUSTOMIZATION = "customization"
    const val COMPETITIVE = "competitive"
    const val NOTIFICATIONS = "notifications"
}

@Composable
fun RunNavGraph(
    appModule: AppModule,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.Factory(appModule.authRepository)
    )

    NavHost(
        navController = navController,
        startDestination = RunDestinations.SPLASH,
        modifier = modifier
    ) {
        composable(RunDestinations.SPLASH) {
            SplashScreen(
                authRepository = appModule.authRepository,
                onNavigateToOnboarding = {
                    navController.navigate(RunDestinations.ONBOARDING) {
                        popUpTo(RunDestinations.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToDashboard = {
                    navController.navigate(RunDestinations.MAP) {
                        popUpTo(RunDestinations.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(RunDestinations.ONBOARDING) {
            OnboardingScreen(
                onNavigateToLogin = {
                    navController.navigate(RunDestinations.LOGIN) {
                        popUpTo(RunDestinations.ONBOARDING) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(RunDestinations.REGISTER) {
                        popUpTo(RunDestinations.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(RunDestinations.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate(RunDestinations.REGISTER)
                },
                onNavigateToDashboard = {
                    navController.navigate(RunDestinations.MAP) {
                        popUpTo(RunDestinations.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(RunDestinations.REGISTER) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = {
                    navController.navigate(RunDestinations.LOGIN)
                },
                onNavigateToDashboard = {
                    navController.navigate(RunDestinations.MAP) {
                        popUpTo(RunDestinations.REGISTER) { inclusive = true }
                    }
                }
            )
        }

        composable(RunDestinations.MAP) {
            val mapViewModel: WorldMapViewModel = viewModel(
                factory = WorldMapViewModel.Factory(
                    locationClient = appModule.locationClient,
                    authRepository = appModule.authRepository,
                    customizationRepository = appModule.customizationRepository,
                    competitiveRepository = appModule.competitiveRepository,
                    notificationRepository = appModule.notificationRepository,
                    locationManager = appModule.locationManager,
                    permissionManager = appModule.locationPermissionManager,
                    territoryRepository = appModule.territoryRepository,
                    battleRepository = appModule.battleRepository
                )
            )

            WorldMapScreen(
                viewModel = mapViewModel,
                onNavigateToCustomization = {
                    navController.navigate(RunDestinations.CUSTOMIZATION)
                },
                onNavigateToIdentity = {
                    navController.navigate(RunDestinations.IDENTITY)
                },
                onNavigateToCompetitive = {
                    navController.navigate(RunDestinations.COMPETITIVE)
                },
                onNavigateToNotifications = {
                    navController.navigate(RunDestinations.NOTIFICATIONS)
                }
            )
        }

        composable(RunDestinations.IDENTITY) {
            PlayerIdentityScreen(
                authRepository = appModule.authRepository,
                healthRepository = appModule.healthRepository,
                customizationRepository = appModule.customizationRepository,
                runSessionDao = appModule.databaseModule.runSessionDao,
                supabaseSyncService = appModule.supabaseSyncService,
                onNavigateToMap = {
                    navController.navigate(RunDestinations.MAP) {
                        popUpTo(RunDestinations.MAP) { inclusive = true }
                    }
                },
                onNavigateToCustomization = {
                    navController.navigate(RunDestinations.CUSTOMIZATION)
                },
                onNavigateToTab = { tab ->
                    when (tab) {
                        com.example.core.designsystem.components.RunNavTab.MAP -> navController.navigate(RunDestinations.MAP) { popUpTo(RunDestinations.MAP) { inclusive = true } }
                        com.example.core.designsystem.components.RunNavTab.BATTLES -> navController.navigate(RunDestinations.NOTIFICATIONS)
                        com.example.core.designsystem.components.RunNavTab.RANK -> navController.navigate(RunDestinations.COMPETITIVE)
                        com.example.core.designsystem.components.RunNavTab.PROFILE -> {}
                    }
                },
                onLogout = {
                    navController.navigate(RunDestinations.LOGIN) {
                        popUpTo(RunDestinations.MAP) { inclusive = true }
                        popUpTo(RunDestinations.IDENTITY) { inclusive = true }
                    }
                }
            )
        }

        composable(RunDestinations.CUSTOMIZATION) {
            val customizationViewModel: FlagCreatorViewModel = viewModel(
                factory = FlagCreatorViewModel.provideFactory(
                    customizationRepository = appModule.customizationRepository,
                    authRepository = appModule.authRepository
                )
            )
            FlagCreatorScreen(
                viewModel = customizationViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(RunDestinations.COMPETITIVE) {
            val competitiveViewModel: com.example.feature.competitive.CompetitiveViewModel = viewModel(
                factory = com.example.feature.competitive.CompetitiveViewModel.Factory(
                    competitiveRepository = appModule.competitiveRepository
                )
            )
            com.example.feature.competitive.CompetitiveScreen(
                viewModel = competitiveViewModel,
                onNavigateBack = {
                    navController.navigate(RunDestinations.MAP) { popUpTo(RunDestinations.MAP) { inclusive = true } }
                },
                onNavigateToTab = { tab ->
                    when (tab) {
                        com.example.core.designsystem.components.RunNavTab.MAP -> navController.navigate(RunDestinations.MAP) { popUpTo(RunDestinations.MAP) { inclusive = true } }
                        com.example.core.designsystem.components.RunNavTab.BATTLES -> navController.navigate(RunDestinations.NOTIFICATIONS)
                        com.example.core.designsystem.components.RunNavTab.RANK -> {}
                        com.example.core.designsystem.components.RunNavTab.PROFILE -> navController.navigate(RunDestinations.IDENTITY)
                    }
                }
            )
        }

        composable(RunDestinations.NOTIFICATIONS) {
            val notificationsViewModel: com.example.feature.notifications.NotificationsViewModel = viewModel(
                factory = com.example.feature.notifications.NotificationsViewModel.Factory(
                    notificationRepository = appModule.notificationRepository
                )
            )
            com.example.feature.notifications.NotificationsScreen(
                viewModel = notificationsViewModel,
                onNavigateBack = {
                    navController.navigate(RunDestinations.MAP) { popUpTo(RunDestinations.MAP) { inclusive = true } }
                },
                onNavigateToTab = { tab ->
                    when (tab) {
                        com.example.core.designsystem.components.RunNavTab.MAP -> navController.navigate(RunDestinations.MAP) { popUpTo(RunDestinations.MAP) { inclusive = true } }
                        com.example.core.designsystem.components.RunNavTab.BATTLES -> {}
                        com.example.core.designsystem.components.RunNavTab.RANK -> navController.navigate(RunDestinations.COMPETITIVE)
                        com.example.core.designsystem.components.RunNavTab.PROFILE -> navController.navigate(RunDestinations.IDENTITY)
                    }
                },
                onNavigateToUrl = { url ->
                    if (url.startsWith("run2capture://map")) {
                        navController.navigate(RunDestinations.MAP) {
                            popUpTo(RunDestinations.MAP) { inclusive = true }
                        }
                    } else if (url.startsWith("run2capture://identity")) {
                        navController.navigate(RunDestinations.IDENTITY)
                    }
                }
            )
        }
    }
}
