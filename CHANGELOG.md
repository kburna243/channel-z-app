# 📜 Changelog

All notable changes to the **Mikes 420 Grindhouse App** suite are documented in this file.

---

## [v1.6.5] - Build 33 (2026-08-20)
### ⚡ Optimized
- **WebP Asset Migration:** Banner, Splashscreen und App-Icons auf WebP umgestellt. APK-Dateigröße um fast 50% von 37 MB auf **20.3 MB** reduziert.
- **Repository-Zentralisierung:** Eigenständiges Monorepo `mikes-420grindhouse-app` für Releases, In-App-Updates und Issue-Tracking.
- **UpdateManager:** Update-Feed-Endpoint nativ auf das neue GitHub-Repository umgestellt.

---

## [v1.6.4] - Build 32 (2026-08-20)
### 💬 Added
- **Chat-Konto & Gastzugang:** Speichern von Anmeldedaten (`Einstellungen > Chat-Konto`), automatischer Login beim Start und Reconnect. Gastzugang ohne Passwort.
- **Vollbild-Chat:** Neuer Button in der Bedienleiste für Mobilgeräte & Tablets.
### 🛠️ Fixed
- **Mobil-Layout:** Ränder und Overlays für Hoch- und Querformat optimiert (schmale Ränder, responsive Warteschlange).
- **Flavored Updates:** Full-Ausgabe lädt beim In-App-Update die eigene `.full`-APK, Light die TV-APK.

---

## [v1.6.3] (2026-08-19)
### 🛠️ Fixed & Improved
- **Mobil- & Tablet-Audit:** D-Pad-Hinweise auf Touch-Geräten ausgeblendet.
- **Filmdetails & Trivia:** Neuer Glühbirnen-Knopf in der mobilen Bedienleiste.
- **Auto-Hide & YouTube Play/Mute:** Verbesserte Watchdog-Stabilität beim Player-Neuaufbau.

---

## [v1.6.2] (2026-08-19)
### 📺 Added
- **CyTube Live-Media-Sync:** Drift-Korrektur (> 3s) und Play/Pause-Gleichlauf mit dem Kanal.
- **Signatur:** APKs nun v2+v3 signiert.

---

## [v1.6.1] (2026-08-19)
### 🎨 Visuals
- **16:9 TV-Banner & App-Icon:** Echtes 16:9-Banner in 4 Auflösungen, Grindhouse-Wirbel-Icon.

---

## [v1.6.0] (2026-08-19)
### 🎬 Features
- **Fortschrittsbalken:** Echte Status- & Zuschaueranzeige aus dem WebSocket.
- **Drei Up-Next-Titel** in der Leiste.
- **Verbesserte Lesbarkeit:** Angepasste Typografie und 5%-Sicherheitsrand für TV.

---

## [v1.5.0] (2026-08-19)
### 🐛 Features
- **1-Click Bug Reporter:** Problem melden direkt aus den Einstellungen (Gerät, Android-Version, laufender Titel).

---

## [v1.4.0] (2026-08-19)
### 🎨 Themes & Parser
- **4 OLED-Themen:** The Cinematic Deep, Premium Cyber Punk, Mystic Editorial, Grindhouse Original.
- **Serienfolgen-Erkennung:** Automatische Auflösung von Staffeln & Episoden aus YouTube-Titeln.
