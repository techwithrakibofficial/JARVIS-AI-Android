# BUSHRA AI

Native personal Android voice and action assistant by **Rakib Chowdhury Labs**.

**Standalone APK: no Termux, Ubuntu, root, ADB, external bridge server or developer options required.** Personal development preview; no claim of unrestricted Android control or unlimited free AI.

## Install

Download `BUSHRA-AI.apk` from this repository's [Releases](https://github.com/techwithrakibofficial/JARVIS-AI-Android/releases).
Read [বাংলা phone setup](docs/PHONE_SETUP.md) and follow the acceptance tests before using real recipients or public posts.

## Included

- Navy/blue/cyan glass-style dashboard with the supplied logo; light theme option.
- Chat, tap-to-talk, phone TTS and optional cloud voice. Default ElevenLabs voice ID: `75CHIF904aWz6uq77J3P`; voice availability/language support requires a real account test.
- API Studio: searchable provider presets, custom names/endpoints/model IDs, protocol and auth fields, compatible model discovery, small connection tests. OpenAI-compatible/Gemini/Anthropic chat, ElevenLabs/OpenAI-compatible TTS. Arbitrary unrelated protocols are not automatically supported.
- Keys encrypted with Android Keystore; no bundled credentials, analytics SDK or cloud account backend. History/aliases/memory are local, but selected chat/voice data is sent to the chosen provider. Backups disabled.
- Contacts: manual number or optional phone import, multiple Bangla/English aliases, spoken alias entry, duplicate/ambiguity guards. Original phone contact names remain unchanged.
- Android actions: app/web launch, dialer and SMS draft; approval-gated screen text, exact-label click, typing, scroll and system navigation with optional Accessibility service. Each model action needs user approval. Every user task has a step limit and Stop.
- Memory and honest action activity. A dispatched click is not reported as a verified outcome.
- Build Studio exports small AI-generated source projects as ZIP after approval. It does not run generated code, compile another APK, auto-push to GitHub or deploy a website.

## Build

JDK 17, Android SDK 35, build tools 35.0.0 and pinned Gradle 8.11.1:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

GitHub Actions performs those checks and publishes the APK and SHA256 checksum. Debug/development signing uses a cached key; see the signing limitations in the phone guide. This preview is not a Play Store submission.

## Important limits

Calls and SMS open the appropriate system dialer/composer; the user makes the final call/send. Carrier fees apply. Screen automation depends on Android/OEM/app UI and cannot bypass locked screens, CAPTCHA, banking approvals or system restrictions. Live microphone/TTS, provider keys and app workflows need phone acceptance testing.

Speech/API/model quotas and prices belong to their providers. No automatic paid fallback. Offline STT availability varies by installed recognizer; network recognition is explicit opt-in. Local Whisper and llama model inference are not bundled in this preview.

## Continuity

See [current handoff](docs/AI_HANDOFF.md). Keep credentials out of issues, screenshots and source.

The user-supplied branding image is included at the user's request. It is not granted as a reusable stock asset. Third-party AndroidX/Kotlin/OkHttp libraries retain their own licenses.
