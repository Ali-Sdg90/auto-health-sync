package com.alisadeghi.autohealthsync

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.core.net.toUri
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alisadeghi.autohealthsync.ui.MainScreen
import com.alisadeghi.autohealthsync.ui.MainViewModel
import com.alisadeghi.autohealthsync.ui.UiEvent
import com.alisadeghi.autohealthsync.ui.theme.AutoHealthSyncTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AutoHealthSyncTheme {
                val viewModel: MainViewModel = viewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val snackbar = remember { SnackbarHostState() }
                val healthLauncher = rememberLauncherForActivityResult(
                    PermissionController.createRequestPermissionResultContract(),
                    viewModel::onHealthPermissionsResult,
                )
                val driveLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartIntentSenderForResult(),
                ) { result -> viewModel.completeDriveConnection(result.data) }
                val notificationLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                    viewModel::onNotificationPermissionResult,
                )

                LaunchedEffect(Unit) {
                    viewModel.events.collect { event ->
                        when (event) {
                            is UiEvent.Message -> snackbar.showSnackbar(event.text)
                            is UiEvent.ResolveDriveAuthorization -> driveLauncher.launch(
                                IntentSenderRequest.Builder(event.pendingIntent.intentSender).build(),
                            )
                            UiEvent.RequestHealthPermissions -> healthLauncher.launch(viewModel.healthPermissions)
                            UiEvent.RequestNotificationPermission -> notificationLauncher.launch(
                                android.Manifest.permission.POST_NOTIFICATIONS,
                            )
                            UiEvent.OpenHealthConnectStore -> openHealthConnectStore()
                            UiEvent.OpenHealthConnect -> openHealthConnect()
                            is UiEvent.OpenGoogleDrive -> openGoogleDrive(event.folderId)
                            is UiEvent.OpenSystemSettings -> openSystemSettings(event.intent)
                        }
                    }
                }
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    viewModel.refreshConnections()
                }
                Scaffold(
                    containerColor = Color.Transparent,
                    snackbarHost = { SnackbarHost(snackbar) },
                ) { padding ->
                    MainScreen(
                        state = state,
                        onHealthConnect = viewModel::requestHealthConnection,
                        onDriveConnect = viewModel::requestDriveConnection,
                        onOpenHealthConnect = viewModel::openHealthConnect,
                        onOpenGoogleDrive = viewModel::openGoogleDrive,
                        onBackupNow = viewModel::backupNow,
                        onBackupDateChange = viewModel::selectBackupDate,
                        onSaveSettings = viewModel::saveSettings,
                        onRequestNotifications = viewModel::requestNotificationPermission,
                        onOpenBatterySettings = viewModel::openBatterySettings,
                        onOpenAutoStartSettings = viewModel::openAutoStartSettings,
                        onConfirmAutoStart = viewModel::confirmAutoStart,
                        onCompleteOnboarding = viewModel::completeOnboarding,
                        contentPadding = padding,
                    )
                }
            }
        }
    }

    private fun openHealthConnectStore() {
        val market = "market://details?id=com.google.android.apps.healthdata".toUri()
        val web = "https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata".toUri()
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, market)) }
            .onFailure { startActivity(Intent(Intent.ACTION_VIEW, web)) }
    }

    private fun openHealthConnect() {
        runCatching {
            startActivity(HealthConnectClient.getHealthConnectManageDataIntent(this))
        }.onFailure {
            openHealthConnectStore()
        }
    }

    private fun openGoogleDrive(folderId: String?) {
        val uri = if (folderId == null) {
            "https://drive.google.com/drive/my-drive".toUri()
        } else {
            "https://drive.google.com/drive/folders/$folderId".toUri()
        }
        val driveIntent = Intent(Intent.ACTION_VIEW, uri).setPackage(GOOGLE_DRIVE_PACKAGE)
        runCatching { startActivity(driveIntent) }
            .onFailure { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }

    private fun openSystemSettings(intent: Intent) {
        runCatching { startActivity(intent) }
            .onFailure {
                startActivity(
                    Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        "package:$packageName".toUri(),
                    ),
                )
            }
    }

    companion object {
        private const val GOOGLE_DRIVE_PACKAGE = "com.google.android.apps.docs"
    }
}
