# PulsePlan: start-to-finish guide

This guide explains PulsePlan from the original idea to the current Android
app. It is written for someone with no Android-development experience.
You do not need to memorize it. Use it as the project's map whenever something
is unclear.

## 1. Why we are building PulsePlan

Many people want to exercise but get stuck on three questions:

1. What workout should I do today?
2. How can I make it fit my available time, equipment, and experience?
3. How can I stay consistent and see what I have completed?

PulsePlan is meant to answer those questions in one place. A user creates an
account, answers a short set of fitness questions, receives a daily and weekly
plan, checks off exercises, and receives a daily reminder.

The three most important product areas are:

- Personalized workout plans based on the user's goal, experience, preferred
  coaching style, equipment, weekly availability, and session length.
- A clear daily workout with individual exercises and completion tracking.
- Daily reminders and a history of the user's activity and progress.

PulsePlan requires an account and uses the internet for cloud backup. Version
0.19.0 supports Google and email/password accounts, cloud backup,
email-verification controls, account/privacy management, and crash reporting.

## 2. Why Android, Kotlin, and Jetpack Compose

PulsePlan is an Android application.

- **Kotlin** is the programming language used for the app.
- **Android Studio** is the program used to open, edit, build, and test it.
- **Jetpack Compose** is the system used to draw the screens and buttons.
- **Firebase Authentication** provides Google and email/password accounts.
- **DataStore** currently saves the profile, dated exercise completion history,
  and the selected reminder time on the device.
- **WorkManager** schedules the daily workout reminder.

Think of Android Studio as the workshop, Kotlin as the written instructions,
and the emulator as a pretend Android phone used for testing.

## 3. What PulsePlan includes

The current flow is:

```text
Open app
  -> Continue with Google / Sign in / Create account
  -> Answer onboarding questions
  -> Generate today's workout and this week's schedule
  -> Log optional exercise results and check exercises as completed
  -> Save the profile, dated progress, and active workout position
  -> Review the calendar, trends, totals, streaks, and milestones
  -> Review personal bests and exercise-specific result charts
  -> Receive an optional, explained next target from the last recorded effort
  -> Update plan preferences when goals or availability change
  -> Schedule a daily reminder
```

The app currently includes:

- A polished sign-in and account-creation screen.
- An Account & Privacy screen for email verification, password reset, sign-out,
  plain-language data use, and protected account/data deletion.
- Seven onboarding questions.
- Four fitness goals.
- Three experience levels.
- Four workout personalities.
- Bodyweight, dumbbell, and full-gym equipment choices.
- Two to five training days per week.
- Session choices of 15, 30, 45, or 60 minutes.
- Six optional movement preferences: gentle transitions, no floor exercises,
  limited overhead movement, limited wrist loading, no kneeling, and extra
  balance support.
- Strength, conditioning, mobility, and recovery workouts.
- Four to six exercises depending on session length.
- A seven-day plan preview.
- Exercise checkboxes and a progress bar.
- A focused active-workout screen with complete and skip controls.
- A rest countdown that can be paused, resumed, or skipped.
- Separate tracking for every planned set, with an editable last set.
- Automatic rest between sets and between exercises.
- Start, pause, resume, and reset controls for timed exercise sets.
- Restoration of the active exercise, current set, entered target, completed
  sets, and timers after an app interruption.
- A session summary for sets, reps, training volume, timed work, and distance.
- Optional logging of reps, weight, time, distance, effort, and workout notes.
- Previous-result comparison and automatic personal-best detection.
- Goal-aware next-target suggestions that explain whether to progress, repeat,
  or ease back based only on the user's recorded result and effort.
- One-tap use of the suggested reps, weight, time, or distance target.
- A Performance Log with exercise-specific history and six-result trend charts.
- Confirmed deletion for correcting an accidentally logged result.
- Local saving of the profile, dated completed exercises, and performance data.
- A Progress Insights screen with an interactive 365-day calendar and
  per-date exercise review.
- A six-week completion trend, honest 28-day completion rate, past-year workout
  and exercise totals, current and best streaks, and recorded milestones.
- A fair workout streak that ignores recovery days and leaves today pending.
- An editable Plan Preferences screen that rebuilds the daily and weekly plan.
- Automatic replacement of conflicting exercises with a visible
  **PLAN ADJUSTMENT** reason. The original workout slot is retained internally
  so completed progress remains honest.
