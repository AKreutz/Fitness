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

## Wearable support

- [ ] Decide on scope: Health Connect integration, a Wear OS companion app, or both
- [ ] Health Connect: write completed workouts as exercise sessions (duration, start/end time)
- [ ] Health Connect: read heart rate during a session and show it in the session summary
- [ ] Wear OS: show the current exercise, set and weight on the watch
- [ ] Wear OS: start and skip the rest timer, and give haptic feedback when it ends
- [ ] Wear OS: log the Easy / Medium / Hard rating from the watch
- [ ] Permissions flow and a settings toggle to enable or disable wearable features

## Animations

Workout session screen: rest ring, set transitions, set-complete feedback, rolling numbers, plate animation and exercise switching are done.

- [x] Rest finished: pulse the rest ring and tint it with `animateColorAsState` once the goal time is reached
- [x] Screen transitions: shared-axis `NavHost` transitions, with the session screen sliding up from the bottom
- [x] Tab switching: crossfade or slide between the bottom-bar tabs and animate the nav indicator
- [x] Dialogs: scale and fade in
- [ ] List item animations: `Modifier.animateItem()` in the plan editor and history lists (add, remove, reorder)
- [ ] Staggered entrance for the cards on Home and Profile on first load
- [ ] Expand/collapse with `AnimatedVisibility` for the exercise chips and overflow menus
- [ ] Progress chart: draw the line in left to right the first time
- [ ] Calendar: slide the month grid on month change and pop in the workout-day markers
- [ ] Count-up animation for summary stats
- [ ] Press feedback: slight scale-down on buttons and cards
- [ ] Workout completion celebration (confetti or checkmark burst), stronger for a personal record
- [ ] Shimmer placeholders while data loads
