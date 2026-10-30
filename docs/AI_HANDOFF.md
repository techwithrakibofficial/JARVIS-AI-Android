# Current handoff

Project: BUSHRA AI, Rakib Chowdhury Labs. Repo: techwithrakibofficial/JARVIS-AI-Android.

User wants a standalone APK, no Termux/Ubuntu/root/ADB, voice conversation and phone actions, glass dashboard with cyan/blue/navy, their supplied logo, generic API provider suggestions plus editable custom names/settings, and contact aliases. Default cloud voice ID: 75CHIF904aWz6uq77J3P. Never put API keys in source/logs.

Implemented first native preview:
- Kotlin/Compose dashboard, chat, contacts, memory/activity, settings, source-export Build Studio.
- OpenAI-compatible/Gemini/Anthropic chat adapters. ElevenLabs/OpenAI speech adapters, Android STT/TTS.
- Provider presets and custom protocol/auth fields; optional model discovery for compatible endpoints; connection tests. Keys encrypted with Android Keystore, backups disabled.
- Local manual/contact-import aliases, spoken alias dictation, duplicate-name guards, exact-match preference and ambiguity handling.
- Approval-gated launch URL/app, dialer, SMS draft, screen reading/typing/exact-label click/scroll/Home/Back/Recents. Accessibility overlay approvals work while another app is in front. No direct SMS sending or completed-call claim.
- Source ZIP generation with safe path checks, no generated code execution. Automatic project cloud builds/deployment remain future work.
- GitHub workflow compiles, tests, lints, publishes an installable debug-signed preview. Cached development signing key is not a permanent signing solution.

Before claiming a release ready, inspect actual CI result, APK signature and release asset URL. Real phone, live API/provider/voice access and third-party app flows need acceptance tests. Do not call compile/unit tests real-device validation. Do not overwrite user's other repositories or upload secrets. Offline Whisper/llama source checkouts in the original session are not integrated Android libraries.