- A written Exercise Guide for every generated, alternative, and automatically
  adjusted movement, available from the dashboard and Active Workout.
- A real-video tutorial search tailored to every exercise guide.
- A **Swap** action with one curated same-equipment alternative for every
  movement. Today's choice is saved, can be restored, and keeps the original
  workout slot for honest completion history.
- A notification permission request and an editable daily reminder schedule.
- Light and dark themes.

## 4. What remains before public release

- Google and email/password sign-in are active through Firebase.
- Profiles and workout data remain available locally and are backed up to
  Firestore for signed-in users. A clean-app restore test has verified that the
  saved profile returns after local app data is removed.
- Email verification and account/data deletion are implemented.
  Disposable-account deletion was verified end to end across Authentication,
  Firestore, local data, and scheduled work.
- Structured movement preferences automatically filter known movement demands.
  The optional free-text note is saved for the user's reference and is not
  interpreted as a diagnosis or as an automatic medical restriction.
- Every written guide can open an exact real-video search, but videos are not
  embedded inside PulsePlan yet. Embedded professional clips require licensed
  video content or videos recorded and owned by the app team.
- There are unit tests for plan generation, reminder scheduling, and workout
  session behavior, but no automated Compose UI tests yet.
- The release App Bundle builds successfully. Secure release signing and Play
  Console preparation are still required.

## 5. How the app works internally

The main pieces pass information to each other like this:

```text
What the user taps or types
            |
            v
PulsePlanApp.kt draws the screens
            |
            v
PulsePlanViewModel.kt decides the current app state
       /            |                 \
      v             v                  v
AuthRepository   PlanGenerator     UserPreferences
accounts         workouts          local saved data
                                      |
                                      v
                              WorkoutReminder
                              daily notification
```

An example:

1. The user chooses "Build strength," "Beginner," and "Dumbbells."
2. `PulsePlanApp.kt` sends those answers to the ViewModel.
3. The ViewModel saves the profile with `UserPreferences`.
4. `PlanGenerator` selects the correct workout and adjusts it for the user's
   experience, personality, session length, and day of the week.
5. The ViewModel gives the result back to the UI.
6. Compose redraws the dashboard with the workout.

## 6. Opening the project

The project folder on this computer is:

```text
C:\Users\PIRATE\Documents\App Development
```

To open it:

1. Start Android Studio.
2. Choose **Open**.
3. Select the `App Development` folder, not only the `app` subfolder.
4. Wait for Gradle Sync and indexing to finish.
5. If Android Studio asks whether to trust the project, choose **Trust
   Project** because this is our own source code.

Do not start editing while Gradle Sync is still running. The progress appears
near the bottom of Android Studio.

### Required Java version

The project requires Java 17 or newer. In Android Studio, open:

**File > Settings > Build, Execution, Deployment > Build Tools > Gradle**

Set **Gradle JDK** to Android Studio's Embedded JDK or another JDK 17+.

The normal Windows terminal on this computer may still find Java 8 first. If a
terminal build says `Gradle requires JVM 17 or later`, the app is not broken;
the terminal is using the wrong Java installation.

## 7. Running the app on the emulator

A virtual Pixel phone named `PulsePlan_API_36` has already been created.

1. In Android Studio's top toolbar, select the **app** run configuration.
2. Open the device dropdown next to it.
3. Select **PulsePlan_API_36**.
4. Click the green **Run** triangle or press **Shift + F10**.
5. Wait for the emulator to start.
6. Android Studio will build, install, and open PulsePlan automatically.

If the virtual phone is not listed:

1. Open **Tools > Device Manager**.
2. Find `PulsePlan_API_36`.
3. Click its play triangle.
4. Wait until the Android home screen appears.
5. Return to the top device dropdown and run the app.

The emulator is the quickest way to run the whole app. The project
does not yet contain individual `@Preview` functions for Android Studio's
Compose Design panel.

## 8. Walking through the app as a new user

Use this path when checking the app:

1. Open PulsePlan.
2. Continue with Google, or sign in with a test email account.
3. Enter a name.
4. Choose a fitness goal.
5. Choose Beginner, Intermediate, or Advanced.
6. Choose the workout personality that best describes the user.
7. Choose the available equipment.
8. Choose realistic training days and session time.
9. Optionally select one or more movement preferences. For a clear test,
    select **No floor exercises**, and optionally enter a personal note.
