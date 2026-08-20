import SwiftUI
import SwiftData

// Favoriten- + Settings-Sheets (analog FavoritesPanel.kt / ChatSettingsPanel.kt).

struct FavoritesPanel: View {
    @Environment(\.modelContext) private var ctx
    @Query(sort: \FavoriteChannel.timestamp, order: .reverse) private var favorites: [FavoriteChannel]
    let onConnect: (String, String) -> Void

    @State private var adding = false
    @State private var newServer = "https://cytu.be"
    @State private var newChannel = ""
    @State private var newName = ""

    var body: some View {
        NavigationStack {
            List {
                if favorites.isEmpty {
                    Text("No favorite channels yet.").foregroundStyle(.secondary)
                }
                ForEach(favorites) { fav in
                    Button { onConnect(fav.serverUrl, fav.channelName) } label: {
                        VStack(alignment: .leading) {
                            Text(fav.displayName).font(.headline)
                            Text("\(fav.channelName) @ \(fav.serverUrl)")
                                .font(.caption).foregroundStyle(.secondary)
                        }
                    }.buttonStyle(.plain)
                }
                .onDelete(perform: delete)
            }
            .navigationTitle("Favorites")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { adding = true } label: { Image(systemName: "plus") }
                }
            }
            .sheet(isPresented: $adding) {
                addSheet
            }
        }
    }

    private var addSheet: some View {
        NavigationStack {
            Form {
                TextField("Display name", text: $newName)
                TextField("Server URL", text: $newServer)
                    .textInputAutocapitalization(.never).autocorrectionDisabled()
                TextField("Channel", text: $newChannel)
                    .textInputAutocapitalization(.never).autocorrectionDisabled()
            }
            .navigationTitle("Add Favorite")
            .toolbar {
                ToolbarItem(placement: .topBarLeading) { Button("Cancel") { adding = false } }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Save") {
                        let name = newName.trimmingCharacters(in: .whitespaces)
                        let ch = newChannel.trimmingCharacters(in: .whitespaces)
                        guard !ch.isEmpty else { return }
                        let id = (favorites.map(\.id).max() ?? 0) + 1
                        ctx.insert(FavoriteChannel(id: id, serverUrl: newServer,
                                                    channelName: ch,
                                                    displayName: name.isEmpty ? ch : name))
                        try? ctx.save()
                        newName = ""; newChannel = ""; adding = false
                    }
                }
            }
        }
    }

    private func delete(at offsets: IndexSet) {
        for i in offsets { ctx.delete(favorites[i]) }
        try? ctx.save()
    }
}

struct ChatSettingsPanel: View {
    @Environment(\.modelContext) private var ctx
    @State private var settings: ChatSettings?

    var body: some View {
        NavigationStack {
            Form {
                Section("Chat list") {
                    Toggle("Show timestamps", isOn: bind(\.showTimestamps, default: true))
                    HStack {
                        Text("Message color")
                        Spacer()
                        ColorPicker("", selection: bindColor)
                            .labelsHidden()
                    }
                }
                // B.7: SubtitleChatOverlay-Settings.
                // Der An/Aus-Schalter bleibt auf der ersten Ebene, die Feineinstellungen liegen
                // eine Ebene tiefer — sonst schiebt die Kosmetik alles andere nach unten.
                Section("Subtitle overlay") {
                    Toggle("Show over player", isOn: bind(\.chatEnabled, default: true))
                    NavigationLink {
                        chatAppearanceForm
                    } label: {
                        LabeledContent("Chat appearance") {
                            Text("\(settings?.chatMaxLines ?? 3) lines · \(settings?.chatFontSize ?? 15) pt")
                                .foregroundStyle(.secondary)
                        }
                    }
                }
                // B.8: YouTube-Untertitel
                Section("Subtitles") {
                    Toggle("YouTube captions (CC)", isOn: bind(\.subtitlesEnabled, default: true))
                }
            }
            .navigationTitle("Chat Settings")
        }
        .onAppear { settings = ensureChatSettings(ctx) }
    }

    /// Unterseite: alles, was nur das Aussehen des Chat-Overlays betrifft.
    private var chatAppearanceForm: some View {
        Form {
            Section("Lines") {
                Stepper("Max lines: \(settings?.chatMaxLines ?? 3)",
                        value: bindInt(\.chatMaxLines, default: 3, lo: 1, hi: 3), in: 1...3)
            }
            Section("Appearance") {
                HStack {
                    Text("Opacity")
                    Spacer()
                    Text("\(Int((settings?.chatOpacity ?? 0.6) * 100))%")
                        .foregroundStyle(.secondary)
                }
                Slider(value: bindDouble(\.chatOpacity, default: 0.6, lo: 0.2, hi: 0.9), in: 0.2...0.9)
                Stepper("Font size: \(settings?.chatFontSize ?? 15) pt",
                        value: bindInt(\.chatFontSize, default: 15, lo: 12, hi: 22), in: 12...22)
            }
        }
        .navigationTitle("Chat appearance")
    }

    // ponytail: bindings persist immediately on change against the singleton
    private func bind<T>(_ keyPath: ReferenceWritableKeyPath<ChatSettings, T>, default def: T) -> Binding<T> {
        Binding(
            get: { settings?[keyPath: keyPath] ?? def },
            set: { v in
                guard let s = settings else { return }
                s[keyPath: keyPath] = v; try? ctx.save()
            }
        )
    }

    // B.7: clamped Int/Double-Bindings für Slider/Stepper (lo/hi statt min/max — shadow stdlib)
    private func bindInt(_ keyPath: ReferenceWritableKeyPath<ChatSettings, Int>,
                         default def: Int, lo: Int, hi: Int) -> Binding<Int> {
        Binding(
            get: { settings?[keyPath: keyPath] ?? def },
            set: { v in
                guard let s = settings else { return }
                s[keyPath: keyPath] = min(max(v, lo), hi); try? ctx.save()
            }
        )
    }

    private func bindDouble(_ keyPath: ReferenceWritableKeyPath<ChatSettings, Double>,
                            default def: Double, lo: Double, hi: Double) -> Binding<Double> {
        Binding(
            get: { settings?[keyPath: keyPath] ?? def },
            set: { v in
                guard let s = settings else { return }
                s[keyPath: keyPath] = min(max(v, lo), hi); try? ctx.save()
            }
        )
    }

    private var bindColor: Binding<Color> {
        Binding(
            get: { Color(hexString: settings?.messageColorHex ?? "#FFFFFF") },
            set: { v in
                guard let s = settings else { return }
                s.messageColorHex = v.toHex(); try? ctx.save()
            }
        )
    }
}

extension Color {
    func toHex() -> String {
        #if canImport(UIKit)
        let uic = UIColor(self)
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        uic.getRed(&r, green: &g, blue: &b, alpha: &a)
        return String(format: "#%02X%02X%02X", Int(r*255), Int(g*255), Int(b*255))
        #else
        return "#FFFFFF"
        #endif
    }
}