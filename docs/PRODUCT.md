# PulsePlan product brief

## Problem

People often know they should exercise but do not know what to do today, how to
fit it into their schedule, or whether they are becoming more consistent.
PulsePlan turns a short preference profile into a clear daily workout and keeps
the plan, reminder, and completion history in one place.

## Personalization inputs

- Primary fitness goal
- Current experience level
- Preferred coaching style ("workout personality")
- Available equipment
- Training days per week
- Preferred session length
- Structured movement preferences
- Optional movement note

The plan generator must never diagnose an injury or represent itself as medical
advice. Movement preferences are user-controlled plan filters, not diagnoses or
a substitute for a qualified clinician or coach.

## MVP

1. Google and email/password accounts
2. First-run personalization
3. Generated daily workout
4. Exercise-level completion tracking
5. Weekly plan preview
6. Daily notification
7. Local cache for profile, current progress, exercise results, active session,
   and reminder time
8. Editable daily reminder with notification-permission recovery
9. Active workout flow with complete and skip actions
10. Pauseable and skippable rest countdown
11. Interactive 365-day workout calendar, date-level exercise review, six-week
    trend, 28-day completion rate, totals, best streak, and milestones
12. Fair streaks based on scheduled workouts, with recovery days ignored
13. Editable plan preferences with immediate plan regeneration
14. Written exercise guides with target areas, steps, form cues, and safety notes
15. Exact real-video tutorial search from every exercise guide
16. One-tap curated exercise alternatives with per-day persistence
17. Timestamped offline-first cloud synchronization for profiles, reminders,
    progress, exercise results, and exercise alternatives
18. Password-reset email flow
19. Automatic exercise filtering from structured movement preferences, with a
    visible reason and stable completion identity
20. Optional result logging for reps, weight, time, distance, effort, and notes
21. Previous-result comparison, automatic personal bests, exercise-specific
    history, six-result trend charts, and confirmed result deletion
22. Active-session restoration after an app-process interruption
23. Goal-aware, explainable next-target suggestions derived only from explicit
    results and Easy, Good, or Hard effort feedback
24. Set-by-set workout mode with per-set results, automatic between-set rest,
    editable last-set recovery, timed-exercise countdowns, interruption
    restoration, set breakdowns, and a measurable session summary
25. Account and Privacy center with email-verification controls, password
    reset, short data-use information, and provider-confirmed deletion of the
    account, cloud history, local cache, and scheduled reminder

Items 1, 17, 18, and 25 depend on Firebase. Registration, sign-in,
password-reset dispatch, Firestore upload, and clean-app restoration are
verified on the configured development project. The destructive deletion path
must receive a final end-to-end test with a disposable account before release.

## Next releases

- Licensed in-app exercise videos with controlled coaching quality