10. Tap **Build my first plan**.
11. Allow notifications when Android asks.
12. Check that the dashboard shows the chosen name, days, and session length.
    If a movement preference was selected, confirm conflicting exercises show
    **PLAN ADJUSTMENT**, a reason, and a disabled **Adjusted** action.
13. Open **Plan preferences** and confirm every saved answer is already
    selected, then use the back arrow without changing anything.
14. Open **Progress insights** and confirm the weekly trend, totals, workout
    calendar, milestones, and recent days appear. Select an earlier calendar
    date and confirm its workout exercises and completion state are shown, then
    use the back arrow.
15. Tap **How to do it** on a dashboard exercise. Confirm its **Find video on
    YouTube** action, target area, equipment, three steps, coaching cue, common
    mistake, and safety note appear. Tap the video action and confirm an exact
    tutorial search opens in YouTube or the browser.
16. Return and tap **Swap** on an unfinished exercise. Confirm its curated
    alternative appears with **TODAY'S ALTERNATIVE**, **How to do it**, and
    **Restore**. Reopen the app to confirm the alternative remains, then restore
    it if this was only a test.
17. Check one exercise. The completed count and progress bar should
    change.
18. Start the set-by-set workout, open **How to do it**, return, and confirm the
    same exercise and set remain active. Confirm the planned target is already
    filled, adjust it if needed, choose an effort, add a short note, and tap
    **Complete set 1**. Confirm the automatic rest screen identifies set 2.
    Test pause, resume, skip rest, and **Undo and edit set 1**.
19. For a timed exercise, start, pause, resume, and reset the exercise timer.
    Close the app during an unfinished set or rest period, reopen it, and
    confirm the same exercise, set values, completed
    sets, and timer position return.
20. Open **Performance log** and confirm the logged result, personal best,
    exercise selector, trend chart, effort, note, and explained **Next target**
    appear. Start that unfinished exercise again and confirm **Use suggested
    suggestion for this set** fills the recommendation without forcing the user
    to accept it. Confirm the history card shows a **SET BREAKDOWN**.
21. Open **Progress insights** again and confirm today's calendar entry, totals,
    and recent activity updated.
22. Return to **Plan preferences**, change one preference, save, and confirm the
    dashboard plan rebuilds. Restore the original preference if this was only a
    test.
23. Scroll down to inspect all exercises and the weekly plan.
24. Close and reopen the app and confirm that the profile and checked exercises
    remain.
25. Open **Account & privacy**, then sign out.

The workout can say **Recovery reset** on a non-training day. That is expected;
the weekly schedule deliberately includes recovery days.

## 9. Understanding the project folders

In Android Studio's left **Project** panel, switch to the **Project** view when
you want to see the real folder structure.

```text
App Development
|-- app
|   |-- src
|   |   |-- main
|   |   |   |-- java/com/ahsanrehmat/pulseplan
|   |   |   |-- res
|   |   |   `-- AndroidManifest.xml
|   |   `-- test
|   `-- build.gradle.kts
|-- docs
|-- build.gradle.kts
|-- settings.gradle.kts
`-- README.md
```

The most important files are:

