# mikes-cytube-ios

Native iOS-Port (Swift/SwiftUI) von Mike's CyTube App. Portiert die Android-Kotlin-App
(Jetpack Compose, CyTube socket.io-Client, Player, Chat, Voice-to-Chat, Reddit-EPG,
3 Cyberpunk-Themes) 1:1 auf iOS.

## Status

Phase 0 — Skeleton (Repo, XcodeGen `project.yml`, Theme/Splash, CI).

- iOS 17+, SwiftUI, Swift 5
- Projekt via XcodeGen: `brew install xcodegen && xcodegen generate`
- Build (lokal auf macOS): `xcodegen generate && xcodebuild -project MikesCyTube.xcodeproj -scheme MikesCyTube -configuration Release -sdk iphoneos CODE_SIGNING_ALLOWED=NO build`
- CI: `.github/workflows/ios-build.yml` (macos-15, Xcode 16, unsigned `.ipa`)

## Distribution

Phase 1: unsigned `.ipa` via GitHub-Actions → Sideloadly/AltStore (7-Tage, $0).
Phase 2 (später): App-Store ($99/Jahr).

Plan: `C:\Users\Berndte\.claude\plans\vivid-jingling-ripple.md`
