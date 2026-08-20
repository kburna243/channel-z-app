import SwiftUI

// Animierter Cyber-Splash (analog CyberSplashScreen.kt). Phase 0: einfach,
// wird in Phase 4 mit MatrixRain + echtem Branding verfeinert.
struct SplashView: View {
    @State private var pulse = false
    var palette: CyTubeColors = .matrix

    var body: some View {
        ZStack {
            palette.background.ignoresSafeArea()
            VStack(spacing: 16) {
                Circle()
                    .stroke(palette.primary.opacity(0.6), lineWidth: 2)
                    .frame(width: 64, height: 64)
                    .scaleEffect(pulse ? 1.25 : 0.9)
                    .opacity(pulse ? 1.0 : 0.4)
                    .animation(.easeInOut(duration: 1.2).repeatForever(autoreverses: true),
                               value: pulse)
                Text("MIKE'S CYTUBE")
                    .font(.system(.title2, design: .monospaced).weight(.heavy))
                    .foregroundStyle(palette.primary)
                Text("CYBERPUNK LIVE-MEDIA COMPANION")
                    .font(.system(size: 10, weight: .regular, design: .monospaced))
                    .foregroundStyle(palette.muted)
            }
        }
        .onAppear { pulse = true }
    }
}

#Preview {
    SplashView()
        .preferredColorScheme(.dark)
}