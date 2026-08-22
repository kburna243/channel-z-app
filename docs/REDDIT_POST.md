# 🍿 [Unofficial App] Channel Z TV — Native Android TV, Fire TV & Mobile Suite

Hey everyone! 👋

Following our work on the Grindhouse TV app, several people from the community asked: *"Could you build this for Channel Z as well?"*

Here it is: **Channel Z TV** — an unofficial, native client for **Android TV, Amazon Fire TV, Android smartphones/tablets, and iOS** to watch the **[Channel Z CyTube room](https://cytu.be/r/Channel-Z)** directly on the big screen with full remote control support!

---

### 🎬 What is it?

The goal: **watch Channel Z on your TV with a remote without clunky web browsers or mouse pointers.**

* 📺 **Light Edition (Fire TV / Android TV):** Pure lean-back playback, 100% remote-driven, minimal RAM usage, transparent chat subtitles overlay over the video.
* 📱 **Full Edition (Smartphones / Tablets):** Full CyTube account login, live chat input, user list, and a dedicated wireless keyboard (`CHAT_ONLY`) mode.
* 🍏 **iOS Edition (iPhone / iPad):** Swift / SwiftUI build ready for sideloading.

---

### 🚀 Key Features

* **⚡ Ruckelfreier Sync:** Adaptive Sonic audio rate nudging (1.04x / 0.96x) and automatic drift compensation.
* **🎥 Hybrid Engine:** AndroidX Media3 ExoPlayer with hardware acceleration for direct streams (HLS/MP4) + accelerated bridge for YouTube/Vimeo feeds.
* **💬 Chat as Subtitles:** Live CyTube chat messages appear smoothly over the stream with configurable opacity, size, and auto-hide.
* **🎬 Automatic Movie Info & Trivia:** Clean scene tag stripper with poster, year, directors, and trivia facts.
* **🎮 100% Remote-Optimized:** D-Pad navigation for Now-Playing HUD, details, queue, and settings.

---

### 📥 Download & Installation

Builds are available on GitHub Releases:
👉 **[Channel Z TV Releases](https://github.com/kburna243/channel-z-app/releases/latest)**

* **Fire TV & Android TV (via Downloader App):**  
  Enter URL: `https://github.com/kburna243/channel-z-app/releases/latest/download/channel-z-light.apk`
* **Android (Phones & Tablets):**  
  Download `channel-z-full.apk`
* **iOS:**  
  Download `channel-z.ipa` for AltStore / Sideloadly.

---

### 🤝 Credits

* **[calzoneman/sync](https://github.com/calzoneman/sync):** The CyTube sync protocol.
* **[SPUDZARENEAT](https://github.com/spudzareneat):** Lead-time inspiration from grindhouse-tv.
* Thanks to the **Channel Z Community** for the feedback!