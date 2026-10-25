# Fitness watch app (Garmin Forerunner 745)

Companion for the Fitness Android app. The phone owns the workout data; the watch shows the current exercise and
sends back logged sets, effort ratings and heart rate. See the "Garmin companion app" section in `TODO.md` for the plan.

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
