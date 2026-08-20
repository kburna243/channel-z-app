import Foundation
import Observation

// CyTube-Client — socket.io EIO=3 über URLSessionWebSocketTask (nativ, kein SPM-Dep).
// Port von CyTubeClient.kt: Connect/Join/Ping, Event-Parsing, Simulation-Fallback,
// Playlist/Queue-Handling, Reconnect-Backoff. B.1 18.08.26.
@Observable
final class CyTubeClient {

    // --- UI state (analog Kotlin StateFlows) ---
    var connectionState: ConnectionState = .disconnected
    var mediaState: CyTubeMedia? = nil
    var chatMessages: [CyTubeChatMessage] = []
    var userCount: Int = 1
    var playlist: [CyTubeMedia] = []        // B.1: full queue (analog cachedPlaylist)
    var upNext: [CyTubeMedia] = []          // B.1: next 4 items (analog _upNext)
    var nickname: String = "Guest_\(Int.random(in: 1000...9999))"

    var currentURL: String = ""
    private(set) var currentChannel: String = ""
    private(set) var isSimulated: Bool = false

    // --- internals ---
    private var webSocket: URLSessionWebSocketTask?
    private let session = URLSession(configuration: .default)
    private var pingTimer: Timer?
    private var simTimer: Timer?
    private var simTime: Double = 0
    private var simMediaIndex = 0
    private var isReceiving = false
    private var isIntentionallyClosed = false   // B.1: flag gegen Reconnect nach User-disconnect
    private var reconnectAttempt = 0
    private var requestPlaylistScheduled = false

    /// CyTube erlaubt requestPlaylist einmal pro Minute (REQ_PLAYLIST_LIMIT_REACHED).
    private static let playlistRequestMinInterval: TimeInterval = 60
    private var lastPlaylistRequest: TimeInterval = 0

    // Demo streams (1:1 from Kotlin)
    private let simMedia: [(String, String)] = [
        ("Night CyTube Live Stream (HLS)", "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"),
        ("Cyberpunk Ambient Music Video", "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"),
        ("Sinthwave Retro Wave (Direct Stream)", "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4")
    ]

    // MARK: - Public API

    func connect(serverURL: String, channel: String, simulateFallback: Bool = false) {
        isIntentionallyClosed = false
        reconnectAttempt = 0
        cleanupConnection()
        isSimulated = simulateFallback
        currentURL = serverURL
        currentChannel = channel

        if simulateFallback {
            startSimulation()
            return
        }

        connectionState = .connecting

        var wsURL = serverURL
        if wsURL.hasPrefix("http://") { wsURL = "ws://" + wsURL.dropFirst("http://".count) }
        else if wsURL.hasPrefix("https://") { wsURL = "wss://" + wsURL.dropFirst("https://".count) }

        if !wsURL.contains("socket.io") {
            let sep = wsURL.hasSuffix("/") ? "" : "/"
            wsURL = "\(wsURL)\(sep)socket.io/?EIO=3&transport=websocket"
        }

        guard let url = URL(string: wsURL) else {
            connectionState = .error
            addSystemMessage("Invalid URL: \(wsURL)")
            return
        }

        var request = URLRequest(url: url)
        request.timeoutInterval = 10
        request.setValue("https://cytu.be", forHTTPHeaderField: "Origin")   // B.1: CyTube erwartet Origin
        let task = session.webSocketTask(with: request)
        webSocket = task
        task.resume()
        isReceiving = false
        receiveLoop()
    }

    func disconnect() {
        isIntentionallyClosed = true
        cleanupConnection()
        connectionState = .disconnected
    }

    // ponytail: internes Cleanup ohne state/flag-Reset (für Reconnect nutzbar)
    private func cleanupConnection() {
        stopSimulation()
        pingTimer?.invalidate(); pingTimer = nil
        webSocket?.cancel(with: .goingAway, reason: nil)
        webSocket = nil
        isReceiving = false
    }