| File | Purpose |
| --- | --- |
| `MainActivity.kt` | Starts the visible Android app. |
| `PulsePlanApplication.kt` | Initializes Firebase when configured and creates the notification channel. |
| `ui/PulsePlanApp.kt` | Contains the sign-in, onboarding, dashboard, exercise cards, and weekly UI. |
| `ui/AccountScreen.kt` | Shows email verification, password reset, privacy information, sign-out, and protected account/data deletion. |
| `ui/ActiveWorkoutScreen.kt` | Runs set-by-set entry, exercise timers, between-set rest, undo/edit, interruption recovery, and the measurable session summary. |
| `ui/ExerciseGuideScreen.kt` | Shows written setup, movement, form, and safety guidance for one exercise. |
| `ui/ProgressHistoryScreen.kt` | Shows the calendar, weekly trend, totals, streaks, milestones, selected-date exercise review, and recent history. |
| `ui/PerformanceHistoryScreen.kt` | Shows personal bests, exercise-specific result history, notes, effort, and trend charts. |
| `ui/PulsePlanViewModel.kt` | The app's coordinator: sign-in, onboarding, workout sessions, results, and completion state. |
| `model/WorkoutModels.kt` | Defines goals, experience levels, personalities, equipment, movement preferences, profiles, workouts, and stable exercise-slot identity. |
| `model/ExerciseResult.kt` | Defines typed exercise and per-set reps, weight, time, distance, effort, notes, and drafts. |
| `domain/PlanGenerator.kt` | Generates daily and weekly plans, filters movement demands, explains automatic adjustments, and provides curated alternatives. |
| `domain/ExerciseGuideCatalog.kt` | Stores the written guide for every exercise the plan generator can use. |
| `domain/ExerciseVideoGuideCatalog.kt` | Maps every exercise to an exact real-video tutorial search. |
| `domain/WorkoutSession.kt` | Contains the testable rules for set progression, set/exercise rest, timers, undo, restoration, skip, and completion. |
| `domain/WorkoutPrescription.kt` | Safely reads planned set counts and rep, time, or distance targets from an exercise prescription. |
| `domain/ProgressTracker.kt` | Calculates calendar history, trends, totals, completion rate, milestones, and fair current/best streaks. |
| `domain/PerformanceTracker.kt` | Calculates previous results, personal bests, exercise histories, chart values, and display labels. |
| `domain/ProgressionCoach.kt` | Turns an explicit result and effort rating into a small, explained, optional next target. |
| `data/UserPreferences.kt` | Saves profiles, reminders, active sessions, dated completion, results, alternatives, and sync timestamps with DataStore. |
| `data/AuthRepository.kt` | Handles Firebase registration, sign-in, sign-out, and password-reset email. |
| `data/CloudSyncModels.kt` | Defines timestamped cloud records, safe decoding, and newest-change conflict rules. |
| `data/CloudSyncRepository.kt` | Reads and writes each signed-in user's private Firestore profile and dated workout records. |
| `firestore.rules` | Restricts every user's cloud records to that authenticated user. |
| `notifications/WorkoutReminder.kt` | Creates and schedules the daily workout notification. |
| `ui/theme/Color.kt` | Stores the main app colors. |
| `ui/theme/Theme.kt` | Applies light and dark color schemes. |
| `AndroidManifest.xml` | Declares the app, activity, internet access, and notification permission. |
| `app/build.gradle.kts` | App version, Android versions, dependencies, tests, and Firebase build values. |
| `PlanGeneratorTest.kt` | Tests important workout-generation rules. |
| `WorkoutSessionTest.kt` | Tests set/exercise progression, timers, rest, undo, completion, and interrupted-session restoration. |
| `WorkoutPrescriptionTest.kt` | Tests rep, seconds, minutes, and safe fallback prescription parsing. |
| `ProgressTrackerTest.kt` | Tests weekly trends, totals, completion rate, calendar details, recovery days, partial progress, and streak rules. |
| `PerformanceTrackerTest.kt` | Tests personal-best rules, result ordering, comparisons, and effort-only entries. |
| `ProgressionCoachTest.kt` | Tests goal-aware progression, repeat, ease-back, and no-invented-target rules. |
| `ExerciseGuideCatalogTest.kt` | Ensures every generated movement has a complete written guide. |

Files inside `app/build`, `.gradle`, or `.idea` are generated by tools. Do not
edit them to make a product change because they can be replaced automatically.

## 10. Where to make common changes

### Change visible words, headings, or button labels

Most current screen text is in:

```text
app/src/main/java/com/ahsanrehmat/pulseplan/ui/PulsePlanApp.kt
```

Use **Ctrl + F** to search for the exact words visible on the screen. Change
only the quoted text, run the app, and inspect the result.

### Change the main colors

Open:

```text
app/src/main/java/com/ahsanrehmat/pulseplan/ui/theme/Color.kt
```

The main brand color is `PulseGreen`. Colors use hexadecimal values such as
`0xFFB8F34A`. Change one color at a time because color contrast affects
readability.

### Change goals, levels, personalities, or equipment labels

Open:

```text
app/src/main/java/com/ahsanrehmat/pulseplan/model/WorkoutModels.kt
```

For a line like:

