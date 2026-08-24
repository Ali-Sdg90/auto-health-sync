package com.alisadeghi.autohealthsync.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alisadeghi.autohealthsync.BuildConfig
import com.alisadeghi.autohealthsync.model.ActivityEntry
import com.alisadeghi.autohealthsync.model.ActivitySeverity
import com.alisadeghi.autohealthsync.model.BackupSettings
import com.alisadeghi.autohealthsync.model.BackupMetric
import com.alisadeghi.autohealthsync.model.ConnectionState
import com.alisadeghi.autohealthsync.model.FileDateSystem
import com.alisadeghi.autohealthsync.model.MAX_DRIVE_FOLDER_NAME_LENGTH
import com.alisadeghi.autohealthsync.util.DateUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun MainScreen(
    state: MainUiState,
    onHealthConnect: () -> Unit,
    onDriveConnect: () -> Unit,
    onOpenHealthConnect: () -> Unit,
    onOpenGoogleDrive: () -> Unit,
    onBackupNow: () -> Unit,
    onBackupDateChange: (LocalDate) -> Unit,
    onSaveSettings: (BackupSettings) -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAutoStartSettings: () -> Unit,
    onConfirmAutoStart: () -> Unit,
    onCompleteOnboarding: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    var settingsVisible by remember { mutableStateOf(false) }

    if (!state.isAppStateLoaded) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    if (state.showOnboarding) {
        OnboardingScreen(
            state = state,
            onHealthConnect = onHealthConnect,
            onDriveConnect = onDriveConnect,
            onRequestNotifications = onRequestNotifications,
            onOpenBatterySettings = onOpenBatterySettings,
            onOpenAutoStartSettings = onOpenAutoStartSettings,
            onConfirmAutoStart = onConfirmAutoStart,
            onComplete = onCompleteOnboarding,
            contentPadding = contentPadding,
        )
        return
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = contentPadding.calculateTopPadding() + 20.dp,
                bottom = contentPadding.calculateBottomPadding() + 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item { AppHeader(onSettingsClick = { settingsVisible = true }) }
            item {
                ConnectionsCard(
                    healthState = state.healthState,
                    driveState = state.driveState,
                    driveFolderName = state.appState.backupSettings.driveFolderName,
                    selectedDate = state.selectedBackupDate,
                    dateSelectionEnabled = !state.isBackingUp,
                    onHealthConnect = onHealthConnect,
                    onDriveConnect = onDriveConnect,
                    onOpenHealthConnect = onOpenHealthConnect,
                    onOpenGoogleDrive = onOpenGoogleDrive,
                    onDateChange = onBackupDateChange,
                )
            }
            item { ScheduleCard(state) }
            item {
                BackupAction(
                    isBackingUp = state.isBackingUp,
                    status = state.operationStatus,
                    enabled = state.healthState == ConnectionState.CONNECTED &&
                        state.driveState == ConnectionState.CONNECTED,
                    onBackupNow = onBackupNow,
                )
            }
            item { RecentActivitySection(state.appState.recentActivity) }
            item { AppFooter() }
        }
    }

    if (settingsVisible) {
        SettingsSheet(
            settings = state.appState.backupSettings,
            onDismiss = { settingsVisible = false },
            onSave = {
                onSaveSettings(it)
                settingsVisible = false
            },
        )
    }
}

