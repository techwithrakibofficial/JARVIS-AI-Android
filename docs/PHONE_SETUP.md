# Phone setup (বাংলা)

1. এই repo-র Releases থেকে `BUSHRA-AI.apk` নামান। এটি Android 8+ ব্যক্তিগত development preview; Android 16-এ বাস্তব পরীক্ষা বাকি থাকতে পারে।
2. Download করা APK খুলে প্রয়োজন হলে ওই downloader/browser-এর `Install unknown apps` অনুমতি দিন।
3. অ্যাপ খুলে data disclosure পড়ুন। কোনো API key chat বা GitHub issue-তে লিখবেন না।
4. Settings > API provider যোগ করো > provider suggestion বাছুন। নাম নিজের মতো দিন। Endpoint, নিজের উপলব্ধ model ID ও API key দিন। OpenRouter model list আনতে `Model suggestions` ব্যবহার করতে পারেন। Model free কি না provider-এ যাচাই করুন।
5. Test connection চাপুন, সফল হলে Save করুন ও `Use` চাপুন। API quota, model capability বা key failure হলে সেটিং বদলে পরীক্ষা করুন। কোনো paid fallback নিজে চালু হয় না।
6. Voice output-এর জন্য Android TTS বিনা আলাদা API subscription-এ ব্যবহার করা যায়। পছন্দের ElevenLabs voice চাইলে ElevenLabs preset-এ আলাদা key বসান। Default voice ID `75CHIF904aWz6uq77J3P`; account access ও বাংলা voice/model support Test করে দেখুন। ElevenLabs quota/charge আলাদা।
7. Mic চাপলে microphone permission দিন। Offline recognizer না থাকলে Settings > Network speech অনুমতি নিজের সিদ্ধান্তে চালু করুন। ফোনে recognizer/TTS language data আলাদা লাগতে পারে।
8. Contacts > নম্বর ও ডাকনাম যোগ করুন, অথবা permission দিয়ে phone contact নির্বাচন করুন। ডাকনাম বলে বা লিখে যোগ করা যায়। আসল contact বদলায় না। একই alias আলাদা নম্বরে ব্যবহার করলে save আটকায়।
9. Screen actions চাইলে Settings > Accessibility settings > BUSHRA phone actions চালু করুন। Android sideload restrictions থাকলে আগে System Settings > Apps > BUSHRA AI > menu > Allow restricted settings লাগতে পারে। Settings-এর নাম OEM অনুযায়ী ভিন্ন হতে পারে। Developer Options/root লাগে না।
10. `WhatsApp খোলো` দিয়ে শুরু করুন। প্রতিটি বাহ্যিক action-এর approval পড়ুন। Screen text provider-এ যাওয়ার আগে আলাদা অনুমতি হয়। Password বাদ দেওয়া হয়, কিন্তু অন্যান্য private text থাকতে পারে।
11. Call shortcut dialer খোলে; Call button আপনাকে চাপতে হবে। SMS shortcut draft খোলে; Send আপনাকে চাপতে হবে। Carrier charge প্রযোজ্য।
12. Stop দিয়ে বর্তমান request/voice থামান। Screen control approval overlay-তে Cancel দিয়েও থামানো যায়। অ্যাপ গোপনে বা সারাক্ষণ microphone চালায় না।

## প্রথম acceptance tests

- API ছাড়াই Home > অ্যাপ খোলো > Settings/অন্য অ্যাপ নির্বাচন।
- Manual contact-এ নিজের নম্বর ও `আমার নম্বর` alias দিন। Dialer সঠিক নম্বর দেখায় কি না দেখুন; test call করতে হবে না।
- SMS draft নিজের নম্বরে খুলে সঠিক text দেখুন; পাঠানো প্রয়োজন নেই।
- Chat API test, বাংলা typed reply, mic transcript ও TTS আলাদা পরীক্ষা করুন।
- Screen read approval deny করলে তথ্য না পাঠিয়ে task বাতিল হয় কি না দেখুন।
- নিজের অগুরুত্বপূর্ণ text field-এ type/click/scroll পরীক্ষা করুন। Financial/login/account screens-এ পরীক্ষা করবেন না।
- Build Studio-তে ছোট single-file website-এর source ZIP তৈরি করুন। Source ZIP-কে compiled APK ভাববেন না।

## Updates and signing

CI APK development/debug key দিয়ে signed। Key Actions cache-এ থাকে; eviction হলে নতুন key হতে পারে এবং Android update reject করতে পারে। সে ক্ষেত্রে app-local data হারিয়ে reinstall লাগতে পারে। স্থায়ী distribution-এর আগে user-controlled release signing key ও GitHub Secrets setup করা উচিত। কখনো signing key repo-তে commit করবেন না।

## What is not included

Always-listening wake word, call audio access, silent SMS sending, guaranteed every-app automation, local Whisper/LLM model downloads, automatic code push/build/deploy for generated projects, or unlimited free AI are not included. Public Play Store distribution has not been prepared.
