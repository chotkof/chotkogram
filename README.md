# ChotkoGram for Android

![ChotkoGram Logo](logo.png)

## What's this fork even about?

**Is it just exteraGram+Telegraher?**

**ChotkoGram** is a fork of [exteraGram](https://github.com/exteraSquad/exteraGram) with
some patches from [Telegraher](https://github.com/nikitasius/Telegraher).

## How to build

1. Clone source code using `git clone https://github.com/ChotkoGram/ChotkoGram.git`
2. Open the project in Android Studio. It should be opened, **not imported**
3. Implement the `AyuMessageUtils` & `AyuHistoryHook` classes. It's not that hard, but if you're
   making your **very** own fork, then you should take some time to write this part of code. Or you can search for a reversed version :)
4. Replace `google-services.json` (we don't want to see crash reports from your app...)
5. Generate application certificate and fill API_KEYS:
   ```
   APP_ID = 6
   APP_HASH = "eb06d4abfb49dc3eeb1aeb98ae0f581e"
   MAPS_V2_API = abcdef12345678
   
   SIGNING_KEY_PASSWORD = password
   SIGNING_KEY_ALIAS = alias
   SIGNING_KEY_STORE_PASSWORD = password
   ```
6. You are ready to compile `ChotkoGram`

- **ChotkoGram** can be built with **Android Studio** or from the command line with **Gradle**:

```
./gradlew assembleAfatRelease
```

## ChotkoGram Localization

[![Crowdin](https://badges.crowdin.net/chotkogram/localized.svg)](https://crowdin.com/project/chotkogram)
[![Crowdin](https://badges.crowdin.net/exteralocales/localized.svg)](https://crowdin.com/project/exteralocales)

We have our own **[Crowdin](https://crowdin.com/project/chotkogram)**.

But since **ChotkoGram** is based on **exteraGram**, also join their project
at **[Crowdin](https://crowdin.com/project/exteralocales)**!

## Credits

- **[exteraGram](https://github.com/exteraSquad/exteraGram)**
- [Telegraher](https://github.com/nikitasius/Telegraher)
- [Cherrygram](https://github.com/arsLan4k1390/Cherrygram)
- [Nagram](https://github.com/NextAlone/Nagram)
- [Telegram FOSS](https://github.com/Telegram-FOSS-Team/Telegram-FOSS)
