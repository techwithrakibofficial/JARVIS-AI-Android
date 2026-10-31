# BUSHRA AI 0.4.0

Native personal Android voice and action assistant by **Rakib Chowdhury Labs**.

**Standalone APK: no Termux, Ubuntu, root, ADB, external bridge server or developer options required.** Personal development preview; no claim of unrestricted Android control or unlimited free AI.

## Install

Download `BUSHRA-AI.apk` from this repository's [Releases](https://github.com/techwithrakibofficial/JARVIS-AI-Android/releases).
Read [বাংলা phone setup](docs/PHONE_SETUP.md) and follow the acceptance tests before using real recipients or public posts.

## Included

- Animated cyber HUD / liquid-glass dashboard with moving highlights, orbit rings, responsive wave animation, changing typography, dark/light themes and motion control. Low-power mode reduces motion.
- Simple provider + key setup; endpoint/protocol/model fields removed from the UI. Automatic model discovery and connection test. OpenRouter free-first, OpenAI/ChatGPT, DeepSeek, Gemini, Claude, Groq, Grok and optional private OpenClaw gateway.
- Foreground voice sessions with listening/thinking/speaking/paused/error states. Stop/interruption, bounded recognizer recovery, on-device language failure fallback (with network consent), TTS-ready queue, automatic next turn and 3/10/30-minute session limits. Mic ends on background/lock; a transient permission dialog no longer triggers onPause shutdown.
- ElevenLabs voice ID/link selection (default `75CHIF904aWz6uq77J3P`) with an optional separate key. Device-language female TTS preference and system fallback. Device female/Bangla voice availability is not guaranteed.
- Existing encrypted keys, aliases, contacts and history/memory preserved. Native Bengali/English routing opens common apps without AI credit. Saved phone contacts resolve locally; re-import is optional. One-time opt-in trusted actions and direct SMS/call plus guarded WhatsApp Send; carrier SMS never substitutes for WhatsApp. Tasks records SMS sent callbacks, not delivery.
- Source ZIP export in Build Studio. Generated code is not run or automatically published.

Voice uses an STT > chat > TTS turn flow with quiet local voice-activity waiting and optional hardware-dependent interruption, not full Gemini Live audio/video parity. One chat API key covers the core flow via phone speech/TTS; a specific ElevenLabs cloud voice still needs its own credentials. Provider quota and installed speech services matter.

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

## Selected-contact auto-chat

Settings → SMS / WhatsApp auto-chat. Pick one saved contact/number, the channel, purpose, expiry and reply limit, then explicitly Start. SMS needs receive/read/send permission; WhatsApp needs Notification access and a notification Reply action. Replies are marked [BUSHRA AI]. Only selected new incoming messages and that short thread are sent to the selected model. Groups are excluded. Stops on quota/send errors, has deduplication and reserves a send before dispatch; no automatic resend. Android WorkManager scheduling/battery restrictions can delay replies. Stop rules in the panel or use the main Stop button. Closing the Activity alone does not stop a deliberately activated rule.

Stable voice no longer runs parallel system SpeechRecognizers or restart-on-silence loops. Quiet local audio-energy waiting is bounded; experimental playback interruption uses hardware echo cancellation/headsets, not a second recognizer. Android's speech service may still make its own start/end cue per real turn. Full low-latency Gemini Live parity is not claimed.
