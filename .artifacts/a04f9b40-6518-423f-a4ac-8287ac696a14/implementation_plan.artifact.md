# SynSound Android Wrapper Implementation Plan

Create a polished, production-ready Android wrapper for the SynSound web application (`https://synsound-beta.base44.app`).

## User Review Required

> [!NOTE]
> The application is primarily a secure WebView wrapper. Most functionality is delivered by the website itself.

> [!IMPORTANT]
> Audio continuity is prioritized. The WebView will not be paused when the activity loses focus, ensuring that audio playback continues as expected for an audio-centric application.

## Proposed Changes

### Core App Component

Summary: Configure the main activity to handle WebView lifecycle, permissions, and navigation as per the requirements.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/Jarred/.gemini/antigravity/scratch/synsound-android/app/src/main/java/com/synsound/app/MainActivity.kt)
- Remove `webView.onPause()` and `webView.onResume()` to prevent audio interruption when the app loses focus.
- Ensure all WebView settings are optimized for a modern JS app.
- Refine permission handling for the microphone.

#### [MODIFY] [activity_main.xml](file:///C:/Users/Jarred/.gemini/antigravity/scratch/synsound-android/app/src/main/res/layout/activity_main.xml)
- Ensure the native back button is correctly positioned and themed.
- Verify fullscreen/edge-to-edge constraints.

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/Jarred/.gemini/antigravity/scratch/synsound-android/app/src/main/AndroidManifest.xml)
- Verify permissions and hardware feature declarations.
- Ensure Splash Screen theme is correctly applied.

### Identity & Resources

#### [MODIFY] [strings.xml](file:///C:/Users/Jarred/.gemini/antigravity/scratch/synsound-android/app/src/main/res/values/strings.xml)
- Polish accessibility labels and error messages.

#### [MODIFY] [colors.xml](file:///C:/Users/Jarred/.gemini/antigravity/scratch/synsound-android/app/src/main/res/values/colors.xml)
- Ensure colors match the SynSound branding (Dark theme).

## Verification Plan

### Automated Tests
- Run `gradlew assembleDebug` to verify the build.

### Manual Verification
- Deploy to a device/emulator.
- Verify initial load of SynSound.
- Test native Back button vs. WebView history.
- Test microphone permission request flow.
- Test audio playback continuity.
- Test network error state (using airplane mode).
- Verify edge-to-edge rendering and keyboard behavior.
