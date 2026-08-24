- [x] Add setting for time of the auto backup, defaulting to 11:00 PM.

- [x] Add setting to allow user to choose between Jalali and Gregorian date in the backup file name.

- [x] Add Setting for choose which Health Connect data types to backup, defaulting to all available types.

- [x] Add CI/CD pipeline to build and test the app on every commit.

- [x] Add CI/CD pipeline to build, test, and publish signed APK/AAB artifacts.

- [x] Add an “Open backup folder in Google Drive” action.

- [x] Add an “Open Health Connect” action.

- [x] Add a first-run onboarding checklist for Health Connect, Drive, notifications, and background access.

- [x] Add a system health/status check to warn when Health Connect permissions, Drive authorization, notifications, or background execution are unavailable.

- [x] Add an About section/footer with app version, repository link, privacy policy, and Created by A.S..

- [x] Add a privacy policy to the repository and app.

- [x] Add Apache License 2.0 to the repository.

---

END GAME:

Identity & Signing

- [x] Finalize com.alisadeghi.autohealthsync
- [x] Finalize one release signing key
- [x] Back up keystore + passwords in 2 secure locations
- [ ] ~~Android Developer Verification~~

Website

- [ ] Public homepage on owned/verified domain
- [ ] Public Privacy Policy
- [ ] Add support/contact information

App

- [ ] Add Privacy link inside app
- [ ] Verify final onboarding and backup-data selection

Code & Release

- [ ] Merge final changes
- [ ] Update Release PR / changelog to v1.5.0
- [ ] Build final signed APK/AAB
- [ ] Verify signatures and checksums

Google

- [ ] Configure production Google Cloud project
- [ ] Set OAuth to External / In production
- [ ] Complete branding and support information
- [ ] Register release SHA-1
- [ ] Register Play/Samsung SHA-1 too, if their signing key differs
- [ ] Verify OAuth with the release-signed build

Store Preparation

- [ ] App icon, screenshots and feature graphic
- [ ] Persian and English descriptions
- [ ] Privacy/support URLs
- [ ] Category, age rating and content declarations
- [ ] Data Safety / Health Apps Declaration where required
- [ ] Health Connect permission justifications
- [ ] Reviewer instructions
- [ ] “Not a medical device” disclaimer in store description

QA

- [ ] Clean-install the RELEASE build
- [ ] Test upgrade from the previous release
- [ ] Test scheduled backup after reboot/Doze
- [ ] Test revoked permissions, expired OAuth and offline states
- [ ] Test Android 9/13/14/16
- [ ] Test on Xiaomi and Samsung
- [ ] Clearly document requirement for Google Play services

Then:

- [ ] Publish to stores 🚀
