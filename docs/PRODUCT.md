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

1. Email/password account
2. First-run personalization
3. Generated daily workout
4. Exercise-level completion tracking
5. Weekly plan preview
6. Daily notification
7. Local cache for profile, current progress, and reminder time
8. Editable daily reminder with notification-permission recovery
9. Active workout flow with complete and skip actions
10. Pauseable and skippable rest countdown
11. Local 28-day workout history and weekly completion summary
12. Fair streaks based on scheduled workouts, with recovery days ignored
13. Editable plan preferences with immediate plan regeneration
14. Written exercise guides with target areas, steps, form cues, and safety notes
15. Exact real-video tutorial search from every exercise guide
16. One-tap curated exercise alternatives with per-day persistence
17. Timestamped offline-first cloud synchronization for profiles, reminders,
    progress, and exercise alternatives
18. Password-reset email flow
19. Automatic exercise filtering from structured movement preferences, with a
    visible reason and stable completion identity

Items 1, 17, and 18 are implemented in the client but are not production-ready
claims until a Firebase project, Authentication, Firestore, and security rules
are configured and tested with real accounts.

## Next releases

- Email verification and account/data deletion
- Licensed in-app exercise videos with controlled coaching quality
- Progress charts, personal records, and calendar filtering
- Google sign-in
