import SwiftUI

// Reddit-EPG — Port von RedditEPG.kt. URLSession gegen reddit .json, User-Agent
// Pflicht (sonst 429). Fallback: statische Demo-Posts bei Fehler. Ponytail: kein
// Codable, JSONSerialization für dynamische Reddit-Struktur.

struct RedditPost: Identifiable, Equatable {
    let id = UUID()
    let title: String
    let author: String
    let score: Int
    let url: String
    let selfText: String
    let timeSlot: String
}

@Observable
final class RedditEPGStore {
    var posts: [RedditPost] = []
    var isLoading = false
    var errorMessage: String?
    var subreddit = "420grindhouse"

    private let slots = [
        "12:00 - 14:00", "14:00 - 16:00", "16:00 - 18:00", "18:00 - 20:00",
        "20:00 - 22:00", "22:00 - 00:00", "00:00 - 02:00", "02:00 - 04:00"
    ]

    func fetch() async {
        isLoading = true
        errorMessage = nil
        let sub = subreddit.trimmingCharacters(in: .whitespaces).lowercased()
        guard !sub.isEmpty, let url = URL(string: "https://www.reddit.com/r/\(sub).json?limit=10") else {
            fallback(); return
        }
        var req = URLRequest(url: url)
        req.setValue("ios:com.mikes.cytube:v1.0.0 (by /u/mike_cytube_dev)", forHTTPHeaderField: "User-Agent")
        req.timeoutInterval = 8
        do {
            let (data, resp) = try await URLSession.shared.data(for: req)
            guard let http = resp as? HTTPURLResponse, http.statusCode == 200 else { fallback(); return }
            let parsed = parse(data)
            if parsed.isEmpty { fallback() } else { posts = parsed }
        } catch {
            fallback()
        }
        isLoading = false
    }

    private func parse(_ data: Data) -> [RedditPost] {
        guard let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let d = root["data"] as? [String: Any],
              let children = d["children"] as? [[String: Any]] else { return [] }
        return children.enumerated().map { (i, child) in
            let dp = (child["data"] as? [String: Any]) ?? [:]
            return RedditPost(
                title: (dp["title"] as? String) ?? "",
                author: (dp["author"] as? String) ?? "",
                score: (dp["score"] as? Int) ?? 0,
                url: (dp["url"] as? String) ?? "",
                selfText: (dp["selftext"] as? String) ?? "[No description details]",
                timeSlot: slots[i % slots.count]
            )
        }
    }

    private func fallback() {
        posts = fallbackPosts
        errorMessage = "Failed to pull live Reddit API feed. Loaded cached local EPG broadcast guide."
        isLoading = false
    }

    private var fallbackPosts: [RedditPost] {
        [
            .init(title: "🚨 Midnight Madness: Akira Cyberpunk Anime Live Stream 🚨",
                  author: "mike_cytube", score: 420,
                  url: "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                  selfText: "Late night anime block.", timeSlot: slots[0]),
            .init(title: "🎥 Retro Grindhouse: Kung Fu vs Neon Cyborgs (1987) 🎥",
                  author: "neon_archive", score: 311,
                  url: "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                  selfText: "VHS-scan restoration.", timeSlot: slots[1]),
            .init(title: "👽 Alien Synths & Ambient Visuals: Deep Space Grindhouse 👽",
                  author: "synthlord", score: 256,
                  url: "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                  selfText: "4-hour ambient loop.", timeSlot: slots[2]),
            .init(title: "💻 Live Hackathon: Writing Kotlin Retro Synthesizers 💻",
                  author: "code_witch", score: 198,
                  url: "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                  selfText: "Code + chips.", timeSlot: slots[3])
        ]
    }
}

struct RedditEPGPanel: View {
    @State private var store = RedditEPGStore()
    let palette: CyTubeColors
    var onQueuePostMedia: (String, String) -> Void = { _, _ in }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                HStack {
                    TextField("subreddit", text: $store.subreddit)
                        .textFieldStyle(.roundedBorder)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                    Button { Task { await store.fetch() } } label: { Image(systemName: "arrow.clockwise") }
                        .tint(palette.primary)
                }
                .padding()

                if let err = store.errorMessage {
                    Text(err).font(.caption2).foregroundStyle(.orange).padding(.horizontal)
                }

                if store.isLoading {
                    ProgressView().tint(palette.primary).padding()
                } else {
                    List(store.posts) { post in
                        Button { onQueuePostMedia(post.title, videoURL(post.url)) } label: {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(post.timeSlot).font(.caption2.monospaced()).foregroundStyle(palette.accent)
                                Text(post.title).font(.subheadline.bold()).foregroundStyle(palette.primary)
                                    .lineLimit(2)
                                Text("u/\(post.author) · ⬆ \(post.score)")
                                    .font(.caption2).foregroundStyle(palette.muted)
                            }
                        }.buttonStyle(.plain)
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle("EPG Guide")
            .task { await store.fetch() }
        }
    }

    // ponytail: queue only direct video URLs; Reddit posts link out otherwise
    private func videoURL(_ u: String) -> String {
        u.hasSuffix(".mp4") || u.contains(".m3u8") ? u : ""
    }
}