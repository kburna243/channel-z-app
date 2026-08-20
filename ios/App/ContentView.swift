import SwiftUI
import SwiftData

// Phase 1: Connect-UI + Chat + Media-Info. Phase 3 ersetzt das durch das echte
// Haupt-UI (Player, Favoriten, Settings) analog MainActivity.kt.
struct ContentView: View {
    @State private var showSplash = true
    @State private var theme: CyTubeTheme = .matrix
    @Environment(CyTubeClient.self) private var cyTube
    @State private var versionChecker = VersionChecker()
    @State private var scraper = DataScraper()   // B.6: Cytubot/Reddit-Schedule-Poller
    @Query private var chatSettings: [ChatSettings]

    var body: some View {
        ZStack {
            CyTubeColors.palette(for: theme).background.ignoresSafeArea()
            if !showSplash {
                MatrixRainView(palette: CyTubeColors.palette(for: theme))
                    .opacity(0.18)
                    .ignoresSafeArea()
                    .transition(.opacity)
            }
            if showSplash {
                SplashView(palette: CyTubeColors.palette(for: theme))
                    .transition(.opacity)
            } else {
                ConnectView(cyTube: cyTube, theme: $theme, chatSettings: chatSettings, scraper: scraper)
                    .transition(.opacity)
            }
        }
        .animation(.easeInOut(duration: 0.4), value: showSplash)
        .task {
            try? await Task.sleep(for: .seconds(2))
            showSplash = false
            await versionChecker.check()
        }
        .alert("Update verfügbar", isPresented: $versionChecker.updateAvailable) {
            Button("OK") {}
        } message: {
            Text("v\(versionChecker.latestVersion ?? "?") ist verfügbar (du hast v\(AppEndpoint.version)). Über Sideloadly aktualisieren — iOS blockt Auto-Install.\n\n\(versionChecker.notes ?? "")")
        }
        // B.6: Scraper lifecycle an Connection-State koppeln (deckt connect/sim/reconnect/disconnect)
        .onChange(of: cyTube.connectionState) { _, state in
            if state == .connected { scraper.start() }
            else if state == .disconnected || state == .error { scraper.stop() }
        }
    }
}

private struct ConnectView: View {
    @Bindable var cyTube: CyTubeClient
    @Binding var theme: CyTubeTheme
    let chatSettings: [ChatSettings]
    let scraper: DataScraper     // B.6: Schedule-Fallback für UpNextOverlay
    private var palette: CyTubeColors { CyTubeColors.palette(for: theme) }

    private var settings: ChatSettings? { chatSettings.first }
    private var msgColor: Color { Color(hexString: settings?.messageColorHex ?? "#FFFFFF") }

    @State private var nickInput = ""
    @State private var chatInput = ""
    @State private var isMuted = false
    @State private var isFullscreen = false
    @State private var showFavorites = false
    @State private var showSettings = false
    @State private var showEPG = false
    @State private var showBugReport = false
    @State private var showUpNext = false    // B.4: Queue-Overlay-Toggle
    @AppStorage("mca.server") private var savedServer = "https://cytu.be"
    @AppStorage("mca.channel") private var savedChannel = "420Grindhouse"
    @AppStorage("mca.nickname") private var savedNick = ""

    var body: some View {
        ZStack {  // ponytail: fullscreen overlay via ZStack, no extra container
            mainColumn
            if isFullscreen, cyTube.mediaState != nil {
                PlayerView(media: cyTube.mediaState, palette: palette, isFullscreen: true,
                           isMuted: isMuted, subtitlesEnabled: settings?.subtitlesEnabled ?? true,
                           onMuteToggle: { isMuted.toggle() },
                           onFullscreenToggle: { isFullscreen = false },
                           onPlaybackUpdate: { t, p in cyTube.sendMediaSync(currentTime: t, isPaused: p) },
                           onNextChannelPressed: { cyTube.triggerNextSimulatedMedia() })
                    .ignoresSafeArea()
                    .transition(.opacity)
            }
        }
        .animation(.easeInOut, value: isFullscreen)
        .sheet(isPresented: $showFavorites) {
            FavoritesPanel { url, ch in
                savedServer = url; savedChannel = ch
                connectWithNick()
                showFavorites = false
            }
        }
        .sheet(isPresented: $showSettings) { ChatSettingsPanel() }
        .sheet(isPresented: $showEPG) {
            RedditEPGPanel(palette: palette) { title, url in
                if cyTube.isSimulated, !url.isEmpty {
                    cyTube.mediaState = CyTubeMedia(title: title, id: url,
                                                    type: url.hasSuffix("m3u8") ? "hl" : "fi",
                                                    duration: 600, currentTime: 0, isPaused: false)
                }
                showEPG = false
            }
        }
        .sheet(isPresented: $showBugReport) { BugReportPanel(palette: palette) }
        .preferredColorScheme(.dark)
    }

