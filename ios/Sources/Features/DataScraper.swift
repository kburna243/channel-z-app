import Foundation
import Observation

// B.6 DataScraper — Port von DataScraper.kt. Pollt cytubot.onrender.com/schedule alle
// 15s, Reddit-Fallback (pullpush.io) bei leer/fehler. Ponytail: 1 Reddit-Endpoint statt
// 3er-Kaskade, JSONSerialization (kein Codable für dynamische CyTube-Struktur),
// Markdown/HTML-Cleaning stark vereinfacht.

struct QueueScheduleItem: Identifiable, Equatable {
    let id = UUID()
    let title: String
    let durationSeconds: Int
    let startTimeFormatted: String
    let durationFormatted: String
    let mediaId: String
}

@Observable
final class DataScraper {
    var scheduleItems: [QueueScheduleItem] = []
    var redditFallback: Bool = false
    var redditScheduleTitle: String? = nil
    var redditScheduleText: String? = nil

    private var pollTask: Task<Void, Never>?
    private let session: URLSession = {
        let cfg = URLSessionConfiguration.default
        cfg.timeoutIntervalForRequest = 6
        cfg.timeoutIntervalForResource = 6
        return URLSession(configuration: cfg)
    }()

    func start(pollIntervalSeconds: TimeInterval = 15) {
        stop()
        pollTask = Task { [weak self] in
            while !Task.isCancelled {
                await self?.fetchSchedule()
                try? await Task.sleep(nanoseconds: UInt64(pollIntervalSeconds * 1_000_000_000))
            }
        }
    }

    func stop() {
        pollTask?.cancel(); pollTask = nil
    }

    // Haupt-Fetch: cytubot zuerst, Reddit-Fallback wenn leer/fehler (analog Android).
    func fetchSchedule() async {
        let ok = await fetchFromCytubot()
        if !ok || scheduleItems.isEmpty {
            await fetchFromReddit()
        }
    }

    // MARK: - Cytubot

    /// Sendeplan-Quellen in Vorzugsreihenfolge.
    ///
    /// Der Kanal betreibt seinen Schedule-Bot selbst; cytu.be bindet ihn ueber
    /// channelOpts.externaljs als Iframe von bot.420grindhouseserver.com ein, und dort liegt
    /// derselbe /schedule-Endpunkt mit gleichem JSON-Format. Die frueher fest verdrahtete
    /// Render-Instanz war zeitweise vom Anbieter abgeschaltet und bleibt Zweitquelle.
    private static let scheduleEndpoints = [
        "https://bot.420grindhouseserver.com/schedule",
        "https://cytubot.onrender.com/schedule"
    ]

    private func fetchFromCytubot() async -> Bool {
        let ts = Int(Date().timeIntervalSince1970 * 1000)
        for base in Self.scheduleEndpoints {
            guard let url = URL(string: "\(base)?t=\(ts)") else { continue }
            var req = URLRequest(url: url)
            req.setValue("Mikes420Grindhouse/2.0", forHTTPHeaderField: "User-Agent")
            req.setValue("no-cache", forHTTPHeaderField: "Cache-Control")
            do {
                let (data, resp) = try await session.data(for: req)
                guard let http = resp as? HTTPURLResponse, http.statusCode == 200,
                      let body = String(data: data, encoding: .utf8),
                      body.trimmingCharacters(in: .whitespaces).hasPrefix("{") else { continue }
                let parsed = parseCytubot(body)
                if parsed > 0 {
                    redditFallback = false
                    redditScheduleTitle = nil
                    redditScheduleText = nil
                    return true
                }
            } catch {
                // ponytail: kein Log, naechste Quelle bzw. Reddit-Fallback uebernimmt
            }
        }
        return false
    }

    private func parseCytubot(_ jsonStr: String) -> Int {
        guard let data = jsonStr.data(using: .utf8),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let playlist = root["playlist"] as? [Any] else { return 0 }
        let remainingSec = (root["remainingSeconds"] as? Double) ?? 0
        let nowMs = Date().timeIntervalSince1970 * 1000
        var accumulated = remainingSec > 0 ? remainingSec : 0
        let fmt = DateFormatter(); fmt.dateFormat = "HH:mm:ss"
        var items: [QueueScheduleItem] = []
        for entry in playlist {
            let obj = (entry as? [String: Any]) ?? [:]
            let media = (obj["media"] as? [String: Any]) ?? obj
            let title = (media["title"] as? String) ?? "Upcoming Video"
            let seconds = Int((media["seconds"] as? Double) ?? Double((media["seconds"] as? Int) ?? 0))
            let id = (media["id"] as? String) ?? ""
            let type = (media["type"] as? String) ?? "raw"
            let startMs = nowMs + accumulated * 1000
            items.append(QueueScheduleItem(
                title: title,
                durationSeconds: seconds,
                startTimeFormatted: fmt.string(from: Date(timeIntervalSince1970: startMs / 1000)),
                durationFormatted: formatDuration(seconds),
                mediaId: id.isEmpty ? type : id
            ))
            accumulated += Double(seconds)
        }
        scheduleItems = items
        return items.count
    }

    // MARK: - Reddit-Fallback (ponytail: nur pullpush.io, 1 Endpoint)

