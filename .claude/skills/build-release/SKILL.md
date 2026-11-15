---
name: build-release
description: >
  Cuts a new release of the Fitness Android app: bumps the version in app/build.gradle.kts,
  commits it to develop, fast-forward merges develop into main, builds a signed release APK, and
  copies it into releases/vX.Y/, pushes develop and main, and installs the APK on the phone. Only
  asks for the new version name. Use this whenever the user asks to "build a release", "cut a
  release", "ship a release build", "bump the version and build", or similar for this project —
  including when they just say "let's release" or ask for a signed APK.
---

# Build a release

This is the release process for the Fitness app (an Android/Kotlin/Compose project). It takes the
repo from "develop has the changes we want to ship" to "main is updated and there's a signed APK
ready," then pushes both branches and installs the APK on the user's phone.

The only question to ask the user is the new `versionName` (step 2). Everything else — bump,
commit, merge, build, copy, push, install — runs automatically with no confirmation prompts. The
user has pre-authorized the push and the phone install by invoking this skill.

Work through the steps below in order. Each one depends on the last succeeding, so don't skip ahead
if something fails — stop and surface the problem instead of guessing at a fix. A failure at any
step also means the later steps (especially the push) must not run.

## 1. Confirm the starting state

- `git status` — the working tree must be clean. If there are uncommitted changes, stop and ask
  the user what to do with them rather than including them in the release commit.
- Confirm the current branch is `develop` (`git branch --show-current`). If not, ask whether to
  switch to it — the release always starts from develop.
- Check that `main` can fast-forward to `develop`: `git merge-base --is-ancestor main develop`.
  If this fails, main has commits develop doesn't (or they've diverged), and a fast-forward merge
  isn't possible. Stop and tell the user — don't fall back to a merge commit or force anything;
  that's a decision for them.

## 2. Bump the version

Read `app/build.gradle.kts` and find `versionCode` and `versionName` in `defaultConfig`.

- **versionCode**: increment by 1 automatically (Play Store requires each upload to have a higher
  versionCode than the last, so there's no real judgment call here).
- **versionName**: ask the user what it should be — this is the one and only prompt in the whole
  process. Default to suggesting the next minor bump (e.g. `1.0` → `1.1`) since that's this
  project's pattern so far, but let them override it — they may want a major bump, a patch, or
  something else. Once they answer, don't ask anything else.

Edit the two lines in place. Don't touch anything else in the file.

## 3. Commit the version bump to develop

```
git add app/build.gradle.kts
git commit -m "Bump version to <versionName> (<versionCode>)"
```

Use the project's usual commit message tone (short, imperative, no fluff — check `git log` if
unsure). Do not add attribution lines beyond whatever the session's standard git commit footer is.

## 4. Fast-forward merge develop into main

```
git checkout main
git merge --ff-only develop
git checkout develop
```

Leave the user back on `develop` afterwards since that's the working branch. If the ff-only merge
fails here despite the step 1 check (e.g. something changed in between), stop and report it rather
than retrying with a different merge strategy.

## 5. Build the signed release APK

Confirm signing is configured before building — check that `local.properties` (at the repo root)
has `release.storeFile` set. If it's missing, `assembleRelease` will silently produce an *unsigned*
artifact, which isn't what a release build means. Stop and ask the user to set up signing (see the
README's "Release signing" section) rather than shipping unsigned output.

Once signing is confirmed, from the repo root:

```
./gradlew.bat assembleRelease
```

This produces only the APK. Do not run `bundleRelease` and do not build the AAB — this process no
longer produces one. If the build fails, stop and report the Gradle error — don't attempt version
or signing changes to work around a build failure without checking with the user first.

## 6. Copy the artifact into releases/

Create `releases/v<versionName>/` at the repo root if it doesn't exist, and copy:

- `app/build/outputs/apk/release/app-release.apk`

into it. This folder is for local tracking across versions, not for committing — make sure
`releases/` is in `.gitignore`; add it there (as its own commit, or folded into a later step,
your judgment) if it isn't already, since these are large binary build outputs that don't belong
in git history.

## 7. Push both branches

Push without asking, as long as every earlier step succeeded:

```
git push origin develop main
```

Never force-push. If the push is rejected (e.g. origin has new commits), stop and report it rather
than pulling, merging or forcing.

## 8. Install the release build on the phone

Install without asking. A finished, signed release build is exactly what the phone is for (unlike
day-to-day dev/debug testing on this project, which is emulator-only).

`adb` is usually not on PATH; it lives at `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`.

The phone is used over Wi-Fi debugging (phone IP `192.168.178.21`), so it shows up in `adb devices`
as `192.168.178.21:<port>` or as an `adb-<serial>._adb-tls-connect._tcp` mDNS name rather than a USB
serial. The port changes whenever Wireless debugging is toggled or the phone rejoins the network, so
if the phone isn't listed, find the current port with `adb mdns services` (look for
`_adb-tls-connect` on 192.168.178.21) and run `adb connect 192.168.178.21:<port>`. Some mDNS entries
are stale and refuse the connection — try the others. If it asks for pairing, the user needs to
supply the pairing IP:port and code from Developer options → Wireless debugging → "Pair device with
pairing code" — that's the one case where it's fine to stop and ask.

Check `adb devices` for a connected physical device (not an emulator, which shows as
`emulator-####`). If one is connected:

```
adb install -r releases/v<versionName>/app-release.apk
```

If no physical device is connected, say so and skip this step rather than falling back to the
emulator — the point of this step is to get the release onto the actual phone, not to run it
somewhere else.

## 9. Report

Summarize for the user:

- Old version → new version (versionCode and versionName)
- The commit made on develop
- That main was fast-forwarded to match, and that both branches were pushed
- Where the APK ended up
- Whether it was installed on the phone
