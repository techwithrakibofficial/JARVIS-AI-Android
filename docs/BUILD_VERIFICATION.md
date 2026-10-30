# Preview verification

Build environment: JDK 17, Android SDK 35 / build tools 35.0.0, Gradle 8.11.1, AGP 8.9.2, Kotlin 2.1.20.

Checks for the first native preview:

- `:app:assembleDebug`: installable APK produced (about 17 MB).
- `:app:testDebugUnitTest`: 28 tests, zero failures/errors. Contact normalization, exact matching, ambiguous names, duplicate aliases, unsafe export paths, custom provider round-trip, and HTTPS mock-server protocol/auth tests for chat/model discovery/ElevenLabs are covered.
- `:app:lintDebug`: completed with no blocking errors. Non-blocking SDK/dependency/style/icon warnings remain.
- `apksigner verify --verbose`: APK signature scheme v2 verifies.
- Manifest package `dev.rakib.bushra`, version `0.1.0`, minimum API 26, target API 35. CI versionCode uses workflow run number.
- GitHub Actions independently repeats compile, tests and lint before a release is published. Check the release's target commit and linked workflow run for the final artifact.

These are build/unit/static checks, not a real TECNO phone test. Real microphone recognition, TTS language data, provider credentials/quota, selected ElevenLabs voice access, accessibility behaviour and third-party app workflows require the user's acceptance tests in [PHONE_SETUP.md](PHONE_SETUP.md). The APK is a personal development preview, not a completed Play Store release.