    private func fetchFromReddit(subreddit: String = "420grindhouse") async {
        guard let url = URL(string: "https://api.pullpush.io/reddit/search/submission/?subreddit=\(subreddit)&size=15") else { return }
        var req = URLRequest(url: url)
        req.setValue("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36", forHTTPHeaderField: "User-Agent")
        req.setValue("application/json", forHTTPHeaderField: "Accept")
        req.timeoutInterval = 8
        do {
            let (data, resp) = try await session.data(for: req)
            guard let http = resp as? HTTPURLResponse, http.statusCode == 200,
                  let body = String(data: data, encoding: .utf8) else { return }
            parseRedditJson(body)
        } catch {
            // ponytail: stiller Fallback; scheduleItems bleibt wie vorher
        }
    }

    private func parseRedditJson(_ jsonStr: String) {
        guard let data = jsonStr.data(using: .utf8),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return }
        // pullpush: {data: [...]}; reddit.com: {data:{children:[...]}}
        let posts: [[String: Any]] = (root["data"] as? [[String: Any]])
            ?? ((root["data"] as? [String: Any])?["children"] as? [[String: Any]])?.compactMap { ($0["data"] as? [String: Any]) ?? $0 }
            ?? []
        let fmt = DateFormatter(); fmt.dateFormat = "HH:mm"
        var accumulatedMs = Date().timeIntervalSince1970 * 1000
        var items: [QueueScheduleItem] = []
        var foundTitle: String?
        var foundText: String?
        for (i, post) in posts.enumerated() {
            let rawTitle = (post["title"] as? String)?.trimmingCharacters(in: .whitespaces) ?? ""
            if rawTitle.isEmpty { continue }
            let isSchedulePost = rawTitle.lowercased().contains("schedule")
                || rawTitle.lowercased().contains("programm")
                || rawTitle.lowercased().contains("lineup")
                || rawTitle.lowercased().contains("weekend")
                || rawTitle.lowercased().contains("marathon")
            let selfText = (post["selftext"] as? String)?.trimmingCharacters(in: .whitespaces) ?? ""
            if isSchedulePost, !selfText.isEmpty, foundText == nil {
                foundTitle = extractCleanMovieTitle(rawTitle)
                foundText = cleanMarkdown(selfText)
            }
            // Chat-Fragen herausfiltern
            let isChatQ = rawTitle.hasSuffix("?")
                || rawTitle.lowercased().contains("down?")
                || rawTitle.lowercased().contains("anyone know")
            if isChatQ && !isSchedulePost { continue }
            let cleanTitle = extractCleanMovieTitle(rawTitle)
            let durSec = 5400  // ponytail: ~90m Default für Spielfilm
            items.append(QueueScheduleItem(
                title: cleanTitle,
                durationSeconds: durSec,
                startTimeFormatted: fmt.string(from: Date(timeIntervalSince1970: accumulatedMs / 1000)),
                durationFormatted: "90m",
                mediaId: "reddit_\(i)"
            ))
            accumulatedMs += Double(durSec) * 1000
        }
        if !items.isEmpty || foundText != nil {
            scheduleItems = items
            redditScheduleTitle = foundTitle ?? "420Grindhouse Reddit Sendeplan"
            redditScheduleText = foundText
            redditFallback = true
        }
    }

    // MARK: - Text-Cleaning (ponytail: stark vereinfacht vs. Android 467 Zeilen)

    private func cleanMarkdown(_ text: String) -> String {
        // Reihenfolge zaehlt: erst Entities aufloesen, dann Tags entfernen. Andersherum
        // ueberleben maskierte Tags ("&lt;div&gt;") die Tag-Regex und werden anschliessend zu
        // echten spitzen Klammern demaskiert — sie landen dann als Quelltext in der Anzeige.
        // &amp; zuletzt, sonst wuerde "&amp;lt;" doppelt aufgeloest.
        text.replacingOccurrences(of: "&lt;", with: "<")
            .replacingOccurrences(of: "&gt;", with: ">")
            .replacingOccurrences(of: "&quot;", with: "\"")
            .replacingOccurrences(of: "&#39;", with: "'")
            .replacingOccurrences(of: "&nbsp;", with: " ")
            .replacingOccurrences(of: "&amp;", with: "&")
            .replacingOccurrences(of: "<!--[\\s\\S]*?-->", with: "", options: .regularExpression)
            .replacingOccurrences(of: "<[^>]*>", with: "", options: .regularExpression)
            .replacingOccurrences(of: "**", with: "")
            .replacingOccurrences(of: #"\[([^\]]+)\]\([^\)]+\)"#, with: "$1", options: .regularExpression)
            .replacingOccurrences(of: #"(?m)^#+\s*"#, with: "", options: .regularExpression)
            .replacingOccurrences(of: #"\n{3,}"#, with: "\n\n", options: .regularExpression)
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private func extractCleanMovieTitle(_ raw: String) -> String {
        let cleaned = cleanMarkdown(raw)
        // "Movie Title (Year) - Plot..." → bis zum " - " abschneiden
        if let dash = cleaned.range(of: " - "), dash.lowerBound != cleaned.startIndex {
            return String(cleaned[..<dash.lowerBound]).trimmingCharacters(in: .whitespaces)
        }
        return cleaned
    }

    private func formatDuration(_ seconds: Int) -> String {
        guard seconds > 0 else { return "0:00" }
        let mins = seconds / 60, secs = seconds % 60
        if mins >= 60 {
            return String(format: "%d:%02d:%02d", mins / 60, mins % 60, secs)
        }
        return String(format: "%d:%02d", mins, secs)
    }
}