    func sendChatMessage(_ msg: String) {
        guard !msg.trimmingCharacters(in: .whitespaces).isEmpty else { return }
        if isSimulated {
            addChatMessage(username: nickname, msg: msg)
            let replies = [
                "Hell yeah, love this track! 🤘",
                "Mike's CyTube never sleeps. 👽",
                "Can you sync the stream again? It buffered.",
                "The Matrix rain looks sick!",
                "Who is streaming tonight? 🔥"
            ]
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) { [weak self] in
                self?.addChatMessage(username: "CyberPunk_420", msg: replies.randomElement() ?? "🤘")
            }
            return
        }
        sendEvent("chatMsg", ["msg": msg])
    }

    func setNickname(_ name: String) {
        let trimmed = name.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return }
        nickname = trimmed
        if isSimulated {
            addSystemMessage("Changed simulated nickname to '\(trimmed)'")
            return
        }
        sendEvent("setNm", ["name": trimmed])
        addSystemMessage("Requested nickname '\(trimmed)'...")
    }

    func sendMediaSync(currentTime: Double, isPaused: Bool) {
        if isSimulated {
            simTime = currentTime
            mediaState = mediaState.map { CyTubeMedia(title: $0.title, id: $0.id, type: $0.type,
                                                      duration: $0.duration, currentTime: currentTime, isPaused: isPaused,
                                                      url: $0.url, directUrl: $0.directUrl) }
            return
        }
        // ponytail: server-side mediaUpdate needs admin perms; client-side sync only. Phase 2 wires real reporting.
    }

    // Simulation controls (used by Phase 2 player UI)
    func triggerNextSimulatedMedia() {
        guard isSimulated else { return }
        setSimulatedMedia(index: (simMediaIndex + 1) % simMedia.count)
        addSystemMessage("Changed station to next simulated media stream.")
    }

    func toggleSimPlayPause() {
        guard let m = mediaState else { return }
        mediaState = CyTubeMedia(title: m.title, id: m.id, type: m.type,
                                 duration: m.duration, currentTime: m.currentTime, isPaused: !m.isPaused,
                                 url: m.url, directUrl: m.directUrl)
    }

    // MARK: - WebSocket receive + EIO=3 framing

    private func receiveLoop() {
        guard let ws = webSocket, !isReceiving else { return }
        isReceiving = true
        ws.receive { [weak self] result in
            guard let self else { return }
            self.isReceiving = false
            switch result {
            case .success(let msg):
                if case .string(let text) = msg { self.handleSocketIO(text) }
                if self.webSocket != nil { self.receiveLoop() }
            case .failure(let err):
                DispatchQueue.main.async {
                    self.connectionState = .error
                    self.addSystemMessage("Connection Error: \(err.localizedDescription)")
                    guard self.webSocket != nil, !self.isIntentionallyClosed else { return }
                    self.scheduleReconnect()
                }
            }
        }
    }

    // B.1: Reconnect-Backoff analog Android (2s/4s/8s), nach 3 Versuchen Sim-Fallback
    private func scheduleReconnect() {
        guard !isIntentionallyClosed else { return }
        guard reconnectAttempt < 3 else {
            addSystemMessage("Server unreachable after 3 attempts. Starting local demo simulation...")
            startSimulation()
            return
        }
        reconnectAttempt += 1
        let backoff = min(2000 * (1 << (reconnectAttempt - 1)), 8000)   // 2s, 4s, 8s
        connectionState = .connecting
        addSystemMessage("Reconnect in \(backoff/1000)s (attempt \(reconnectAttempt)/3)...")
        DispatchQueue.main.asyncAfter(deadline: .now() + Double(backoff) / 1000.0) { [weak self] in
            guard let self, !self.isIntentionallyClosed else { return }
            self.connect(serverURL: self.currentURL, channel: self.currentChannel)
        }
    }

    private func handleSocketIO(_ text: String) {
        guard !text.isEmpty else { return }
        if text == "0" || text.first == "0" {
            DispatchQueue.main.async { self.connectionState = .connected }
            reconnectAttempt = 0
            addSystemMessage("Connected to server! Handshaking...")
            startPingLoop()
            joinChannel()
        } else if text.first == "2" {
            webSocket?.send(.string("3")) { _ in }
        } else if text.first == "4" && text.count > 2 && text[text.index(text.startIndex, offsetBy: 1)] == "2" {
            let jsonStr = String(text.dropFirst(2))
            parseEvent(jsonStr)
        }
    }

    private func parseEvent(_ jsonStr: String) {
        guard let data = jsonStr.data(using: .utf8),
              let arr = try? JSONSerialization.jsonObject(with: data) as? [Any],
              let eventName = arr.first as? String else { return }
        let obj = arr.count > 1 ? (arr[1] as? [String: Any]) : nil
        let arr2 = arr.count > 1 ? (arr[1] as? [Any]) : nil

        switch eventName {
        case "chatMsg":
            guard let d = obj else { return }
            let username = (d["username"] as? String) ?? "System"
            let raw = (d["msg"] as? String) ?? ""
            let time = (d["time"] as? Double) ?? Date().timeIntervalSince1970 * 1000
            addChatMessage(username: username, msg: stripHTML(raw), time: time)
        case "changeMedia", "setCurrent":    // B.1: setCurrent == changeMedia (analog Android)
            guard let d = obj else { return }
            handleMediaChange(d)
        case "mediaUpdate":
            guard let d = obj else { return }
            let t = (d["time"] as? Double) ?? (d["currentTime"] as? Double) ?? 0
            let paused = (d["paused"] as? Bool) ?? false
            DispatchQueue.main.async {
                if var m = self.mediaState {
                    m.currentTime = t; m.isPaused = paused
                    self.mediaState = m
                }
            }
        case "playlist", "setPlaylist":       // B.1: playlist kann Array oder {playlist:[...]} sein
            let array: [Any]? = {
                if let a = arr2 { return a }
                if let d = obj, let p = d["playlist"] as? [Any] { return p }
                if let d = obj, let p = d["items"] as? [Any] { return p }
                return nil
            }()
            if let a = array { handlePlaylist(a) }
        case "queue":                          // B.1: item added
            guard let d = obj else { return }
            handleQueueItem(d)
        case "delete":                         // B.1: item removed by uid
            guard let d = obj else { return }
            let uid = (d["uid"] as? String) ?? ""
            if !uid.isEmpty {
                DispatchQueue.main.async {
                    self.playlist.removeAll { $0.id == uid }
                    self.updateUpNextList()
                }
            }
        case "userlist", "setUserlist":        // B.1: array length = count
            let count = arr2?.count ?? 1
            DispatchQueue.main.async { self.userCount = count }
        case "usercount":                      // B.1: int payload
            let count = (arr.count > 1 ? (arr[1] as? Int) : nil) ?? 0
            DispatchQueue.main.async { self.userCount = count }
        case "addUser":
            DispatchQueue.main.async { self.userCount += 1 }
        case "userLeave":
            DispatchQueue.main.async { self.userCount = max(0, self.userCount - 1) }
        case "setUser":
            let name = (arr.count > 1 ? (arr[1] as? String) : "") ?? ""
            if !name.isEmpty {
                DispatchQueue.main.async { self.nickname = name }
                addSystemMessage("Nickname confirmed as '\(name)'")
            }
        default:
            break
        }
    }

    // MARK: - B.1 Playlist/Queue handlers (port from CyTubeSocketClient.kt)

    private func handleMediaChange(_ d: [String: Any]) {
        let id = (d["id"] as? String) ?? ""
        let title = (d["title"] as? String) ?? "420 Grindhouse Live"
        let dur = (d["seconds"] as? Double) ?? (d["duration"] as? Double) ?? 0
        let type = (d["type"] as? String) ?? "raw"
        let currentTime = (d["currentTime"] as? Double) ?? (d["time"] as? Double) ?? 0
        let paused = (d["paused"] as? Bool) ?? false
        let direct = parseDirectUrl(from: d)
        let url = direct.isEmpty ? (id.hasPrefix("http") ? id : nil) : direct

        let item = CyTubeMedia(title: title, id: id, type: type, duration: dur,
                               currentTime: currentTime, isPaused: paused,
                               url: url, directUrl: direct.isEmpty ? nil : direct)
        DispatchQueue.main.async {
            self.mediaState = item
            self.updateUpNextList()
            // Playlist nachfordern, wenn sie leer ist. CyTube limitiert requestPlaylist auf
            // einen Aufruf pro Minute und antwortet sonst mit REQ_PLAYLIST_LIMIT_REACHED —
            // ungedrosselt lief das bei jedem Medienwechsel und damit regelmaessig ins Limit.
            let now = Date().timeIntervalSince1970
            if self.playlist.isEmpty, now - self.lastPlaylistRequest >= Self.playlistRequestMinInterval {
                self.lastPlaylistRequest = now
                self.sendEvent("requestPlaylist", [:])
            }
        }
    }

    private func handlePlaylist(_ array: [Any]) {
        var items: [CyTubeMedia] = []
        for (i, entry) in array.enumerated() {
            guard let entry = entry as? [String: Any] else { continue }
            let mediaObj = (entry["media"] as? [String: Any]) ?? entry
            let mediaId = (mediaObj["id"] as? String) ?? ""
            let uid = (entry["uid"] as? String) ?? ""
            let id = mediaId.isEmpty ? (uid.isEmpty ? "\(i)" : uid) : mediaId
            let title = (mediaObj["title"] as? String) ?? (entry["title"] as? String) ?? "Upcoming Video"
            let type = (mediaObj["type"] as? String) ?? "raw"
            let dur = (mediaObj["seconds"] as? Double) ?? (mediaObj["duration"] as? Double) ?? 0
            let direct = parseDirectUrl(from: mediaObj)
            let url = direct.isEmpty ? (id.hasPrefix("http") ? id : nil) : direct
            items.append(CyTubeMedia(title: title, id: id, type: type, duration: dur,
                                      currentTime: 0, isPaused: false, url: url,
                                      directUrl: direct.isEmpty ? nil : direct))
        }
        DispatchQueue.main.async {
            self.playlist = items
            self.updateUpNextList()
        }
    }

    private func handleQueueItem(_ d: [String: Any]) {
        let item = d["item"] as? [String: Any]
        let mediaObj = (item?["media"] as? [String: Any]) ?? item ?? d
        let id = (mediaObj["id"] as? String) ?? ""
        let type = (mediaObj["type"] as? String) ?? "raw"
        let title = (mediaObj["title"] as? String) ?? "Queued Media"
        let dur = (mediaObj["seconds"] as? Double) ?? (mediaObj["duration"] as? Double) ?? 0
        let direct = parseDirectUrl(from: mediaObj)
        let url = direct.isEmpty ? (id.hasPrefix("http") ? id : nil) : direct
        let newMedia = CyTubeMedia(title: title, id: id, type: type, duration: dur,
                                   currentTime: 0, isPaused: false, url: url,
                                   directUrl: direct.isEmpty ? nil : direct)
        DispatchQueue.main.async {
            self.playlist.append(newMedia)
            self.updateUpNextList()
        }
    }

    // B.1: upNext = nächste 4 Items nach aktuellem (analog Android updateUpNextList)
    private func updateUpNextList() {
        guard !playlist.isEmpty else { upNext = []; return }
        guard let current = mediaState else { upNext = Array(playlist.dropFirst().prefix(4)); return }
        let idx = playlist.firstIndex { ($0.id == current.id && !current.id.isEmpty) ||
                                        ($0.title == current.title && !current.title.isEmpty) }
        if let i = idx, i + 1 < playlist.count {
            upNext = Array(playlist[(i + 1)...].prefix(4))
            return
        }
        let remaining = playlist.filter { (current.id.isEmpty || $0.id != current.id) &&
                                          (current.title.isEmpty || $0.title != current.title) }
        upNext = remaining.isEmpty ? Array(playlist.dropFirst().prefix(4)) : Array(remaining.prefix(4))
    }

    // ponytail: vereinfacht — nur ersten direct-Link (Android sortiert nach Quality).
    // meta.direct als Object {quality: [{link}]} → erster link; oder als String; oder id wenn http.
    private func parseDirectUrl(from d: [String: Any]) -> String {
        if let meta = d["meta"] as? [String: Any] {
            if let direct = meta["direct"] as? [String: Any] {
                for (_, v) in direct {
                    if let arr = v as? [Any], let first = arr.first as? [String: Any],
                       let link = first["link"] as? String, !link.isEmpty { return link }
                }
            }
            if let directStr = meta["direct"] as? String, directStr.hasPrefix("http") { return directStr }
        }
        if let id = d["id"] as? String, id.hasPrefix("http") { return id }
        return ""
    }

    private func joinChannel() {
        sendEvent("joinChannel", ["name": currentChannel])
        addSystemMessage("Joining room '\(currentChannel)'...")
        sendEvent("setNm", ["name": nickname])
        // B.1: requestPlaylist nach 300ms (analog Android) — einmal pro Join
        if !requestPlaylistScheduled {
            requestPlaylistScheduled = true
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { [weak self] in
                self?.sendEvent("requestPlaylist", [:])
                self?.requestPlaylistScheduled = false
            }
        }
    }

    // ponytail: leeres Payload → nur [name] (CyTube erwartet 42["requestPlaylist"] ohne {}, vgl. Android)
    private func sendEvent(_ name: String, _ payload: [String: Any] = [:]) {
        guard let ws = webSocket else { return }
        let items: [Any] = payload.isEmpty ? [name] : [name, payload]
        let frame = "42\(serialise(items))"
        ws.send(.string(frame)) { _ in }
    }

    private func serialise(_ items: [Any]) -> String {
        guard let data = try? JSONSerialization.data(withJSONObject: items),
              let s = String(data: data, encoding: .utf8) else { return "[]" }
        return s
    }

    private func startPingLoop() {
        pingTimer?.invalidate()
        let t = Timer(timeInterval: 25, repeats: true) { [weak self] _ in
            self?.webSocket?.send(.string("2")) { _ in }
        }
        RunLoop.main.add(t, forMode: .common)
        pingTimer = t
    }

    // MARK: - Chat helpers

    private func addChatMessage(username: String, msg: String, time: Double = Date().timeIntervalSince1970 * 1000) {
        let entry = CyTubeChatMessage(username: username, message: msg, time: Date(timeIntervalSince1970: time / 1000))
        DispatchQueue.main.async {
            self.chatMessages.append(entry)
            if self.chatMessages.count > 100 { self.chatMessages.removeFirst(self.chatMessages.count - 100) }
        }
    }

    private func addSystemMessage(_ text: String) {
        addChatMessage(username: "[System]", msg: text)
    }

    private func stripHTML(_ s: String) -> String {
        s.replacingOccurrences(of: "<[^>]*>", with: "", options: .regularExpression)
    }

    // MARK: - Simulation

    private func startSimulation() {
        isSimulated = true
        connectionState = .connected
        addSystemMessage("DEMO SIMULATION ACTIVE (No CORS restrictions)")
        addSystemMessage("Press the Mic icon to use native Voice-to-Chat!")
        addSystemMessage("Try the EPG panel on the side to browse guide posts!")
        setSimulatedMedia(index: 0)

        simTimer?.invalidate()
        let t = Timer(timeInterval: 1, repeats: true) { [weak self] _ in self?.simTick() }
        RunLoop.main.add(t, forMode: .common)
        simTimer = t
    }

    private func simTick() {
        guard let m = mediaState else { return }
        if !m.isPaused {
            simTime += 1
            if m.duration > 0 && simTime >= m.duration {
                setSimulatedMedia(index: (simMediaIndex + 1) % simMedia.count)
            } else {
                mediaState = CyTubeMedia(title: m.title, id: m.id, type: m.type,
                                         duration: m.duration, currentTime: simTime, isPaused: false,
                                         url: m.url, directUrl: m.directUrl)
            }
        }
        if Double.random(in: 0..<1) < 0.15 {
            let users = ["MikeCyTubeFan", "Neon_Glitch", "RetroRider", "GreenCode", "AliceInWire"]
            let msgs = [
                "Who is watching from the mobile app? This client runs beautifully! 📱",
                "Matrix Rain visual is standard cyberpunk bliss. Code-green is best.",
                "That program guide feed scraped from Reddit is extremely helpful.",
                "Direct stream streaming is incredibly responsive. 🤘",
                "Love the Voice transcription feature, literally hands-free chatting!",
                "Mike's CyTube live show is epic today!"
            ]
            addChatMessage(username: users.randomElement() ?? "Anon", msg: msgs.randomElement() ?? "🤘")
        }
    }

    private func setSimulatedMedia(index: Int) {
        simMediaIndex = index
        simTime = 0
        let media = simMedia[index]
        mediaState = CyTubeMedia(title: media.0, id: media.1,
                                 type: media.1.hasSuffix("m3u8") ? "hl" : "fi",
                                 duration: 180, currentTime: 0, isPaused: false,
                                 url: media.1, directUrl: nil)
    }

    private func stopSimulation() {
        simTimer?.invalidate(); simTimer = nil
        isSimulated = false
    }
}

enum ConnectionState: String {
    case disconnected, connecting, connected, error
}

struct CyTubeChatMessage: Identifiable, Equatable {
    let id = UUID()
    let username: String
    let message: String
    let time: Date
    var isSystem: Bool { username == "[System]" }
}

struct CyTubeMedia: Equatable, Identifiable {
    let title: String
    let id: String
    let type: String        // "yt" | "gd" | "fi" | "hl" | ...
    let duration: Double
    var currentTime: Double
    var isPaused: Bool
    var url: String? = nil       // B.1: direct file url (fi/hl)
    var directUrl: String? = nil  // B.1: meta.direct resolved url
}