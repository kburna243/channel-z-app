<div align="center">
  <img src="docs/app-icon.png" alt="Channel Z Logo" width="128" style="border-radius: 28px; margin-bottom: 12px;" />
  <h1>🍿 Channel Z TV App</h1>
  <p><strong>Nativer Client für den <a href="https://cytu.be/r/Channel-Z">Channel Z CyTube-Kanal</a></strong></p>

  [![Latest Release](https://img.shields.io/github/v/release/kburna243/channel-z-app?style=for-the-badge&color=8A2BE2)](https://github.com/kburna243/channel-z-app/releases/latest)
  [![Platform - Android](https://img.shields.io/badge/Platform-Android%20%7C%20Fire%20TV-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/kburna243/channel-z-app/releases)
  [![Platform - iOS](https://img.shields.io/badge/Platform-iOS%20%28IPA%29-000000?style=for-the-badge&logo=apple&logoColor=white)](https://github.com/kburna243/channel-z-app/releases)
  [![License](https://img.shields.io/badge/License-GPL%20v3-blue?style=for-the-badge)](LICENSE)
</div>

---

<div align="center">
  <img src="docs/screenshots/00-splash.png" alt="Channel Z Splash" width="760" style="border-radius: 12px; box-shadow: 0 8px 24px rgba(0,0,0,0.5);" />
</div>

---

Ein nativer Client für den **[Channel Z CyTube-Kanal](https://cytu.be/r/Channel-Z)** – gebaut für alle, die Filme am liebsten entspannt von der Couch aus mit Fernbedienung schauen, statt sich mit Browsern auf dem Smart-TV herumzuärgern.

Entwickelt für **Amazon Fire TV, Android TV, Android-Smartphones/Tablets und iOS**.

---

## 🎬 Worum geht's?

Die Idee war denkbar simpel: **CyTube auf dem Fernseher schauen, ohne Mauszeiger-Gefummel und winzige Web-Buttons.**

Jeder, der schon mal versucht hat, eine normale Website über den TV-Browser mit einer Fernbedienung zu steuern, kennt den Horror:
* Winzige Steuerelemente
* Hakelige Mauszeiger
* Unlesbare Menüs
* Ständige Resync-Probleme

Die App bringt das Ganze als echtes **10-Foot-TV-Interface**: App starten, zurücklehnen, D-Pad nutzen und den Stream nativ im Vollbild genießen.

---

## 🍿 Wie es dazu kam

Ursprünglich hatten mein Kollege Mike und ich die App als privates Wochenendprojekt für uns selbst und die Grindhouse-Ecke gebaut. 

Nachdem wir das Ganze auf Reddit geteilt hatten, kam aus der Community prompt die Frage: *"Könnt ihr das nicht bitte auch für Channel Z bauen?"*

Da wir den Channel selbst feiern und der native Player-Core schon stand, haben wir uns rangesetzt, die Logik, EPG-Scraper und Sync-Pipelines für Channel Z angepasst und das Repo sauber neu aufgesetzt. 

Kein Businessplan, kein Startup-Quatsch – einfach ein Tool von Fans für Fans.

> *"Nicht perfekt, aber stabil, bequem und mit viel Herzblut gebastelt."*

---

## 🛋️ Das Wohnzimmer-Setup

Filme schaut man am besten auf dem großen Schirm, Tippen macht am Fernseher aber keinen Spaß. Deshalb trennt die Suite beides:

* **TV-App (Light Edition):** Maximal schlank, fokussiert auf ruckelfreie Wiedergabe, gesteuert rein über das Steuerkreuz (D-Pad).
* **Handy-App (Full Edition):** Dient im `CHAT_ONLY`-Modus als kabellose Tastatur. Du schreibst am Smartphone und die Nachrichten tauchen im Stream auf dem Fernseher auf.

---

## 📺 Die Versionen

| Edition | Zielgeräte | Fokus | Chat |
| :--- | :--- | :--- | :--- |
| **📺 Android Light** | Fire TV, Android TV, Smart-TVs | Lean-Back-Erlebnis, 100 % D-Pad-optimiert, minimaler RAM-Verbrauch | Untertitel-Overlay über dem Video |
| **📱 Android Full** | Smartphones, Tablets, Handhelds | CyTube-Konto-Login, Chat-Eingabe, Userliste | Subtitles, Sidebar oder Vollbild-Chat |
| **🍏 iOS** | iPhone, iPad | Swift / SwiftUI Build für Sideloading | Chat-Overlay & Queue |

---

## 📸 Screenshots

| Player HUD & Stream | Chat-Untertitel |
| :---: | :---: |
| <img src="docs/screenshots/01-player-fullscreen.png" width="400" /> | <img src="docs/screenshots/03-chat-as-subtitles.png" width="400" /> |

| Film-Details & Trivia | Sendeplan & Queue |
| :---: | :---: |
| <img src="docs/screenshots/04-movie-details-trivia.png" width="400" /> | <img src="docs/screenshots/07-schedule-queue.png" width="400" /> |

---

## 🚀 Technische Features

* **⚡ Synchronisation ohne Ruckler:**  
  Natives Sync-Handling mit Lead-Time-Puffer und feiner Tempo-Korrektur (1.04x / 0.96x via Sonic Audio-Prozessor). Kleine Netzwerkschwankungen werden unmerklich ausgeglichen, ohne dass der Player Buffering-Pausen einlegen muss.
* **🎥 Hybride Video-Engine:**  
  Direkte Streams (HLS `.m3u8`, MP4 etc.) laufen über **AndroidX Media3 ExoPlayer** mit Hardware-Dekodierung. Externe Feeds (YouTube, Vimeo) laufen über eine hardwarebeschleunigte WebView-Bridge mit automatischem Watchdog.
* **💬 Chat als TV-Untertitel:**  
  CyTube-Nachrichten laufen dezent am unteren Bildschirmrand ein. Schriftgröße, Deckkraft und Zeilenanzahl lassen sich direkt über das Schnellmenü anpassen.
* **🎬 Automatische Film-Infos & Trivia:**  
  Filtert Scene-Tags (`1080p`, `BluRay`, `x264`) aus dem Titel und lädt Poster, Regie, Erscheinungsjahr und Trivia-Fakten ohne nötige API-Keys.
* **📅 Multi-Tier Schedule:**  
  Liest den aktuellen Ablaufplan direkt via WebSocket aus und greift bei Bedarf auf Fallback-Parser zurück.
* **🎨 OLED-Themes:**  
  Vier dunkle Farbprofile, optimiert für Kontrast und OLED-Displays.
* **🔄 In-App Updater:**  
  Prüft beim Start automatisch auf neue GitHub-Releases und aktualisiert direkt auf dem Gerät.

---

## 🎮 Fernbedienungs-Belegung

| Taste | Funktion |
| :--- | :--- |
| **D-Pad HOCH** | Now-Playing HUD einblenden (Titel, Poster, Infos, Laufzeit) |
| **D-Pad RUNTER** | Chat-Untertitel an / aus |
| **D-Pad LINKS** | Film-Details & Trivia öffnen |
| **D-Pad RECHTS** | Playlist & Queue öffnen |
| **D-Pad MITTE (OK)** | Play / Pause |
| **MENU / OPTIONEN** | Einstellungen öffnen |
| **ZURÜCK** | Overlay schließen / App beenden |

---

## 📥 Download & Installation

Die fertigen Builds gibt es direkt unter [Releases](https://github.com/kburna243/channel-z-app/releases/latest).

### 📺 Fire TV & Android TV (via Downloader App)
1. **Downloader**-App auf dem TV aus dem Store installieren.
2. In den TV-Einstellungen unter Entwickleroptionen die Berechtigung für *Unbekannte Apps* bei der Downloader-App auf **AN** stellen.
3. Im Downloader die URL zur TV-Version eingeben:  
   `https://github.com/kburna243/channel-z-app/releases/latest/download/channel-z-light.apk`
4. Installieren, öffnen und direkt losstreamen.

### 📱 Android (Smartphones & Tablets)
1. **[channel-z-full.apk](https://github.com/kburna243/channel-z-app/releases/latest/download/channel-z-full.apk)** herunterladen.
2. Datei öffnen und installieren.

### 🍏 iOS (iPhone & iPad)
1. **[channel-z.ipa](https://github.com/kburna243/channel-z-app/releases/latest/download/channel-z.ipa)** herunterladen.
2. Über **AltStore**, **Sideloadly** oder **TrollStore** installieren.

---

## 🤝 Credits & Dank

* **[calzoneman/sync](https://github.com/calzoneman/sync):** Danke für die gesamte CyTube-Basis und das WebSocket-Protokoll.
* **[SPUDZARENEAT](https://github.com/spudzareneat):** Für die Inspiration rund um das Lead-Time-Sync-Modell aus grindhouse-tv.
* Danke an die **Channel Z Community** auf Reddit für den Input und das Feedback!

---

## 🐛 Feedback & Bugs

Wenn etwas hakt oder ihr Ideen habt:
* Direkt über die App: **Einstellungen ➔ Problem melden** (Bug-Report mit System-Infos)
* Über GitHub: [Issue eröffnen](https://github.com/kburna243/channel-z-app/issues)

---

## ⚖️ Disclaimer

*Inoffizielles Community-Projekt. Keine Verbindung zu den Betreibern von CyTube oder Channel Z. Alle Trademarks und Streams gehören ihren jeweiligen Eigentümern.*
