package com.example.feature.map

import android.Manifest
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.ColorApexRed
import com.example.core.designsystem.ColorCipherCyan
import com.example.core.designsystem.ColorDarkBackground
import com.example.core.designsystem.ColorDarkCard
import com.example.core.designsystem.ColorDarkSurfaceElevated
import com.example.core.designsystem.ColorElectricLime
import com.example.core.designsystem.ColorSolarisGold
import com.example.core.designsystem.ColorTextPrimary
import com.example.core.designsystem.ColorTextSecondary
import com.example.core.designsystem.RunColors
import com.example.core.designsystem.components.RunPrimaryButton
import com.example.core.designsystem.components.RunSecondaryButton
import com.example.core.location.LocationPermissionManager
import com.example.domain.model.ActiveRunStats
import com.example.domain.model.DevTerritory
import com.example.domain.model.Faction
import com.example.domain.model.GpsSignalStatus
import com.example.domain.model.LocationError
import com.example.domain.model.LocationPermissionState
import com.example.domain.model.RunSessionResult
import com.example.domain.model.RunState
import com.example.domain.model.SyncStatus
import com.example.domain.model.TrackingState
import com.example.feature.map.components.LeafletMapView
import com.example.feature.map.components.LocationAcquisitionOverlay
import com.example.feature.map.components.TerritoryDetailsModal
import com.example.feature.map.components.AttackPreparationModal
import com.example.feature.map.components.BattleEvaluationModal
import kotlinx.coroutines.delay

