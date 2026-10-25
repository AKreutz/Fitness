# TODO

## Strength metrics

- [ ] Estimated 1RM per exercise (e.g. Epley formula) from logged weight and reps
- [ ] Personal record tracking per exercise (heaviest weight, best estimated 1RM, most reps at a weight)
- [ ] Highlight new PRs at the end of a session and in the Workouts history
- [ ] Training volume (sets x reps x weight) per session, per week and per exercise
- [ ] Volume per muscle group (needs a muscle group field on exercises)
- [ ] Show the new metrics as charts in the Progress tab
- [ ] Include the new fields in Drive sync and plan export (Room migration + snapshot DTO)

## Streaks

- [ ] Weekly streak: consecutive weeks in which the planned number of workouts was completed
- [ ] Current and longest streak, shown on the Home tab
- [ ] Mark streak days on the training calendar
- [ ] Optional workout reminders (notification) to protect a streak
- [ ] Define how rest days and missed weeks affect a streak

## Garmin companion app (Forerunner 745)

Connect IQ watch app (Monkey C) talking to the phone app through the Connect IQ Mobile SDK. The phone stays the source of truth: it runs the session and the watch shows it and sends commands. The message format is documented on `WatchProtocol` (`data/watch/WatchProtocol.kt`). The app records one weight and one effort rating per exercise, not per set, so the watch works at that level too.

Phone app: done, untested against a real watch.

- [x] Set up the Connect IQ project (`garmin/`, builds for fr745)
- [x] Phone-to-watch message channel (`GarminWatchConnection`, kept alive during a session by `WatchSyncService`)
- [x] Message format: session state to the watch, commands and heart rate back (`WatchProtocol`, `WatchSessionBridge`)

Phone-free set logging

- [x] Phone: publish the session state (exercise, set, target weight and reps, rest goal) to the watch
- [x] Phone: Advance (finish set / start next set) and Skip commands from the watch
- [ ] Watch: screen showing the current exercise and target, e.g. "Squat 80 kg x 8"
- [ ] Watch: one tap to finish a set or start the next one, send `adv`
- [ ] Watch: rest timer from the state's `rt` / `rg`, vibrating when the goal is reached

Effort capture

- [x] Phone: `rate` command feeds the effort-based rep stepping, and `inc` answers the weight-increase offer
- [ ] Watch: Easy / Medium / Hard prompt when the exercise's last set is done, send `rate`
- [ ] Watch: weight-increase prompt ("Raise to X kg?") with `nw`, send `inc`

Heart rate per exercise and session

- [x] Phone: store the watch's average and max heart rate per exercise and per session (Room migration 15 to 16)
- [x] Phone: include them in Drive sync and plan export
- [x] Phone: show the session's heart rate in the finish screen and the Workouts history
- [ ] Watch: measure heart rate per exercise and for the session, send `hr` regularly
- [ ] Heart rate recovery between sets
- [ ] Write the workout as a FIT strength activity so it appears in Garmin Connect
- [ ] Show per-exercise heart rate in the expanded Workouts history

## Animations

Workout session screen: rest ring, set transitions, set-complete feedback, rolling numbers, plate animation and exercise switching are done.

- [x] Rest finished: pulse the rest ring and tint it with `animateColorAsState` once the goal time is reached
- [x] Screen transitions: shared-axis `NavHost` transitions, with the session screen sliding up from the bottom
- [x] Tab switching: crossfade or slide between the bottom-bar tabs and animate the nav indicator
- [x] Dialogs: scale and fade in
- [x] List item animations: `Modifier.animateItem()` in the plan editor and history lists (add, remove, reorder)
- [x] Staggered entrance for the cards on Home and Profile on first load
- [x] Expand/collapse with `AnimatedVisibility` for the exercise chips and overflow menus
- [x] Progress chart: draw the line in left to right the first time
- [x] Calendar: slide the month grid on month change and pop in the workout-day markers
- [x] Count-up animation for summary stats
- [x] Press feedback: slight scale-down on buttons and cards
- [x] Workout completion celebration (confetti or checkmark burst), stronger for a personal record
- [x] Shimmer placeholders while data loads
