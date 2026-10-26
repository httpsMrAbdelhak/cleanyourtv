# Building and installing

## What you need

- JDK 17 (a JDK, not only a JRE).
- The Android SDK with `platforms;android-34` and `build-tools;34.0.0`. Android Studio installs both; on its own, use the command-line tools' `sdkmanager`.
- Tell Gradle where the SDK is, once: `echo "sdk.dir=/path/to/Android/Sdk" > local.properties` in this folder (the file is ignored by git), or set `ANDROID_HOME`.

The project has **no library dependency** and no native code, so the same APK runs on 32-bit ARM (`armeabi-v7a`), 64-bit ARM and x86.

## Build a test APK (debug)

From `cleanyourtv-next/`:

```
chmod +x gradlew
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug
```

The APK is `app/build/outputs/apk/debug/app-debug.apk`. It is signed with the public Android debug key: right for a test on your own TV, never for publishing. It installs on Android 8.0 and later, so on Android TV 9.

## Build from GitHub, with nothing installed locally

The workflow `.github/workflows/next.yml` builds and tests every push that touches this folder.

To get the APK from it: **Actions**, then **CleanYourTV Next**, then **Run workflow**, tick **Keep the debug APK**, and run it on the branch. When it finishes, open the run and download the artifact `cleanyourtv-next-debug-apk` (a zip holding `app-debug.apk`; kept 7 days).

## A release build, signed with your own key

Create a key once and keep the file and its passwords out of the repository (`*.jks` and `*.keystore` are git-ignored here):

```
keytool -genkeypair -v -keystore ~/cleanyourtv-release.jks -alias cleanyourtv \
  -keyalg RSA -keysize 4096 -validity 10000
```

Build with the key passed as environment variables, which keeps the passwords out of shell history and files:

```
export CYTV_KEYSTORE=~/cleanyourtv-release.jks
export CYTV_KEYSTORE_PASSWORD=...   CYTV_KEY_ALIAS=cleanyourtv   CYTV_KEY_PASSWORD=...
./gradlew --no-daemon assembleRelease
```

Without these variables the release APK is **unsigned and Android refuses to install it**: use the debug build for a first test. An APK signed with one key cannot be updated by an APK signed with another: uninstall first when you switch from debug to release.

## Put the APK on the TV

Any of these works; none needs a command typed on the TV.

1. **USB stick**: copy `app-debug.apk` to a stick, plug it into the TV (the Mi TV Stick needs a USB adapter), and open it with a file manager app from the Play Store. Android asks once to allow that file manager to install apps.
2. **"Send Files to TV"** (or a similar app) installed on the TV and on your phone.
3. **ADB**, if you already use it. Run it yourself, on your own TV, with your own address:

   ```
   adb connect <tv ip>:5555
   adb install -r app-debug.apk
   ```

   When you are done, turn USB debugging and network debugging off again on the TV.

Then open **CleanYourTV** from the TV's app row: it shows up there without being your Home screen. Becoming the Home screen is a separate step, covered in [RECOVERY.md](RECOVERY.md).

## Tests

`./gradlew testDebugUnitTest` runs all of them. They are plain JVM tests: the logic (dock, sorting, greeting, settings cycling) and `ManifestPolicyTest`, which checks the manifest for the promises in the README.

Without the Android SDK, the logic and manifest tests can still run with `javac` and JUnit 4.13.2 against any `android.jar` of API 26 or later.

## Getting the cinema rows and the weather to work

- **Weather**: nothing to set up. Open Settings, Weather city, type a city name, pick the match.
- **Cinema**: create a free key on themoviedb.org (your account, Settings, API), then enter it in Settings, Cinema, TMDB key. Typing a 32-character key with a remote is slow: the long "read access token" works too, and so does typing the key from the on-screen keyboard of a paired phone or keyboard. Details and terms in [CINEMA-WEATHER.md](CINEMA-WEATHER.md).

## Verified and not verified

Said plainly, so nothing is assumed. The GitHub workflow runs the unit tests, the lint and the debug build on every push: its result is the proof of those three. Not verified until you try on the TV: installation, the look of every screen, D-pad behaviour, smoothness and memory on the Mi TV Stick, live TMDB and Open-Meteo answers, and whether the firmware lets this app take the Home button.
