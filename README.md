# Auto Health Sync

Auto Health Sync is a focused Android utility that creates one compact health summary per day from Health Connect and stores it in the user's Google Drive.

It follows one deliberately small pipeline:

**Read → Summarize → Serialize → Upload → Log**

## What version 1 does

- Reads steps, distance, active calories, workouts, heart rate, resting heart rate, sleep, and SpO₂ from Health Connect.
- Filters every read to the Health Sync Android package (`nl.appyhapps.healthsync`), preventing phone-originated steps and unrelated sources from being mixed into the archive.
- Generates `health-data-YYYY-MM-DD.json` using the Persian/Jalali date and includes an unambiguous Gregorian date in the JSON.
- Creates or reuses `Auto: Health Data` in Google Drive using the narrow `drive.file` scope.
- Updates an existing daily file instead of creating duplicates.
- Runs around 23:00 in `Asia/Tehran`, survives process restarts through WorkManager, and repairs its next scheduled job whenever the app opens.
- Retries recoverable automatic failures five times at roughly three-minute intervals.
- Checks only the previous two days for missing backups.
- Keeps the latest 50 operational events locally and sends notifications only for final failures, access problems, and recovered missing days.

The app never writes to Health Connect, never stores raw sensor samples, and has no backend, Firebase, analytics, or user database.

## Requirements

- Android Studio with JDK 17
- Android SDK 36
- Android 9 (API 28) or newer test device
- Health Connect (built into Android 14+, available as an app on compatible older versions)
- Health Sync configured to write Huawei Health data into Health Connect
- A Google Cloud project with the Google Drive API enabled

## Google Cloud setup

Google Drive authorization cannot work until the installed APK is registered with Google:

1. Create or select a project in [Google Cloud Console](https://console.cloud.google.com/).
2. Enable **Google Drive API**.
3. Configure the OAuth consent screen and declare only `https://www.googleapis.com/auth/drive.file`.
4. Create an **Android OAuth client** with package name `com.autohealthsync`.
5. Add the SHA-1 fingerprint of the signing key. For the debug build, run:

   ```powershell
   .\gradlew.bat signingReport
   ```

No OAuth client secret or access token belongs in this repository. Google Identity Services identifies the app from its signed package registration, and the app requests a fresh short-lived access token when each backup runs.

## Build

```powershell
$env:ANDROID_HOME = "C:\Android" # adjust for your SDK
.\gradlew.bat testDebugUnitTest assembleDebug
```

The debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

## First-run checklist

1. Open the app and allow backup-status notifications.
2. Tap **Connect** beside Health Connect and grant all requested read permissions, including background access.
3. Tap **Connect** beside Google Drive and authorize `drive.file` access.
4. Tap **Backup now**.
5. Confirm that `Auto: Health Data/health-data-<Jalali date>.json` exists in Drive and inspect Recent Activity in the app.

Health Connect permissions can be revoked at any time. The app checks access before every operation and fails visibly rather than crashing or silently skipping a backup.

## JSON shape

Sections with no meaningful source data are omitted:

```json
{
    "date": "1405-05-27",
    "dateGregorian": "2026-08-18",
    "steps": 8432,
    "activity": {
        "exerciseMinutes": 52,
        "distanceKm": 6.3,
        "activeCalories": 418.0,
        "workouts": [{ "type": "walking", "durationMinutes": 50 }]
    },
    "heart": { "resting": 61, "average": 72, "min": 48, "max": 137 },
    "sleep": {
        "bedTime": "01:14",
        "wakeTime": "08:42",
        "totalMinutes": 448,
        "deepMinutes": 91,
        "lightMinutes": 240,
        "remMinutes": 78,
        "awakeMinutes": 19
    },
    "spo2": { "average": 97.0, "min": 94.0 }
}
```

## Important release notes

- A Play Store release must complete Google's Health Connect declaration and provide a public privacy policy matching [PRIVACY.md](PRIVACY.md).
- Register the release signing certificate SHA-1 as another Android OAuth client before testing the release build.
- Battery optimization and Doze may delay the 23:00 run. This is intentional: WorkManager reliability is preferred over exact-alarm permissions.
- Real Health Connect and Drive behavior must be validated on a physical device. Unit tests cover deterministic date conversion, scheduling boundaries, the two-day recovery window, and omission of absent JSON metrics.

For the complete product intent and constraints, see [vision.md](vision.md).
