import SwiftUI

// Bug-Report (Telegrow GET) + VersionCheck. Portiert das Landingpage-Bug-Modal
// + UpdateManager.kt (ohne Self-Install — iOS blockt das, nur Hinweis).
// ponytail: GET fire-and-forget, version.json Vergleich, kein Auto-Install.

enum AppEndpoint {
    static let base = "https://servermitte.tailecbf0f.ts.net/mca"
    static let bugReport = "\(base)/bug-report"

    /// Quellen des Update-Feeds in Vorzugsreihenfolge.
    ///
    /// GitHub steht vorn, weil es aus jedem Netz erreichbar ist. Die servermitte-Adresse liegt
    /// hinter einem Tailscale-Funnel, der von aussen den TLS-Handshake abbricht — Geraete ohne
    /// Tailscale kamen dort nicht an Updates. Sie bleibt als Zweitquelle.
    static let versionFeeds = [
        "https://raw.githubusercontent.com/kburna243/mikes-420grindhouse-app/main/version.json",
        "\(base)/version.json"
    ]

    static let versionJSON = versionFeeds[0]
    static let appID = "mca-ios"
    static var version: String {
        (Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String) ?? "1.0.0"
    }
}

struct BugReportPanel: View {
    @Environment(\.dismiss) private var dismiss
    @State private var severity = "medium"
    @State private var description = ""
    @State private var contact = ""
    @State private var status = ""
    @State private var sending = false

    let palette: CyTubeColors

    var body: some View {
        NavigationStack {
            Form {
                Picker("Severity", selection: $severity) {
                    Text("Low").tag("low"); Text("Medium").tag("medium")
                    Text("High").tag("high"); Text("Critical").tag("critical")
                }
                VStack(alignment: .leading) {
                    Text("Description")
                    TextEditor(text: $description)
                        .frame(height: 100)
                        .overlay(RoundedRectangle(cornerRadius: 6).stroke(palette.muted.opacity(0.4)))
                }
                TextField("Contact (optional)", text: $contact)
                    .textInputAutocapitalization(.never)
                if !status.isEmpty {
                    Text(status).font(.caption).foregroundStyle(palette.accent)
                }
                Button { send() } label: {
                    Text(sending ? "Sende…" : "Send Bug Report").frame(maxWidth: .infinity)
                }
                .disabled(sending || description.trimmingCharacters(in: .whitespaces).count < 3)
            }
            .navigationTitle("Bug Report")
            .toolbar { ToolbarItem(placement: .topBarLeading) { Button("Close") { dismiss() } } }
        }
    }

    private func send() {
        let desc = description.trimmingCharacters(in: .whitespaces)
        guard desc.count >= 3 else { status = "Beschreibung zu kurz"; return }
        sending = true; status = ""
        var comps = URLComponents(string: AppEndpoint.bugReport)!
        comps.queryItems = [
            URLQueryItem(name: "app", value: AppEndpoint.appID),
            URLQueryItem(name: "version", value: AppEndpoint.version),
            URLQueryItem(name: "severity", value: severity),
            URLQueryItem(name: "description", value: desc),
            URLQueryItem(name: "contact", value: contact.trimmingCharacters(in: .whitespaces))
        ]
        guard let url = comps.url else { status = "Ungültige URL"; sending = false; return }
        var req = URLRequest(url: url)
        req.timeoutInterval = 10
        URLSession.shared.dataTask(with: req) { [self] _, _, _ in
            DispatchQueue.main.async {
                self.sending = false
                self.status = "✓ Gesendet — Danke!"
                self.description = ""; self.contact = ""
            }
        }.resume()
    }
}

@Observable
final class VersionChecker {
    var updateAvailable = false
    var latestVersion: String?
    var notes: String?

    /// Grund des letzten Fehlschlags, damit die UI nicht nur schweigt.
    var lastFailureReason: String?

    func check() async {
        var failures: [String] = []
        for feed in AppEndpoint.versionFeeds {
            guard let url = URL(string: feed) else { continue }
            let host = URL(string: feed)?.host ?? feed
            var req = URLRequest(url: url)
            req.timeoutInterval = 8
            do {
                let (data, resp) = try await URLSession.shared.data(for: req)
                guard let http = resp as? HTTPURLResponse, http.statusCode == 200 else {
                    failures.append("\(host): HTTP \((resp as? HTTPURLResponse)?.statusCode ?? -1)")
                    continue
                }
                if let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                    // ponytail: iOS vergleicht gegen iosVersion (separat von Android version),
                    // Fallback auf version falls iosVersion fehlt (alte version.json)
                    latestVersion = (root["iosVersion"] as? String) ?? (root["version"] as? String)
                    notes = root["notes"] as? String
                    updateAvailable = isNewer(latestVersion ?? AppEndpoint.version, AppEndpoint.version)
                    lastFailureReason = nil
                    return
                }
                failures.append("\(host): kein JSON")
            } catch {
                failures.append("\(host): \(type(of: error))")
            }
        }
        lastFailureReason = failures.isEmpty ? nil : failures.joined(separator: " · ")
    }

    // ponytail: simple semver major.minor.patch compare, no pre-release tags
    private func isNewer(_ remote: String, _ local: String) -> Bool {
        let r = remote.split(separator: ".").map { Int($0) ?? 0 }
        let l = local.split(separator: ".").map { Int($0) ?? 0 }
        for i in 0..<max(r.count, l.count) {
            let a = i < r.count ? r[i] : 0
            let b = i < l.count ? l[i] : 0
            if a > b { return true }
            if a < b { return false }
        }
        return false
    }
}