@Composable
fun WorldMapScreen(
    viewModel: WorldMapViewModel,
    onNavigateToCustomization: () -> Unit,
    onNavigateToIdentity: () -> Unit,
    onNavigateToCompetitive: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasCenteredOnUser by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onPermissionResult(fineGranted, coarseGranted)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.retryGpsAcquisition(context as? Activity)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.permissionManager?.hasLocationPermission() == false) {
            permissionLauncher.launch(LocationPermissionManager.ALL_RUN_PERMISSIONS)
        }
        com.example.core.sync.SyncManager.scheduleSync(context)
    }

    // Smoothly focus on the user's location when GPS location fix is acquired
    LaunchedEffect(uiState.userLocation, uiState.isMapReady) {
        if (uiState.userLocation != null && uiState.isMapReady && !hasCenteredOnUser) {
            hasCenteredOnUser = true
            viewModel.flyToUser(zoom = 16, durationSec = 1.0)
        }
    }

    val isRunActiveOrPaused = uiState.runState == RunState.RUNNING || uiState.runState == RunState.PAUSED

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ColorDarkBackground)
            .testTag("world_map_screen")
    ) {
        // 1. Full Screen Interactive Leaflet Map
        LeafletMapView(
            onBridgeReady = { bridge ->
                viewModel.bindMapController(bridge)
            },
            eventListener = viewModel,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Top HUD Overlay: Sleek Glassmorphic Floating Profile Pill
        TopMapHud(
            uiState = uiState,
            onNotificationClick = { viewModel.openNotificationModal() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        )

        // 3. Tile Loading / Error Banner Overlay / GPS Accuracy Banner
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 84.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TileStatusBanner(
                isTileLoading = uiState.isTileLoading,
                tileError = uiState.tileError,
                onRetry = { viewModel.onRetryMapLoad() }
            )

            // Dynamic GPS / Permission Callout Banner
            if (uiState.permissionState != LocationPermissionState.GRANTED) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF330000))
                        .border(1.dp, ColorApexRed, RoundedCornerShape(12.dp))
                        .clickable {
                            permissionLauncher.launch(LocationPermissionManager.ALL_RUN_PERMISSIONS)
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("permission_alert_banner"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOff,
                        contentDescription = null,
                        tint = ColorApexRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "LOCATION PERMISSION REQUIRED • TAP TO ENABLE",
                        color = Color(0xFFFFB3B3),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (uiState.gpsStatus == GpsSignalStatus.DISABLED) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF332000))
                        .border(1.dp, Color(0xFFFF9500), RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.promptEnableGps(context as? Activity)
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("gps_disabled_banner"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsNotFixed,
                        contentDescription = null,
                        tint = Color(0xFFFF9500),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "GPS OFFLINE • TAP TO ACTIVATE SATELLITE LINK",
                        color = Color(0xFFFFD699),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (uiState.gpsStatus == GpsSignalStatus.SEARCHING && uiState.userLocation == null) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ColorDarkSurfaceElevated.copy(alpha = 0.9f))
                        .border(1.dp, ColorCipherCyan.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("gps_searching_banner"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        color = ColorCipherCyan,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "ACQUIRING GPS LOCK • STAND BY...",
                        color = ColorCipherCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Poor GPS Accuracy Warning Banner
            if (uiState.locationError is LocationError.PoorAccuracy) {
                val acc = (uiState.locationError as LocationError.PoorAccuracy).accuracyMeters
                PoorAccuracyBanner(
                    accuracyMeters = acc,
                    onDismiss = { viewModel.dismissLocationError() }
                )
            }
        }

        // 4. Floating Map & Location Controls (Right side - Sleek Rounded White Cards)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Recenter on Player GPS Location
            MapControlCardButton(
                icon = Icons.Default.MyLocation,
                contentDescription = "Center on GPS Location",
                onClick = {
                    val activity = context as? Activity
                    if (uiState.permissionState != LocationPermissionState.GRANTED) {
                        permissionLauncher.launch(LocationPermissionManager.ALL_RUN_PERMISSIONS)
                    } else if (uiState.gpsStatus == GpsSignalStatus.DISABLED) {
                        viewModel.promptEnableGps(activity)
                    } else {
                        viewModel.centerOnUser(activity)
                    }
                },
                testTag = "recenter_location_button"
            )

            // Map Layer Selector Button
            MapControlCardButton(
                icon = Icons.Default.Layers,
                contentDescription = "Select Map View Layers",
                onClick = { viewModel.toggleLayerSelector() },
                testTag = "map_layer_selector_button"
            )

            // Combined Zoom In / Zoom Out Card
            MapZoomControlCard(
                onZoomIn = { viewModel.zoomIn() },
                onZoomOut = { viewModel.zoomOut() }
            )
        }

        // 5. Bottom Navigation Bar OR Active Run Telemetry HUD
        AnimatedVisibility(
            visible = isRunActiveOrPaused,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .fillMaxWidth()
        ) {
            ActiveRunHud(
                stats = uiState.activeRunStats,
                runState = uiState.runState,
                onPause = { viewModel.pauseRun() },
                onResume = { viewModel.resumeRun() },
                onFinish = { viewModel.requestFinishRun() },
                onCancel = { viewModel.cancelRun() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }

        AnimatedVisibility(
            visible = !isRunActiveOrPaused,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .fillMaxWidth()
        ) {
            BottomMapHud(
                onStartRunClick = { viewModel.openRunPreparation() },
                onNavigateToBattles = onNavigateToNotifications,
                onNavigateToRank = onNavigateToCompetitive,
                onNavigateToProfile = onNavigateToIdentity,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 6. Run Preparation Modal (Phase 7: GPS status, accuracy, current location, START RUN)
        if (uiState.showRunPreparationSheet || uiState.runState == RunState.PREPARING) {
            RunPreparationDialog(
                uiState = uiState,
                onDismiss = { viewModel.closeRunPreparation() },
                onStartRun = { viewModel.startRun() }
            )
        }

        // 7. Finish Run Confirmation Dialog
        if (uiState.showFinishConfirmDialog) {
            FinishRunConfirmDialog(
                stats = uiState.activeRunStats,
                onConfirm = { viewModel.confirmFinishRun() },
                onDismiss = { viewModel.dismissFinishConfirmDialog() }
            )
        }

        // 8. Finishing / Uploading / Validating Pipeline Progress Dialog
        if (uiState.runState == RunState.FINISHING || uiState.runState == RunState.UPLOADING || uiState.runState == RunState.VALIDATING) {
            FinishingPipelineDialog(runState = uiState.runState)
        }

        // 9. Completed Run Summary Dialog
        if (uiState.runState == RunState.COMPLETED && uiState.completedRunResult != null) {
            CompletedRunSummaryDialog(
                result = uiState.completedRunResult!!,
                onDismiss = { viewModel.closeCompletedRunSummary() }
            )
        }

        // 10. Selected Dev Territory Modal
        if (uiState.showTerritoryDetailsModal && uiState.selectedDevTerritory != null) {
            TerritoryDetailsModal(
                territory = uiState.selectedDevTerritory!!,
                eligibility = uiState.attackEligibility,
                onDismiss = { viewModel.dismissTerritoryDetailsModal() },
                onInitiateAttack = { viewModel.initiateAttackOnSelectedTerritory() }
            )
        }

        // 10b. Attack Preparation Modal
        if (uiState.showAttackPreparationModal && uiState.activeBattle != null) {
            AttackPreparationModal(
                battle = uiState.activeBattle!!,
                onDismiss = { viewModel.dismissAttackPreparationModal() },
                onStartBattle = { viewModel.startBattleRun() }
            )
        }
        
        // 10c. Battle Evaluation Modal
        if (uiState.battleEvaluation != null) {
            BattleEvaluationModal(
                evaluation = uiState.battleEvaluation!!,
                onDismiss = { viewModel.dismissBattleEvaluation() }
            )
        }

        // 11. Notifications Dialog
        if (uiState.showNotificationModal) {
            TacticalNotificationsDialog(
                onDismiss = { viewModel.closeNotificationModal() }
            )
        }

        // 12. Contextual Permission Rationale Dialog
        if (uiState.showPermissionRationaleDialog) {
            PermissionRationaleDialog(
                onConfirm = {
                    viewModel.dismissRationaleDialog()
                    permissionLauncher.launch(LocationPermissionManager.ALL_RUN_PERMISSIONS)
                },
                onDismiss = { viewModel.dismissRationaleDialog() }
            )
        }

        // 13. Permanently Denied Permission Dialog
        if (uiState.showPermanentlyDeniedDialog) {
            PermanentlyDeniedDialog(
                onOpenSettings = {
                    viewModel.dismissPermanentlyDeniedDialog()
                    viewModel.permissionManager?.openAppSettings()
                },
                onDismiss = { viewModel.dismissPermanentlyDeniedDialog() }
            )
        }

        // 14. GPS Hardware Disabled Dialog
        if (uiState.showGpsDisabledDialog) {
            GpsDisabledDialog(
                onEnableGps = {
                    viewModel.dismissGpsDisabledDialog()
                    viewModel.promptEnableGps(context as? Activity)
                },
                onOpenLocationSettings = {
                    viewModel.dismissGpsDisabledDialog()
                    viewModel.permissionManager?.openLocationSettings()
                },
                onDismiss = { viewModel.dismissGpsDisabledDialog() }
            )
        }

        // 15. Map Layer Selector Dialog
        if (uiState.showLayerSelectorModal) {
            MapLayerSelectionDialog(
                currentLayer = uiState.selectedMapLayer,
                onSelectLayer = { viewModel.selectMapLayer(it) },
                onDismiss = { viewModel.dismissLayerSelector() }
            )
        }
    }
}

@Composable
private fun TopMapHud(
    uiState: MapUiState,
    onNotificationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayCallsign = if (uiState.username.isNotBlank()) uiState.username else "NIGHTHAWK_07"
    val initials = if (displayCallsign.length >= 2) displayCallsign.take(2).uppercase() else "NH"

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        // Main Top Glass HUD
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = RunColors.Glass,
            border = androidx.compose.foundation.BorderStroke(1.dp, RunColors.GlassBorder),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x1414171A), spotColor = Color(0x1F14171A))
                .testTag("top_map_hud")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar Ring with cyan border
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(RunColors.Cyan, RunColors.Cyan)))
                        .padding(2.5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFFDAD8D0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = RunColors.Ink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Name, LV chip & XP Track
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(
                            text = displayCallsign.uppercase(),
                            color = RunColors.Ink,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = RunColors.LimeTint,
                            border = androidx.compose.foundation.BorderStroke(1.dp, RunColors.Lime.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "LV ${uiState.playerLevel}",
                                color = RunColors.LimeDeep,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                                    .testTag("player_level_badge")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // XP Track (5px height with lime-cyan gradient)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color(0xFFE9E8E2))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(uiState.playerXpProgress.coerceIn(0.1f, 1f))
                                .height(5.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(
                                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        listOf(RunColors.Lime, RunColors.Cyan)
                                    )
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Notification Bell Wrap with Dot
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF4F3EF))
                        .clickable { onNotificationClick() }
                        .testTag("notification_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Tactical Notifications",
                        tint = RunColors.Ink,
                        modifier = Modifier.size(16.dp)
                    )
                    // Notification indicator dot (lime)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-3).dp, y = 3.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(RunColors.LimeDeep)
                            .border(1.5.dp, Color.White, CircleShape)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // GPS Status Pill
        Surface(
            shape = RoundedCornerShape(50),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, RunColors.GlassBorder),
            modifier = Modifier.shadow(4.dp, RoundedCornerShape(50))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            when (uiState.gpsStatus) {
                                GpsSignalStatus.GOOD -> Color(0xFF8FD98C)
                                GpsSignalStatus.POOR -> RunColors.Warning
                                else -> RunColors.Error
                            }
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when (uiState.gpsStatus) {
                        GpsSignalStatus.GOOD -> "GPS GOOD"
                        GpsSignalStatus.POOR -> "GPS POOR"
                        GpsSignalStatus.SEARCHING -> "GPS SEARCHING"
                        GpsSignalStatus.DISABLED -> "GPS DISABLED"
                    },
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = RunColors.Body
                    )
                )
            }
        }
    }
}

