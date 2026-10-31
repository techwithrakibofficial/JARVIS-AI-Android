# BUSHRA AI 0.3.0 — ফোন setup

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
