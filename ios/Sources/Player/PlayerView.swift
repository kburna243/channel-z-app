import SwiftUI
import AVKit
import WebKit

// Player — portiert CustomMediaPlayer.kt. AVPlayer (HLS/Direct/GDrive) via
// natives SwiftUI VideoPlayer; YouTube/Twitch/Vimeo via WKWebView (UIViewRepresentable).
// B.8: Twitch/Vimeo-Embed + YouTube-CC (cc_load_policy), Reconnect bei failedToPlayToEndTime.

func convertGoogleDriveURL(_ url: String) -> String {
    if url.contains("drive.google.com/file/d/") {
        let s = (url.range(of: "/file/d/")?.upperBound) ?? url.startIndex
        let rest = url[s...]
        let end = rest.firstIndex(of: "/") ?? rest.endIndex
        let id = rest[..<end]
        return "https://drive.google.com/uc?export=download&id=\(id)"
    }
    if url.contains("drive.google.com/open?id=") {
        let after = url.split(separator: "open?id=", maxSplits: 1).last ?? ""
        let id = after.split(separator: "&").first ?? ""
        return "https://drive.google.com/uc?export=download&id=\(id)"
    }
    return url
}

func isWebStream(_ media: CyTubeMedia?) -> Bool {
    guard let media else { return false }
    let id = media.id
    let type = media.type.lowercased()
    // B.8: Twitch/Vimeo explizit via type (tw/vi), sonst YouTube/11-char-ID Heuristik
    if type == "tw" || type == "vi" || type == "yt" { return true }
    // Google Drive is directly streamable via AVPlayer → not a web stream
    return (id.contains("youtube") || id.contains("twitch") || id.count == 11 || !id.contains("."))
        && type != "gd"
}

struct PlayerView: View {
    let media: CyTubeMedia?
    let palette: CyTubeColors
    var isFullscreen: Bool = false
    var isMuted: Bool = false
    var subtitlesEnabled: Bool = true   // B.8: YouTube-CC (cc_load_policy)
    var onMuteToggle: () -> Void = {}
    var volume: Double = 1.0
    var onVolumeChange: (Double) -> Void = { _ in }
    var onFullscreenToggle: () -> Void = {}
    var onPlaybackUpdate: (Double, Bool) -> Void = { _, _ in }
    var onNextChannelPressed: () -> Void = {}

    @StateObject private var model = PlayerModel()

    var body: some View {
        Group {
            if media == nil {
                loadingState
            } else if isWebStream(media) {
                YouTubeWebView(media: media!, isMuted: isMuted, subtitlesEnabled: subtitlesEnabled)
                    .overlay(alignment: .topTrailing) { controlRow }
            } else {
                VideoPlayer(player: model.player)
                    .overlay(alignment: .topTrailing) { controlRow }
                    .onAppear { model.attach(media: media!, onPlaybackUpdate: onPlaybackUpdate) }
                    .onChange(of: media?.id) { _, _ in
                        if let m = media { model.attach(media: m, onPlaybackUpdate: onPlaybackUpdate) }
                    }
                    .onChange(of: media?.isPaused) { _, paused in
                        if let p = paused { model.setPaused(p) }
                    }
            }
        }
        .background(Color.black)
        .frame(maxWidth: .infinity)
        .frame(height: isFullscreen ? .infinity : 220)
        .clipShape(RoundedRectangle(cornerRadius: isFullscreen ? 0 : 8))
        .overlay(
            RoundedRectangle(cornerRadius: isFullscreen ? 0 : 8)
                .stroke(palette.primary.opacity(0.4), lineWidth: 1)
        )
    }

    private var loadingState: some View {
        ZStack {
            LinearGradient(colors: [palette.surface.opacity(0.8), .black],
                           startPoint: .top, endPoint: .bottom)
            VStack(spacing: 12) {
                ProgressView().tint(palette.primary)
                Text("AWAITING CYTUBE BEACON...")
                    .font(.system(.caption, design: .monospaced).bold())
                    .foregroundStyle(palette.primary)
                Text("[CONNECT TO START CYBER SYNC]")
                    .font(.system(.caption2, design: .monospaced))
                    .foregroundStyle(.gray)
            }
        }
    }

    private var controlRow: some View {
        HStack(spacing: 10) {
            Button { onMuteToggle() } label: {
                Image(systemName: isMuted ? "speaker.slash.fill" : "speaker.wave.2.fill")
            }
            if isFullscreen {
                Button { onFullscreenToggle() } label: { Image(systemName: "arrow.down.right.and.arrow.up.left") }
            } else {
                Button { onFullscreenToggle() } label: { Image(systemName: "arrow.up.left.and.arrow.down.right") }
            }
        }
        .font(.system(size: 16))
        .foregroundStyle(.white)
        .padding(8)
        .background(.black.opacity(0.5), in: Circle())
        .padding(8)
    }
}

