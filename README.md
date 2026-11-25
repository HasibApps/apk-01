# 📚 Study Tracker — HSC Study Reminder & Lecture Tracker

**Study Tracker** একটি Android অ্যাপ, যা HSC শিক্ষার্থীদের পড়াশোনা ট্র্যাক করতে, লেকচার রিমাইন্ডার সেট করতে, এবং অ্যাপের ভেতরেই YouTube প্লেয়ার + yt-dlp ডাউনলোডার ব্যবহার করতে সাহায্য করে।

<p>
  <a href="https://github.com/HasibApps/apk-01/releases/latest">
    <img alt="Download APK" src="https://img.shields.io/badge/Download-APK%20v4.0.0-brightgreen?style=for-the-badge&logo=android" />
  </a>
  <a href="https://github.com/HasibApps/apk-01/actions/workflows/build-apk.yml">
    <img alt="Build APK" src="https://img.shields.io/github/actions/workflow/status/HasibApps/apk-01/build-apk.yml?style=for-the-badge&label=Build" />
  </a>
</p>

## ⬇️ সরাসরি ডাউনলোড

| Release | Link |
|---|---|
| **Latest (v4.0.0)** | [StudyTracker-v4.0.0-arm64-v8a.apk](https://github.com/HasibApps/apk-01/releases/download/v4.0.0/StudyTracker-v4.0.0-arm64-v8a.apk) |
| All Releases | [GitHub Releases](https://github.com/HasibApps/apk-01/releases) |

## 📦 APK Details (v4.0.0)

| Field | Value |
|---|---|
| **App Name** | Study Tracker |
| **Package** | `com.aistudio.studytracker.kzrm` |
| **Version** | `4.0.0` |
| **Version Code** | `4` |
| **ABI** | `arm64-v8a` |
| **Min Android** | Android 7.0 (API 24) |
| **Target SDK** | 36 |
| **Compile SDK** | 36 |
| **Signing** | Same persistent keystore as v3.0.0 ✅ |
| **Source** | `Study_App_YTDL_v5nov25.zip` |
| **Release Tag** | [`v4.0.0`](https://github.com/HasibApps/apk-01/releases/tag/v4.0.0) |

## ✨ মূল ফিচার

- 📅 **Study Reminder & Lecture Tracker** — HSC পড়াশোনার রুটিন/লেকচার ট্র্যাক করা।
- ▶️ **In-app YouTube Player** — অ্যাপের ভেতরেই YouTube ভিডিও দেখা।
- ⬇️ **yt-dlp Downloader** — yt-dlp ইঞ্জিন ব্যবহার করে ভিডিও/অডিও ডাউনলোড সাপোর্ট।
- 📱 **arm64-v8a Optimized** — modern Android ডিভাইসের জন্য অপ্টিমাইজড APK।

## 🔄 আপডেট নির্দেশনা (v3.0.0 → v4.0.0)

> ✅ **গুরুত্বপূর্ণ:** v4.0.0 APK টি **v3.0.0 এর একই signing key** দিয়ে সাইন করা হয়েছে।

এর মানে:
- যারা **v3.0.0** ইনস্টল করে আছেন, তারা সরাসরি **v4.0.0** ইনস্টল/আপডেট করতে পারবেন।
- **আনইনস্টল করার দরকার নেই।**
- আগের অ্যাপ ডাটা/সেটআপ স্বাভাবিকভাবেই থাকবে (Android package/signature একই থাকায়)।

### ইনস্টল / আপডেট ধাপ
1. [v4.0.0 APK](https://github.com/HasibApps/apk-01/releases/download/v4.0.0/StudyTracker-v4.0.0-arm64-v8a.apk) ডাউনলোড করুন।
2. ফোনে **Unknown sources / Install unknown apps** অনুমতি দিন।
3. APK ফাইল খুলে **Install** বা **Update** চাপুন।
4. ইনস্টল শেষে অ্যাপটি চালু করুন।

## 🔐 Signing & Security Note

- এই প্রজেক্টে persistent signing keystore (`keystore/debug.keystore.b64`) ব্যবহার করা হয়।
- v3.0.0 এবং v4.0.0 **একই keystore + একই alias** (`androiddebugkey`) দিয়ে সাইন করা হয়েছে।
- GitHub Actions workflow release build করার সময় এই একই keystore restore করে `assembleRelease` তৈরি করে।
- Release-এর সাথে `sha256.txt` দেওয়া থাকে, যাতে APK integrity verify করা যায়।

## 🛠️ GitHub Actions Build

Workflow: [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml)

এই workflow স্বয়ংক্রিয়ভাবে চলে:
- `main`, `master`, `arena/**` ব্রাঞ্চে push হলে
- অথবা Actions ট্যাব থেকে manual **Run workflow** করলে

### Build Pipeline
1. Checkout repository
2. JDK 21 setup
3. Android SDK licenses accept
4. Persistent signing keystore restore (`keystore/debug.keystore.b64`)
5. Gradle 9.3.1 দিয়ে `assembleRelease` (signed APK)
6. APK verify (badging, ABI, signature)
7. APK artifact upload
8. GitHub Release-এ APK + `sha256.txt` publish

## 🧪 Local Build

লোকাল বিল্ডের জন্য `TERMUX_BUILD.md` দেখুন। সাধারণভাবে Android/Gradle environment সেটআপ করে release build করা যাবে:

```bash
# Signing env vars (same key as v3/v4)
export KEYSTORE_PATH="$(pwd)/debug.keystore"
export STORE_PASSWORD="android"
export KEY_ALIAS="androiddebugkey"
export KEY_PASSWORD="android"

# decode persistent keystore
base64 -d keystore/debug.keystore.b64 > debug.keystore

# build
gradle assembleRelease
```

Build output:
```
app/build/outputs/apk/release/app-release.apk
```

## 📁 Repository Notes

- `Study_App_YTDL_v5nov25.zip` — v4.0.0 build-এর source zip।
- `keystore/debug.keystore.b64` — persistent signing keystore (v3/v4 update continuity এর জন্য অপরিহার্য)।
- `.github/workflows/build-apk.yml` — CI build + release workflow।

---

**Study Tracker v4.0.0** — built from `Study_App_YTDL_v5nov25.zip`, signed with the same persistent key as v3.0.0 for seamless updates.
