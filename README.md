# Study Tracker (Study App YTDL)

**Study Tracker** — HSC Study Reminder & Lecture Tracker, in-app YouTube player এবং yt-dlp downloader সহ একটি Android অ্যাপ।

> এই রিপোজিটরির `Study_App_YTDL_v4nov,25.zip` সোর্স ব্যবহার করে **v3.0.0** বিল্ড/রিলিজ করা হয়েছে।

## 📥 APK ডাউনলোড (v3.0.0)

| ফিল্ড | মান |
|---|---|
| **Version** | `3.0.0` (`versionCode 3`) |
| **Package** | `com.aistudio.studytracker.kzrm` |
| **APK** | `StudyTracker-v3.0.0-arm64-v8a.apk` |
| **ABI** | `arm64-v8a` |
| **Min Android** | Android 7.0 (API 24) |
| **Target / Compile SDK** | 36 |
| **Signing** | Same persistent signing keystore (certificate-backed) |
| **Release Tag** | `v3.0.0` |

**সরাসরি ডাউনলোড:**  
➡️ [StudyTracker-v3.0.0-arm64-v8a.apk (GitHub Release)](https://github.com/HasibApps/apk-01/releases/download/v3.0.0/StudyTracker-v3.0.0-arm64-v8a.apk)

**Release page:**  
➡️ [v3.0.0 Release](https://github.com/HasibApps/apk-01/releases/tag/v3.0.0)

## 🔐 Signing / আপডেট নোট

- এই APK **একই signing keystore/certificate** দিয়ে সাইন করা হয়েছে।
- তাই ভবিষ্যতে **v4.0.0** একই কী দিয়ে রিলিজ করলে ব্যবহারকারীরা **আনইনস্টল না করেই** v3.0.0 থেকে v4.0.0-এ আপডেট করতে পারবেন।
- Signing keystore source: `keystore/debug.keystore.b64` (CI-তে decode করে ব্যবহার করা হয়)।
- Workflow build-এ keystore fingerprint verify করা হয় (Actions log/summary-তে দেখা যাবে)।

## ✅ ইনস্টল করার নিয়ম

1. উপরের Release লিংক থেকে `StudyTracker-v3.0.0-arm64-v8a.apk` ডাউনলোড করুন।
2. Android ফোনে **Unknown sources / Install unknown apps** অনুমতি দিন।
3. APK ফাইল খুলে **Install** বা **Update** চাপুন।
4. আগে থেকে v3.0.0 ইনস্টল থাকা থাকলে, একই signing key থাকায় সরাসরি আপডেট হবে।

## ✨ মূল ফিচার

- HSC Study Reminder & Lecture Tracker
- In-app YouTube player
- yt-dlp ভিত্তিক video downloader integration
- Jetpack Compose ভিত্তিক Android UI
- Firebase / Media3 / Retrofit / Room সহ modern Android stack

## 🛠️ Build তথ্য

- **Build system:** Gradle
- **JDK:** 21
- **Android Gradle setup:** compileSdk 36, minSdk 24, targetSdk 36
- **ABI filter:** `arm64-v8a`
- **CI:** GitHub Actions (`.github/workflows/build-apk.yml`)
- **Build command (CI):** `gradle assembleRelease`

### GitHub Actions দিয়ে বিল্ড

1. Repository-তে push করুন বা Actions tab থেকে **Build APK** workflow manually run করুন।
2. Workflow স্বয়ংক্রিয়ভাবে:
   - signing keystore restore করবে,
   - signed release APK build করবে,
   - APK verify করবে,
   - GitHub Release (`v3.0.0`) এ APK আপলোড করবে।

## 📁 গুরুত্বপূর্ণ ফাইল

- `Study_App_YTDL_v4nov,25.zip` — সোর্স zip
- `app/build.gradle.kts` — app config, version, signing
- `.github/workflows/build-apk.yml` — CI build + release workflow
- `keystore/debug.keystore.b64` — persistent signing keystore (base64)
- `debug.keystore` — local/CI decoded keystore (gitignored)

## 🔧 Local Development

**Prerequisites:** [Android Studio](https://developer.android.com/studio), JDK 21, Android SDK

1. Repo clone/open করুন।
2. `.env.example` দেখে প্রয়োজনে `.env` তৈরি করুন (যেমন `GEMINI_API_KEY`)।
3. Android Studio দিয়ে project sync করুন।
4. Debug build চালাতে:

   ```bash
   gradle assembleDebug
   ```

5. Signed release build (same keystore) চালাতে:

   ```bash
   gradle assembleRelease
   ```

> নোট: `debug.keystore` gitignored; CI-তে `keystore/debug.keystore.b64` থেকে restore করা হয়।

## 📌 Release Checklist (v3.0.0)

- [x] Source: `Study_App_YTDL_v4nov,25.zip`
- [x] Version set: `3.0.0 (3)`
- [x] Signing certificate configured (same persistent key)
- [x] GitHub Actions workflow configured
- [x] GitHub Release upload configured (`v3.0.0`)
- [x] v3 -> v4 update continuity maintained (same signing key)