// ponytail: AVPlayer lives in an ObservableObject so SwiftUI keeps it across body recomputations.
// B.8: Reconnect bei failedToPlayToEndTime (max 3), Notification-Observer.
@MainActor
final class PlayerModel: ObservableObject {
    let player = AVPlayer()
    private var ticker: Timer?
    private var onPlaybackUpdate: (Double, Bool) -> Void = { _, _ in }
    private var currentItem: AVPlayerItem?
    private var failObserver: NSObjectProtocol?
    private var reconnectAttempts = 0
    private var pendingMedia: CyTubeMedia?

    func attach(media: CyTubeMedia, onPlaybackUpdate: @escaping (Double, Bool) -> Void) {
        self.onPlaybackUpdate = onPlaybackUpdate
        pendingMedia = media
        reconnectAttempts = 0
        loadItem(media)
    }

    private func loadItem(_ media: CyTubeMedia) {
        let urlStr = convertGoogleDriveURL(media.id)
        guard let url = URL(string: urlStr) else { return }
        // ponytail: vorherigen Fail-Observer ablösen, neuen für dieses Item setzen
        if let o = failObserver { NotificationCenter.default.removeObserver(o); failObserver = nil }
        let item = AVPlayerItem(url: url)
        currentItem = item
        failObserver = NotificationCenter.default.addObserver(
            forName: .AVPlayerItemFailedToPlayToEndTime, object: item, queue: .main
        ) { [weak self] _ in
            // ponytail: Notification-Closure ist nonisolated → MainActor-Hop für handlePlaybackFailure
            Task { @MainActor in self?.handlePlaybackFailure() }
        }
        player.replaceCurrentItem(with: item)
        player.seek(to: CMTime(seconds: media.currentTime, preferredTimescale: 600))
        setPaused(media.isPaused)
        startTicker()
    }

    // B.8: max 3 Reconnects bei failedToPlayToEndTime (analog Android VideoPlayerManager)
    private func handlePlaybackFailure() {
        guard reconnectAttempts < 3, let media = pendingMedia else { return }
        reconnectAttempts += 1
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) { [weak self] in
            self?.loadItem(media)
        }
    }

    func setPaused(_ paused: Bool) {
        if paused { player.pause() } else { player.play() }
    }

    private func startTicker() {
        ticker?.invalidate()
        let t = Timer(timeInterval: 0.5, repeats: true) { [weak self] _ in
            guard let self else { return }
            Task { @MainActor in
                let cur = self.player.currentTime().seconds
                if cur.isFinite && self.player.rate > 0 {
                    self.onPlaybackUpdate(cur, false)
                }
            }
        }
        RunLoop.main.add(t, forMode: .common)
        ticker = t
    }

    deinit {
        ticker?.invalidate()
        if let o = failObserver { NotificationCenter.default.removeObserver(o) }
    }
}

// MARK: - YouTube / Twitch / Vimeo WebView (B.8)

struct YouTubeWebView: UIViewRepresentable {
    let media: CyTubeMedia
    let isMuted: Bool
    var subtitlesEnabled: Bool = true

    func makeUIView(context: Context) -> WKWebView {
        let cfg = WKWebViewConfiguration()
        cfg.allowsInlineMediaPlayback = true
        cfg.mediaTypesRequiringUserActionForPlayback = []
        let web = WKWebView(frame: .zero, configuration: cfg)
        web.scrollView.isScrollEnabled = false
        web.isOpaque = false
        web.backgroundColor = .black
        return web
    }

    func updateUIView(_ web: WKWebView, context: Context) {
        let embedURL = buildEmbedURL()
        guard let url = URL(string: embedURL), web.url != url else { return }
        web.load(URLRequest(url: url))
    }

    // B.8: Embed-URL je nach type/id (analog Android buildPlayerHtml iframeSrc).
    // ponytail: direkte URL-Laden statt HTML-Wrapper — WKWebView resolved Embeds nativ.
    private func buildEmbedURL() -> String {
        let type = media.type.lowercased()
        let id = media.id
        let startSec = Int(media.currentTime)
        let muted = isMuted ? 1 : 0
        let ccParam = subtitlesEnabled ? "&cc_load_policy=1&cc_lang_pref=de" : "&cc_load_policy=0"

        if type == "tw" || id.contains("twitch.tv/") {
            // ponytail: parent=cytu.be nötig für Twitch-Embed; localhost als 2. parent
            let channel = id.contains("twitch.tv/") ? id.split(separator: "/").last.map(String.init) ?? id : id
            return "https://player.twitch.tv/?channel=\(channel)&parent=cytu.be&parent=localhost&autoplay=true&muted=\(isMuted ? "true" : "false")"
        }
        if type == "vi" {
            return "https://player.vimeo.com/video/\(id)?autoplay=1&muted=\(muted)#t=\(startSec)s"
        }
        if id.hasPrefix("http") {
            return id
        }
        // YouTube (default): jsapi nicht nötig ohne PostMessage-Steuerung
        return "https://www.youtube.com/embed/\(id)?autoplay=1&controls=0&mute=\(muted)&start=\(startSec)\(ccParam)&playsinline=1&rel=0"
    }
}