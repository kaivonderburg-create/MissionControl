# dynamicOS Sentinel X — Android + iOS

One shared Capacitor WebView UI, built from the supplied Sentinel Enterprise X HTML demo. This is an **offline simulation**, not an integrated camera-security system. Local password verification is not remotely enforceable security.

## Build

GitHub Actions: **Sentinel Android + iOS**. Android job creates a debug APK. iOS macOS job compiles an unsigned iPhoneOS application and packages an **unsigned IPA for development inspection only**; iPhone installation requires Apple signing and a provisioning profile.

Run locally: `npm install && npx cap add android && npx cap sync android`, then `cd android && ./gradlew assembleDebug`.
