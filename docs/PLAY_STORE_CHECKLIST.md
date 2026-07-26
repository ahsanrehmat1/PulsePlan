# PulsePlan Play Store checklist

## Complete

- Package name: `com.ahsanrehmat.pulseplan`
- Google and email/password accounts
- In-app account and data deletion
- Public privacy policy:
  https://ahsanrehmat1.github.io/pulseplan-privacy.html
- Public account-deletion request:
  https://ahsanrehmat1.github.io/pulseplan-delete-account.html
- Firebase Crashlytics SDK
- Controlled Crashlytics report verified from an SM-N986N
- Unit tests, debug APK build, and Android lint

## Data safety notes

PulsePlan collects these user-provided or app-generated categories:

- Account information: email and Firebase account ID
- Fitness plan information: goals, experience, equipment, schedule, movement
  preferences, and optional movement note
- App activity: workout completion, exercise results, effort, notes, reminders,
  progress, and saved exercise alternatives
- Diagnostics: crashes, ANRs, app version, build type, and device diagnostics

The data is used for account management, app functionality, cloud backup, and
reliability. Firebase Authentication, Cloud Firestore, and Crashlytics process
the relevant data. PulsePlan has no ads and does not sell user data.

Recheck every Play Console Data safety answer against the final release and all
included SDKs before submitting.

## Before internal testing

- Delete one disposable account end to end
- Test offline launch, sign-in errors, and cloud recovery
- Test TalkBack, large text, and a small screen
- Create and securely back up the release signing key
- Build and inspect a signed Android App Bundle
- Prepare the app icon, feature graphic, screenshots, and store copy

## Before production

- Run an internal test
- Collect and resolve tester feedback
- Complete the Play Console Data safety and content forms
- Start the required closed test if the developer account is subject to it
- Review Android vitals and Crashlytics before staged rollout
