import SwiftUI

// B.7 SubtitleChatOverlay — Port von SubtitleChatOverlay.kt. Zeigt die letzten 1–3
// Chat-Nachrichten als Subtitle-Stil über dem Player-Bottom. Ponytail: kein userRank
// (iOS CyTubeChatMessage hat keinen), Username-Farbe via Hash auf 3 Akzentfarben;
// System-Nachrichten in palette.muted. Konfiguration aus ChatSettings.

struct SubtitleChatOverlay: View {
    let messages: [CyTubeChatMessage]
    let isVisible: Bool
    var maxLines: Int = 3
    var backgroundOpacity: Double = 0.6
    var fontSize: Int = 15

    private var recent: [CyTubeChatMessage] {
        Array(messages.suffix(max(min(maxLines, 1), 3)))
    }

    var body: some View {
        if isVisible && !recent.isEmpty {
            VStack(spacing: 4) {
                ForEach(recent) { msg in
                    subtitleLine(msg)
                }
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 24)
            .transition(.opacity.combined(with: .move(edge: .bottom)))
            .animation(.easeInOut(duration: 0.25), value: recent.last?.id)
        }
    }

    // ponytail: HStack mit zwei Text-Views statt Text+Text-Konkatenation (letztere frickelig
    // mit Modifier-Chains — Typ-Inferenz probleme). Name farbig, Message weiß, lineLimit auf HStack.
    private func subtitleLine(_ msg: CyTubeChatMessage) -> some View {
        let nameColor: Color = msg.isSystem ? .orange : nameColor(for: msg.username)
        return HStack(spacing: 0) {
            Text("\(msg.username): ")
                .font(.system(size: CGFloat(fontSize), weight: .bold))
                .foregroundColor(nameColor)
            Text(msg.message)
                .font(.system(size: CGFloat(fontSize), weight: .medium))
                .foregroundColor(.white)
        }
        .shadow(color: .black.opacity(0.8), radius: 2, y: 1)
        .lineLimit(2)
        .padding(.horizontal, 14)
        .padding(.vertical, 5)
        .background(.black.opacity(min(max(backgroundOpacity, 0.2), 0.9)),
                    in: RoundedRectangle(cornerRadius: 8))
    }

    // ponytail: 3-Farben-Rotation wie Android AccentIceBlue/Coral/Lavender
    private func nameColor(for name: String) -> Color {
        let absHash = abs(name.hashValue)
        switch absHash % 3 {
        case 0: return .cyan      // AccentIceBlue
        case 1: return .orange    // AccentCoral
        default: return .purple   // AccentLavender
        }
    }
}