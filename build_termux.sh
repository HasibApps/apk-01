#!/data/data/com.termux/files/usr/bin/bash
# Termux এ Study Tracker APK বিল্ড স্ক্রিপ্ট
# ব্যবহার:  bash build_termux.sh
set -e

GRADLE_VERSION="9.3.1"
GRADLE_HOME_DIR="$HOME/gradle-$GRADLE_VERSION"
SDK_DIR="$HOME/android-sdk"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
PREFIX="${PREFIX:-/data/data/com.termux/files/usr}"

echo "==> [1/6] প্যাকেজ ইনস্টল হচ্ছে (openjdk-21, aapt2, wget, unzip)"
pkg update -y
pkg install -y openjdk-21 aapt2 wget unzip

export JAVA_HOME="$PREFIX/lib/jvm/java-21-openjdk"
export ANDROID_HOME="$SDK_DIR"
export ANDROID_SDK_ROOT="$SDK_DIR"

echo "==> [2/6] Gradle $GRADLE_VERSION"
if [ ! -x "$GRADLE_HOME_DIR/bin/gradle" ]; then
  cd "$HOME"
  wget -c "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  unzip -q -o "gradle-$GRADLE_VERSION-bin.zip"
  rm -f "gradle-$GRADLE_VERSION-bin.zip"
fi
export PATH="$GRADLE_HOME_DIR/bin:$PATH"

echo "==> [3/6] Android SDK লাইসেন্স"
mkdir -p "$SDK_DIR/licenses"
printf "\n24333f8a63b6825ea9c5514f83c2829b004d1fee\nd56f5187479451eabf01fb78af6dfcb131a6481e\n8933bad161af4178b1185d1a37fbf41ea5269c55\n" > "$SDK_DIR/licenses/android-sdk-license"

cd "$PROJECT_DIR"

echo "==> [4/6] local.properties ও debug.keystore"
echo "sdk.dir=$SDK_DIR" > local.properties
if [ ! -f debug.keystore ]; then
  keytool -genkey -v -keystore debug.keystore -storepass android \
    -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 \
    -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
fi

echo "==> [5/6] APK বিল্ড হচ্ছে (প্রথমবার ২০-৪০ মিনিট লাগতে পারে)"
# RAM কম হলে নিচের -Xmx2g কমিয়ে -Xmx1536m করুন
gradle assembleDebug --no-daemon --max-workers=2 \
  -Pandroid.aapt2FromMavenOverride="$PREFIX/bin/aapt2" \
  -Dorg.gradle.jvmargs="-Xmx2g -Dfile.encoding=UTF-8"

echo "==> [6/6] APK কপি হচ্ছে"
APK="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK" ]; then
  if [ -d "$HOME/storage/downloads" ]; then
    cp "$APK" "$HOME/storage/downloads/StudyTracker.apk"
    echo "সফল! APK: Download ফোল্ডারে StudyTracker.apk"
  else
    echo "APK তৈরি হয়েছে: $PROJECT_DIR/$APK"
    echo "(Download এ কপি করতে আগে চালান: termux-setup-storage)"
  fi
else
  echo "APK পাওয়া যায়নি, উপরের এরর দেখুন।" && exit 1
fi
