## Auto Health Sync

Auto Health Sync is a small, reliable Android application that automatically creates a lightweight daily summary of health data stored in Android Health Connect and backs it up to the user's Google Drive.

The application is designed primarily for personal use. Its goal is not to become a full health-tracking application, dashboard, or Health Connect replacement.

Its responsibility is intentionally simple:

**Read → Summarize → Serialize → Upload → Log**

The application should remain small, understandable, maintainable, and reliable.

---

## Core Goal

Every night at approximately **23:00 Asia/Tehran time**, the application should:

1. Read the important health information for the current day from Health Connect.
2. Reduce the data into a small daily summary.
3. Generate one JSON file for that day.
4. Upload the file to Google Drive.
5. Store it inside a folder named:

`Auto: Health Data`

Example:

```text
Auto: Health Data/
├── health-data-1405-05-27.json
├── health-data-1405-05-28.json
├── health-data-1405-05-29.json
└── ...
```

There must **not** be one large cumulative database or JSON file.

Each day gets one independent, small JSON file.

---

## Technology

Build the application as a native Android app using:

- Kotlin
- Jetpack Compose
- Material 3
- Health Connect
- WorkManager
- Google Identity Services authorization
- Google Drive API v3
- Kotlin Coroutines
- Kotlin Serialization

Prefer official Android and Google APIs whenever practical.

Do not use deprecated Google Sign-In APIs.

Do not introduce a backend, Firebase, server, analytics service, or unnecessary infrastructure.

This is a single-user utility and should remain local-first.

---

# Health Data Source

The application reads health information from **Android Health Connect**.

The expected external data pipeline is:

```text
Huawei Watch
    ↓
Huawei Health
    ↓
Health Sync
    ↓
Health Connect
    ↓
Auto Health Sync
    ↓
Google Drive
```

Health Sync and Huawei Health are external dependencies and are **outside the scope of this project**.

This application must never attempt to communicate directly with the Huawei watch or Huawei Health.

The app should only read from Health Connect.

It should never modify, delete, or write health records back into Health Connect.

---

# Health Connect Permissions

Request only the health permissions necessary for the data actually used by the application.

Also request Health Connect background-read access so scheduled backups can run without opening the app manually.

If required Health Connect permissions are missing or revoked:

- Do not crash.
- Record the problem in Recent Activity.
- Clearly show the disconnected/permission-required state in the UI.
- If a scheduled backup cannot run because of missing permissions, notify the user.

Do not request unrelated health permissions.

---

# Daily JSON

The purpose of the JSON is to preserve **important daily health information**, not raw sensor data.

Do not store every heart-rate measurement, every sleep-stage transition, every GPS point, every step record, or other high-frequency raw data.

A typical file should look similar to:

```json
{
    "date": "1405-05-27",
    "dateGregorian": "2026-08-18",

    "steps": 8432,

    "activity": {
        "exerciseMinutes": 52,
        "distanceKm": 6.3,
        "activeCalories": 418,
        "workouts": [
            {
                "type": "walking",
                "durationMinutes": 50
            }
        ]
    },

    "heart": {
        "resting": 61,
        "average": 72,
        "min": 48,
        "max": 137
    },

    "sleep": {
        "bedTime": "01:14",
        "wakeTime": "08:42",
        "totalMinutes": 428,
        "deepMinutes": 91,
        "lightMinutes": 240,
        "remMinutes": 78,
        "awakeMinutes": 19
    },

    "spo2": {
        "average": 97,
        "min": 94
    }
}
```

This is an example schema, not a requirement to populate values that do not exist.

If Health Connect does not contain a meaningful value for an optional metric, omit that field rather than creating misleading data.

---

# Important Data to Preserve

## Steps

Store the daily step count.

The desired source is the **Huawei wearable data transferred through Health Sync**, not steps independently recorded by the phone.

Use Health Connect data-origin information and device metadata where available to avoid mixing unrelated phone step sources into the result.

Do not blindly aggregate step records from every application in Health Connect.

