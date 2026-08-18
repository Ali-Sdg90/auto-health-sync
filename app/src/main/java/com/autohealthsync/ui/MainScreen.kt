package com.autohealthsync.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autohealthsync.BuildConfig
import com.autohealthsync.model.ActivityEntry
import com.autohealthsync.model.ActivitySeverity
import com.autohealthsync.model.ConnectionState
import com.autohealthsync.util.DateUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun MainScreen(
    state: MainUiState,
    onHealthConnect: () -> Unit,
    onDriveConnect: () -> Unit,
    onBackupNow: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
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
            item { AppHeader() }
            item {
                ConnectionsCard(
                    healthState = state.healthState,
                    driveState = state.driveState,
                    onHealthConnect = onHealthConnect,
                    onDriveConnect = onDriveConnect,
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
            item { SourceNotice() }
            item { RecentActivitySection(state.appState.recentActivity) }
            item { AppFooter() }
        }
    }
}

@Composable
private fun AppFooter() {
    Text(
        text = "v${BuildConfig.VERSION_NAME} · Created by A.S.",
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
        style = MaterialTheme.typography.labelMedium,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

@Composable
private fun AppHeader() {
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
        Column {
            Text(
                "Auto Health Sync",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
            )
            Text(
                "Quietly backing up your day",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ConnectionsCard(
    healthState: ConnectionState,
    driveState: ConnectionState,
    onHealthConnect: () -> Unit,
    onDriveConnect: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
            ConnectionRow(
                icon = Icons.Rounded.HealthAndSafety,
                title = "Health Connect",
                subtitle = "Read-only daily summaries",
                state = healthState,
                onClick = onHealthConnect,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
            ConnectionRow(
                icon = Icons.Rounded.Cloud,
                title = "Google Drive",
                subtitle = "Auto: Health Data",
                state = driveState,
                onClick = onDriveConnect,
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
            .padding(vertical = 14.dp),
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
        ConnectionStatus(state = state, onClick = onClick)
    }
}

@Composable
private fun ConnectionStatus(state: ConnectionState, onClick: () -> Unit) {
    when (state) {
        ConnectionState.CHECKING -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        ConnectionState.CONNECTED -> Row(verticalAlignment = Alignment.CenterVertically) {
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
        -> TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 10.dp)) {
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

@Composable
private fun BackupAction(
    isBackingUp: Boolean,
    status: String?,
    enabled: Boolean,
    onBackupNow: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                    Spacer(Modifier.width(10.dp))
                    Text(if (loading) "Backing up…" else "Backup now", fontWeight = FontWeight.SemiBold)
                }
            }
        }
        AnimatedVisibility(status != null, enter = fadeIn(), exit = fadeOut()) {
            Text(
                status.orEmpty(),
                modifier = Modifier.padding(top = 9.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!enabled && !isBackingUp) {
            Text(
                "Connect both services to enable backups",
                modifier = Modifier.padding(top = 9.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SourceNotice() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.58f))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Rounded.Shield,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(19.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "Data is read from all sources available in Health Connect. Manage connected apps and stored data in Health Connect settings.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
        )
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
                    .offset(y = if (entry.severity == ActivitySeverity.ERROR) 1.dp else 0.dp),
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
