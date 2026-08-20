# 🍿 Mikes 420 Grindhouse App

[![Latest Release](https://img.shields.io/github/v/release/kburna243/mikes-420grindhouse-app?style=for-the-badge&color=8A2BE2)](https://github.com/kburna243/mikes-420grindhouse-app/releases/latest)
[![Platform - Android](https://img.shields.io/badge/Platform-Android%20%7C%20Fire%20TV-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/kburna243/mikes-420grindhouse-app/releases)
[![Platform - iOS](https://img.shields.io/badge/Platform-iOS%20%28IPA%29-000000?style=for-the-badge&logo=apple&logoColor=white)](https://github.com/kburna243/mikes-420grindhouse-app/releases)
[![License](https://img.shields.io/badge/License-GPL%20v3-blue?style=for-the-badge)](LICENSE)

An unofficial native client suite for the **[420Grindhouse CyTube channel](https://cytu.be/r/420Grindhouse)**, made for people who enjoy watching movies the old-fashioned way: sitting on the couch, grabbing the remote, and watching whatever strange, weird, trashy, cheesy or downright awesome movie comes next.

Built from scratch for **Amazon Fire TV, Android TV, Android phones and tablets, and iOS**.

---

<div align="center">
  <img src="docs/screenshots/00-splash.png" alt="Mikes 420 Grindhouse Splash" width="720" style="border-radius: 12px; box-shadow: 0 8px 24px rgba(0,0,0,0.5);" />
</div>

---

## 🎬 What is this?

This started with a pretty simple idea:

**We wanted to watch 420Grindhouse on the TV without having to fight with a clunky web browser.**

If you've ever tried using a standard website on a TV with a remote, you probably know the problem: tiny buttons, awkward mouse pointers, unreadable text, menus that were never designed for a couch, and the inevitable frustration of having to grab a laptop instead.

We wanted something that felt like a **true 10-foot TV streaming app**:
* Start the app with one click.
* Lean back on the couch.
* Pick up the TV remote.
* Enjoy the movie in glorious fullscreen.

And since 420Grindhouse is all about cult cinema, B-movies, exploitation, horror, sci-fi, kung fu, weird gems and the occasional **"what the hell are we watching?"** masterpiece, the app was designed to feel right at home in that universe.

---

## 🍿 How it started

A good friend of mine (**Mike**) and I (**Fried**) originally built this native app for ourselves.

There was no commercial plan behind it. No startup pitch. No roadmap with 200 Jira tickets. We just wanted a clean, native way to watch the channel together in the living room without compromises.

The early versions were a hobby project that slowly expanded as we kept adding features we personally wanted: subtitle chat overlays, instant IMDB trivia, EPG schedules, and custom dark themes.

When we showed an early version to the channel operators, they loved it and explicitly encouraged us to share it with the wider community!

So here we are. What started as a private living-room experiment is now an open-source community suite with native TV navigation, mobile companion modes, chat integration, zero-latency sync, and automatic in-app updates.

> *"It's not perfect, and it probably never will be. But we had a lot of fun building it, and hopefully you'll have just as much fun using it."*

---

## 🎞️ Built for the Grindhouse

The name isn't an accident. The whole idea is rooted in the authentic Grindhouse theater experience: rough around the edges, sometimes hilarious, sometimes mind-blowing, and often both at once:

- 🎃 **Horror & Slasher** classics
- 🚀 **Retro Sci-Fi** & cosmic weirdness
- 🔫 **Action, Crime & Kung-Fu**
- 🧟 **Zombies, Monsters & Creature Features**
- 🩸 **B-Movies, Exploitation & Cult Cinema**
- 💀 **Golden Age Trash** & lost gems
- 🤘 Movies that shouldn't work on paper, but somehow become legend
- 🍿 And hidden cinematic treasures tucked between the madness

You don't always know what's coming next, you'll discover movies you never knew existed, and you get to experience it live with an amazing chat community.

---

## 🛋️ The Living Room Setup

One of our core goals was separating **watching** on the big screen from **typing** on a keyboard:

* The **TV screen** is built for immersive, borderless movie viewing.
* Your **Smartphone** is built for typing in chat.

You can run the **Light Edition** on your Fire TV or Android TV for the film, and open the **Full Edition** on your phone in `CHAT_ONLY` mode. 

Your phone acts as a silent, wireless keyboard for the TV: type a message on your phone, and it instantly appears in the live CyTube chat on your television screen!

---

## 📺 Three Specialized App Editions

| Edition | Target Devices | Highlights | Chat Mode |
| :--- | :--- | :--- | :--- |
| **📺 Android Light** | Amazon Fire TV, Android TV, Smart TVs | Pure lean-back cinema experience, 100% D-Pad remote control, ultra-low resource footprint | Subtitle-style overlay over movie |
| **📱 Android Full** | Phones, Tablets, Handhelds (Steam Deck) | Full CyTube companion: account login, chat composer, user list, spellcheck | Subtitles, Sidebar, or Fullscreen Chat-Only |
| **🍏 iOS** | iPhone, iPad | Native Swift / SwiftUI app for sideloading | Subtitle chat overlay & EPG queue |

### 📺 Android Light (TV Edition)
Designed exclusively for remote control. No mouse pointer, no tiny web buttons. Every feature is mapped directly to standard D-Pad directions.

### 📱 Android Full (Mobile Edition)
Adds features optimized for touchscreens: CyTube user account login, chat input with auto-complete and spellcheck, guest login, and dedicated mobile layouts.

### 🍏 iOS (iPhone & iPad Edition)
A native Swift & SwiftUI build designed for seamless sideloading with AltStore, Sideloadly, or TrollStore.

---

## 🚀 Key Features

* **⚡ Zero-Stutter Adaptive Synchronization:**  
  Engineered with a **+2.0s Lead-Time Model** (matching *spudzareneat/grindhouse-tv*) and **adaptive speed-nudging (1.04x / 0.96x)** via ExoPlayer's Sonic audio processor. Small network drifts are smoothed out without decoder flushes, maintaining frame-accurate synchronization with the CyTube web player.
* **🎥 Hybrid Video Pipeline:**  
  Direct video streams (HLS `.m3u8`, MP4, Google Drive on-the-fly resolution) render natively via **AndroidX Media3 ExoPlayer** with automatic AV1 hardware decoding checks. YouTube, Twitch, and Vimeo feeds stream via a hardware-accelerated WebView bridge with automatic iframe watchdogs.
* **💬 CyTube Chat as TV Subtitles:**  
  Chat messages appear unobtrusively at the bottom of the screen like movie subtitles. Adjust opacity, font size, line count, and color themes on the fly.
* **🎬 Automatic Movie Metadata & Trivia (Zero API Keys):**  
  Automated regex title sanitizer strips scene tags (`1080p`, `x264`, `BluRay`, `S01E05`) and queries Wikidata and IMDb for posters, director, release year, IMDb ratings, and up to 25 trivia facts in a fullscreen overlay.
* **📅 Multi-Tier EPG & Schedule Scraping:**  
  Pulls live schedule directly from the CyTube WebSocket, backed by automated fallback parsers for the channel Schedule-Bot and Reddit EPG broadcast.
* **🎨 OLED-Tuned Themes:**  
  Handcrafted dark themes: *The Cinematic Deep* (OLED black/purple), *Premium Cyber Punk*, *Mystic Editorial*, and *Grindhouse Original*.
* **🔄 In-App Update Engine:**  
  Checks the official GitHub release feed at startup and updates in-place without losing your settings.
* **🐛 1-Click Bug Reporter:**  
  Send diagnostic logs directly from the settings menu with auto-attached device model, Android version, and current movie title.

---

## 🎮 TV Remote Controls

| Button | Action |
| :--- | :--- |
| **D-Pad UP** | Show Now-Playing HUD (Title, Poster, Year, Director, Progress) |
| **D-Pad DOWN** | Toggle Subtitle-Chat overlay on / off |
| **D-Pad LEFT** | Open Movie Details & Trivia panel |
| **D-Pad RIGHT** | Open Schedule & Upcoming Queue sidebar |
| **D-Pad CENTER (OK)** | Play / Pause video |
| **MENU / OPTIONS** | Open Main Settings |
| **BACK** | Close active overlay / show Exit confirmation |

---

## 📸 Screenshots

<div align="center">
  <table>
    <tr>
      <td width="50%"><img src="docs/screenshots/01-player-fullscreen.png" alt="Fullscreen Player" /><br/><em>1. Borderless Cinema Player</em></td>
      <td width="50%"><img src="docs/screenshots/02-now-playing-hud.png" alt="Now Playing HUD" /><br/><em>2. Now-Playing HUD & Metadata</em></td>
    </tr>
    <tr>
      <td width="50%"><img src="docs/screenshots/03-chat-as-subtitles.png" alt="Chat Subtitles" /><br/><em>3. Chat rendered as TV Subtitles</em></td>
      <td width="50%"><img src="docs/screenshots/04-movie-details-trivia.png" alt="Movie Trivia" /><br/><em>4. Fullscreen Movie Details & Trivia</em></td>
    </tr>
    <tr>
      <td width="50%"><img src="docs/screenshots/07-schedule-queue.png" alt="Schedule & Queue" /><br/><em>5. Schedule & Up-Next Queue</em></td>
      <td width="50%"><img src="docs/screenshots/09-color-themes.png" alt="Color Themes" /><br/><em>6. 4 OLED-Tuned Color Themes</em></td>
    </tr>
    <tr>
      <td width="50%"><img src="docs/screenshots/06-chat-account.png" alt="Chat Account" /><br/><em>7. Chat Account & Guest Login</em></td>
      <td width="50%"><img src="docs/screenshots/11-bug-reporter.png" alt="Bug Reporter" /><br/><em>8. Built-in 1-Click Bug Reporter</em></td>
    </tr>
  </table>
</div>

---

## 📥 Download & Installation

All release assets are signed and verified. Grab the latest build from [GitHub Releases](https://github.com/kburna243/mikes-420grindhouse-app/releases/latest).

### 📺 Fire TV & Android TV (via Downloader App)
1. Install the **Downloader** app from the Amazon Appstore or Google Play Store.
2. In Fire TV Settings: Navigate to `My Fire TV` ➔ `Developer Options` ➔ `Install Unknown Apps` ➔ Set **Downloader** to **ON**.
3. Open Downloader and download the TV build:
   * **[mikes-grindhouse-light.apk](https://github.com/kburna243/mikes-420grindhouse-app/releases/latest/download/mikes-grindhouse-light.apk)**
4. Click **Install**. Launch the app — it connects automatically to 420Grindhouse!

### 📱 Android Phones & Tablets
1. Download **[mikes-grindhouse-full.apk](https://github.com/kburna243/mikes-420grindhouse-app/releases/latest/download/mikes-grindhouse-full.apk)**.
2. Open the downloaded `.apk` to install and start watching.

### 🍏 iPhone & iPad
1. Download **[mikes-grindhouse.ipa](https://github.com/kburna243/mikes-420grindhouse-app/releases/latest/download/mikes-grindhouse.ipa)**.
2. Sideload via **AltStore**, **Sideloadly**, or **TrollStore**.

---

## 🤝 Community & Credits

* 🌟 **[SPUDZARENEAT](https://github.com/spudzareneat):**  
  Special shoutout to SPUDZARENEAT! While we were building this native suite, he independently authored the great web-based TV companion **[grindhouse-tv](https://github.com/spudzareneat/grindhouse-tv)**. His work inspired our lead-time synchronization model.
* ⚙️ **[calzoneman/sync](https://github.com/calzoneman/sync):**  
  Immense appreciation to calzoneman and the developers behind the CyTube synchronization and WebSocket architecture.

---

## 🐛 Bugs, Ideas & Feedback

Encountered an issue or have a feature suggestion?
* 🚀 **In-App:** Open **Settings ➔ Problem melden** directly inside the app.
* 💻 **GitHub Issues:** Open an issue via our [GitHub Issue Templates](https://github.com/kburna243/mikes-420grindhouse-app/issues/new/choose) (dedicated forms for Android Light, Android Full, and iOS).

---

## ❤️ Why We Made It

At the end of the day, this isn't a commercial product. It's a passion project made by fans, for fans, so we can all enjoy great cinema together without fighting with browser interfaces.

Grab some popcorn, dim the lights, and see what the Grindhouse is playing tonight! 🍿🎬

---

## ⚖️ Disclaimer

*This is an **unofficial, non-commercial community project**. It is not affiliated with, sponsored by, or endorsed by CyTube or the 420Grindhouse channel administrators. All trademarks, media, and third-party services belong to their respective owners.*