---

## Activity

Preserve useful daily activity summary information:

- Exercise minutes
- Distance
- Active calories
- Workout sessions
- Workout type
- Workout duration

Do not store full GPS routes in the daily JSON.

Do not store raw workout samples unless a future requirement explicitly asks for them.

---

## Heart

Preserve:

- Resting heart rate
- Average heart rate
- Minimum heart rate
- Maximum heart rate

Calculate summaries from available Health Connect information where necessary.

Do not save individual heart-rate samples to Google Drive.

---

## Sleep

Sleep is one of the most important parts of the backup.

Preserve:

- Bedtime
- Wake time
- Total sleep duration
- Deep sleep duration
- Light sleep duration
- REM sleep duration
- Awake duration

For a calendar day's health file, associate the sleep session with the day on which the user woke up.

For example, sleep beginning late on August 17 and ending on the morning of August 18 belongs to the August 18 daily summary.

---

## SpO2

If available, preserve:

- Average SpO2
- Minimum SpO2

If no meaningful SpO2 data exists for that day, omit the section.

---

# Date and Time

Use:

```text
Asia/Tehran
```

as the application's health-day timezone.

Daily boundaries, filenames, displayed backup dates, and scheduled backup calculations should use this timezone.

Use the Persian/Jalali date for filenames:

```text
health-data-1405-05-27.json
```

Also keep the Gregorian date inside the JSON so the data remains machine-friendly and unambiguous.

Do not use Jalali dates internally for timestamp calculations.

---

# Automatic Daily Backup

Schedule one automatic backup for approximately:

```text
23:00 Asia/Tehran
```

every day.

Use Android WorkManager for persistent background execution.

The backup does not need second-level timing accuracy.

Android may delay background work slightly because of operating-system scheduling, battery state, Doze, or other restrictions.

Reliability is more important than exact execution at `23:00:00`.

Do not introduce Exact Alarm permissions or unnecessary background services simply to achieve exact clock timing.

After a daily job completes, ensure the next daily backup remains scheduled.

Use unique work where appropriate so duplicate scheduled jobs cannot accumulate.

---

# Backup Pipeline

The automatic and manual backup operations should share the same core pipeline.

Conceptually:

```text
Start Backup
    ↓
Check Health Connect access
    ↓
Check Google Drive authorization
    ↓
Check previous two days for missing backups
    ↓
Read current day's Health Connect data
    ↓
Build DailyHealthSummary
    ↓
Serialize JSON
    ↓
Upload/update Google Drive file
    ↓
Record success
    ↓
Schedule next automatic backup
```

Keep the individual responsibilities separated enough to test, but do not build unnecessary abstraction layers.

---

# Retry Behavior

Reliability is important.

If a scheduled backup fails because of a temporary problem such as:

- No internet
- Google Drive request failure
- Temporary Health Connect failure
- Temporary Google authorization/token issue
- Other recoverable error

retry the operation.

Desired behavior:

```text
23:00   Initial attempt
23:03   Retry 1
23:06   Retry 2
23:09   Retry 3
23:12   Retry 4
23:15   Retry 5
```

Therefore:

**Initial attempt + maximum 5 retries**

with approximately:

**3 minutes between attempts**

The timing should be treated as best-effort under Android background execution restrictions.

Do not implement a blocking 15-minute loop inside a Worker.

Each retry should be a separately scheduled background attempt or another safe WorkManager-based mechanism.

Persist enough retry state so process death does not reset the attempt count incorrectly.

After the fifth retry fails:

1. Stop retrying that backup automatically.
2. Record the final failure in Recent Activity.
3. Send a device notification explaining that the health backup failed.
4. Keep enough local state that the missing-day recovery system can attempt to recover it later.

---

# Missing-Day Recovery

The application should recover recent missed backups, but it must **not scan the entire history**.

Only inspect the previous **two calendar days**.

Example:

If today is August 18, check:

```text
August 17
August 16
```

Do not automatically inspect August 15 or anything older.

This keeps recovery simple and predictable.

