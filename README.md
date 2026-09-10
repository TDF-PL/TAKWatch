# TAKWatch

<img src="https://raw.githubusercontent.com/TDF-PL/TAKWatch-IQ/main/images/screenshot-2.jpeg" width="200" height="200">

## Description
TAKWatch is an ATAK plugin that communicates with Garmin devices running TAKWatch-IQ (https://github.com/TDF-PL/TAKWatch-IQ) application.

## Build the APK

TAKWatch is an ATAK plugin, not a standalone Android application. Building it requires:

- JDK 11 or 17 (JDK 8 is not supported by Android Gradle Plugin 7.4.2)
- Android SDK platform 33
- Android build tools 30.0.3
- The ATAK 4.10.0 CIV plugin development kit

The Garmin Connect IQ companion SDK is resolved automatically from Maven Central.

The ATAK Gradle plugin must be available either as a local JAR or through an ATAK Maven repository. By default, the project looks for `atak-gradle-takdev.jar` two directories above the project root. To use a different location, create `local.properties` in the project root:

```properties
takdev.plugin=C:/path/to/atak-gradle-takdev.jar
```

Alternatively, configure an ATAK Maven repository in `local.properties`:

```properties
takrepo.url=https://your-atak-maven-repository/
takrepo.user=your-username
takrepo.password=your-password
```

With the required tools configured, run the CIV debug build from the project root.

On Windows:

```bat
.\gradlew.bat assembleCivDebug
```

On macOS or Linux:

```sh
./gradlew assembleCivDebug
```

Replace `assembleCivDebug` with `assembleCivRelease` to create a release build.

The generated APK is written to `app/build/outputs/apk/civ/debug/` or `app/build/outputs/apk/civ/release/`. Its filename includes the plugin version, source revision, ATAK version, flavor, and build type.

Install the APK through ATAK's plugin manager or copy it to the target device and open it with ATAK. The device must have the compatible ATAK CIV version installed; TAKWatch cannot be launched as a standalone app.

If the ATAK Dev Kit is not configured, Gradle fails with an error similar to:

```text
Plugin with id 'atak-takdev-plugin' not found
```

Prebuilt APKs are available on the [v1.0.0 release page](https://github.com/TDF-PL/TAKWatch/releases/tag/v1.0.0).

## Features
- Sending heart rate to ATAK
- Receiving waypoints from ATAK (persisted on the watch)
- Receiving markers from ATAK (not persisted on the watch)
- Triggering Emergency alert from watch (when SELECT button pressed 5 times rapidly)
- Creating vectors to markers
- Sending routes from ATAK to watch
- Sending chat messages
- Triggering ATAK wipe from watch (when BACK button pressed 5 times rapidly)

## Equipment supported
- epix™ (Gen 2) / quatix® 7 Sapphire
- epix™ Pro (Gen 2) 42mm
- epix™ Pro (Gen 2) 47mm
- epix™ Pro (Gen 2) 51mm
- Forerunner® 945 LTE
- Forerunner® 945
- Forerunner® 955 / Solar
- Forerunner® 965
- fēnix® 5 Plus
- fēnix® 5S Plus
- fēnix® 5X / tactix® Charlie
- fēnix® 5X Plus
- fēnix® 6 Pro / 6 Sapphire / 6 Pro Solar / 6 Pro Dual Power / quatix® 6
- fēnix® 6S Pro / 6S Sapphire / 6S Pro Solar / 6S Pro Dual Power
- fēnix® 6X Pro / 6X Sapphire / 6X Pro Solar / tactix® Delta Sapphire / Delta Solar / Delta Solar - Ballistics Edition / quatix® 6X / 6X Solar / 6X Dual Power
- fēnix® 7 / quatix® 7
- fēnix® 7 Pro
- fēnix® 7S Pro
- fēnix® 7S
- fēnix® 7X / tactix® 7 / quatix® 7X Solar / Enduro™ 2
- fēnix® 7X Pro

## Screenshots

<img src="https://raw.githubusercontent.com/TDF-PL/TAKWatch-IQ/main/images/screenshot-1.png" width="200" height="200">
<img src="https://raw.githubusercontent.com/TDF-PL/TAKWatch-IQ/main/images/screenshot-3.jpeg" width="200" height="200">


## Releases

https://github.com/TDF-PL/TAKWatch/releases