@Composable
private fun TileStatusBanner(
    isTileLoading: Boolean,
    tileError: String?,
    onRetry: () -> Unit
) {
    if (isTileLoading) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xEEFFFFFF))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                color = Color(0xFF0284C7),
                strokeWidth = 2.dp,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "LOADING MAP TILES...",
                color = Color(0xFF1E293B),
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold
            )
        }
    } else if (tileError != null) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFEE2E2))
                .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(12.dp))
                .clickable { onRetry() }
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("tile_error_banner"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Retry Map Load",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "TILE LINK DISRUPTED • TAP TO RETRY",
                color = Color(0xFFDC2626),
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MapControlCardButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .shadow(4.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color(0xFF1E293B),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun MapZoomControlCard(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(48.dp)
            .shadow(4.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(14.dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable { onZoomIn() }
                .testTag("map_zoom_in_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Zoom In",
                tint = Color(0xFF1E293B),
                modifier = Modifier.size(20.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFF1F5F9))
        )

        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable { onZoomOut() }
                .testTag("map_zoom_out_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Zoom Out",
                tint = Color(0xFF1E293B),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun BottomMapHud(
    onStartRunClick: () -> Unit,
    onNavigateToBattles: () -> Unit,
    onNavigateToRank: () -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .testTag("bottom_map_hud")
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Floating Action Card: Territory Held Stat + START RUN Button (Screen 09)
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = RunColors.Glass,
            border = androidx.compose.foundation.BorderStroke(1.dp, RunColors.GlassBorder),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x1414171A), spotColor = Color(0x1F14171A))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Territory Held Stat
                Column {
                    Text(
                        text = "TERRITORY HELD",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = RunColors.Faint
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "1.84 km²",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = RunColors.Ink
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "▲ +0.12 today",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RunColors.CyanDeep
                            )
                        )
                    }
                }

                // Right: START RUN Pill Button
                Button(
                    onClick = onStartRunClick,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RunColors.Lime,
                        contentColor = RunColors.LimeText
                    ),
                    modifier = Modifier
                        .height(46.dp)
                        .shadow(8.dp, RoundedCornerShape(999.dp), ambientColor = Color(0x33CFF23A), spotColor = Color(0x66CFF23A))
                        .testTag("start_run_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "START RUN",
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Floating Bottom Navigation Bar (MAP, BATTLES, RANK, PROFILE)
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = RunColors.Glass,
            border = androidx.compose.foundation.BorderStroke(1.dp, RunColors.GlassBorder),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(10.dp, RoundedCornerShape(26.dp), ambientColor = Color(0x1414171A), spotColor = Color(0x1F14171A))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // MAP (Active tab)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(RunColors.LimeTint)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = "Map View",
                        tint = RunColors.LimeDeep,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "MAP",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = RunColors.LimeDeep,
                            letterSpacing = 0.5.sp
                        )
                    )
                }

                // BATTLES
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onNavigateToBattles() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("nav_battles_tab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Battles",
                        tint = RunColors.Faint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "BATTLES",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = RunColors.Faint
                        )
                    )
                }

                // RANK
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onNavigateToRank() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("nav_rank_tab")
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Rank",
                        tint = RunColors.Faint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "RANK",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = RunColors.Faint
                        )
                    )
                }

                // PROFILE
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onNavigateToProfile() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("nav_profile_tab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = RunColors.Faint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "PROFILE",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = RunColors.Faint
                        )
                    )
                }
            }
        }
    }
}