```kotlin
BUILD_STRENGTH("Build strength"),
```

the uppercase part is the internal identifier. The quoted part is what the
user sees. Changing only the quoted text is safer.

### Add or edit exercises

Open:

```text
app/src/main/java/com/ahsanrehmat/pulseplan/domain/PlanGenerator.kt
```

This file contains separate lists for bodyweight strength, dumbbell strength,
gym strength, conditioning, and mobility. Every exercise has:

- a stable internal ID;
- a visible name;
- sets, repetitions, or time;
- a coaching instruction.

Exercise IDs must stay unique. Changing an ID can make previously saved
completion data stop matching that exercise.

### Change which weekdays are training days

In `PlanGenerator.kt`, find `workoutIndexes`. The numbers represent:

```text
0 Monday
1 Tuesday
2 Wednesday
3 Thursday
4 Friday
5 Saturday
6 Sunday
```

For example, the current three-day plan uses Monday, Wednesday, and Friday.

### Change the reminder

On the dashboard, tap **Change** in the **Daily reminder** card and select a
time. PulsePlan saves the hour and minute for the current account and
reschedules the WorkManager job. The default remains 7:00 AM.

If Android notifications are off, the card explains the problem and provides
an **Open notification settings** action.

The scheduling implementation and notification text are in:

```text
app/src/main/java/com/ahsanrehmat/pulseplan/notifications/WorkoutReminder.kt
```

Android may deliver scheduled background work a little after the requested
time to protect battery life. It should not be treated like an exact alarm.

### Change the app name

The launcher name is in:

```text
app/src/main/res/values/strings.xml
```

The visible PULSEPLAN headings in the screens are currently written directly
inside `PulsePlanApp.kt`, so both locations must be updated during a rename.

### Change how information is saved

Local profile and exercise completion storage is in `UserPreferences.kt`.
Account behavior is in `AuthRepository.kt`. These are higher-risk changes
because they affect existing user data. Back up and test before editing them.

## 11. The safe change-and-test routine

Use this routine for every change, even a small one:

1. Write down the single result you want.
2. Find the responsible file using the table above.
3. Change one small thing.
4. Check Android Studio's red error markers.
5. Choose **Build > Make Project** or press **Ctrl + F9**.
6. Run the app with **Shift + F10**.
7. Visit both the changed screen and the screens before and after it.
8. Test light and dark mode if colors or layout changed.
9. Test a small and a long piece of text if wording changed.
10. Save the working version in Git before starting the next change.

Do not make ten unrelated changes and test them all at the end. Small changes
make mistakes much easier to find and reverse.

### Useful build checks

From a PowerShell terminal that is using Java 17+, run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

This runs the current unit tests, builds the debug APK, and checks the source
for Android problems.

The APK is created at:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## 12. Error colors and Android Studio messages

- A **red underline** usually means the code cannot compile.
- A **yellow underline** is usually a warning or improvement suggestion.
- The **Build** window shows which file and line caused a build failure.
- **Logcat** shows messages from the running app and Android device.

When an error appears:

1. Read the first meaningful error, not only the final `BUILD FAILED` line.
2. Double-click the file link in the Build window.
3. Check the line above and below the highlighted code.
4. Undo the latest small edit with **Ctrl + Z** if the cause is unclear.
5. Build again.

## 13. Connecting real accounts with Firebase

Version 0.16.0 connected PulsePlan to the real `pulseplan-503519` Firebase
project. Version 0.18.0 added Google Sign-In. Version 0.19.0 adds Crashlytics.

The broad process is:

1. Create a Firebase project for PulsePlan.
2. Add an Android app with package name
   `com.ahsanrehmat.pulseplan`.
3. Enable Email/Password and Google in Firebase Authentication.
4. Add the debug or release SHA-1 certificate to the Android app.
5. Create a Cloud Firestore database.
6. Publish the checked-in `firestore.rules`. These rules allow a signed-in user
   to access only `users/<their Firebase uid>` and its `days` subcollection.
7. Put the API key, app ID, project ID, and Web OAuth client ID into the untracked
   `local.properties` file using the names documented in `README.md`.
8. Rebuild the app.
9. Test Google sign-in, email registration, sign-out, password reset, invalid
   passwords, duplicate emails, and app restart.
