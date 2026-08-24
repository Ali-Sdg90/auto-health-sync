# Privacy

Auto Health Sync processes health data only to create the backups explicitly described in the app.

- Health data is read from Android Health Connect and is never modified or deleted.
- Records available through Health Connect for the granted data types may be included, regardless of which connected app wrote them.
- Raw heart-rate samples, sleep-stage events, routes, GPS points, and other high-frequency measurements are not uploaded. They are reduced to daily summaries in memory.
- Daily summary files are uploaded directly from the device to a folder created by the app in the user's Google Drive.
- The app requests the limited Google Drive `drive.file` scope and cannot access unrelated Drive files.
- Auto Health Sync does not independently store Google OAuth access tokens.
- Recent Activity contains operational messages only and never raw health values.
- There is no developer-operated server, advertising, analytics, telemetry, or sale of data.

The user can revoke Health Connect permissions in system settings and revoke Google access from their Google Account at any time. Uninstalling the app removes its local operational state; existing user-owned Drive backup files remain in the user's Drive until the user deletes them.