    private func connectWithNick() {
        cyTube.connect(serverURL: savedServer, channel: savedChannel)
        if !savedNick.isEmpty { cyTube.setNickname(savedNick) }
    }

    private var mainColumn: some View {
        VStack(spacing: 0) {
            // --- Connect bar ---
            VStack(spacing: 8) {
                HStack {
                    TextField("Server", text: $savedServer)
                        .textFieldStyle(.roundedBorder)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)
                    TextField("Channel", text: $savedChannel)
                        .textFieldStyle(.roundedBorder)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)
                }
                HStack {
                    Button(cyTube.connectionState == .connected ? "Disconnect" : "Connect") {
                        if cyTube.connectionState == .connected || cyTube.connectionState == .connecting {
                            cyTube.disconnect()
                        } else {
                            connectWithNick()
                        }
                    }
                    .tint(palette.primary)
                    Button("Simulate") {
                        cyTube.connect(serverURL: savedServer, channel: savedChannel, simulateFallback: true)
                    }
                    .tint(palette.accent)
                    Button { showFavorites = true } label: { Image(systemName: "star") }
                        .tint(palette.accent)
                    Button { showSettings = true } label: { Image(systemName: "gearshape") }
                        .tint(palette.accent)
                    Button { showEPG = true } label: { Image(systemName: "tv") }
                        .tint(palette.accent)
                    Button { showUpNext.toggle() } label: { Image(systemName: "list.bullet") }
                        .tint(palette.accent)
                    Button { showBugReport = true } label: { Image(systemName: "ladybug") }
                        .tint(palette.accent)
                    Spacer()
                    Picker("Theme", selection: $theme) {
                        ForEach(CyTubeTheme.allCases) { t in Text(t.rawValue).tag(t) }
                    }
                    .pickerStyle(.segmented)
                    .frame(width: 160)
                }
            }
            .padding()
            .background(palette.surface.opacity(0.6))

            // --- Status line (B.5: pulsierender Dot + Retry-on-Tap) ---
            HStack {
                StatusIndicatorDot(state: cyTube.connectionState, userCount: cyTube.userCount,
                                   palette: palette, isSimulated: cyTube.isSimulated,
                                   onRetry: { connectWithNick() })
                Spacer()
            }
            .padding(.horizontal)
            .padding(.vertical, 6)