10. Change the profile, reminder, one completed exercise, and one alternative.
    Sign into the same account on a second device and verify all four appear.
11. Make different changes on both devices and confirm the latest timestamp
    wins without losing the other records.
12. Open **Account & privacy**, send a verification email, open its link, and
    refresh the verification status.
13. With a disposable account only, type **DELETE**, confirm with its sign-in
    provider, and verify the account, cloud history, local cache, and scheduled
    reminder are removed.

Never paste Firebase values, passwords, signing keys, or other secrets into a
Kotlin file, Git commit, screenshot, or public message. `local.properties` is
ignored by Git for this reason.

Firebase Authentication provides identity. Firestore stores the user's
profile, reminder, completion history, exercise results, and substitutions. DataStore remains the
fast local source so a failed connection does not erase progress. The dashboard
shows whether data is syncing, backed up, or saved locally.

## 14. Git is the project's safety net

Git records working versions of the code so a bad change can be compared,
reversed, or moved to another computer.

The repository currently exists, but the first baseline commit has not been
created yet. Before major upgrades:

1. Review which files will be included.
2. Confirm `local.properties`, `.idea`, `.tooling`, build folders, and secrets
   are ignored.
3. Create a baseline commit containing the known-working app.
4. Create a new branch for each meaningful feature.
5. Commit only after building and testing.

A commit is a saved checkpoint, not the same as publishing the app.

## 15. How we should collaborate

For every feature, agree on these points before coding:

1. **Problem:** What user difficulty are we solving?
2. **User story:** What should the user be able to do?
3. **Screen behavior:** What should appear before, during, and after the action?
4. **Data:** What must be saved, and where?
5. **Unhappy paths:** What happens if input is missing, internet is unavailable,
   or a request fails?
6. **Definition of done:** What exact tests prove the feature is finished?

The editable reminder delivered in version 0.2.0 used this specification:

```text
Problem:
Users may not want reminders at 7:00 AM.

User story:
As a user, I can choose my reminder time.

Data:
Save the chosen hour and minute for that account.

Unhappy path:
If notifications are denied, explain how to enable them.

Done:
The chosen time survives restart and a reminder is scheduled for it.
```

This prevents design, code, and expectations from moving in different
directions.

## 16. Recommended upgrade order

Build in this order so later work rests on a stable base:

### Phase 1: protect and stabilize

- Create the first Git commit.
- Add automated tests for authentication screens, onboarding, dashboard, and
  exercise completion.
- Break the large `PulsePlanApp.kt` file into smaller screen and component
  files.
- Add individual Compose previews for important components.

### Phase 2: real accounts and cloud data

- Firebase Authentication client completed in version 0.10.0 and live
  registration/sign-in verification completed in version 0.16.0.
- Password-reset client completed in version 0.10.0 and live reset dispatch
  verification completed in version 0.16.0. Delivery to a controlled inbox
  still needs a manual test.
- Firestore profile, reminder, workout-history, and substitution sync completed
  in version 0.10.0; live upload and clean-app profile restore were verified in
  version 0.16.0.
- Timestamped newest-change conflict behavior completed in version 0.10.0.
- Email-verification send/resend and refresh controls completed in version
  0.17.0.
- Password-reauthenticated account, cloud-data, local-cache, and reminder
  deletion completed in version 0.17.0.
- Google Sign-In and provider-aware deletion completed in version 0.18.0.
- Crashlytics integration and public privacy/deletion pages completed in
  version 0.19.0. A controlled fatal report was verified from an SM-N986N.
- Disposable-account deletion was verified end to end on an isolated Android
  16 emulator.

### Phase 3: better workout experience

- Written exercise guides completed in version 0.6.0.
- Native motion demonstrations completed in version 0.7.0 and replaced after
  user testing.
- Real-video tutorial searches completed in version 0.8.0.
- Add licensed, professionally filmed videos directly inside each guide.
- One-tap curated alternatives completed in version 0.9.0.
- Active workout screen completed in version 0.3.0.
- Rest countdown, pause, resume, and skip completed in version 0.3.0.
- Editable plan preferences and regeneration completed in version 0.5.0.
- Structured movement preferences and automatic exercise filtering completed in
  version 0.11.0.

### Phase 4: consistency and progress

