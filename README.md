# PermissionsLab — Practice 8

Package: `uz.ttpu.permissionslab`

## What this project demonstrates
1. Normal permissions: `INTERNET`, `ACCESS_NETWORK_STATE`, and network status.
2. Runtime camera permission: request, rationale state, and a shortcut to app settings after repeated denials in the current session.
3. Special overlay permission: opens the dedicated Settings screen and refreshes the state on `ON_RESUME`.
4. Multiple location permissions: `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION`, reading the `Map<String, Boolean>` result and refreshing on resume.
5. Notifications: requests `POST_NOTIFICATIONS` only on Android 13 / API 33+ and can send a test notification.
6. All main demos are on one scrollable screen for easy classroom demonstration.

## Open in Android Studio
1. Extract the ZIP.
2. Android Studio → Open → select the `PermissionsLab` folder.
3. Wait for Gradle sync. Use JDK 17.
4. Create/start an emulator (API 34 or 35 recommended), then press Run.
5. For notification version comparison, use one API 32 emulator and one API 34 emulator.

## Demo steps
- Network: toggle Airplane mode and tap Refresh network status.
- Camera: tap Request camera permission; test Allow and Deny. To repeat the flow, reset the app's permissions in Android Settings.
- Overlay: tap the switch; enable/disable “Display over other apps”, then return to the app.
- Location: request location and try “Only this time” where offered; send the app to background and return.
- Notifications: on API 33+, request permission, then send a test notification.

## Notes
- The camera and location features demonstrate permission handling; they do not implement actual camera capture or GPS tracking.
- Android's exact permission dialog wording and denial behavior vary by OS version and prior choices.
- Android Studio must download the Android Gradle Plugin and dependencies during first sync.
