import SwiftUI

// 3 Cyberpunk-Themes (analog Android MainActivity Theme-Switch + Landingpage-Farben).
// Matrix = echte Werte aus der mca-Landingpage; Vaporwave/Cyberpunk = plausible
// Schemes, werden in Phase 4 gegen die echten Android-Werte verfeinert.
enum CyTubeTheme: String, CaseIterable, Identifiable {
    case matrix = "Matrix"
    case vaporwave = "Vaporwave"
    case cyberpunk = "Cyber Blue"
    var id: String { rawValue }
}

struct CyTubeColors {
    let primary: Color
    let accent: Color
    let background: Color
    let surface: Color
    let text: Color
    let neonCyan: Color
    let neonPink: Color
    let muted: Color

    // MATRIX — High Density Green (echte Android-Werte)
    static let matrix = CyTubeColors(
        primary: Color(hex: 0x00FF41),
        accent: Color(hex: 0xD1FF00),
        background: Color(hex: 0x050A05),
        surface: Color(hex: 0x001100),
        text: Color(hex: 0xC8FFD0),
        neonCyan: Color(hex: 0x00F0FF),
        neonPink: Color(hex: 0xFF007F),
        muted: Color(hex: 0x5A7A5A)
    )

    // VAPORWAVE — Neon Vaporwave (echte Android-Werte)
    static let vaporwave = CyTubeColors(
        primary: Color(hex: 0xFF007F),
        accent: Color(hex: 0x00FFFF),
        background: Color(hex: 0x14001F),
        surface: Color(hex: 0x2E0042),
        text: Color(hex: 0xFFE0F0),
        neonCyan: Color(hex: 0x00FFFF),
        neonPink: Color(hex: 0xFF007F),
        muted: Color(hex: 0x9A6BB0)
    )

    // CYBER_BLUE — Sleek Cyberpunk (echte Android-Werte)
    static let cyberpunk = CyTubeColors(
        primary: Color(hex: 0x00F0FF),
        accent: Color(hex: 0xFF0055),
        background: Color(hex: 0x050510),
        surface: Color(hex: 0x0E0E25),
        text: Color(hex: 0xC8FFF0),
        neonCyan: Color(hex: 0x00F0FF),
        neonPink: Color(hex: 0xFF0055),
        muted: Color(hex: 0x5A7A8A)
    )

    static func palette(for theme: CyTubeTheme) -> CyTubeColors {
        switch theme {
        case .matrix: return .matrix
        case .vaporwave: return .vaporwave
        case .cyberpunk: return .cyberpunk
        }
    }
}

extension Color {
    init(hex: UInt32, alpha: Double = 1.0) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255.0,
            green: Double((hex >> 8) & 0xFF) / 255.0,
            blue: Double(hex & 0xFF) / 255.0,
            opacity: alpha
        )
    }

    // ponytail: parse "#RRGGBB" (stored in ChatSettings.messageColorHex)
    init(hexString: String) {
        let h = hexString.trimmingCharacters(in: CharacterSet(charactersIn: "#"))
        var v: UInt64 = 0
        Scanner(string: h).scanHexInt64(&v)
        self.init(hex: UInt32(truncatingIfNeeded: v))
    }
}