- Editable reminder time completed in version 0.2.0.
- 28-day workout history completed in version 0.4.0.
- Fair workout streaks completed in version 0.4.0.
- Weekly completion summary completed in version 0.4.0.
- Interactive calendar, date-level workout review, six-week trend, totals,
  completion rate, best streak, and milestones completed in version 0.12.0.
- Explicit exercise results, active-session restoration, previous-result
  comparison, personal bests, and result charts completed in version 0.13.0.
- Explainable, goal-aware, optional next-target suggestions completed in version
  0.14.0.
- Set-by-set results, editable set recovery, automatic rest, timed-set
  countdowns, interruption restoration, set breakdowns, and session totals
  completed in version 0.15.0.

### Phase 5: production release

- Accessibility review.
- Public privacy-policy and account-deletion pages completed in version 0.19.0.
- Disposable-account deletion test completed.
- Security review.
- Real-device testing on multiple Android versions and screen sizes.
- Release signing, optimized release build, store listing, and staged rollout.

## 17. Troubleshooting quick reference

### The Run button is disabled

Wait for Gradle Sync to finish. Confirm the `app` run configuration is selected.

### The emulator is missing

Open **Tools > Device Manager** and start `PulsePlan_API_36`.

### The emulator says it is already running

Open **View > Tool Windows > Running Devices**. If no visible device appears,
use Device Manager's menu to stop the old instance, then start it again.

### A code change does not appear

Press **Shift + F10** for a full rerun. If necessary, uninstall PulsePlan from
the emulator and run it again.

### Sign-in is unavailable

Rebuild after checking the `firebase.*` and `google.webClientId` values in the
untracked `local.properties` file. A new computer needs its own local values as
described in section 13.

### The workout says Recovery

Check the selected number of training days and today's weekday. Recovery days
are intentional.

### The terminal says Java 8 or JVM 17 is required

Use Android Studio's Embedded JDK or configure the terminal to use JDK 17+.

### The emulator is slow

Wait for its first boot to finish, close heavy applications, and use a physical
phone for realistic performance testing.

## 18. Safety and privacy rules

- PulsePlan is not medical advice and must not diagnose injuries.
- Users who report pain, dizziness, or unusual discomfort should be told to
  stop and consult a qualified professional.
- Movement preferences should guide future substitutions, not pretend to be
  clinical treatment.
- Collect only data the product genuinely needs.
- Never log or display passwords.
- Keep the version 0.17.0 account/data deletion path visible, protected by
  password reauthentication, and covered by release testing.
- Keep Firebase configuration and release signing secrets out of Git.

## 19. Small glossary

- **Activity:** The Android entry point that opens the app.
- **APK:** The installable Android application file.
- **Build:** Turning the source code into an app Android can run.
- **Composable:** A Kotlin function that draws part of a Compose screen.
- **DataStore:** Local on-device storage used by the app.
- **Dependency:** A library the project uses instead of rebuilding everything.
- **Emulator:** A virtual Android device running on the computer.
- **Firebase:** Google's services used here for account authentication.
- **Gradle:** The system that configures and builds the Android project.
- **Kotlin:** The programming language used by PulsePlan.
- **Lint:** Automated checks for Android code quality and common mistakes.
- **Logcat:** Android's running event and error log.
- **Repository:** The project folder tracked by Git.
- **State:** The current information that decides what the screen displays.
- **ViewModel:** The coordinator between screens, data, and business rules.
- **WorkManager:** Android's system for dependable scheduled background work.

## 20. First-day checklist for a new collaborator

- [ ] Read sections 1 through 5.
- [ ] Open the project in Android Studio.
- [ ] Confirm Gradle uses JDK 17+.
- [ ] Start `PulsePlan_API_36`.
- [ ] Run the app.
- [ ] Sign in and complete onboarding.
- [ ] Check and uncheck an exercise.
- [ ] Find `PulsePlanApp.kt`, `WorkoutModels.kt`, and `PlanGenerator.kt`.
- [ ] Make one harmless text change.
- [ ] Build and run the change.
- [ ] Undo it and confirm the original returns.
- [ ] Review the upgrade order before starting a new feature.

Once these steps make sense, the whole project becomes much less mysterious:
screens collect choices, the ViewModel coordinates them, the plan generator
creates workouts, storage remembers progress, and Android displays the result.
