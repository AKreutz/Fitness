# Fitness

An Android app for building weightlifting training plans and tracking workouts against them.

## Features

- **Training plans** made up of workouts, each with an ordered list of exercises (free-weight or cable).
- **Guided workout sessions** that walk through an exercise's prescribed sets/reps, suggest a barbell plate
  breakdown for free-weight exercises, and track rest between sets.
- **Progressive overload prompts**: after a workout, the app offers a weight increase based on how the last
  session felt (perceived effort).
- **Workout history** of completed sessions.
- **Multiple plans**, with the ability to switch, create, edit, and delete them from the Profile tab.

## Tech stack

- Kotlin, Jetpack Compose, Material 3
- [Room](https://developer.android.com/training/data-storage/room) for local persistence
- Jetpack DataStore (Preferences) for lightweight app state (e.g. the active plan)
- Jetpack Navigation Compose
- No DI framework — dependencies are plain lazily-built singletons exposed from `FitnessApplication`

## Project structure

```
app/src/main/java/com/akreutz/fitness/
├── data/
│   ├── model/       # Room entities and value types (TrainingPlan, Workout, Exercise, ...)
│   ├── db/          # Room database and DAOs
│   ├── prefs/        # DataStore-backed preferences
│   └── repository/   # TrainingPlanRepository — the single point apps talk to for data
├── ui/
│   ├── home/         # Home tab: active plan, onboarding
│   ├── workouts/      # Workouts tab: completed-session history
│   ├── profile/       # Profile tab: plan management, plan editor
│   ├── session/       # Guided workout tracking screen
│   ├── common/        # Shared composables (dialogs, draggable list, ...)
│   └── theme/         # Colors, typography, theme
├── FitnessApplication.kt
└── MainActivity.kt     # Navigation graph and bottom-nav scaffold
```

## Building

Open the project in Android Studio, or build from the command line:

```
./gradlew.bat assembleDebug     # debug build, installs alongside release
./gradlew.bat assembleRelease   # signed release APK (requires signing config, see below)
./gradlew.bat bundleRelease     # signed release AAB, for Play Store upload
```

The debug build type applies `.debug` as an `applicationIdSuffix` and shows as "Fitness Debug" on the
home screen, so it can be installed side by side with a release build.

### Release signing

Release builds are signed using credentials read from `local.properties` (which is gitignored and never
committed). Add the following keys to your `local.properties`:

```properties
release.storeFile=C:\\path\\to\\your\\release.keystore
release.storePassword=...
release.keyAlias=...
release.keyPassword=...
```

If these keys are absent, `assembleRelease`/`bundleRelease` will still build an unsigned artifact.

To generate a new keystore:

```
keytool -genkeypair -v -keystore release.keystore -alias <alias> -keyalg RSA -keysize 2048 -validity 10000
```

Release builds have R8 minification and resource shrinking enabled (`app/proguard-rules.pro` holds any
project-specific keep rules).

## Requirements

- Android Studio (current stable)
- Min SDK 24, target/compile SDK 37
- JDK 11 (via `compileOptions` / core library desugaring)
