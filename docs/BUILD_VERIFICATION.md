# 0.3.0 verification scope

CI builds APK, runs JVM unit tests and static lint. Tests cover contact safety, chat/auth adapters, automatic model filtering, free-only OpenRouter selection and voice turn gating (duplicate callbacks, stale results, speech echo, pause/stop/interruption, bounded silence retries). A separate Android 16 emulator job checks the simplified setup UI and captures actual app screens before publishing.

Check the specific release workflow and test artifacts for final results; do not reuse earlier release test counts. Emulator UI tests do not establish real TECNO microphone accuracy, live provider credits, ElevenLabs voice access, or third-party app automation success. Phone acceptance steps are in PHONE_SETUP.md.

APK signing is development/debug signing with a specifically cached key. Cache eviction can still require reinstall; this is not permanent production signing.

0.3 regression tests add native app/channel routing, WhatsApp/SMS separation, cross-script saved contacts and ambiguity, country-code normalization, custom voice links, and playback echo rejection. CI never sends a real SMS, places a call or sends a WhatsApp message. These must be tested with the owner’s own numbers on a physical phone.