// ==========================================
// PHASE 7: RUN PREPARATION DIALOG
// ==========================================

@Composable
private fun RunPreparationDialog(
    uiState: MapUiState,
    onDismiss: () -> Unit,
    onStartRun: () -> Unit
) {
    val userLoc = uiState.userLocation
    val acc = userLoc?.accuracyMeters ?: 0f
    val lat = userLoc?.latitude ?: 0.0
    val lng = userLoc?.longitude ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorDarkCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DirectionsRun,
                    contentDescription = null,
                    tint = ColorElectricLime,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RUN PREPARATION",
                    color = ColorTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Acquiring tactical satellite fix before engaging route tracking...",
                    color = ColorTextSecondary,
                    fontSize = 12.sp
                )

                // GPS Status Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ColorDarkSurfaceElevated)
                        .border(1.dp, RunColors.CardBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GPS STATUS",
                            color = ColorTextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.gpsStatus.isGood) ColorElectricLime else Color(0xFFFF9500))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = uiState.gpsStatus.label,
                                color = if (uiState.gpsStatus.isGood) ColorElectricLime else Color(0xFFFF9500),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACCURACY",
                            color = ColorTextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (acc > 0) "± %.1f m".format(acc) else "SEARCHING...",
                            color = if (acc in 0.1f..15.0f) ColorCipherCyan else ColorTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CURRENT LOCATION",
                            color = ColorTextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (lat != 0.0) "%.5f, %.5f".format(lat, lng) else "AQUIRING...",
                            color = ColorTextPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        },
        confirmButton = {
            RunPrimaryButton(
                text = "START RUN",
                onClick = onStartRun,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("confirm_start_run_button")
            )
        },
        dismissButton = {
            RunSecondaryButton(
                text = "CANCEL",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

// ==========================================
// PHASE 7: ACTIVE RUN TELEMETRY HUD
// ==========================================

@Composable
private fun ActiveRunHud(
    stats: ActiveRunStats,
    runState: RunState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPaused = runState == RunState.PAUSED
    val km = stats.distanceMeters / 1000.0
    val formattedDuration = stats.formattedDuration
    val formattedPace = stats.formattedPace
    val acc = stats.lastKnownLocation?.accuracyMeters ?: 0f

    Column(
        modifier = modifier
            .testTag("active_run_hud")
            .clip(RoundedCornerShape(20.dp))
            .background(ColorDarkCard.copy(alpha = 0.96f))
            .border(1.dp, if (isPaused) Color(0xFFFF9500) else ColorElectricLime, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top row: Header, GPS accuracy, and Sync Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isPaused) Color(0xFFFF9500) else ColorElectricLime)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isPaused) "RUN PAUSED" else "RUNNING",
                    color = if (isPaused) Color(0xFFFF9500) else ColorElectricLime,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Online/Offline status pill
                if (stats.isOffline) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF332000))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Offline Mode",
                            tint = Color(0xFFFF9500),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "OFFLINE",
                            color = Color(0xFFFF9500),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Text(
                    text = "± %.0fm • %d pts".format(acc, stats.pointsCount),
                    color = ColorTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Metrics Grid (Timer, Distance, Pace, Accuracy)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            RunMetricItem(
                label = "TIMER",
                value = formattedDuration,
                unit = "",
                color = ColorTextPrimary
            )
            RunMetricItem(
                label = "DISTANCE",
                value = "%.2f".format(km),
                unit = "km",
                color = ColorCipherCyan
            )
            RunMetricItem(
                label = "PACE",
                value = formattedPace,
                unit = "/km",
                color = ColorElectricLime
            )
            RunMetricItem(
                label = "ACCURACY",
                value = if (acc > 0) "%.0f".format(acc) else "--",
                unit = "m",
                color = if (acc in 0.1f..20.0f) ColorCipherCyan else Color(0xFFFF9500)
            )
        }

        // Action Buttons: PAUSE / RESUME + FINISH
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isPaused) {
                RunPrimaryButton(
                    text = "RESUME",
                    onClick = onResume,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("resume_run_button")
                )
            } else {
                RunSecondaryButton(
                    text = "PAUSE",
                    onClick = onPause,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pause_run_button")
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ColorApexRed.copy(alpha = 0.15f))
                    .border(1.dp, ColorApexRed, RoundedCornerShape(12.dp))
                    .clickable { onFinish() }
                    .testTag("finish_run_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Finish Run",
                        tint = ColorApexRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FINISH",
                        color = ColorApexRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// ==========================================
// PHASE 7: FINISH CONFIRMATION DIALOG
// ==========================================

@Composable
private fun FinishRunConfirmDialog(
    stats: ActiveRunStats,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val km = stats.distanceMeters / 1000.0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorDarkCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = null,
                    tint = ColorApexRed,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "FINISH RUN?",
                    color = ColorTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Are you ready to stop tracking and finalize this run session?",
                    color = ColorTextSecondary,
                    fontSize = 13.sp
                )
                Text(
                    text = "Session Stats: %.2f km • %s • %d GPS points".format(km, stats.formattedDuration, stats.pointsCount),
                    color = ColorCipherCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        confirmButton = {
            RunPrimaryButton(
                text = "FINISH & SAVE",
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("confirm_finish_run_button")
            )
        },
        dismissButton = {
            RunSecondaryButton(
                text = "RESUME RUNNING",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

// ==========================================
// PHASE 7: FINISHING / UPLOADING / VALIDATING PROGRESS
// ==========================================

@Composable
private fun FinishingPipelineDialog(runState: RunState) {
    AlertDialog(
        onDismissRequest = { /* Non-dismissible while processing */ },
        containerColor = ColorDarkCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    color = ColorElectricLime,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "PROCESSING RUN",
                    color = ColorTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PipelineStepRow(
                    title = "1. Freezing route telemetry",
                    isDone = runState == RunState.UPLOADING || runState == RunState.VALIDATING || runState == RunState.COMPLETED,
                    isInProgress = runState == RunState.FINISHING
                )
                PipelineStepRow(
                    title = "2. Uploading & local persistence",
                    isDone = runState == RunState.VALIDATING || runState == RunState.COMPLETED,
                    isInProgress = runState == RunState.UPLOADING
                )
                PipelineStepRow(
                    title = "3. Validating trajectory & anti-cheat",
                    isDone = runState == RunState.COMPLETED,
                    isInProgress = runState == RunState.VALIDATING
                )
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun PipelineStepRow(
    title: String,
    isDone: Boolean,
    isInProgress: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = if (isDone || isInProgress) ColorTextPrimary else ColorTextSecondary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        if (isDone) {
            Icon(
                imageVector = Icons.Default.Done,
                contentDescription = "Completed",
                tint = ColorElectricLime,
                modifier = Modifier.size(16.dp)
            )
        } else if (isInProgress) {
            CircularProgressIndicator(
                color = ColorCipherCyan,
                strokeWidth = 2.dp,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// ==========================================
// PHASE 7: COMPLETED RUN SUMMARY DIALOG
// ==========================================

@Composable
private fun CompletedRunSummaryDialog(
    result: RunSessionResult,
    onDismiss: () -> Unit
) {
    val km = result.distanceMeters / 1000.0
    val durationMin = result.durationSeconds / 60
    val durationSec = result.durationSeconds % 60
    val formattedDuration = "%02d:%02d".format(durationMin, durationSec)
    val paceMin = result.avgPaceMinPerKm.toInt()
    val paceSec = ((result.avgPaceMinPerKm - paceMin) * 60).toInt()
    val formattedPace = "%d:%02d".format(paceMin, paceSec)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorDarkCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = ColorElectricLime,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RUN COMPLETED",
                    color = ColorTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Large distance callout
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ColorDarkSurfaceElevated)
                        .border(1.dp, ColorElectricLime.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TOTAL DISTANCE",
                        color = ColorTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "%.2f".format(km),
                            color = ColorElectricLime,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "km",
                            color = ColorTextSecondary,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Grid of stats: Time, Avg Pace, Calories, Points
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RunMetricItem(
                        label = "TIME",
                        value = formattedDuration,
                        unit = "min",
                        color = ColorTextPrimary
                    )
                    RunMetricItem(
                        label = "AVG PACE",
                        value = formattedPace,
                        unit = "/km",
                        color = ColorCipherCyan
                    )
                    RunMetricItem(
                        label = "CALORIES",
                        value = "${result.caloriesBurned}",
                        unit = "kcal",
                        color = ColorSolarisGold
                    )
                    RunMetricItem(
                        label = "GPS POINTS",
                        value = "${result.pointsCount}",
                        unit = "pts",
                        color = ColorTextPrimary
                    )
                }

                // Sync status note
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (result.isOffline) Color(0xFF332000) else ColorDarkSurfaceElevated)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (result.isOffline) Icons.Default.CloudOff else Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = if (result.isOffline) Color(0xFFFF9500) else ColorElectricLime,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (result.isOffline) "SAVED LOCALLY (OFFLINE QUEUED)" else "TELEMETRY SYNCED TO CLOUD",
                        color = if (result.isOffline) Color(0xFFFF9500) else ColorElectricLime,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        confirmButton = {
            RunPrimaryButton(
                text = "RETURN TO RADAR",
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("return_to_radar_button")
            )
        }
    )
}

@Composable
private fun RunMetricItem(
    label: String,
    value: String,
    unit: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = ColorTextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                color = color,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            if (unit.isNotEmpty()) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    color = ColorTextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun PoorAccuracyBanner(
    accuracyMeters: Float,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF332000))
            .border(1.dp, Color(0xFFFF9500), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("poor_accuracy_banner"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = Color(0xFFFF9500),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "DEGRADED GPS ACCURACY (%.0fm) • SEEK OPEN SKY".format(accuracyMeters),
            color = Color(0xFFFFD699),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "OK",
            color = Color(0xFFFF9500),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onDismiss() }
        )
    }
}

@Composable
private fun TerritoryInspectionDialog(
    territory: DevTerritory,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorDarkCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = Color(android.graphics.Color.parseColor(territory.colorHex)),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = territory.name,
                    color = ColorTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SECTOR ID: ${territory.id}",
                    color = ColorTextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "CONTROLLING SYNDICATE: ${territory.factionId}",
                    color = ColorCipherCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "AREA: %.0f m²".format(territory.areaSqMeters),
                    color = ColorTextPrimary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "DEFENSE SHIELD: ${territory.defenseLevel}%",
                    color = ColorElectricLime,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "* Development sector simulation overlay.",
                    color = ColorTextSecondary,
                    fontSize = 10.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ACKNOWLEDGE", color = ColorElectricLime, fontFamily = FontFamily.Monospace)
            }
        }
    )
}

@Composable
private fun TacticalNotificationsDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorDarkCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = ColorElectricLime,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TACTICAL INTEL DISPATCH",
                    color = ColorTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Hero Graphic Picture
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_tactical_runner_hero),
                    contentDescription = "Tactical Runner Radar",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, ColorCipherCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )

                NotificationItem(
                    title = "RADAR SATELLITE LINK ACTIVE",
                    time = "JUST NOW",
                    desc = "Leaflet tactical engine synchronized with GPS constellation.",
                    accent = ColorCipherCyan
                )
                NotificationItem(
                    title = "SECTOR TERRITORY INTEL",
                    time = "10M AGO",
                    desc = "Syndicate activity detected in adjacent city grids.",
                    accent = ColorElectricLime
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE DISPATCH", color = ColorElectricLime, fontFamily = FontFamily.Monospace)
            }
        }
    )
}

