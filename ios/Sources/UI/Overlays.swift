import SwiftUI

// B.3+B.4+B.5 18.08.26 — Player-Overlays + Status-Dot (port MetadataOverlay.kt /
// UpNextOverlay.kt / StatusIndicatorDot.kt, bewusst vereinfacht für iOS-Sideload).

// MARK: - B.3 Now-Playing HUD (Top-Center über dem Player)

struct MetadataOverlay: View {
    let cyTube: CyTubeClient
    let palette: CyTubeColors
    @State private var pulse = false

    var body: some View {
        if let m = cyTube.mediaState {
            VStack(spacing: 4) {
                HStack(spacing: 6) {
                    Image(systemName: "tv.fill")
                        .foregroundStyle(palette.primary)
                    Text("NOW PLAYING")
                        .font(.system(.caption2, design: .monospaced).bold())
                        .foregroundStyle(palette.muted)
                    if cyTube.connectionState == .connected {
                        HStack(spacing: 3) {
                            Circle().fill(.green).frame(width: 7, height: 7)
                                .scaleEffect(pulse ? 1.4 : 1.0)
                            Text("LIVE")
                                .font(.system(.caption2, design: .monospaced).bold())
                                .foregroundStyle(.green)
                        }
                    }
                }
                Text(m.title)
                    .font(.system(.caption, design: .monospaced).bold())
                    .foregroundStyle(.white)
                    .lineLimit(2)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: 240)
                Text("cytu.be/r/\(cyTube.currentChannel)")
                    .font(.system(.caption2, design: .monospaced))
                    .foregroundStyle(palette.accent)
                if let next = cyTube.upNext.first {
                    HStack(spacing: 4) {
                        Text("UP NEXT")
                            .font(.system(.caption2, design: .monospaced).bold())
                            .foregroundStyle(palette.muted)
                        Text(next.title)
                            .font(.system(.caption2, design: .monospaced))
                            .foregroundStyle(.white)
                            .lineLimit(1)
                    }
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(.black.opacity(0.6), in: RoundedRectangle(cornerRadius: 8))
            .padding(.top, 8)
            .onAppear {
                withAnimation(.easeInOut(duration: 0.9).repeatForever(autoreverses: true)) { pulse = true }
            }
        }
    }
}

// MARK: - B.4 Queue-Schedule (Slide-in von rechts)

struct UpNextOverlay: View {
    let cyTube: CyTubeClient
    let palette: CyTubeColors
    var scheduleItems: [QueueScheduleItem] = []   // B.6: Scraper-Fallback
    var isRedditFallback: Bool = false             // B.6: Badge-Quelle
    var onClose: () -> Void = {}

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Image(systemName: "list.bullet")
                Text("QUEUE")
                    .font(.system(.caption, design: .monospaced).bold())
                if cyTube.upNext.isEmpty && !scheduleItems.isEmpty {
                    Text(isRedditFallback ? "· REDDIT" : "· CYTUBOT")
                        .font(.system(.caption2, design: .monospaced))
                        .foregroundStyle(palette.accent)
                }
                Spacer()
                Button { onClose() } label: { Image(systemName: "xmark") }
                    .tint(palette.muted)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(palette.surface.opacity(0.9))

            Divider()

            ScrollView {
                LazyVStack(alignment: .leading, spacing: 6) {
                    if !cyTube.upNext.isEmpty {
                        ForEach(Array(cyTube.upNext.enumerated()), id: \.element.id) { idx, item in
                            queueRow(idx + 1, item)
                        }
                    } else if !scheduleItems.isEmpty {
                        ForEach(scheduleItems) { item in scheduleRow(item) }
                    } else {
                        Text("Queue empty")
                            .font(.system(.caption, design: .monospaced))
                            .foregroundStyle(palette.muted)
                            .padding(12)
                    }
                }
                .padding(8)
            }
        }
        .frame(width: UIScreen.main.bounds.width * 0.58)
        .frame(maxHeight: .infinity)
        .background(palette.surface.opacity(0.95))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    private func queueRow(_ idx: Int, _ item: CyTubeMedia) -> some View {
        HStack(alignment: .top, spacing: 6) {
            Text("\(idx)")
                .font(.system(.caption2, design: .monospaced))
                .foregroundStyle(palette.muted)
                .frame(width: 18)
            VStack(alignment: .leading, spacing: 2) {
                Text(item.title)
                    .font(.system(.caption2, design: .monospaced))
                    .foregroundStyle(.white)
                    .lineLimit(2)
                Text(fmtDur(item.duration))
                    .font(.system(.caption2, design: .monospaced))
                    .foregroundStyle(palette.muted)
            }
            Spacer(minLength: 0)
        }
    }

    // B.6: Scraper-Zeile mit Startzeit + Duration (aus QueueScheduleItem)
    private func scheduleRow(_ item: QueueScheduleItem) -> some View {
        HStack(alignment: .top, spacing: 6) {
            Text(item.startTimeFormatted)
                .font(.system(.caption2, design: .monospaced))
                .foregroundStyle(palette.accent)
                .frame(width: 56, alignment: .leading)
            VStack(alignment: .leading, spacing: 2) {
                Text(item.title)
                    .font(.system(.caption2, design: .monospaced))
                    .foregroundStyle(.white)
                    .lineLimit(2)
                Text(item.durationFormatted)
                    .font(.system(.caption2, design: .monospaced))
                    .foregroundStyle(palette.muted)
            }
            Spacer(minLength: 0)
        }
    }

    private func fmtDur(_ s: Double) -> String {
        let m = Int(s) / 60, sec = Int(s) % 60
        return String(format: "%d:%02d", m, sec)
    }
}

// MARK: - B.5 Pulsierender Status-Dot (ersetzt statische Status-Zeile)

struct StatusIndicatorDot: View {
    let state: ConnectionState
    let userCount: Int
    let palette: CyTubeColors
    var isSimulated: Bool = false
    var onRetry: () -> Void = {}
    @State private var pulse = false

    private var color: Color {
        switch state {
        case .connected: return .green
        case .connecting: return .yellow
        case .disconnected: return palette.muted
        case .error: return .red
        }
    }

    private var label: String {
        switch state {
        case .connected: return "LIVE"
        case .connecting: return "RECONNECTING"
        case .disconnected: return "OFFLINE"
        case .error: return "IDLE"
        }
    }

    var body: some View {
        HStack(spacing: 6) {
            Circle()
                .fill(color)
                .frame(width: 10, height: 10)
                .scaleEffect(pulse && (state == .connected || state == .connecting) ? 1.3 : 1.0)
            Text("\(label)\(isSimulated ? " · SIM" : "") · 👤 \(userCount)")
                .font(.system(.caption, design: .monospaced))
                .foregroundStyle(palette.muted)
        }
        .onAppear {
            if state == .connected || state == .connecting {
                withAnimation(.easeInOut(duration: 0.9).repeatForever(autoreverses: true)) { pulse = true }
            }
        }
        .onTapGesture {
            if state == .disconnected || state == .error { onRetry() }
        }
    }
}