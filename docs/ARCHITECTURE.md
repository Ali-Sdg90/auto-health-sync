# Architecture

Auto Health Sync is an Android app that creates one health summary per day and stores it in Google Drive.

## Data flow

```text
Health Connect → HealthConnectManager → DailyHealthSummary
               → BackupCoordinator → DriveBackupManager → Google Drive
```

## Main components

| Component | Responsibility |
| --- | --- |
| `MainScreen` / `MainViewModel` | Gated onboarding, UI state, settings, and manual backups |
| `BackgroundAccessManager` | Battery-optimization status and supported OEM Auto Start settings |
| `HealthConnectManager` | Permissions, record reads, and daily aggregation |
| `BackupCoordinator` | Validation, selected-metric serialization, recovery, and backup orchestration |
| `DriveBackupManager` | Folder discovery and daily file upload/update |
| `AppStateStore` | Persistent settings, backup state, and recent activity |
| `BackupScheduler` / `BackupWorker` | Daily WorkManager execution |

The app has no backend, analytics, or user database. It requests read-only Health Connect access and the narrow Google Drive `drive.file` scope. Automatic work is scheduled only after required onboarding is complete. Scheduling and daily boundaries use `Asia/Tehran`.
