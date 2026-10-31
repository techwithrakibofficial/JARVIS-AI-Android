# BUSHRA AI 0.2.0 — সহজ setup

## আগের APK থেকে update

নতুন APK আগের app-এর ওপর install করুন। Uninstall করবেন না; contacts, history ও encrypted keys ফোনে থাকবে। প্রথমবার Settings থেকে saved provider-এ Reconnect দিন, যাতে automatic model setup পরীক্ষা হয়। Development signing key CI cache-এ থাকে; cache হারালে Android update reject করতে পারে।

## দুই ধাপে AI

1. Settings > AI connect করো। OpenRouter, OpenAI/ChatGPT, DeepSeek, Gemini, Claude, Groq বা Grok বেছে নিজের API key দিন।
2. Connect & save চাপুন। Model ID, endpoint বা protocol পূরণ করতে হবে না। অ্যাপ provider-এর model list পড়ে উপযুক্ত chat model নেয় এবং ছোট request দিয়ে পরীক্ষা করে। OpenRouter-এ free router/free model ছাড়া paid fallback নেই। অন্য provider-এর নিজস্ব billing/quota প্রযোজ্য।

OpenAI/ChatGPT এখানে OpenAI API connection; ChatGPT login/password নয়। OpenClaw হলো নিজের gateway agent: তার private HTTPS address ও token প্রয়োজন। Gateway-এ OpenAI-compatible Chat Completions চালু থাকতে হবে। এটি শুধু একটি hosted key service নয়।

একটি chat key-তে chat, phone actions এবং ফোনের speech/TTS দিয়ে voice conversation চলে। OpenRouter key দিয়ে ElevenLabs voice পাওয়া যায় না।

## Voice mode

- Home-এর বড় voice orbit বা “ভয়েসে কথা বলো” চাপুন। Microphone permission দিন।
- প্রথমবার offline বাংলা service ব্যর্থ হলে network speech ব্যবহারের অনুমতি দিন। এতে অডিও ফোনের recognizer provider-এ যেতে পারে। শুধু offline বেছে নিলে ভাষার data না থাকলে স্পষ্ট error ও retry option পাবেন।
- App শুনবে, final transcript নেবে, AI-তে পাঠাবে, উত্তর বলবে, তারপর আবার শুনবে। উত্তর বলার সময় recognizer বন্ধ থাকবে; নিজের উত্তরকে নতুন নির্দেশ হিসেবে নেবে না।
- Orbit/ডান দিকের waveform চাপলে উত্তর থামিয়ে নতুন কথা নেবে। Mic pause-তে বর্তমান turn থামে; resume-এ আবার শুনবে। লাল button session শেষ করে।
- Default session ১০ মিনিট; Settings-এ ৩/১০/৩০ মিনিট বাছাই করা যায়। নীরবতায় ৯০ সেকেন্ডে session শেষ হয়। No-match/timeout-এর retry সীমিত; বারবার mic restart করার loop নেই।
- অ্যাপ background/phone lock হলে mic session বন্ধ হয়। ফেরত এসে নিজে চালু করুন। Permission dialog-এর সাময়িক onPause-এ mic অকারণে বন্ধ হয় না।
- এটি foreground, turn-based half-duplex conversation; Gemini Live-এর পূর্ণ real-time audio/video capability বা background wake word নয়।

## কণ্ঠ

আপনার নির্বাচিত ElevenLabs voice ID fixed: `75CHIF904aWz6uq77J3P`। Settings-এ ঐচ্ছিক ElevenLabs key সংরক্ষণ করুন। Voice/model/account access ও ভাষা বাস্তব key দিয়ে পরীক্ষা করতে হবে। সাধারণ chat-এ cloud voice ব্যর্থ হলে ফোনের উপযুক্ত female voice-কে অগ্রাধিকার দিয়ে TTS fallback হয়। Test voice আলাদাভাবে error জানায়।

ফোনে female/Bangla voice data না থাকলে app নতুন proprietary voice তৈরি করতে পারে না। “ফোনের voice data সেটআপ” দিয়ে Android TTS voice install/enable করুন। নারীকণ্ঠ না থাকলে system voice ব্যবহৃত হতে পারে।

## Phone actions ও contacts

Contacts-এ manual নম্বর বা phone contact import করে নিজের বাংলা/English ডাকনাম দিন। আসল contact name বদলায় না। একই alias একাধিক নম্বরে দেওয়া আটকানো হয়। Call shortcut dialer এবং SMS shortcut draft খোলে; শেষ Call/Send user করবেন। Carrier charge প্রযোজ্য।

Screen control-এর জন্য Settings > Screen control চালু করো > BUSHRA Accessibility service enable করুন। Sideload restriction থাকলে Android App info menu-তে Allow restricted settings লাগতে পারে। Root/ADB/Developer Options লাগে না। Screen text AI-তে পাঠানো বা click/type/scroll করার আগে approval পড়ুন। যেকোনো সময়ে Stop/Cancel।

## Privacy

Keys Android Keystore দিয়ে encrypted, app backup বন্ধ, tracking SDK নেই। Recent conversation, saved memory, alias labels ও app names chat provider-এ যায়। Screen text যায় আলাদা approval-এর পরে। Password field বাদ দেওয়া হলেও অন্য private text থাকতে পারে। Keys chat/issue/screenshots-এ প্রকাশ করবেন না।

## পরীক্ষা

API connect, বাংলা typed chat, mic transcript, selected/cloud/fallback TTS, interruption, pause/resume, background stop এবং nickname দিয়ে নিজের নম্বরে dialer/SMS draft পরীক্ষা করুন। Screen actions প্রথমে অগুরুত্বপূর্ণ screen-এ পরীক্ষা করুন। Build Studio source ZIP তৈরি করে; অন্য APK compile বা website auto publish এখনো করে না।

APK compile, unit tests, static lint এবং Android emulator UI smoke test-কে real TECNO microphone বা live account test বলা হয় না। সমস্যা হলে error text/কোন ধাপে সমস্যা তা জানান; API key পাঠাবেন না।
