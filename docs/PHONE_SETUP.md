# BUSHRA AI 0.4.0 — ফোন setup

আগের APK uninstall করবেন না; নতুন APK তার ওপর install করুন। Settings → COMMAND ACCESS → সব permission নিয়ে automation চালু করো। Android Contacts, Mic, SMS, Call-এর জন্য আলাদা dialog দেখাতে পারে। Accessibility-ও Settings থেকে নিজে enable করুন। Root/Termux লাগে না।

App launch, Home/Back-এর জন্য app confirmation লাগে না। Trusted screen actions ও Direct SMS/call/WhatsApp চালু করলে নিজের নির্দেশে বারবার app confirmation ছাড়াই কাজ হবে; toggle বন্ধ করা যায়। ফোনে save থাকা contact name-ই বলুন; app-এ আবার import/save বাধ্যতামূলক নয়। বাংলা থেকে English sound match-এ একাধিক নম্বর মিললে নিজে নম্বর দিয়ে নির্দিষ্ট করুন। পুরো address book AI-তে upload হয় না।

উদাহরণ:
- “হোয়াটসঅ্যাপ ওপেন করো”
- “হোয়াটসঅ্যাপ খুলে রাকিব ব্রো কে হাই লিখে এসএমএস সেন্ড করো” — WhatsApp-এই যাবে, carrier SMS নয়।
- “রাকিব ব্রো কে হাই এসএমএস পাঠাও” — default SMS SIM থেকে direct mode-এ পাঠাবে। SIM settings-এ default SMS SIM বেছে রাখুন। Carrier charge প্রযোজ্য। Tasks-এ sent callback দেখুন; Sent মানে Delivered নয়। ব্যর্থ হলে স্বয়ংক্রিয় retry নেই।

WhatsApp install/enable থাকতে হবে। Deep link saved number দিয়ে chat খোলে। Automatic Send শুধু Accessibility-তে exact contact title, message এবং WhatsApp-এর পরিচিত composer/Send control একসঙ্গে মিললে চলে। WhatsApp version অনুযায়ী control বদলালে Send চাপবে না; fallback SMS-ও পাঠাবে না। তখন WhatsApp-এর Send নিজে দিন। WhatsApp Business fallback supported।

Settings → ElevenLabs key ও Voice ID অথবা voice link → Voice key রাখো → কণ্ঠ শুনে দেখো। ID/link voice নির্বাচন করে; key-এর বিকল্প নয়। নিজের account-এ voice available/library-তে থাকা ও প্রয়োজনীয় quota লাগবে। ব্যর্থ হলে সাধারণ conversation ফোনের TTS নেয়; female voice থাকলে অগ্রাধিকার। Test voice সত্যিকারের cloud error দেখায়।

Hands-free interruption default on: উত্তর বলার সময় আলাদা speech listener নতুন কথা ও নিজের playback-এর পার্থক্য ধরার চেষ্টা করে; partial result-এ থামে, final transcript-এ নতুন নির্দেশ নেয়। “থামো” বলা যায়। এটি experimental; speaker echo, ভাষার pack ও Android speech service-এর ওপর নির্ভরশীল। Headset-এ বেশি উপযোগী। Service simultaneous playback/listening না দিলে orbit/pause button ব্যবহার করুন। Full Gemini Live বা guaranteed hardware echo cancellation নয়। Network speech enable করলে playback চলার সময় microphone audio-ও speech provider-এ যেতে পারে। Background/lock-এ voice session বন্ধ হয়।

API quota/credit শেষের error phone action-এর permission error নয়। App launch-এর সাধারণ নির্দিষ্ট নির্দেশ এখন local router দিয়ে AI credit ছাড়াও চলে। Chat/অন্যান্য AI planning-এ valid provider key প্রয়োজন। কোনো key chat/screenshot-এ দেবেন না।

Physical acceptance: নিজের test number-এ একটি SMS, নিজের WhatsApp chat-এ একটি message, saved Bengali/English name, duplicate-name ambiguity, SIM choice, revoke permission, interruption/echo, pause/background পরীক্ষা করুন। CI-এ কোনো বাস্তব contact-এ SMS/call/message পাঠানো হয় না।


## 0.4 Auto-chat এবং স্থির voice

Settings → SMS / WhatsApp auto-chat → saved contact/নম্বর → channel → কী বিষয়ে কথা বলবে → সময় ও reply limit → Start। চাইলে প্রথম message আলাদা field-এ দিন। SMS receive/read/send permissions, default SMS SIM ও Direct messaging লাগবে। WhatsApp-এর Notification access নিজে চালু করুন; ফোনে WhatsApp notification ও Reply action থাকতে হবে। AI reply [BUSHRA AI] নামে যাবে। নির্দিষ্ট contact-এর নতুন message ছাড়া কেউ auto-reply পাবে না। Groups বাদ; অন্য channel-এ fallback নেই। নির্বাচিত message ও ওই chat-এর সীমিত context AI provider-এ যায়। App খোলা না থাকলেও WorkManager কাজ করতে পারে; Android battery restriction-এ দেরি হতে পারে। Error/quota শেষ হলে rule থামে, auto-resend নেই। Panel থেকে contact Stop/সব Stop, অথবা মূল Stop ব্যবহার করুন। ইতিমধ্যে dispatch হওয়া message ফেরানো যায় না।

“রাকিব ব্রো কী বলেছে, WhatsApp-এর message পড়ে বলো” বললে Notification access-এ থাকা message পড়া যাবে; পুরো WhatsApp history নয়। SMS read permission দিলে নির্দিষ্ট contact-এর সাম্প্রতিক SMS পড়া যায়।

Default voice স্থির: playback চলার সময় দ্বিতীয় system recognizer চলে না; silence-এ একই recognizer restart করতে থাকে না। স্থানীয় voice detector অপেক্ষা করে; বারবার false wake হলে pause করে। Android speech provider নিজের per-turn start/end sound করতে পারে; ফোনের সব system sound mute করা হয় না। Optional hands-free interruption local AEC/headset-dependent: থামার পরে পুরো নির্দেশ বলুন। Physical TECNO test আবশ্যক।