@Composable
private fun NotificationItem(
    title: String,
    time: String,
    desc: String,
    accent: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ColorDarkSurfaceElevated)
            .border(1.dp, RunColors.CardBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = time,
                color = ColorTextSecondary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = desc,
            color = ColorTextSecondary,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun PermissionRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorDarkCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = ColorElectricLime,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = LocationPermissionManager.RATIONALE_TITLE,
                    color = ColorTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp
                )
            }
        },
        text = {
            Text(
                text = LocationPermissionManager.RATIONALE_MESSAGE,
                color = ColorTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        },
        confirmButton = {
            RunPrimaryButton(
                text = "GRANT ACCESS",
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            RunSecondaryButton(
                text = "NOT NOW",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@Composable
private fun PermanentlyDeniedDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorDarkCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOff,
                    contentDescription = null,
                    tint = ColorApexRed,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LOCATION PERMISSION BLOCKED",
                    color = ColorTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp
                )
            }
        },
        text = {
            Text(
                text = "Precise location telemetry is permanently disabled for RUN2CAPTURE. Please allow Location permissions in Android System App Settings to restore GPS tracking.",
                color = ColorTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        },
        confirmButton = {
            RunPrimaryButton(
                text = "OPEN APP SETTINGS",
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            RunSecondaryButton(
                text = "CANCEL",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@Composable
private fun GpsDisabledDialog(
    onEnableGps: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorDarkCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.GpsNotFixed,
                    contentDescription = null,
                    tint = Color(0xFFFF9500),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GPS SATELLITE LINK DISABLED",
                    color = ColorTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp
                )
            }
        },
        text = {
            Text(
                text = "Device GPS location services are turned off. Please activate GPS to acquire real-time satellite coordinates and position your operative on the tactical radar.",
                color = ColorTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                RunPrimaryButton(
                    text = "ENABLE GPS NOW",
                    onClick = onEnableGps,
                    modifier = Modifier.fillMaxWidth()
                )
                RunSecondaryButton(
                    text = "OPEN LOCATION SETTINGS",
                    onClick = onOpenLocationSettings,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        dismissButton = {
            RunSecondaryButton(
                text = "CANCEL",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@Composable
private fun MapLayerSelectionDialog(
    currentLayer: MapLayerType,
    onSelectLayer: (MapLayerType) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorDarkCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    tint = ColorElectricLime,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MAP VIEW SELECTION",
                    color = ColorTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Choose your preferred tactical map rendering style:",
                    color = ColorTextSecondary,
                    fontSize = 12.sp
                )

                MapLayerType.entries.forEach { layer ->
                    val isSelected = layer == currentLayer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) ColorDarkSurfaceElevated else Color(0xFF14171E))
                            .border(
                                1.dp,
                                if (isSelected) ColorElectricLime else RunColors.CardBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectLayer(layer) }
                            .padding(14.dp)
                            .testTag("layer_option_${layer.name.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = layer.title.uppercase(),
                                    color = if (isSelected) ColorElectricLime else ColorTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp
                                )
                                if (layer == MapConfig.DEFAULT_LAYER) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(DEFAULT)",
                                        color = ColorCipherCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = layer.description,
                                color = ColorTextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = ColorElectricLime,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = ColorElectricLime, fontFamily = FontFamily.Monospace)
            }
        }
    )
}
