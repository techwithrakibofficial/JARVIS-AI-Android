# Current handoff — 0.2.0

Repo: techwithrakibofficial/JARVIS-AI-Android. App: BUSHRA AI. Developer: Rakib Chowdhury Labs. Package dev.rakib.bushra. Native Kotlin/Compose, no Termux/Ubuntu/root/ADB.

User found v0.1 UI plain and API/voice settings confusing; mic did not start. v0.2 removes manual model/protocol/endpoint fields, supports requested provider families with automatic model discovery/test, keeps OpenAI/ChatGPT one API, and explains the optional OpenClaw private gateway address requirement. Never imply one provider's key unlocks ElevenLabs or all unrelated APIs.

Voice rewritten around VoiceTurnGate: explicit phases, duplicate/stale transcript rejection, no recognizer while speaking, interruption, bounded silence/busy retries, local-language failure network fallback with consent, delayed TTS initialization queue, generation-safe completion, and foreground session/idle limits. MainActivity ends voice onStop instead of onPause so permission dialogs do not break startup. System speech service, actual API keys, installed female/Bangla voices and TECNO OEM behaviour still need phone acceptance testing.

UI: PremiumUi.kt implements animated cosmic background, orbit, waveform, type transitions, glass action cards, voice room and simplified settings. Original user logo is kept. Data/keys/history/aliases remain app-local, secrets encrypted. Existing permission-gated phone controller retained. Build Studio source ZIP only.

CI: unit tests + compile + lint, followed by Android 16 emulator UI smoke/screenshots and release publishing. Check actual run/artifacts before claiming success. Reuse the cached personal debug signing key (v2) for upgrades; permanent distribution still requires owner-managed release signing. Do not delete user data or put keys in source/logs. No unlimited free inference promise.
