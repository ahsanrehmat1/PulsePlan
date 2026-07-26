# PulsePlan

I built PulsePlan as an Android workout planner with Kotlin and Jetpack Compose.
It creates a daily plan from a user's goal, experience, workout personality,
equipment, schedule, and preferred session length.

**[View my portfolio](https://ahsanrehmat1.github.io)**

![PulsePlan sign-in screen](docs/images/pulseplan-auth.png)

New to the project? Start with my plain-language
[beginner guide](docs/BEGINNER_GUIDE.md).

## Current vertical slice

- Google and email/password sign-in through Firebase Authentication
- Password-reset email flow for returning users
- Email verification status with send, resend, and refresh controls
- Account & Privacy center with plain-language data-use information
- Provider-confirmed account and data deletion
- Firebase Crashlytics reporting for crashes, ANRs, and non-fatal errors
- Public privacy policy and account-deletion request pages
- Timestamped offline-first Firestore sync for profiles, reminders, progress,
  exercise results, and exercise alternatives
- Clear cloud-backup status
- Personalized onboarding
- Six structured movement preferences with automatic exercise filtering
- Clear adjustment reasons while preserving honest completion history
- Deterministic daily and weekly workout generation
- Exercise completion persistence with DataStore
- Editable per-account daily reminder with WorkManager
- Clear recovery path when Android notifications are disabled
- Focused active-workout mode with exercise progression
- Pauseable and skippable rest countdowns
- Active-workout restoration after an app-process interruption
- Set-by-set tracking with editable reps, weight, time, or distance for each set
- Automatic between-set rest plus start, pause, resume, reset, and skip controls
- Exercise-specific countdown timers for timed sets
- Set totals, reps, training volume, timed work, and distance in the session summary
- Optional result entry for reps, weight, time, distance, effort, and notes
- Automatic personal-best detection with previous-result comparison
- Explainable next-target suggestions based on the user's goal and recorded effort
- One-tap use of a small reps, weight, time, or distance progression
- Exercise-specific performance history and six-result trend charts
- Confirmed result deletion for correcting an accidental log
- Interactive 365-day workout calendar with per-date exercise review
- Six-week completion trend, 28-day completion rate, and past-year totals
- Current and best workout streaks with honest recovery-day handling
- Progress milestones based only on recorded workout activity
- Fair workout streaks that ignore recovery days
- Editable plan preferences with immediate workout regeneration
- Step-by-step guides for every generated exercise
- Exact real-video tutorial searches from every exercise guide
- One-tap same-equipment exercise alternatives saved for the current date
- Material 3 Compose interface

## Toolchain

- JDK 17
- Android Studio with Android SDK 36
- Android Gradle Plugin 9.3.1
- Gradle 9.6.1

I do not commit secrets to this repository. Version 0.19.0 is connected on the
current development machine to the Firebase project `pulseplan-503519`.
Email/password registration, sign-out and sign-in, password-reset dispatch,
Firestore upload, and profile restoration after clearing all local app data
have been verified on a clean emulator. Google Sign-In and provider-aware
account deletion are also working on a physical Android device. Version 0.19.0
adds Crashlytics and public Play Store policy pages. A controlled fatal report
was verified in the Firebase dashboard from an SM-N986N. A final destructive
deletion test should use a disposable account before release.

- [Privacy policy](https://ahsanrehmat1.github.io/pulseplan-privacy.html)
- [Account deletion](https://ahsanrehmat1.github.io/pulseplan-delete-account.html)

To connect another development machine or Firebase project, create a Firebase
Android app whose package is
`com.ahsanrehmat.pulseplan`, enable Email/Password and Google under Firebase
Authentication, create a Cloud Firestore database, publish the checked-in
`firestore.rules`, add the app's SHA-1 certificate, and add these values to the
untracked `local.properties` file:

```properties
sdk.dir=C\:\\Users\\YOUR_NAME\\AppData\\Local\\Android\\Sdk
firebase.apiKey=YOUR_WEB_API_KEY
firebase.appId=YOUR_FIREBASE_APP_ID
firebase.projectId=YOUR_FIREBASE_PROJECT_ID
google.webClientId=YOUR_WEB_OAUTH_CLIENT_ID
```

Without these values, account sign-in is unavailable.

## Build

Open the repository in Android Studio, allow Gradle sync to complete, and run
the `app` configuration on an Android 8.0+ device or emulator.

```powershell
./gradlew.bat test
./gradlew.bat assembleDebug
```

See [docs/PRODUCT.md](docs/PRODUCT.md) for product scope and safety boundaries.
