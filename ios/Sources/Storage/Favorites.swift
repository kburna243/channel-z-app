import Foundation
import SwiftData
import SwiftUI

// SwiftData-Models (analog Room: FavoriteChannel, ChatSettings).
// ChatSettings ist ein Singleton (id=1) wie in Android.

@Model
final class FavoriteChannel {
    @Attribute(.unique) var id: Int
    var serverUrl: String
    var channelName: String
    var displayName: String
    var isCustom: Bool
    var timestamp: Date

    init(id: Int = 0, serverUrl: String, channelName: String, displayName: String,
         isCustom: Bool = true, timestamp: Date = Date()) {
        self.id = id
        self.serverUrl = serverUrl
        self.channelName = channelName
        self.displayName = displayName
        self.isCustom = isCustom
        self.timestamp = timestamp
    }
}

@Model
final class ChatSettings {
    @Attribute(.unique) var id: Int
    var showTimestamps: Bool
    var messageColorHex: String
    // B.7: SubtitleChatOverlay-Settings (analog Android SettingsRepository)
    var chatEnabled: Bool          // SubtitleChatOverlay-Sichtbarkeit über dem Player
    var chatMaxLines: Int          // 1–3
    var chatOpacity: Double        // 0.2–0.9
    var chatFontSize: Int          // pt
    var subtitlesEnabled: Bool     // B.8: YouTube-CC (cc_load_policy)

    init(id: Int = 1, showTimestamps: Bool = true, messageColorHex: String = "#FFFFFF",
         chatEnabled: Bool = true, chatMaxLines: Int = 3, chatOpacity: Double = 0.6,
         chatFontSize: Int = 15, subtitlesEnabled: Bool = true) {
        self.id = id
        self.showTimestamps = showTimestamps
        self.messageColorHex = messageColorHex
        self.chatEnabled = chatEnabled
        self.chatMaxLines = chatMaxLines
        self.chatOpacity = chatOpacity
        self.chatFontSize = chatFontSize
        self.subtitlesEnabled = subtitlesEnabled
    }
}

// ponytail: one shared container for the app, created once at launch.
enum AppStore {
    static let container: ModelContainer = {
        let schema = Schema([FavoriteChannel.self, ChatSettings.self])
        let config = ModelConfiguration(schema: schema, isStoredInMemoryOnly: false)
        do {
            return try ModelContainer(for: schema, configurations: [config])
        } catch {
            // Fallback: in-memory so app still runs if on-disk store is corrupt
            let mem = ModelConfiguration(schema: schema, isStoredInMemoryOnly: true)
            return try! ModelContainer(for: schema, configurations: [mem])
        }
    }()
}

// Singleton accessor for ChatSettings (always id=1, created lazily).
@MainActor
func ensureChatSettings(_ ctx: ModelContext) -> ChatSettings {
    let id = 1
    let descriptor = FetchDescriptor<ChatSettings>(predicate: #Predicate { $0.id == id })
    if let existing = (try? ctx.fetch(descriptor))?.first { return existing }
    let s = ChatSettings(id: id)
    ctx.insert(s)
    try? ctx.save()
    return s
}