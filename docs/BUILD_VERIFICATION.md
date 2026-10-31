# 0.2.0 verification scope

CI builds APK, runs JVM unit tests and static lint. Tests cover contact safety, chat/auth adapters, automatic model filtering, free-only OpenRouter selection and voice turn gating (duplicate callbacks, stale results, speech echo, pause/stop/interruption, bounded silence retries). A separate Android 16 emulator job checks the simplified setup UI and captures actual app screens before publishing.

Check the specific release workflow and test artifacts for final results; do not reuse v0.1 test counts for v0.2. Emulator UI tests do not establish real TECNO microphone accuracy, live provider credits, ElevenLabs voice access, or third-party app automation success. Phone acceptance steps are in PHONE_SETUP.md.

APK signing is development/debug signing with a specifically cached key. Cache eviction can still require reinstall; this is not permanent production signing.
