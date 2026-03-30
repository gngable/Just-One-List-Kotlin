# Just One List

A simple Android list app for quick, one-off lists like grocery shopping. Add items, check them off, and clear when done.

## Features

- Add items to a single list
- Checked items move to a "Done" section below unchecked items
- Tap an item (or its checkbox) to toggle it; tap again to uncheck
- Clear the entire list with one tap
- **Export** — copy the full list to the clipboard as a newline-delimited string
- **Import** — paste a list from the clipboard (newline, comma, or tab delimited); items are appended to the existing list
- List persists across app restarts and device reboots
- Screen stays on while the app is in the foreground
- Follows the system dark/light theme (dynamic color on Android 12+)

## Tech Stack

- Kotlin + Jetpack Compose
- Room (local SQLite persistence)
- ViewModel + StateFlow
- Material 3
- Min SDK 26 / Target SDK 35

## Building

Open in Android Studio (Hedgehog or later) and sync Gradle, or run:

```bash
./gradlew assembleDebug
```

Requires JDK 17 and Android SDK with build tools for API 35.

## Package

`com.mercangelsoftware.JustOneList`