            // --- Player + B.3/B.4 Overlays ---
            PlayerView(media: cyTube.mediaState, palette: palette,
                       isMuted: isMuted, subtitlesEnabled: settings?.subtitlesEnabled ?? true,
                       onMuteToggle: { isMuted.toggle() },
                       onFullscreenToggle: { isFullscreen = true },
                       onPlaybackUpdate: { t, p in cyTube.sendMediaSync(currentTime: t, isPaused: p) },
                       onNextChannelPressed: { cyTube.triggerNextSimulatedMedia() })
                .overlay(alignment: .top) {
                    if cyTube.mediaState != nil {
                        MetadataOverlay(cyTube: cyTube, palette: palette)
                            .allowsHitTesting(false)
                            .id(cyTube.mediaState?.id ?? "")
                            .transition(.move(edge: .top).combined(with: .opacity))
                    }
                }
                .overlay(alignment: .trailing) {
                    if showUpNext && !isFullscreen {
                        UpNextOverlay(cyTube: cyTube, palette: palette,
                                      scheduleItems: scraper.scheduleItems,
                                      isRedditFallback: scraper.redditFallback,
                                      onClose: { showUpNext = false })
                            .transition(.move(edge: .trailing))
                    }
                }
                // B.7: Subtitle-Chat-Overlay über dem Player-Bottom (analog Android)
                .overlay(alignment: .bottom) {
                    if !isFullscreen && (settings?.chatEnabled ?? true) && cyTube.mediaState != nil {
                        SubtitleChatOverlay(
                            messages: cyTube.chatMessages,
                            isVisible: true,
                            maxLines: settings?.chatMaxLines ?? 3,
                            backgroundOpacity: settings?.chatOpacity ?? 0.6,
                            fontSize: settings?.chatFontSize ?? 15
                        )
                        .allowsHitTesting(false)
                    }
                }
                .animation(.easeInOut(duration: 0.4), value: cyTube.mediaState?.id)
                .animation(.easeInOut(duration: 0.3), value: showUpNext)

            // --- Sim next/pause row (compact, sim only) ---
            if cyTube.isSimulated {
                HStack(spacing: 12) {
                    Button("⏮ Next station") { cyTube.triggerNextSimulatedMedia() }
                    Button(cyTube.mediaState?.isPaused == true ? "▶ Play" : "⏸ Pause") { cyTube.toggleSimPlayPause() }
                    Spacer()
                    if let m = cyTube.mediaState {
                        Text("\(formatTime(m.currentTime))/\(formatTime(m.duration))")
                            .font(.system(.caption2, design: .monospaced))
                            .foregroundStyle(palette.muted)
                    }
                }
                .padding(.horizontal)
                .padding(.vertical, 6)
                .tint(palette.accent)
            }

            // --- Chat list ---
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 6) {
                        ForEach(cyTube.chatMessages) { msg in
                            chatRow(msg)
                                .id(msg.id)
                        }
                    }
                    .padding(.horizontal)
                    .padding(.bottom, 8)
                }
                .onChange(of: cyTube.chatMessages.count) { _, _ in
                    if let last = cyTube.chatMessages.last { proxy.scrollTo(last.id, anchor: .bottom) }
                }
            }

            // --- Nickname + chat input ---
            HStack {
                TextField("Nick", text: $nickInput)
                    .textFieldStyle(.roundedBorder)
                    .frame(width: 90)
                    .onSubmit {
                        savedNick = nickInput
                        cyTube.setNickname(nickInput); nickInput = ""
                    }
                VoiceToChatButton(palette: palette) { heard in
                    chatInput = chatInput.isEmpty ? heard : "\(chatInput) \(heard)"
                }
                TextField("Say something…", text: $chatInput)
                    .textFieldStyle(.roundedBorder)
                    .onSubmit {
                        cyTube.sendChatMessage(chatInput); chatInput = ""
                    }
            }
            .padding()
            .background(palette.surface.opacity(0.6))
        }
    }

    private func chatRow(_ msg: CyTubeChatMessage) -> some View {
        HStack(alignment: .top, spacing: 6) {
            if settings?.showTimestamps ?? true {
                Text(timeFmt(msg.time))
                    .foregroundStyle(palette.muted)
                    .font(.system(.caption2, design: .monospaced))
            }
            Text(msg.username).bold()
                .foregroundStyle(msg.isSystem ? palette.muted : palette.accent)
                .font(.system(.caption, design: .monospaced))
            Text(msg.message)
                .foregroundStyle(msg.isSystem ? palette.muted : msgColor)
                .font(.system(.caption, design: .monospaced))
            Spacer(minLength: 0)
        }
    }

    private func timeFmt(_ d: Date) -> String {
        let f = DateFormatter(); f.dateFormat = "HH:mm"; return f.string(from: d)
    }

    private func formatTime(_ s: Double) -> String {
        let m = Int(s) / 60, sec = Int(s) % 60
        return String(format: "%d:%02d", m, sec)
    }
}

#Preview {
    ContentView()
        .environment(CyTubeClient())
        .preferredColorScheme(.dark)
}