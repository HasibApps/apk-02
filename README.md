<div align="center">

<img src="assets/app-logo.png" alt="Hasib_bro — PDF Gallery logo" width="230" />

# 📕 PDF Gallery — Hasib_bro

**Native Android PDF Reader · Snippet Cropper · Google Lens Text Assistant · Unlimited Notes**

![Version](https://img.shields.io/badge/version-v3.0.0-red?style=for-the-badge)
![Signed](https://img.shields.io/badge/signed-release%20APK-blue?style=for-the-badge)
![Android](https://img.shields.io/badge/Android-7.0%2B-green?style=for-the-badge)
![Kotlin](https://img.shields.io/badge/Kotlin%20%2B%20Compose-Material%203-purple?style=for-the-badge)

[⬇️ **v3.0.0 APK ডাউনলোড**](https://github.com/HasibApps/apk-02/releases/download/v3.0.0/PDF-Gallery-v3.0.0-release.apk) ·
[📦 **GitHub Release**](https://github.com/HasibApps/apk-02/releases/tag/v3.0.0) ·
[🔗 **Latest APK**](https://github.com/HasibApps/apk-02/releases/latest/download/PDF-Gallery-release.apk) ·
[🧾 **SHA256SUMS.txt**](https://github.com/HasibApps/apk-02/releases/download/v3.0.0/SHA256SUMS.txt)

</div>

---

## 📲 APK ডিটেইলস

| বিষয় | তথ্য |
| --- | --- |
| **অ্যাপের নাম** | **PDF Gallery** (Hasib_bro) |
| **ভার্সন** | **v3.0.0** (`versionCode = 3`) |
| **প্যাকেজ আইডি (`applicationId`)** | `com.hasib.pdfgallery` *(v2.0.0 ও v4.0.0-এও একই)* |
| **সোর্স আর্কাইভ** | `pdf-gallery-text-handel.zip` |
| **রিলিজ ফাইল** | [`PDF-Gallery-v3.0.0-release.apk`](https://github.com/HasibApps/apk-02/releases/download/v3.0.0/PDF-Gallery-v3.0.0-release.apk) |
| **স্থায়ী Latest ফাইল** | [`PDF-Gallery-release.apk`](https://github.com/HasibApps/apk-02/releases/latest/download/PDF-Gallery-release.apk) |
| **সাইজ** | **55.50 MiB** (58.19 MB · 58,192,491 bytes) — অফলাইন ML Kit OCR মডেলসহ |
| **আর্কিটেকচার (ABI)** | **Universal** (সকল 32-bit ও 64-bit Android ডিভাইসে চলবে) |
| **Android সংস্করণ** | `minSdk 24` (**Android 7.0**) → `targetSdk 36` / `compileSdk 36.1` (**Android 16**) |
| **টেক স্ট্যাক** | Kotlin `2.2.10` · Jetpack Compose (Material 3) · ML Kit OCR · OkHttp · AGP `9.1.1` · Gradle `9.3.1` |
| **বিল্ড টাইপ** | `release` (**Signed & Verified**) — GitHub Actions দিয়ে বিল্ড |
| **সাইনিং স্কিম** | APK Signature Scheme **v2 + v3** (`RSA 2048` / `SHA256withRSA`) |
| **সার্টিফিকেট SHA-256** | `80:7F:93:B3:8B:7E:B4:AB:B7:30:B2:EF:13:5C:83:26:02:A9:B4:BA:40:00:39:4B:D0:5D:56:59:8E:E6:49:3A` |
| **APK SHA-256** | `27845ad84521f46a9e2698b189ebb53d8a4f4c833a9f32e77b349eb6a56ec6ec` |
| **বিল্ড তথ্য** | [GitHub Actions Run #3](https://github.com/HasibApps/apk-02/actions/runs/44263545316) · Commit [`5437b6a`](https://github.com/HasibApps/apk-02/commit/5437b6a965153b966741941fc01ac08706f02c96) · `18 Nov 2026, 08:06 UTC` |
| **অ্যাপ লোগো** | ব্যবহারকারীর দেওয়া ছবি — [`assets/app-logo.png`](assets/app-logo.png) (লঞ্চার আইকন, টুলবার ও Lens বারে ব্যবহৃত) |

<details>
<summary><b>🔍 APK-এর SHA-256 চেকসাম ও সিগনেচার যাচাই করবেন যেভাবে</b></summary>

```bash
# Linux / macOS / Termux — ফাইল চেকসাম যাচাই
sha256sum PDF-Gallery-v3.0.0-release.apk
# -> 27845ad84521f46a9e2698b189ebb53d8a4f4c833a9f32e77b349eb6a56ec6ec

# Windows PowerShell — ফাইল চেকসাম যাচাই
Get-FileHash PDF-Gallery-v3.0.0-release.apk -Algorithm SHA256

# Android SDK build-tools — সাইনিং সার্টিফিকেট যাচাই
apksigner verify --verbose --print-certs PDF-Gallery-v3.0.0-release.apk
# SHA-256 digest অবশ্যই উপরের সার্টিফিকেট SHA-256-এর সাথে মিলতে হবে
```

> নোট: নতুন ভার্সন বিল্ড হলে APK ফাইলের SHA-256 বদলাবে (সবসময় রিলিজের `SHA256SUMS.txt` দেখুন), কিন্তু **Signing Certificate SHA-256 অপরিবর্তিত থাকবে**।

</details>

---

## 🆕 v3.0.0-এ যা নতুন

| নতুনত্ব | বিস্তারিত |
| --- | --- |
| 🆕 **অটো এজেন্ট – শুরুর পৃষ্ঠা** | নতুন `AgentStartPageActivity` ডায়ালগে Lens Agent চালু করার আগে শুরুর পৃষ্ঠা নম্বর লেখা যায় — বাংলা (`১`, `০১`) বা ASCII অঙ্ক, লাইভ প্রিভিউসহ |
| 🆕 **কপি করা টেক্সট অটো-ফরম্যাট** | Google Lens থেকে কপি করা প্রতিটি টেক্সট আনলিমিটেড নোটে স্বয়ংক্রিয়ভাবে `[ পৃষ্ঠা : ০১ ]`, `[ পৃষ্ঠা : ০২ ]` … ক্রমিক বাংলা পৃষ্ঠা-ট্যাগসহ যুক্ত হয়; এজেন্ট স্ট্যাটাস বারে বর্তমান নোট-পৃষ্ঠা দেখায় |
| 🆕 **নতুন Hasib_bro লোগো** | উপরের ছবিটি অ্যাপের Adaptive launcher icon (সব `mipmap-*` ডেনসিটি), টুলবার আইকন ও Google Lens নেভিগেটর বারে ব্যবহার করা হয়েছে |
| 🆕 **টেক্সট হ্যান্ডেল উন্নতি** | `TextHistoryManager`-ভিত্তিক ৬০-স্টেপ Undo/Redo (কার্সর পজিশনসহ), দ্রুত টাইপিংয়ে স্ন্যাপশট মার্জ, বাংলা/ASCII পেজ নম্বর পার্সিং |

---

## ✨ পূর্ণ ফিচারসমূহ

| ফিচার | বিস্তারিত |
| --- | --- |
| 🎨 **Jetpack Compose Material 3 UI** | সম্পূর্ণ নেটিভ Compose ইন্টারফেস — Light, Dark ও System থিম সাপোর্ট |
| 🗂️ **হোম লাইব্রেরি ও ফোল্ডার ম্যানেজার** | ডিভাইসের MediaStore PDF, ইমপোর্ট করা ও ডাউনলোড করা PDF অটো-স্ক্যান; কাস্টম ফোল্ডার, মাল্টি-সিলেক্ট মুভ, ফেভারিট (★), দ্রুত সার্চ, রিনেম, PDF কমপ্রেস ও হোম-স্ক্রিন শর্টকাট |
| ☁️ **Google Drive ও URL PDF ডাউনলোডার** | যেকোনো PDF/Google Drive লিংক বা অন্য অ্যাপের **Share** মেনু থেকে সরাসরি ইমপোর্ট, অটো ক্লিপবোর্ড পেস্ট, Drive টাইটেল ও কভার সংগ্রহ, ব্যাকগ্রাউন্ড সার্ভিসে Pause/Resume ডাউনলোড এবং Photo Picker দিয়ে কাস্টম কভার ছবি সেট |
| 📖 **হাই-প্রিসিশন PDF ভিউয়ার** | `PdfRenderer`-ভিত্তিক মসৃণ ভার্টিক্যাল পেজ স্ট্রিমিং, পিঞ্চ-টু-জুম, ডাবল-ট্যাপ জুম, ডান পাশে ড্র্যাগযোগ্য ফাস্ট-স্ক্রল বার (পেজ টুলটিপসহ), সরাসরি পেজ জাম্প ডায়ালগ ও PDF শেয়ার |
| ✂️ **পেজ ক্রপ ও লসলেস স্নিপেট** | ইন্টারঅ্যাক্টিভ ক্রপ ওভারলে, **Invert** ও **B&W** ফিল্টার, `Pictures/PDF_Gallery`-তে JPEG সেভ, লসলেস ট্রান্সপারেন্ট ইঙ্ক এক্সট্রাকশন এবং কাস্টম প্রম্পটসহ সরাসরি AI অ্যাপে শেয়ার |
| 🔍 **Google Lens ও স্ক্রিন স্নিপেট ওভারলে** | অফিশিয়াল Google Lens-এ পেজ পাঠানো, ভাসমান **Prev / Next / Note** পেজ নেভিগেটর বার, Accessibility অটো-কপি হেল্পার, অফলাইন ML Kit OCR ফলব্যাক এবং `MediaProjection` স্ক্রিন স্নিপেট ক্যাপচার |
| 🤖 **অটো Lens Agent** | একাধিক পেজ থেকে স্বয়ংক্রিয়ভাবে টেক্সট কপি করে আনলিমিটেড নোটে ক্রমিক পৃষ্ঠা-ট্যাগসহ জমা করা; শুরুর পৃষ্ঠা নির্বাচন, থামানো/চালু করা ও প্রগ্রেস দেখা যায় |
| 📝 **আনলিমিটেড নোট ও ২-কলাম চ্যাপ্টার ইনডেক্স** | **Noto Serif Bengali** ফন্টে বাংলা ও ইংরেজি নোট, Undo/Redo হিস্ট্রি, KaTeX গণিত/বিজ্ঞান ফর্মুলা প্রিভিউ, রাসায়নিক ও পদার্থবিজ্ঞানের চিহ্ন অটো-ফরম্যাটিং, ক্লিকযোগ্য `(পৃষ্ঠা= N)` ট্যাগ, ১-কলাম / ২-কলাম চ্যাপ্টার ইনডেক্স ও `Downloads`-এ `.txt` এক্সপোর্ট |
| 💾 **পূর্ণ ব্যাকআপ ও রিস্টোর** | ফোল্ডার, ফেভারিট, URL তালিকা ও নোটের সম্পূর্ণ JSON ব্যাকআপ এক্সপোর্ট ও এক ট্যাপে রিস্টোর |

---

## 🔐 সাইনিং সার্টিফিকেট ও নির্বিঘ্ন আপডেট

<div align="center">

**`v2.0.0` → `v3.0.0` → `v4.0.0` — একই `com.hasib.pdfgallery`, একই Signing Key** ✅

</div>

এই রিপোজিটরিতে [`signing/release-keystore.b64`](signing/release-keystore.b64) হিসেবে অ্যাপের স্থায়ী রিলিজ সাইনিং সার্টিফিকেট সংরক্ষিত আছে। GitHub Actions ওয়ার্কফ্লো প্রতিটি রিলিজ **একই কী** দিয়ে সাইন করে, `apksigner` দিয়ে যাচাই করে এবং সার্টিফিকেটের SHA-256 fixed fingerprint-এর সাথে মেলে কি না চেক করে (না মিললে বিল্ড ফেল করে)।

ফলে **একই Package ID** এবং **একই Signing Certificate** থাকায়:

- `v2.0.0` ইনস্টল করা ব্যবহারকারীরা আনইনস্টল না করেই `v3.0.0` ইনস্টল (আপডেট) করতে পারবেন;
- **`v3.0.0` আনইনস্টল না করেই `v4.0.0` আপডেট পাবেন** — Android সরাসরি in-place update করবে;
- অ্যাপের সব নোট, ফোল্ডার, ফেভারিট, ইতিহাস ও ডাউনলোড করা PDF অক্ষত থাকবে।

| Field | Value |
| --- | --- |
| **Keystore Type** | `PKCS12` |
| **Key / Signature Algorithm** | `RSA 2048` / `SHA256withRSA` |
| **Key Alias** | `pdfgallery` |
| **Subject (DN)** | `CN=Hasib Apps, OU=Android, O=HasibApps, L=Dhaka, ST=Dhaka, C=BD` |
| **মেয়াদ (Validity)** | `18 Nov 2026` → `05 Apr 2054` (১০,০০০ দিন) |
| **Certificate SHA-256** | `80:7F:93:B3:8B:7E:B4:AB:B7:30:B2:EF:13:5C:83:26:02:A9:B4:BA:40:00:39:4B:D0:5D:56:59:8E:E6:49:3A` |
| **Certificate SHA-1** | `98:1D:4C:08:D1:AA:63:FD:1B:8E:62:68:CE:0E:79:11:0B:B7:A2:3B` |

> 🔒 **GitHub Secrets সাপোর্ট:** ওয়ার্কফ্লো প্রথমে রিপোর `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` Secrets খোঁজে, না থাকলে `signing/release-keystore.b64` ব্যবহার করে। বিস্তারিত: [`signing/README.md`](signing/README.md)।

---

## 📥 ইনস্টল ও আপডেট করার নিয়ম

1. উপরের [**PDF-Gallery-v3.0.0-release.apk**](https://github.com/HasibApps/apk-02/releases/download/v3.0.0/PDF-Gallery-v3.0.0-release.apk) লিংক থেকে APK ফাইলটি ডাউনলোড করুন।
2. ফাইলটিতে ট্যাপ করুন — ব্রাউজার বা ফাইল ম্যানেজারে **Install unknown apps / অজানা উৎস** অনুমতি চাইলে **Allow** দিন।
3. **Install** চাপুন এবং শেষে **Open** করুন। `v2.0.0` থেকে আসলে Android সরাসরি আপডেট হিসেবে ইনস্টল করবে।
4. Google Play Protect সতর্কতা দেখালে **More details → Install anyway** চাপুন (স্ব-স্বাক্ষরিত APK হওয়ায় এটি স্বাভাবিক)।
5. **পরবর্তী `v4.0.0`:** নতুন APK-টি ডাউনলোড করে ইনস্টল করলেই আপডেট হয়ে যাবে — `v3.0.0` আনইনস্টল করার দরকার নেই, ডেটাও মুছবে না।

> ⚠️ শুধু প্রথমবার: যদি আগে ভিন্ন প্যাকেজ নামে (`com.example` / `com.aistudio.*`) বা অস্থায়ী debug কী-তে বিল্ড করা পুরোনো কোনো APK ইনস্টল থাকে, সেটি একবার আনইনস্টল করে (প্রয়োজনে অ্যাপের ভেতর থেকে JSON ব্যাকআপ নিয়ে) এই সাইনড APK ইনস্টল করুন।

---

## 🛡️ ব্যবহৃত পারমিশনসমূহ

অ্যাপের ভেতরের **Settings & Permissions** পেজ থেকে যেকোনো সময় পারমিশনগুলো নিয়ন্ত্রণ করা যায়:

- **Internet & Network State** — Google Drive ও সরাসরি URL থেকে PDF ডাউনলোড করার জন্য
- **Read External Storage** *(Android 12 ও তার নিচে)* — ডিভাইসের PDF ফাইল স্ক্যান করার জন্য
- **Post Notifications & Foreground Service** — ব্যাকগ্রাউন্ডে PDF ডাউনলোড প্রগ্রেস ও ভাসমান টুলবার চালু রাখার জন্য
- **Display Over Other Apps (`SYSTEM_ALERT_WINDOW`)** — Google Lens পেজ নেভিগেটর বার ও ফ্লোটিং স্নিপেট বাটনের জন্য *(ঐচ্ছিক)*
- **Accessibility Service** — শুধু Google / Google Lens স্ক্রিনে স্বয়ংক্রিয়ভাবে টেক্সট সিলেক্ট ও কপি করার জন্য *(ঐচ্ছিক)*
- **Install Shortcut** — যেকোনো PDF-এর শর্টকাট হোম স্ক্রিনে যোগ করার জন্য *(ঐচ্ছিক)*

---

## ⚙️ GitHub Actions CI/CD ও নতুন ভার্সন রিলিজ

ওয়ার্কফ্লো ফাইল: [`.github/workflows/build.yml`](.github/workflows/build.yml) — **Build & Release APK**

প্রতিটি কোড পুশে অথবা **Actions → Build & Release APK → Run workflow** চাপলে স্বয়ংক্রিয়ভাবে:

1. Temurin JDK 17, Android SDK (API 36.1) ও Gradle `9.3.1` সেটআপ হয়
2. স্থায়ী keystore বের করে **সার্টিফিকেট ফিঙ্গারপ্রিন্ট যাচাই** করা হয় (v2/v3/v4 সব জায়গায় একই কী নিশ্চিত করতে)
3. সাইন করা `release` APK বিল্ড হয় (`gradle :app:assembleRelease`)
4. `apksigner` দিয়ে APK Signature Scheme (`v2 + v3`) ও সার্টিফিকেট যাচাই করে `SHA256SUMS.txt` তৈরি হয়
5. GitHub Actions Artifact এবং **GitHub Releases**-এ `v<versionName>` ট্যাগে সাইনড APK, `PDF-Gallery-release.apk`, `SHA256SUMS.txt` ও `app-logo.png` আপলোড হয়

### `v4.0.0` রিলিজ করতে

`app/build.gradle.kts`-এ শুধু দুই লাইন বদলান —

```kotlin
versionCode = 4
versionName = "4.0.0"
```

— `applicationId` (`com.hasib.pdfgallery`) ও [`signing/release-keystore.b64`](signing/release-keystore.b64) **অপরিবর্তিত** রেখে পুশ করুন। ওয়ার্কফ্লো একই সাইনিং কী দিয়ে `v4.0.0` বিল্ড করে Release-এ আপলোড করবে এবং ব্যবহারকারীরা `v3.0.0` আনইনস্টল না করেই আপডেট পাবেন।

### লোকাল বিল্ড

```bash
# JDK 17 + Android SDK 36.1 + Gradle 9.3.1 প্রয়োজন
gradle :app:assembleRelease
# আউটপুট: app/build/outputs/apk/release/app-release.apk (স্বয়ংক্রিয়ভাবে সাইনড)
# signing/release-keystore.b64 থেকে keystore নিজে decode হয়
```

---

## 🔗 গুরুত্বপূর্ণ লিংকসমূহ

- 📦 **Repository:** https://github.com/HasibApps/apk-02
- 🚀 **Latest Release (v3.0.0):** https://github.com/HasibApps/apk-02/releases/tag/v3.0.0
- ⬇️ **Direct APK Download:** https://github.com/HasibApps/apk-02/releases/download/v3.0.0/PDF-Gallery-v3.0.0-release.apk
- 📄 **v2.0.0 Release:** https://github.com/HasibApps/apk-02/releases/tag/v2.0.0
- ⚙️ **GitHub Actions Workflow:** https://github.com/HasibApps/apk-02/actions/workflows/build.yml

---

<div align="center">Made with ❤️ by <b>Hasib_bro</b> · Built & signed automatically with GitHub Actions</div>