A missing day means the expected daily backup file created by this app does not exist successfully in Google Drive or is not marked as successfully backed up locally.

If a missing file from either of the previous two days is detected:

1. Read that day's data from Health Connect.
2. Build the corresponding daily JSON.
3. Upload it to Google Drive.
4. Record the recovery in Recent Activity.
5. Send a device notification telling the user that a missing daily backup was recovered.

Example notification:

```text
Missing health backup recovered
health-data-1405-05-26.json was created successfully.
```

Only inspect two previous days.

Never perform a full historical scan.

---

# Late Data

Do not implement complex late-data reconciliation.

Once a previous day's backup exists successfully, assume it is good enough.

Do not continuously regenerate past files looking for tiny changes in delayed Health Connect data.

The goal is a practical daily health archive, not exact medical-grade synchronization.

The current day's file may be updated if the user manually backs up earlier in the day and the automatic 23:00 backup later produces a more complete version.

---

# Duplicate Protection

There should only be one file per day.

Before creating:

```text
health-data-1405-05-27.json
```

check whether the application has already created that day's file.

If it exists, update/replace that file rather than creating:

```text
health-data-1405-05-27 (1).json
health-data-1405-05-27 (2).json
```

Manual backup and automatic backup must follow this same rule.

---

# Google Drive

The application needs a simple **Connect Google Drive** flow.

Use current Google Identity Services authorization APIs and request only the Drive access actually required by the application.

Prefer the limited:

```text
drive.file
```

scope rather than broad access to the entire Google Drive.

After authorization:

1. Look for the application's Drive folder if already known/created by the app.
2. Otherwise create:

```text
Auto: Health Data
```

3. Store its Drive file/folder ID locally.
4. Upload daily JSON files into that folder.

If Drive authorization expires or requires user interaction:

- Do not crash.
- Mark Google Drive as requiring attention.
- Record it in Recent Activity.
- Notify the user if an automatic backup cannot proceed.
- Allow the user to reconnect from the app.

Do not request unnecessary Google account information.

---

# Manual Backup

The main screen must include a clear button:

```text
Backup Now
```

This is important for testing and diagnostics.

Pressing it should trigger the same real backup pipeline used by automatic backups.

It should:

- Read real Health Connect data.
- Generate today's JSON.
- Upload/update today's file in Google Drive.
- Check the two previous days for missing backups.
- Write useful Recent Activity entries.
- Display success or failure in the UI.

Do not implement a fake/demo-only backup path.

---

# Local State

The app needs only a small amount of local state, such as:

- Google Drive folder ID
- Last successful backup date/time
- Retry state if required
- Backup status
- Recent Activity entries
- Necessary scheduling state

Avoid introducing a full database unless it becomes genuinely necessary.

A simple persistent local store is preferred.

The health data itself should not be permanently duplicated into a local database.

The primary persistent archive is the daily JSON files in Google Drive.

---

# Recent Activity

The application should have a **Recent Activity** section on the main screen.

This is intentionally a lightweight operational log, not an analytics system.

Examples:

```text
23:04  Backup completed
       health-data-1405-05-27.json

23:03  Retrying backup
       Attempt 2 of 5

23:00  Reading Health Connect

22:59  Scheduled backup started

18:42  Google Drive connected
```

Recovery example:

```text
23:01  Missing backup recovered
       health-data-1405-05-26.json
```

Failure example:

```text
23:15  Backup failed
       Google Drive upload failed after 5 retries
```

Keep only a bounded number of recent entries, for example the latest **50 events**.

Logs should include:

- Timestamp
- Severity/status
- Human-readable description

Do not put raw health values into operational logs.

Useful event categories may internally include:

- Backup started
- Health data read
- JSON created
- Upload started
- Upload succeeded
- Upload failed
- Retry scheduled
- Manual backup
- Missing backup found
- Missing backup recovered
- Health permission missing
- Drive authorization required
- Final backup failure

The UI should display friendly text rather than internal enum names.

---

# Notifications

