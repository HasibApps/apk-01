# Study Tracker — APK

HSC Study Reminder & Lecture Tracker (in-app YouTube player + yt-dlp downloader)।

**সরাসরি APK ডাউনলোড:** https://github.com/HasibApps/apk-01/releases/latest

| | |
|---|---|
| Package | `com.aistudio.studytracker.kzrm` |
| Version | 1.0 (versionCode 1) |
| ABI | arm64-v8a |
| Min Android | 7.0 (API 24) |
| Target SDK | 36 |
| সাইজ | ~58 MB |

## GitHub Actions দিয়ে বিল্ড

`.github/workflows/build-apk.yml` প্রতিটি `main` / `arena/**` ব্রাঞ্চ পুশে এবং
Actions ট্যাব থেকে ম্যানুয়াল "Run workflow"-তে চলে:

1. JDK 21 + Gradle 9.3.1 সেটআপ
2. `debug.keystore` রিস্টোর (বেস64: `keystore/debug.keystore.b64`)
3. `gradle assembleDebug`
4. APK যাচাই (badging, ABI, signature)
5. Artifact আপলোড + GitHub Release `v1.0.0`-এ APK পাবলিশ

লোকাল বিল্ডের জন্য দেখুন `TERMUX_BUILD.md`।

---

<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/91c45f65-7e3a-4acb-acba-6926824322e2

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device
7. If you have already published your app in AI Studio, please [request upload key reset](https://support.google.com/googleplay/android-developer/answer/9842756#zippy=%2Crequest-an-upload-key-reset) in Google Play Console.
