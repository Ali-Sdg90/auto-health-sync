# Architecture

Auto Health Sync is a local-first Android app that creates one health summary per day and stores it in Google Drive.

## Data flow

```text
Health Connect → HealthConnectManager → DailyHealthSummary
               → BackupCoordinator → DriveBackupManager → Google Drive
```

## Main components

| Component | Responsibility |
| --- | --- |
| `MainScreen` / `MainViewModel` | UI, connection state, settings, and manual backups |
| `HealthConnectManager` | Permissions, record reads, and daily aggregation |
| `BackupCoordinator` | Validation, serialization, recovery, and backup orchestration |
| `DriveBackupManager` | Folder discovery and daily file upload/update |
| `AppStateStore` | Persistent settings, backup state, and recent activity |
| `BackupScheduler` / `BackupWorker` | Daily WorkManager execution |

The app has no backend, analytics, or user database. It requests read-only Health Connect access and the narrow Google Drive `drive.file` scope. Scheduling and daily boundaries use `Asia/Tehran`.
