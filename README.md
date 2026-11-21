# Parvaz | پرواز

Open-source Android VPN client built on Xray-core. Kotlin + Jetpack Compose, Persian/English UI, light/dark theme.

**Protocols (v1):** VLESS, VMess, Trojan, Shadowsocks — over TCP, WebSocket, gRPC, HTTPUpgrade, with TLS and REALITY.
**Features:** link / subscription import, server latency test, foreground VPN service, per-app language and theme.

## Get the APK from GitHub
1. Create a new repository and upload all files of this project (keep the `.github` folder).
2. Open the **Actions** tab → *Build APK* → wait for the green check.
3. Download **Parvaz-apk** from the run's *Artifacts* (a ZIP containing the APK).
4. To publish a release: create a tag like `v1.0.0` — the APK is attached automatically.

## Licence
Parvaz links against AndroidLibXrayLite (GPL-3.0) and Xray-core (MPL-2.0), so this project must be distributed under **GPL-3.0**.
Add a `LICENSE` file (GitHub: *Add file → Create new file → name it LICENSE → "Choose a license template" → GPL-3.0*).

## Before publishing
The release build is signed with the debug key so it installs immediately. Create your own keystore for real releases.

---

## فارسی
کلاینت VPN متن‌باز اندروید بر پایه Xray-core، با رابط فارسی/انگلیسی و تم روشن/تیره.

**دریافت APK از گیت‌هاب:** همه‌ی فایل‌ها را (همراه پوشه‌ی `.github`) در یک مخزن جدید آپلود کنید ← تب **Actions** ← *Build APK* ← پس از تیک سبز، فایل **Parvaz-apk** را از بخش Artifacts دانلود کنید.
برای انتشار نسخه، یک تگ مثل `v1.0.0` بسازید تا APK خودکار به Release اضافه شود.

**مجوز:** به‌دلیل استفاده از AndroidLibXrayLite، مجوز پروژه باید GPL-3.0 باشد؛ فایل LICENSE را از قالب‌های گیت‌هاب اضافه کنید.
