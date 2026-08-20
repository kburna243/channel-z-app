# 🍿 [Unofficial App] Mikes 420 Grindhouse (MCA) — Native Android TV, Fire TV & Mobile Suite

Hey everyone! 👋

A good friend of mine (**Mike**) and I (**Fried**) recently built an unofficial native app suite for **Android TV, Amazon Fire TV, Android smartphones, tablets, and iOS** to watch the [420 Grindhouse CyTube channel](https://cytu.be/r/420Grindhouse).

Our goal was simple: **create an easy-to-use, native 10-foot UI that feels amazing on the big screen**, completely eliminating the need for a clunky web browser and mouse pointer on the TV. Originally, this was just a private project for our own living room. We never intended to release it publicly.

However, after showing early builds to the channel operators, they were so supportive and encouraging that they explicitly asked us to share it with the community! That gave us the final push and the confidence to polish everything and make it available to all of you. Maybe someone else will have just as much fun using it as we had building it.

---

### 🤝 Community Inspiration & Shoutouts

* **A massive shoutout to SPUDZARENEAT:**  
  After working on our native app for several weeks, we discovered that **SPUDZARENEAT** had independently built a great web-based TV companion. Since our native Kotlin/Swift apps were already nearly complete, we didn't start over, but we took his approach as inspiration and incorporated some of his great ideas. Please check out his project as well:  
  👉 **[SPUDZARENEAT / grindhouse-tv on GitHub](https://github.com/spudzareneat/grindhouse-tv)**
* **The CyTube Sync Platform:**  
  Built with deep integration into the CyTube sync protocol and client architecture by [calzoneman/sync](https://github.com/calzoneman/sync).

---

### 🛋️ The Concept: A Two-App Ecosystem

Watching on a big screen with a TV remote and chatting on a phone keyboard are two very different experiences. That’s why we built the app from a single codebase into two specialized editions that work together seamlessly:

* 📺 **Light Edition (For Android TV / Fire TV):**  
  Stripped down to pure performance. It gives you a borderless, full-screen cinematic experience. The CyTube chat is rendered elegantly as transparent "subtitles" at the bottom of the screen. No code bloat, no login screens needed.
* 📱 **Full Edition (For Smartphones / Tablets / PC Handhelds):**  
  The ultimate companion app. Includes the full CyTube login, a complete chat composer, user list, spellcheck, and a dedicated fullscreen chat mode.
* 🍏 **iOS Edition (iPhone / iPad):**  
  A native Swift / SwiftUI port ready for sideloading via AltStore or Sideloadly.

> 💡 **The Living Room Setup:** You run the **Light Edition** on your Fire TV / Android TV for the big picture. Your phone lies on the couch running the **Full Edition** in `CHAT_ONLY` mode. Your phone acts as a silent wireless keyboard — you type your messages on your phone, and they appear in the CyTube chat on your TV in real-time!

---

### 🚀 Key Features

* **⚡ Hybrid Video Engine:**  
  Automatically detects the media format. Uses native **AndroidX Media3 ExoPlayer** for direct streams/HLS/DASH (including on-the-fly Google Drive link resolution) and a specialized, hardware-accelerated **WebView bridge** for YouTube/Twitch/Vimeo. It even auto-detects if your TV supports **AV1 hardware decoding** to prevent software-decoder stutter!
* **⏱️ Real-Time Media Sync:**  
  Seamlessly syncs playback with the channel. Automatic drift correction (> 3s) and synched play/pause states keep you perfectly aligned with everyone else in chat.
* **💬 CyTube Chat as Subtitles:**  
  Chat messages appear discreetly over the video. You can customize visible lines, opacity, colors (Grindhouse or Classic CyTube themes), and enable auto-hide.
* **🎬 Smart Metadata & Trivia (Zero API Keys):**  
  As soon as a video starts, our parser strips away scene tags (`1080p`, `x264`, series formats like `S01E10`), recognizes series structures, and fetches high-res posters, IMDb ratings, and director info via Wikidata. It even includes a fullscreen Trivia overlay with up to 25 facts about the movie!
* **📅 Multi-Tier EPG Scraping:**  
  Never wonder what's next. The app pulls the schedule directly from the CyTube WebSocket, falls back to the Schedule-Bot API, and scrapes the Reddit EPG broadcast if needed.
* **🎨 OLED-Friendly Themes:**  
  4 hand-tuned color themes (*The Cinematic Deep*, *Premium Cyber Punk*, *Mystic Editorial*, and *Grindhouse Original*).
* **🪶 Ultra-Lightweight (WebP Optimized):**  
  APK size cut in half down to just **~20.3 MB**.
* **🔐 Production Signed:**  
  Signed with an official RSA-4096-bit release certificate.

---

### 📥 Download & Installation

All binaries are pre-compiled, signed with an official RSA-4096 production keystore, and available directly from the [GitHub Releases Page](https://github.com/kburna243/mikes-420grindhouse-app/releases/latest).

#### 📺 Option 1: Amazon Fire TV & Android TV (via Downloader App)
1. Install the **Downloader** app from the Amazon Appstore or Google Play Store.
2. In Fire TV Settings: Go to `My Fire TV` ➔ `Developer Options` ➔ `Install Unknown Apps` ➔ Set **Downloader** to **ON**.
3. Open **Downloader** and enter the direct download link into the URL field:  
   👉 **`https://github.com/kburna243/mikes-420grindhouse-app/releases/latest/download/mikes-grindhouse-light.apk`**
4. Click **Download** and then **Install**.  
   *(No server configuration needed – it connects to 420Grindhouse automatically on startup!)*

#### 📱 Option 2: Android Smartphones & Tablets
* Download the Full Edition APK:  
  👉 **`https://github.com/kburna243/mikes-420grindhouse-app/releases/latest/download/mikes-grindhouse-full.apk`**
* Tap the downloaded `.apk` file to install.

#### 🍏 Option 3: iOS (iPhone / iPad)
* Download the native IPA:  
  👉 **`https://github.com/kburna243/mikes-420grindhouse-app/releases/latest/download/mikes-grindhouse.ipa`**
* Sideload using **AltStore**, **Sideloadly**, or **TrollStore**.

---

#### 🔐 SHA-256 Checksums (Release v1.6.6)
| File | Architecture / Platform | SHA-256 Checksum |
| :--- | :--- | :--- |
| `mikes-grindhouse-light.apk` | Android TV / Fire TV | `4B5A6D66EB90B035BFA8E15EFEEB6C6121C2155343E94FB14517DD9A5AE33FB8` |
| `mikes-grindhouse-full.apk` | Android Phone / Tablet | `A49E2994C7D08787E469B6F3703FAC725F0B9ABE31D677ED90D614F3DD82026F` |
| `mikes-grindhouse.ipa` | iOS (Unsigned Sideload) | `95027F5E6A97750F7AF93567E38D9DF3E83AFAC328913CD66D4C7E55DA36C84A` |

*(Note on Sideloading: If Google Play Protect shows a notice on first install because it has not yet seen apps from this new developer certificate, simply click **"More details"** ➔ **"Install anyway"**).*

---

### 🎮 Controls (TV Remote / D-Pad)

We optimized everything for the D-Pad so you can control the app blindly from the couch:

* **D-Pad UP:** Show Now-Playing HUD (Title, Poster, Year, Director, Progress) for 5s.
* **D-Pad DOWN:** Toggle Subtitle-Chat overlay on / off.
* **D-Pad LEFT:** Open Movie Details & Trivia panel.
* **D-Pad RIGHT:** Open Schedule & Up-Next Queue sidebar.
* **D-Pad CENTER (OK):** Play / Pause video.
* **MENU / SETTINGS:** Open the main settings menu.
* **BACK:** Close open overlays or open the clean exit confirmation dialog.

---

### 🐛 Found a Bug? Help us fix it!

The app is built with love, but there might be occasional hiccups. If you run into something, please don't hesitate to let us know:
* 🚀 **In-App:** Open **Settings > Problem melden** directly inside the app — it automatically gathers device model, Android version, and current movie title.
* 💻 **GitHub:** Open an issue on our [GitHub Issue Tracker](https://github.com/kburna243/mikes-420grindhouse-app/issues).

---

### 🔗 Quick Links

* 📦 **All Releases (Latest):** https://github.com/kburna243/mikes-420grindhouse-app/releases/latest
* 📺 **Light Edition APK (TV):** https://github.com/kburna243/mikes-420grindhouse-app/releases/download/v1.6.6/mikes-grindhouse-light.apk
* 📱 **Full Edition APK (Mobile):** https://github.com/kburna243/mikes-420grindhouse-app/releases/download/v1.6.6/mikes-grindhouse-full.apk
* 🍏 **iOS IPA (Sideload):** https://github.com/kburna243/mikes-420grindhouse-app/releases/download/v1.6.6/mikes-grindhouse.ipa
* 💻 **Source Code / GitHub:** https://github.com/kburna243/mikes-420grindhouse-app
* 🌐 **Webseite:** https://kburna243.github.io/mikes-cytube-dist/

Enjoy the app, grab some popcorn, and have fun at the 420 Grindhouse! 🍿🌿