Create a notification channel specifically for backup status problems/recovery.

Do not send noisy notifications for every normal successful nightly backup.

Send notifications for important situations:

### Final automatic failure

After all retries fail:

```text
Health backup failed
Today's health data could not be backed up.
Open the app for details.
```

### Missing-day recovery

When a previous day's missing backup is successfully created:

```text
Missing health backup recovered
health-data-1405-05-26.json was created successfully.
```

### User action required

If Health Connect permission or Google Drive authorization prevents automatic backups:

```text
Health backup needs attention
Open the app to restore the required access.
```

Keep notifications useful and minimal.

---

# Main UI

The application should have a single polished main screen.

Do not add unnecessary navigation or multiple screens unless authorization flows require them.

Suggested structure:

```text
Auto Health Sync

Health Connect                 Connected ✓
Google Drive                   Connected ✓

Next Backup
Today, 23:00

Last Backup
Yesterday, 23:04 ✓

[ Backup Now ]

──────────────────────────

Recent Activity

23:04   Backup completed
        health-data-1405-05-27.json

23:03   Uploading to Google Drive

23:02   Health data collected

18:42   Google Drive connected
```

Use Material 3 and Jetpack Compose.

The design should be:

- Minimal
- Modern
- Clean
- Calm
- Professionally spaced
- Visually polished enough for portfolio screenshots
- Easy to understand at a glance

Status information should have clear visual hierarchy.

Connection states should be obvious.

The Backup Now action should be prominent but not oversized.

Recent Activity should look polished and readable.

Support dark mode if it can be done naturally through Material theming.

Avoid:

- Excessive gradients
- Complex animations
- Decorative graphics with no purpose
- Bottom navigation with only one meaningful destination
- Unnecessary settings screens
- Dashboard clutter

This is a utility, not a lifestyle health application.

---

# Architecture

Keep the architecture straightforward.

A reasonable structure would be:

```text
ui/
    MainScreen
    MainViewModel

health/
    HealthConnectManager
    HealthSummaryBuilder

drive/
    DriveAuthManager
    DriveBackupManager

backup/
    BackupCoordinator
    BackupWorker
    BackupScheduler

model/
    DailyHealthSummary
    ActivitySummary
    HeartSummary
    SleepSummary
    SpO2Summary
    RecentActivity

storage/
    AppStateStore

notification/
    NotificationManager

util/
    DateUtils
    JalaliDateUtils
```

This structure is only guidance.

Do not create layers or interfaces merely for architectural purity.

Use repositories/interfaces only where they genuinely improve testability or separation.

---

# Error Handling Principles

Every external dependency can fail.

Handle failures from:

- Health Connect
- Missing Health Connect permissions
- Google authorization
- Google Drive API
- Network
- Serialization
- Scheduling
- Date conversion

Errors should never silently disappear.

Every meaningful backup failure should produce:

1. A useful Recent Activity entry.
2. Retry when appropriate.
3. A notification when retries are exhausted or user action is required.

Do not expose stack traces or raw exceptions in the normal UI.

Log technical exceptions through Android logging for development/debugging.

---

# Reliability Rules

Prefer simple defensive behavior over clever behavior.

Important rules:

- One JSON per day.
- One Drive file per day.
- Never create accidental duplicates.
- Never delete Health Connect data.
- Never modify Health Connect data.
- Do not scan more than the previous two days.
- Never silently lose a failed backup.
- Retry temporary failures up to five times.
- Notify after final failure.
- Notify when an old missing day is recovered.
- Manual and automatic backups should use the same pipeline.
- A phone/process restart should not permanently break future scheduled backups.
- Reopening the app should verify that the next scheduled backup still exists.
- Avoid unnecessary permissions.
- Avoid unnecessary services.
- Avoid unnecessary dependencies.

---

# Non-Goals

Do **not** build:

