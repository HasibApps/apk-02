# 🔐 PDF Gallery — Release Signing Keystore

এই ফোল্ডারে **`release-keystore.b64`** নামে **PDF Gallery (`com.hasib.pdfgallery`)**-এর স্থায়ী release signing keystore সংরক্ষিত আছে।

`v2.0.0` → **`v3.0.0`** → **`v4.0.0`** → … প্রতিটি ভার্সন **একই Package ID** (`com.hasib.pdfgallery`) এবং **একই Signing Certificate** দিয়ে সাইন হওয়ায় ব্যবহারকারীরা পুরোনো ভার্সন আনইনস্টল না করেই সরাসরি আপডেট ইনস্টল করতে পারবেন (সব নোট, ফোল্ডার, ফেভারিট ও ডাউনলোড করা PDF অক্ষত থাকবে)।

GitHub Actions ওয়ার্কফ্লো [`.github/workflows/build.yml`](../.github/workflows/build.yml) প্রতিটি বিল্ডে এই keystore-এর সার্টিফিকেট ফিঙ্গারপ্রিন্ট নিচের fixed SHA-256-এর সাথে মিলিয়ে যাচাই করে; মিসম্যাচ হলে বিল্ড **fail** করে — অর্থাৎ ভুল কী দিয়ে ভবিষ্যৎ ভার্সন রিলিজ হওয়ার সুযোগ নেই।

## সার্টিফিকেট তথ্য

| Field | Value |
| --- | --- |
| **Keystore Type** | `PKCS12` |
| **Key Algorithm** | `RSA 2048` (`SHA256withRSA`) |
| **Alias** | `pdfgallery` |
| **Subject (DN)** | `CN=Hasib Apps, OU=Android, O=HasibApps, L=Dhaka, ST=Dhaka, C=BD` |
| **Validity** | `18 Nov 2026` → `05 Apr 2054` (10,000 days) |
| **SHA-256** | `80:7F:93:B3:8B:7E:B4:AB:B7:30:B2:EF:13:5C:83:26:02:A9:B4:BA:40:00:39:4B:D0:5D:56:59:8E:E6:49:3A` |
| **SHA-1** | `98:1D:4C:08:D1:AA:63:FD:1B:8E:62:68:CE:0E:79:11:0B:B7:A2:3B` |

## সাইন করা APK যাচাই

```bash
apksigner verify --verbose --print-certs PDF-Gallery-v3.0.0-release.apk
# SHA-256 fingerprint অবশ্যই উপরের মানের সাথে মিলতে হবে
```

## v4.0.0 রিলিজ করার নিয়ম

1. `app/build.gradle.kts`-এ শুধু `versionCode = 4` ও `versionName = "4.0.0"` করুন (`applicationId` ও এই keystore **একই রাখুন**)।
2. কমিট → পুশ করলে ওয়ার্কফ্লো স্বয়ংক্রিয়ভাবে একই কী দিয়ে সাইন করা APK বিল্ড করে `v4.0.0` ট্যাগে GitHub Release-এ আপলোড করবে।
3. ব্যবহারকারীরা `v3.0.0` আনইনস্টল না করেই `v4.0.0` আপডেট পাবেন।

## GitHub Secrets সাপোর্ট (ঐচ্ছিক, বাড়তি নিরাপত্তার জন্য)

ওয়ার্কফ্লো প্রথমে রিপোজিটরির **Settings → Secrets and variables → Actions** চেক করে। চাইলে `release-keystore.b64`-এর কনটেন্ট `KEYSTORE_BASE64` Secret-এ রেখে ফাইলটি রিপো থেকে সরিয়ে দিতে পারেন — **সার্টিফিকেট একই থাকতে হবে**:

- `KEYSTORE_BASE64` — `signing/release-keystore.b64` ফাইলের পুরো টেক্সট
- `KEYSTORE_PASSWORD` — keystore পাসওয়ার্ড
- `KEY_ALIAS` — `pdfgallery`
- `KEY_PASSWORD` — key পাসওয়ার্ড
- `KEYSTORE_TYPE` — `PKCS12` (ঐচ্ছিক, ডিফল্ট `PKCS12`)

> ⚠️ কখনো নতুন keystore তৈরি করে পুরোনোটা বদলাবেন না। নতুন কী মানেই আগের ভার্সন আনইনস্টল করতে বাধ্য হওয়া।