@Composable
private fun OnboardingScreen(
    state: MainUiState,
    onHealthConnect: () -> Unit,
    onDriveConnect: () -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAutoStartSettings: () -> Unit,
    onConfirmAutoStart: () -> Unit,
    onComplete: () -> Unit,
    contentPadding: PaddingValues,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = contentPadding.calculateTopPadding() + 28.dp,
                bottom = contentPadding.calculateBottomPadding() + 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.HealthAndSafety,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Set up automatic backups",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Complete the required access once so Auto Health Sync can back up in the background.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SetupStepCard(
                    icon = Icons.Rounded.HealthAndSafety,
                    title = "Health Connect",
                    description = "Allow health reads, history, and background access.",
                    complete = state.healthState == ConnectionState.CONNECTED,
                    checking = state.healthState == ConnectionState.CHECKING,
                    unavailable = state.healthState == ConnectionState.UNAVAILABLE,
                    actionLabel = if (state.healthState == ConnectionState.UNAVAILABLE) "Install" else "Allow",
                    onAction = onHealthConnect,
                )
            }
            item {
                SetupStepCard(
                    icon = Icons.Rounded.Cloud,
                    title = "Google Drive",
                    description = "Allow access to files created by this app.",
                    complete = state.driveState == ConnectionState.CONNECTED,
                    checking = state.driveState == ConnectionState.CHECKING,
                    unavailable = state.driveState == ConnectionState.UNAVAILABLE,
                    actionLabel = "Connect",
                    onAction = onDriveConnect,
                )
            }
            item {
                SetupStepCard(
                    icon = Icons.Rounded.BatteryChargingFull,
                    title = "Unrestricted battery use",
                    description = if (state.backgroundAccess.backgroundRestricted) {
                        "Background use is restricted. Open settings and select Unrestricted."
                    } else {
                        "Open battery settings, find Auto Health Sync, and select Unrestricted."
                    },
                    complete = state.backgroundAccess.batteryAccessGranted,
                    actionLabel = "Open settings",
                    onAction = onOpenBatterySettings,
                )
            }
            if (state.backgroundAccess.autoStartSettingsAvailable) {
                item {
                    AutoStartStepCard(
                        manufacturer = state.backgroundAccess.manufacturerName,
                        complete = state.autoStartReady,
                        onOpenSettings = onOpenAutoStartSettings,
                        onConfirm = onConfirmAutoStart,
                    )
                }
            }
            item {
                SetupStepCard(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "Backup notifications",
                    description = "Recommended for failures and recovered backups.",
                    complete = state.notificationGranted,
                    optional = true,
                    actionLabel = "Allow",
                    onAction = onRequestNotifications,
                )
            }
            item {
                Button(
                    onClick = onComplete,
                    enabled = state.requiredSetupComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text("Finish setup", fontWeight = FontWeight.SemiBold)
                }
                if (!state.requiredSetupComplete) {
                    Text(
                        "Complete every required step to continue.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupStepCard(
    icon: ImageVector,
    title: String,
    description: String,
    complete: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    checking: Boolean = false,
    unavailable: Boolean = false,
    optional: Boolean = false,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(17.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (complete) Color(0xFF20A67A).copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.primaryContainer,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (complete) Icons.Rounded.Check else icon,
                    contentDescription = null,
                    tint = if (complete) Color(0xFF16805F) else MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (optional) {
                        Text(
                            "  Optional",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    when {
                        complete -> "Ready"
                        unavailable -> "Not available on this device"
                        else -> description
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            when {
                checking -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                complete -> Unit
                else -> TextButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

@Composable
private fun AutoStartStepCard(
    manufacturer: String,
    complete: Boolean,
    onOpenSettings: () -> Unit,
    onConfirm: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (complete) Color(0xFF20A67A).copy(alpha = 0.14f)
                            else MaterialTheme.colorScheme.primaryContainer,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (complete) Icons.Rounded.Check else Icons.Rounded.RocketLaunch,
                        contentDescription = null,
                        tint = if (complete) Color(0xFF16805F) else MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text("Auto Start", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (complete) "Ready" else "Enable Auto Health Sync in $manufacturer startup settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!complete) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text("Open settings") }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text("I've enabled it") }
                }
            }
        }
    }
}

@Composable
private fun AppFooter() {
    val uriHandler = LocalUriHandler.current
    Text(
        text = "v${BuildConfig.VERSION_NAME} · Built with care by A.S.",
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { uriHandler.openUri(CREATOR_GITHUB_URL) }
            .padding(top = 8.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
        style = MaterialTheme.typography.labelMedium,
        textAlign = TextAlign.Center,
    )
}

private const val CREATOR_GITHUB_URL = "https://github.com/Ali-Sdg90"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    settings: BackupSettings,
    onDismiss: () -> Unit,
    onSave: (BackupSettings) -> Unit,
) {
    var backupHour by remember(settings.backupHour) { mutableStateOf(settings.backupHour) }
    var backupMinute by remember(settings.backupMinute) { mutableStateOf(settings.backupMinute) }
    var folderName by remember(settings.driveFolderName) { mutableStateOf(settings.driveFolderName) }
    var dateSystem by remember(settings.fileDateSystem) { mutableStateOf(settings.fileDateSystem) }
    var includedMetrics by remember(settings.includedMetrics) {
        mutableStateOf(settings.includedMetrics)
    }
    var timePickerVisible by remember { mutableStateOf(false) }
    var backupDataVisible by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 22.dp, end = 22.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(24.dp))
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close settings")
                }
            }

            Text("SCHEDULE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            SettingsActionCard(
                icon = Icons.Rounded.Schedule,
                title = "Automatic backup",
                value = "%02d:%02d".format(backupHour, backupMinute),
                onClick = { timePickerVisible = true },
            )

            Text("BACKUP DATA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            SettingsActionCard(
                icon = Icons.Rounded.Checklist,
                title = "Included data",
                value = includedMetrics.summaryLabel(),
                onClick = { backupDataVisible = true },
            )

            Text("GOOGLE DRIVE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it.take(MAX_DRIVE_FOLDER_NAME_LENGTH) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Backup folder name") },
                leadingIcon = { Icon(Icons.Rounded.Folder, contentDescription = null) },
                supportingText = {
                    Text(
                        if (folderName.isBlank()) {
                            "Enter a folder name"
                        } else {
                            "Backups will be stored in this Drive folder"
                        },
                    )
                },
                isError = folderName.isBlank(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )

            Text("FILE DATE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DateSystemCard(
                    title = "Jalali",
                    example = DateUtils.jalaliDate(LocalDate.now(DateUtils.HEALTH_ZONE)),
                    selected = dateSystem == FileDateSystem.JALALI,
                    modifier = Modifier.weight(1f),
                    onClick = { dateSystem = FileDateSystem.JALALI },
                )
                DateSystemCard(
                    title = "Gregorian",
                    example = DateUtils.gregorianDate(LocalDate.now(DateUtils.HEALTH_ZONE)),
                    selected = dateSystem == FileDateSystem.GREGORIAN,
                    modifier = Modifier.weight(1f),
                    onClick = { dateSystem = FileDateSystem.GREGORIAN },
                )
            }

            Button(
                onClick = {
                    onSave(
                        BackupSettings(
                            backupHour = backupHour,
                            backupMinute = backupMinute,
                            driveFolderName = folderName,
                            fileDateSystem = dateSystem,
                            includedMetrics = includedMetrics,
                        ),
                    )
                },
                enabled = folderName.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(17.dp),
            ) {
                Text("Save settings", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (timePickerVisible) {
        val timePickerState = rememberTimePickerState(
            initialHour = backupHour,
            initialMinute = backupMinute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { timePickerVisible = false },
            title = { Text("Automatic backup time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        backupHour = timePickerState.hour
                        backupMinute = timePickerState.minute
                        timePickerVisible = false
                    },
                ) { Text("Set time") }
            },
            dismissButton = {
                TextButton(onClick = { timePickerVisible = false }) { Text("Cancel") }
            },
        )
    }

    if (backupDataVisible) {
        BackupDataDialog(
            selected = includedMetrics,
            onDismiss = { backupDataVisible = false },
            onConfirm = {
                includedMetrics = it
                backupDataVisible = false
            },
        )
    }
}

@Composable
private fun BackupDataDialog(
    selected: Set<BackupMetric>,
    onDismiss: () -> Unit,
    onConfirm: (Set<BackupMetric>) -> Unit,
) {
    var draft by remember(selected) { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Backup data") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Choose which health data appears in each backup.",
                    modifier = Modifier.padding(bottom = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BackupMetric.entries.forEach { metric ->
                    val checked = metric in draft
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                draft = if (checked) draft - metric else draft + metric
                            }
                            .padding(horizontal = 4.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                draft = if (checked) draft - metric else draft + metric
                            },
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(metric.title, fontWeight = FontWeight.SemiBold)
                            Text(
                                metric.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(draft) }) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private val BackupMetric.title: String
    get() = when (this) {
        BackupMetric.STEPS -> "Steps"
        BackupMetric.WEIGHT -> "Weight"
        BackupMetric.ACTIVITY -> "Activity"
        BackupMetric.HEART -> "Heart"
        BackupMetric.SLEEP -> "Sleep"
        BackupMetric.SPO2 -> "Blood oxygen"
    }

private val BackupMetric.description: String
    get() = when (this) {
        BackupMetric.STEPS -> "Daily step count"
        BackupMetric.WEIGHT -> "Latest daily weight"
        BackupMetric.ACTIVITY -> "Distance and workouts"
        BackupMetric.HEART -> "Resting and recorded heart rate"
        BackupMetric.SLEEP -> "Sleep sessions and stages"
        BackupMetric.SPO2 -> "Daily SpO₂ summary"
    }

private fun Set<BackupMetric>.summaryLabel(): String = when (size) {
    BackupMetric.entries.size -> "All data"
    0 -> "No data"
    else -> "$size of ${BackupMetric.entries.size}"
}

@Composable
private fun SettingsActionCard(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(13.dp))
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Text(value, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DateSystemCard(
    title: String,
    example: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp),
                )
                Spacer(Modifier.weight(1f))
                RadioButton(selected = selected, onClick = onClick, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(example, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AppHeader(onSettingsClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Icon(
                Icons.Rounded.Sync,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
                    .size(14.dp)
                    .rotate(90f),
            )
        }
        Spacer(Modifier.width(15.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Auto Health Sync",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
            )
            Text(
                "Daily Health data backups to Google Drive",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
        }
        IconButton(onClick = onSettingsClick) {
            Icon(
                Icons.Rounded.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ConnectionsCard(
    healthState: ConnectionState,
    driveState: ConnectionState,
    driveFolderName: String,
    selectedDate: LocalDate,
    dateSelectionEnabled: Boolean,
    onHealthConnect: () -> Unit,
    onDriveConnect: () -> Unit,
    onOpenHealthConnect: () -> Unit,
    onOpenGoogleDrive: () -> Unit,
    onDateChange: (LocalDate) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            ConnectionRow(
                icon = Icons.Rounded.HealthAndSafety,
                title = "Health Connect",
                subtitle = "Read-only daily summaries",
                state = healthState,
                onClick = if (healthState == ConnectionState.CONNECTED) {
                    onOpenHealthConnect
                } else {
                    onHealthConnect
                },
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 18.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            )
            ConnectionRow(
                icon = Icons.Rounded.Cloud,
                title = "Google Drive",
                subtitle = driveFolderName,
                state = driveState,
                onClick = if (driveState == ConnectionState.CONNECTED) {
                    onOpenGoogleDrive
                } else {
                    onDriveConnect
                },
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 18.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            )
            ReportDateSelector(
                selectedDate = selectedDate,
                enabled = dateSelectionEnabled,
                onDateChange = onDateChange,
            )
        }
    }
}

@Composable
private fun ConnectionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    state: ConnectionState,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ConnectionStatus(state = state)
    }
}

@Composable
private fun ConnectionStatus(state: ConnectionState) {
    when (state) {
        ConnectionState.CHECKING -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        ConnectionState.CONNECTED -> Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF20A67A)),
            )
            Spacer(Modifier.width(7.dp))
            Text("Connected", style = MaterialTheme.typography.labelLarge, color = Color(0xFF16805F))
        }
        ConnectionState.ACTION_REQUIRED,
        ConnectionState.UNAVAILABLE,
        -> Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (state == ConnectionState.UNAVAILABLE) "Install" else "Connect")
            Spacer(Modifier.width(3.dp))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ScheduleCard(state: MainUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(19.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Next backup",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                )
                Text(
                    formatScheduled(state.nextBackupEpochMillis),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Last backup",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                )
                Text(
                    state.appState.lastSuccessfulBackupEpochMillis?.let(::formatLastBackup) ?: "Not yet",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportDateSelector(
    selectedDate: LocalDate,
    enabled: Boolean,
    onDateChange: (LocalDate) -> Unit,
) {
    var datePickerVisible by remember { mutableStateOf(false) }
    val today = LocalDate.now(DateUtils.HEALTH_ZONE)
    val selectedDateText = when (selectedDate) {
        today -> "Today · ${selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}"
        today.minusDays(1) -> "Yesterday · ${selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}"
        else -> selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { datePickerVisible = true }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Report date",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                selectedDateText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "Change",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    if (datePickerVisible) {
        val todayUtcMillis = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli(),
            selectableDates = remember(today) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                        utcTimeMillis <= todayUtcMillis

                    override fun isSelectableYear(year: Int): Boolean = year <= today.year
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = { datePickerVisible = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onDateChange(
                                Instant.ofEpochMilli(millis)
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate(),
                            )
                        }
                        datePickerVisible = false
                    },
                    enabled = pickerState.selectedDateMillis != null,
                ) { Text("Use date") }
            },
            dismissButton = {
                TextButton(onClick = { datePickerVisible = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(
                state = pickerState,
                title = {
                    Text(
                        "Choose report date",
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                    )
                },
                headline = null,
                showModeToggle = false,
            )
        }
    }
}

@Composable
private fun BackupAction(
    isBackingUp: Boolean,
    status: String?,
    enabled: Boolean,
    onBackupNow: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = onBackupNow,
            enabled = enabled && !isBackingUp,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(17.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            AnimatedContent(isBackingUp, label = "backup-button") { loading ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(21.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (loading) "Backing up…" else "Back up now",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        AnimatedVisibility(status != null, enter = fadeIn(), exit = fadeOut()) {
            Text(
                status.orEmpty(),
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!enabled && !isBackingUp) {
            Text(
                "Connect both services to enable backups",
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecentActivitySection(entries: List<ActivityEntry>) {
    val listState = rememberLazyListState()
    val newestEntryId = entries.firstOrNull()?.id

    LaunchedEffect(newestEntryId) {
        if (newestEntryId != null) listState.animateScrollToItem(0)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(21.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Recent activity",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "Latest ${entries.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(22.dp),
        ) {
            if (entries.isEmpty()) {
                EmptyActivity()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(entries, key = { it.id }) { entry ->
                        ActivityRow(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyActivity() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Rounded.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text("Your backup activity will appear here", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ActivityRow(entry: ActivityEntry) {
    val (icon, tint) = when (entry.severity) {
        ActivitySeverity.SUCCESS -> Icons.Rounded.Check to Color(0xFF16805F)
        ActivitySeverity.WARNING -> Icons.Rounded.WarningAmber to Color(0xFFA15C00)
        ActivitySeverity.ERROR -> Icons.Rounded.ErrorOutline to MaterialTheme.colorScheme.error
        ActivitySeverity.INFO -> Icons.Rounded.Info to MaterialTheme.colorScheme.tertiary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(18.dp)
                    .offset(
                        y = if (
                            entry.severity == ActivitySeverity.ERROR ||
                            entry.severity == ActivitySeverity.WARNING
                        ) {
                            (-1).dp
                        } else {
                            0.dp
                        },
                    ),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth()) {
                Text(
                    entry.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    Instant.ofEpochMilli(entry.timestampEpochMillis)
                        .atZone(DateUtils.HEALTH_ZONE)
                        .format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            entry.detail?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun formatScheduled(epochMillis: Long): String {
    val scheduled = Instant.ofEpochMilli(epochMillis).atZone(DateUtils.HEALTH_ZONE)
    val today = LocalDate.now(DateUtils.HEALTH_ZONE)
    val day = when (scheduled.toLocalDate()) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> scheduled.format(DateTimeFormatter.ofPattern("MMM d"))
    }
    return "$day, ${scheduled.format(DateTimeFormatter.ofPattern("HH:mm"))}"
}

private fun formatLastBackup(epochMillis: Long): String {
    val backup = Instant.ofEpochMilli(epochMillis).atZone(DateUtils.HEALTH_ZONE)
    val today = LocalDate.now(DateUtils.HEALTH_ZONE)
    val day = when (backup.toLocalDate()) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> backup.format(DateTimeFormatter.ofPattern("MMM d"))
    }
    return "$day, ${backup.format(DateTimeFormatter.ofPattern("HH:mm"))}"
}
