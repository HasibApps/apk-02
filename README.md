<div align="center">

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png" width="120" alt="PDF Gallery Logo">

# 📕 PDF Gallery — v2.0.0

**অ্যাডভান্সড PDF রিডার · পেজ ক্রপ ও লসলেস স্নিপেট · Google Lens টেক্সট অ্যাসিস্ট্যান্ট · Google Drive ডাউনলোডার · ফোল্ডার ম্যানেজার ও আনলিমিটেড বাংলা/বিজ্ঞান নোট**

*Kotlin · Jetpack Compose · Material Design 3*

[![Build & Release APK](https://github.com/HasibApps/apk-02/actions/workflows/build.yml/badge.svg)](https://github.com/HasibApps/apk-02/actions/workflows/build.yml)
[![Latest Release](https://img.shields.io/github/v/release/HasibApps/apk-02?label=release&color=2ea44f)](https://github.com/HasibApps/apk-02/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/HasibApps/apk-02/total?color=0969da)](https://github.com/HasibApps/apk-02/releases)
[![APK Size](https://img.shields.io/badge/APK-56.00%20MiB%20%C2%B7%20Universal-bb2622)](#-apk-ডিটেইলস)
[![Android](https://img.shields.io/badge/Android-7.0%2B%20(API%2024--36)-3DDC84?logo=android&logoColor=white)](#-apk-ডিটেইলস)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)

### [⬇️ PDF Gallery v2.0.0 ডাউনলোড করুন (Signed APK)](https://github.com/HasibApps/apk-02/releases/download/v2.0.0/PDF-Gallery-v2.0.0-release.apk)

[**স্থায়ী Latest লিংক**](https://github.com/HasibApps/apk-02/releases/latest/download/PDF-Gallery-release.apk) · [**Release v2.0.0 পেজ →**](https://github.com/HasibApps/apk-02/releases/tag/v2.0.0) · [**SHA256SUMS.txt**](https://github.com/HasibApps/apk-02/releases/download/v2.0.0/SHA256SUMS.txt)

</div>

---

## 📲 APK ডিটেইলস

| বিষয় | তথ্য |
| --- | --- |
| **অ্যাপের নাম** | **PDF Gallery** |
| **ভার্সন** | **v2.0.0** (`versionCode = 2`) |
| **প্যাকেজ আইডি (`applicationId`)** | `com.hasib.pdfgallery` |
| **সোর্স আর্কাইভ** | `pdf-g-v5-ai.zip` |
| **রিলিজ ফাইল** | [`PDF-Gallery-v2.0.0-release.apk`](https://github.com/HasibApps/apk-02/releases/download/v2.0.0/PDF-Gallery-v2.0.0-release.apk) |
| **স্থায়ী Latest ফাইল** | [`PDF-Gallery-release.apk`](https://github.com/HasibApps/apk-02/releases/latest/download/PDF-Gallery-release.apk) |
| **সাইজ** | **56.00 MiB** (`58.72 MB` · `58,717,918 bytes`) — অফলাইন ML Kit OCR মডেলসহ |
| **আর্কিটেকচার (ABI)** | **Universal** (সকল 32-bit ও 64-bit Android ডিভাইসে চলবে) |
| **Android সংস্করণ** | `minSdk 24` (**Android 7.0**) → `targetSdk 36` / `compileSdk 36.1` (**Android 16**) |
| **টেক স্ট্যাক** | Kotlin `2.2.10` · Jetpack Compose (Material 3) · ML Kit OCR · OkHttp · AGP `9.1.1` · Gradle `9.3.1` |
| **বিল্ড টাইপ** | `release` (**Signed & Verified**) |
| **সাইনিং স্কিম** | APK Signature Scheme **v2 + v3** (`RSA 2048` / `SHA256withRSA`) |
| **সার্টিফিকেট SHA-256** | `80:7F:93:B3:8B:7E:B4:AB:B7:30:B2:EF:13:5C:83:26:02:A9:B4:BA:40:00:39:4B:D0:5D:56:59:8E:E6:49:3A` |
| **APK SHA-256** | `daa7d893a0f311052253ef33cbba0071fb12e859562c1095ec0e361a48b50f04` |
| **বিল্ড তথ্য** | [GitHub Actions Run #2](https://github.com/HasibApps/apk-02/actions/runs/44210868819) · Commit [`92580aa`](https://github.com/HasibApps/apk-02/commit/92580aa) · `18 Nov 2026, 00:52 UTC` |

<details>
<summary><b>🔍 APK-এর SHA-256 চেকসাম ও সিগনেচার যাচাই করবেন যেভাবে</b></summary>

```bash
# Linux / macOS / Termux — ফাইল চেকসাম যাচাই
sha256sum PDF-Gallery-v2.0.0-release.apk
# -> daa7d893a0f311052253ef33cbba0071fb12e859562c1095ec0e361a48b50f04

# Windows PowerShell — ফাইল চেকসাম যাচাই
Get-FileHash PDF-Gallery-v2.0.0-release.apk -Algorithm SHA256

# Android SDK build-tools — সাইনিং সার্টিফিকেট যাচাই
apksigner verify --verbose --print-certs PDF-Gallery-v2.0.0-release.apk
```

> নোট: নতুন ভার্সন বিল্ড হলে APK ফাইলের SHA-256 বদলাবে (সবসময় রিলিজের `SHA256SUMS.txt` দেখুন), কিন্তু **Signing Certificate SHA-256 অপরিবর্তিত থাকবে**।

</details>

---

## ✨ PDF Gallery v2.0.0-এর ফিচারসমূহ

| ফিচার | বিস্তারিত |
| --- | --- |
| 🎨 **Jetpack Compose Material 3 UI** | সম্পূর্ণ আধুনিক নেটিভ Compose ইন্টারফেস — Light, Dark ও System/Dynamic থিম সাপোর্ট |
| 🗂️ **হোম লাইব্রেরি ও ফোল্ডার ম্যানেজার** | ডিভাইসের MediaStore PDF, ইমপোর্ট করা ও ডাউনলোড করা PDF অটো-স্ক্যান; কাস্টম ফোল্ডার তৈরি, মাল্টি-সিলেক্ট মুভ, ফেভারিট (★), দ্রুত সার্চ, রিনেম, PDF কমপ্রেস ও হোম-স্ক্রিন শর্টকাট |
| ☁️ **Google Drive ও URL PDF ডাউনলোডার** | যেকোনো PDF/Google Drive লিংক বা অন্য অ্যাপের **Share** মেনু থেকে সরাসরি ইমপোর্ট, অটো ক্লিপবোর্ড পেস্ট, Drive টাইটেল ও কভার সংগ্রহ, ব্যাকগ্রাউন্ড সার্ভিসে Pause/Resume ডাউনলোড এবং Photo Picker দিয়ে কাস্টম কভার ছবি সেট |
| 📖 **হাই-প্রিসিশন PDF ভিউয়ার** | `PdfRenderer`-ভিত্তিক মসৃণ ভার্টিক্যাল পেজ স্ট্রিমিং, পিঞ্চ-টু-জুম, ডাবল-ট্যাপ জুম, ডান পাশে ড্র্যাগযোগ্য ফাস্ট-স্ক্রল বার (পেজ টুলটিপসহ), সরাসরি পেজ জাম্প ডায়ালগ ও PDF শেয়ার |
| ✂️ **পেজ ক্রপ ও লসলেস স্নিপেট** | ইন্টারঅ্যাক্টিভ ক্রপ ওভারলে, **Invert** ও **B&W** ফিল্টার, `Pictures/PDF_Gallery`-তে JPEG সেভ, লসলেস ট্রান্সপারেন্ট ইঙ্ক এক্সট্রাকশন এবং কাস্টম প্রম্পটসহ সরাসরি AI অ্যাপে শেয়ার |
| 🔍 **Google Lens ও স্ক্রিন স্নিপেট ওভারলে** | অফিশিয়াল Google Lens-এ পেজ পাঠানো, ভাসমান **Prev / Next / Note** পেজ নেভিগেটর বার, Accessibility অটো-কপি হেল্পার (শুধু Google Lens স্ক্রিনে "Select text" ও "Copy text" ট্যাপ করে), অফলাইন ML Kit OCR ফলব্যাক এবং `MediaProjection` স্ক্রিন স্নিপেট ক্যাপচার |
| 📝 **আনলিমিটেড নোট ও ২-কলাম চ্যাপ্টার ইনডেক্স** | **Noto Serif Bengali** ফন্টে বাংলা ও ইংরেজি নোট, Undo/Redo হিস্ট্রি, KaTeX গণিত/বিজ্ঞান ফর্মুলা প্রিভিউ, রাসায়নিক ও পদার্থবিজ্ঞানের চিহ্ন অটো-ফরম্যাটিং, ক্লিকযোগ্য `(পৃষ্ঠা= N)` ট্যাগ (ট্যাপ করলেই সরাসরি সেই পেজে যাবে), ১-কলাম / ২-কলাম চ্যাপ্টার ইনডেক্স ভিউ এবং `Downloads`-এ `.txt` এক্সপোর্ট |
| 💾 **পূর্ণ ব্যাকআপ ও রিস্টোর** | ফোল্ডার, ফেভারিট, URL তালিকা ও নোটের সম্পূর্ণ JSON ব্যাকআপ এক্সপোর্ট ও এক ট্যাপে রিস্টোর |

---

## 🔐 সাইনিং সার্টিফিকেট ও নির্বিঘ্ন আপডেট

এই রিপোজিটরিতে [`signing/release-keystore.b64`](signing/release-keystore.b64) হিসেবে অ্যাপের স্থায়ী রিলিজ সাইনিং সার্টিফিকেট সংরক্ষিত আছে এবং GitHub Actions ওয়ার্কফ্লো প্রতিটি রিলিজ একই কী দিয়ে সাইন ও `apksigner` দিয়ে যাচাই করে।

ফলে **একই Package ID (`com.hasib.pdfgallery`)** এবং **একই Signing Certificate** থাকায় ব্যবহারকারীরা পুরোনো ভার্সন আনইনস্টল না করেই পরবর্তী সব আপডেট (`v2.0.0` → `v2.1.0` → `v3.0.0`+) সরাসরি ইনস্টল করতে পারবেন — অ্যাপের সব নোট, ফোল্ডার, ফেভারিট ও ডাউনলোড অক্ষত থাকবে।

| Field | Value |
| --- | --- |
| **Keystore Type** | `PKCS12` |
| **Key / Signature Algorithm** | `RSA 2048` / `SHA256withRSA` |
| **Key Alias** | `pdfgallery` |
| **Subject (DN)** | `CN=Hasib Apps, OU=Android, O=HasibApps, L=Dhaka, ST=Dhaka, C=BD` |
| **মেয়াদ (Validity)** | `18 Nov 2026` → `05 Apr 2054` (১০,০০০ দিন) |
| **Certificate SHA-256** | `80:7F:93:B3:8B:7E:B4:AB:B7:30:B2:EF:13:5C:83:26:02:A9:B4:BA:40:00:39:4B:D0:5D:56:59:8E:E6:49:3A` |
| **Certificate SHA-1** | `98:1D:4C:08:D1:AA:63:FD:1B:8E:62:68:CE:0E:79:11:0B:B7:A2:3B` |

> 🔒 **GitHub Secrets সাপোর্ট:** ওয়ার্কফ্লো প্রথমে রিপোর `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` Secrets খোঁজে, না থাকলে `signing/release-keystore.b64` ব্যবহার করে। ভবিষ্যতে রিপো পাবলিক রেখে প্রাইভেট কী গোপন রাখতে চাইলে [`signing/README.md`](signing/README.md) দেখুন।

---

## 📥 ইনস্টল ও আপডেট করার নিয়ম

1. উপরের [**PDF-Gallery-v2.0.0-release.apk**](https://github.com/HasibApps/apk-02/releases/download/v2.0.0/PDF-Gallery-v2.0.0-release.apk) লিংক থেকে APK ফাইলটি ডাউনলোড করুন।
2. ফাইলটিতে ট্যাপ করুন — ব্রাউজার বা ফাইল ম্যানেজারে **Install unknown apps / অজানা উৎস** অনুমতি চাইলে **Allow** দিন।
3. **Install** চাপুন এবং শেষে **Open** করুন।
4. Google Play Protect সতর্কতা দেখালে **More details → Install anyway** চাপুন (স্ব-স্বাক্ষরিত APK হওয়ায় এটি স্বাভাবিক)।
5. **আপডেট নোট:** আগে যদি অস্থায়ী debug কী বা ভিন্ন প্যাকেজ নামে (`com.example` / `com.aistudio.*`) বিল্ড করা পুরোনো APK ইনস্টল থাকে, শুধু প্রথমবার সেটি আনইনস্টল করে (প্রয়োজনে JSON ব্যাকআপ নিয়ে) এই সাইনড `v2.0.0` ইনস্টল করুন। এরপর থেকে সব নতুন ভার্সন আনইনস্টল ছাড়াই সরাসরি আপডেট হবে।

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

ওয়ার্কফ্লো ফাইল: [`.github/workflows/build.yml`](.github/workflows/build.yml)

প্রতিটি কোড পুশে অথবা **Actions → Build & Release APK → Run workflow** চাপলে স্বয়ংক্রিয়ভাবে:
1. Temurin JDK 17, Android SDK (API 36.1) ও Gradle `9.3.1` সেটআপ হয়
2. সাইনিং keystore যাচাই করে সাইন করা `release` APK বিল্ড করে
3. `apksigner` দিয়ে APK Signature Scheme (`v2 + v3`) ও সার্টিফিকেট ফিঙ্গারপ্রিন্ট যাচাই করে এবং `SHA256SUMS.txt` তৈরি করে
4. GitHub Actions Artifact এবং **GitHub Releases**-এ `v<versionName>` ট্যাগে সাইনড APK আপলোড করে

**পরবর্তী ভার্সন (যেমন `v2.1.0` বা `v3.0.0`) রিলিজ করতে:**
শুধু `app/build.gradle.kts`-এ `versionCode` বাড়ান (যেমন `3`) এবং `versionName = "2.1.0"` করে পুশ করুন — একই `com.hasib.pdfgallery` ও একই সাইনিং কী-তে নতুন রিলিজ স্বয়ংক্রিয়ভাবে তৈরি হবে।

### লোকাল বিল্ড

```bash
gradle :app:assembleRelease
# আউটপুট: app/build/outputs/apk/release/app-release.apk (স্বয়ংক্রিয়ভাবে সাইনড)
```

---

## 🔗 গুরুত্বপূর্ণ লিংকসমূহ

- 📦 **Repository:** https://github.com/HasibApps/apk-02
- 🚀 **Latest Release (v2.0.0):** https://github.com/HasibApps/apk-02/releases/tag/v2.0.0
- ⬇️ **Direct APK Download:** https://github.com/HasibApps/apk-02/releases/download/v2.0.0/PDF-Gallery-v2.0.0-release.apk
- ⚙️ **GitHub Actions Workflow:** https://github.com/HasibApps/apk-02/actions/workflows/build.yml
