<div align="center">
  <img src="app/src/main/res/drawable/app_logo.jpg" alt="ZeroTrack Logo" width="120" height="120">
  <h1>ZeroTrack</h1>
  <p><b>A Minimalist, Zero-Click Multi-Wallet Expense Tracker</b></p>
</div>

<br>

**ZeroTrack** is a fully automated, offline-first financial tracker that autonomously intercepts banking push notifications, determines if it's an income or expense, and logs it directly into a local multi-wallet database—zero clicks required.

## 🚀 Features

- **Zero-Click Automation:** Uses Android's Notification Listener Service to parse incoming banking alerts in the background.
- **Universal Multi-Currency Engine:** Built to detect transactions globally (USD `$`, EUR `€`, GBP `£`, THB `฿`, MMK `Ks`, JPY `¥`, KRW `₩`, INR `₹`, MYR `RM`) and automatically assign them to their native wallets!
- **Isolated Multi-Wallet Dashboard:** Spend in Thai Baht and Myanmar Kyat simultaneously? ZeroTrack generates independent wallet tabs so your net balances never improperly mix.
- **🎙️ Voice Recognition Entry:** Pull down your Android Quick Settings menu and tap the ZeroTrack tile to log expenses hands-free using just your voice!
- **✍️ Manual Entry:** Beautifully designed interactive dialogs for manually adding or editing expenses with on-the-fly currency switching.
- **100% Offline & Private:** No cloud servers. No API keys. Your financial data is securely locked inside a local Room/SQLite database on your device.

---

## 📥 Installation & Setup (Crucial for Android 13+)

Because ZeroTrack is a powerful automation tool that intercepts bank notifications offline, **Google Play Protect and Android 13+ Security will initially block it.** Your data is 100% safe and never leaves your device. 

Follow these exact steps to magically unlock the automation engine:

### Step 1: Temporarily Disable Play Protect
1. Open the **Google Play Store**.
2. Tap your profile picture -> **Play Protect**.
3. Tap the **Gear Icon (⚙️)** in the top right.
4. Turn **Off** *"Scan apps with Play Protect"*.
5. Download and install **`ZeroTrack.apk`** from the [Releases](../../releases/latest) page.

### Step 2: Trigger the Android 13 Security Block
Modern Android devices hide the unlock option until you actually hit the security wall! You must intentionally fail to grant the permission first:
1. Go to your phone's **Settings -> Special App Access -> Device & app notifications**.
2. Find **ZeroTrack** and try to toggle it ON.
3. A popup will say **"Restricted Setting"**. Click OK (This is good! You just triggered the unlock).

### Step 3: Unlock and Grant Magic Access
Now that the system knows you tried, the hidden menu is revealed:
1. Go to your phone's main **Settings -> Apps -> ZeroTrack**.
2. Look at the top right corner of the screen and tap the **Three Dots (⋮)** (which just appeared!).
3. Tap **"Allow restricted settings"** (you may need to scan your fingerprint).
4. Finally, go back to **Settings -> Special App Access -> Device & app notifications**.
5. Find **ZeroTrack** and toggle it **ON** successfully!

**Done!** Go transfer some money or buy something using your banking app—ZeroTrack will instantly log it in the background like magic! 🪄

---

## 🏗️ Tech Stack
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Architecture:** MVVM (Model-View-ViewModel)
- **Database:** Room (SQLite) + Kotlin Coroutines & Flows
- **Background Engine:** NotificationListenerService

---

## 🤝 Contributing
Feel free to open an issue or submit a pull request if you want to add support for specific bank parsers in your country!
