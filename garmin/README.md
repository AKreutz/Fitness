# Fitness watch app (Garmin Forerunner 745)

Companion for the Fitness Android app. The phone owns the workout; the watch shows where you are, sends your taps
back, and records your heart rate, one reading per second, for the whole workout. The messages are documented on
`WatchProtocol` in the Android app (`data/watch/WatchProtocol.kt`).

| Where the workout is | Screen | START |
| --- | --- | --- |
| Warm-up | Exercise, weight and reps | Start the first set |
| Working set | "Set 2 / 4" | Finish the set |
| Resting | Count-up timer, green at the rest goal (the watch buzzes then) | Start the next set |
| Exercise done | Easy / Medium / Hard | Confirm the highlighted rating |
| Easy at the top rep step | "Raise the weight?" with Yes / No | Confirm the highlighted answer |
| Finishing | "Finishing…" while the heart rate readings go to the phone, which saves the workout once it has them | |
| Workout done | Personal records, average heart rate, and whether the heart rate was saved | |

| Button | Action |
| --- | --- |
| **UP** / **DOWN** | Move the highlight while rating or answering the weight offer |
| **START** | The action in the table above |
| **Hold UP** | Skip the current exercise |
| **BACK** | Leave the app |

The screen changes when the phone confirms a tap, and the watch buzzes briefly then. A second tap before that is
ignored. Open the app after starting a workout on the phone (the connection only runs while the workout screen is
open); until the phone answers, the watch asks for the state every 5 seconds.

## Heart rate

While a workout runs, the watch keeps a heart rate reading for every second (0 where the sensor had none). Nothing is
sent during the workout, so the link stays free for the phone's answers.

When the last exercise is rated, the phone does not save the workout yet. Both screens say "Finishing…" while the watch
sends all the readings, in pieces, one at a time. The phone saves the workout with them once they are all there, and
works out the average and highest heart rate of the session and of each exercise from them. If nothing arrives for 20
seconds, or you tap "Save without heart rate" on the phone, it saves the workout without them. The phone only waits
when a watch is connected and its app has been in touch during the workout.

Stay on the "Finishing…" screen until it says "Workout done": leaving the watch app first loses the readings, because
they are only kept in memory. A failed piece is sent again a few times, and the whole transfer starts over twice more
before the watch gives up ("Heart rate not saved").

## Build

Needs the Connect IQ SDK (9.2 or newer, with the Forerunner 745 device files), a developer key and Java 11 or newer
(Android Studio's bundled JBR works):

```
cd garmin
monkeyc -d fr745 -f monkey.jungle -o bin/FitnessWatch.prg -y <path to developer_key.der> -w -l 2
```

Copy `bin/FitnessWatch.prg` into `GARMIN/Apps` on the watch over USB. Keep the developer key out of the repository.

## Connect it to the Android app

Add the app id from `manifest.xml` to the Android project's `local.properties`:

```
garmin.appId=be46e47d1d3848ccacb4376e192b2958
```

Without it the Android app never starts the watch connection. For the Connect IQ simulator, also set
`garmin.useSimulator=true` and run `adb forward tcp:7381 tcp:7381`.
