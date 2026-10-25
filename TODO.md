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

Connect IQ watch app (Monkey C) talking to the phone app through the Connect IQ Mobile SDK. The phone stays the source of truth.

- [ ] Set up the Connect IQ project and a phone-to-watch message channel (Garmin Connect Mobile required)
- [ ] Define the compact message format: workout plan to the watch, logged sets back to the phone

Phone-free set logging

- [ ] Push the current workout plan to the watch (exercise, set number, target weight and reps)
- [ ] Watch screen showing the current exercise and target, e.g. "Squat 80 kg x 8"
- [ ] Log a set with one tap, with a way to adjust reps and weight first
- [ ] Sync logged sets back to the phone and merge them into the active session
- [ ] Start the rest timer when a set is logged and vibrate when it ends

Effort capture

- [ ] Rate each set (Easy / Medium / Hard) on the watch
- [ ] Send the rating to the phone so it feeds the effort-based rep stepping and weight-increase offers

Heart rate per set and session

- [ ] Record average and max heart rate per set and for the whole session
- [ ] Record heart rate recovery between sets
- [ ] Send heart rate data to the phone and show it in the session summary
- [ ] Write the workout as a FIT strength activity so it appears in Garmin Connect
- [ ] Include the new fields in Drive sync and plan export (Room migration + snapshot DTO)

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