- A backend
- User accounts
- A web application
- Firebase integration
- Health dashboards
- Charts
- Medical advice
- AI analysis
- Cloud database
- Continuous real-time synchronization
- Full historical backup scanning
- Raw sensor archival
- GPS route archival
- Health Connect write support
- Huawei API integration
- Multiple-user support
- Complicated configuration screens

If a feature does not directly help daily backup reliability, observability, or basic usability, question whether it belongs in version 1.

---

# Development Priority

Implement the project incrementally.

## Phase 1 — Health Connect

- Create the Android project.
- Implement the basic Compose UI.
- Request Health Connect permissions.
- Read actual Health Connect records.
- Build the daily summary.
- Display/debug the resulting summary locally.
- Verify step data origin.

## Phase 2 — JSON

- Define the compact daily schema.
- Serialize the summary.
- Generate Jalali filenames.
- Confirm correct day boundaries and sleep association.

## Phase 3 — Google Drive

- Implement Google authorization.
- Create/find `Auto: Health Data`.
- Upload JSON.
- Update an existing daily file instead of duplicating it.
- Handle authorization failures cleanly.

## Phase 4 — Manual Backup

- Connect `Backup Now` to the complete production backup pipeline.
- Test on a real device.
- Confirm the generated Drive file contains correct data.

## Phase 5 — Automation

- Implement approximately-23:00 daily scheduling.
- Add retry behavior.
- Add two-day missing-backup recovery.
- Ensure scheduling survives normal process/device lifecycle events.

## Phase 6 — Observability

- Add Recent Activity.
- Add failure/recovery notifications.
- Make status information clear in the UI.

## Phase 7 — Polish

- Improve spacing, typography, state indicators, loading states, and error states.
- Ensure the app looks clean enough for portfolio screenshots.
- Remove unused code and dependencies.
- Write README setup instructions.

---

# Testing Priorities

Prioritize real-world behavior over excessive unit-test coverage.

At minimum verify:

1. Health Connect is unavailable.
2. Health permission is denied.
3. Health permission is revoked after setup.
4. Health Connect contains no data for a metric.
5. Google Drive is not connected.
6. Drive authorization needs user interaction.
7. Internet is unavailable.
8. Upload fails temporarily.
9. Retry succeeds.
10. All retries fail.
11. Existing daily file is updated rather than duplicated.
12. Yesterday's file is missing and gets recovered.
13. A file older than the two-day recovery window is missing and is intentionally ignored.
14. Manual Backup Now succeeds.
15. The automatic job runs after the app has not been opened recently.
16. Rebooting/restarting the device does not permanently disable future backups.
17. Recent Activity accurately represents what happened.

---

# Coding Style

Favor readable production code over clever abstractions.

Use:

- Kotlin idioms
- Coroutines for asynchronous work
- Immutable UI state where practical
- Clear names
- Small focused functions
- Sealed/state models where they genuinely help
- Comments only where behavior is not obvious

Avoid:

- Premature abstraction
- Generic framework-style architecture
- Excessive interfaces
- Dependency injection frameworks unless there is a clear need
- Huge utility classes
- Deep inheritance
- Over-engineered clean architecture

The entire application should remain understandable to one developer.

---

# Definition of Done

Version 1 is complete when:

1. The user can connect Health Connect.
2. The user can authorize Google Drive.
3. `Auto: Health Data` is created or reused.
4. Backup Now generates a real daily health JSON and uploads it.
5. One small file exists per day.
6. Important health summaries are correct.
7. Phone-originated step data is not unintentionally mixed into the desired wearable step count.
8. Automatic backup runs around 23:00 Asia/Tehran.
9. Temporary failures are retried approximately every three minutes, up to five retries.
10. Final failure generates a notification.
11. Only the previous two days are checked for missing backups.
12. Recovered missing backups generate a notification.
13. Recent Activity clearly shows operational events.
14. The UI is minimal, stable, and polished.
15. The app can reasonably be installed and forgotten while continuing to perform its job.

The central engineering principle is:

> **Keep it simple, keep it observable, and make the backup reliable.**

Do not expand the scope unless a new feature is required to make the core backup workflow work correctly.
