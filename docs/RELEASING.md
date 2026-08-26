# CI/CD and GitHub releases

Health Data Relay uses GitHub Actions, Conventional Commits, and Release Please. Builds from pull requests never receive signing credentials. Only a GitHub Release created from a reviewed release PR can start the signed publication job.

## Pipeline

1. A pull request targets `main`.
2. `CI / Build`, `CI / Dependency review`, and `PR title / Conventional PR title` must pass.
3. The pull request is squash-merged. Its title becomes the Conventional Commit on `main`.
4. Release Please opens or refreshes one release PR with the next version and `CHANGELOG.md`.
5. Merging the release PR creates the version tag and GitHub Release.
6. `Publish GitHub release` checks out that immutable tag, repeats tests and release lint, builds and verifies signed APK/AAB files, attests their provenance, and attaches them to the release with SHA-256 checksums and the R8 mapping file.

The publication workflow can also be rerun manually with `workflow_dispatch` and an existing tag such as `v1.5.0`. Existing assets are replaced, which makes recovery from a transient runner or upload failure straightforward.

The initial bootstrap starts after the `1.3.0` commit. The already completed `feat` commit for `1.4.0` is therefore included in the first generated release PR, producing the first GitHub Release as `v1.4.0` without replaying the older project history.

## Versioning

`APP_VERSION_NAME` in `gradle.properties` is the only maintained version value. Release Please updates it in the release PR.

Android requires an increasing integer `versionCode`. Gradle derives it deterministically from stable SemVer:

```text
versionCode = major * 1,000,000 + minor * 1,000 + patch
```

For example, `1.5.2` becomes `1005002`. Minor and patch values must each stay below 1000. Pre-release versions such as `1.6.0-beta.1` are intentionally rejected until a separate prerelease channel is designed.

Release behavior:

| Conventional type | Example | Version effect |
| --- | --- | --- |
| `fix` | `fix(backup): retry expired Drive authorization` | patch |
| `feat` | `feat(settings): select backup fields` | minor |
| breaking | `feat(storage)!: replace the backup schema` | major |
| `perf`, `revert` | `perf(health): reduce record reads` | patch |
| `docs`, `test`, `ci`, `build`, `refactor`, `chore` | `ci: pin GitHub Actions` | no release alone |

## One-time GitHub setup

### 1. Create and back up the release key

Run this outside the repository and choose strong, unique passwords:

```powershell
keytool -genkeypair -v `
  -keystore auto-health-sync-release.jks `
  -alias auto-health-sync `
  -keyalg RSA `
  -keysize 4096 `
  -validity 10000
```

Back up the keystore and passwords in at least two secure locations. Losing this key prevents future updates signed as the same Android application. Never commit it; `.gitignore` ignores `.jks` and `.keystore` files.

### 2. Create the protected release environment

In **Repository settings → Environments**, create an environment named `github-release`. Store these environment secrets in it:

| Secret | Value |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | Base64 representation of the complete keystore file |
| `RELEASE_KEYSTORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Key alias, for example `auto-health-sync` |
| `RELEASE_KEY_PASSWORD` | Private-key password |

PowerShell can encode the keystore without modifying it:

```powershell
[Convert]::ToBase64String(
  [IO.File]::ReadAllBytes((Resolve-Path .\auto-health-sync-release.jks))
) | Set-Clipboard
```

Paste the clipboard value into `RELEASE_KEYSTORE_BASE64`. Do not print it in logs or save it in the repository.

### 3. Create the Release Please token

Create a fine-grained personal access token restricted to this repository with:

- Contents: read and write
- Pull requests: read and write
- Issues: read and write

Store it as the repository secret `RELEASE_PLEASE_TOKEN`. A separate token is used instead of the workflow's default token so CI and PR-title workflows also run on the automatically created release PR and the `release.published` event starts the publication workflow.

Give the token an expiry date and rotate it before expiry. A dedicated GitHub App can replace this PAT later without changing the release model.

### 4. Protect `main`

Under **Repository settings → Security → Code security**, enable the dependency graph, Dependabot alerts, and Dependabot security updates so dependency review and automated update PRs have complete repository data.

After the workflows have run once, configure a repository ruleset for `main`:

- Require a pull request before merging.
- Require `Build`, `Dependency review`, and `Conventional PR title` status checks.
- Require branches to be up to date before merging.
- Require conversation resolution.
- Require linear history.
- Block force pushes and branch deletion.
- Do not allow bypassing for normal development.

Under **Repository settings → General → Pull Requests**:

- Enable only **Squash merging**.
- Use the pull request title as the default squash commit title.
- Automatically delete head branches after merge.

The Release Please PR itself must pass the same checks and be merged normally.

### 5. Register the release certificate with Google

Display the release certificate fingerprint:

```powershell
keytool -list -v `
  -keystore .\auto-health-sync-release.jks `
  -alias auto-health-sync
```

Register its SHA-1 as an Android OAuth client for package `com.alisadeghi.autohealthsync` in the Google Cloud project. Without this registration, Google Drive authorization will fail in the signed release even if debug builds work.

## Release assets

Each GitHub Release receives:

- `auto-health-sync-vX.Y.Z.apk` — signed, directly installable Android package
- `auto-health-sync-vX.Y.Z.aab` — signed app bundle for future store distribution
- `auto-health-sync-vX.Y.Z-mapping.txt` — R8 mapping for deobfuscating release crashes
- `SHA256SUMS.txt` — checksums for the three files
- GitHub build-provenance attestations for the APK and AAB

The workflow verifies the APK with Android `apksigner` and verifies the AAB JAR signature before upload. A missing secret, mismatched tag/version, absent output, invalid signature, failed test, lint error, or R8/build failure stops publication.

## Action maintenance

All third-party actions are pinned to immutable commit SHAs. Dependabot checks both Gradle dependencies and GitHub Actions weekly; action-update PRs preserve the immutable pin while moving it to the reviewed release SHA.
