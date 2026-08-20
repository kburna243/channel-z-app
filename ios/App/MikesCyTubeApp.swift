import SwiftUI

@main
struct MikesCyTubeApp: App {
    @State private var cyTube = CyTubeClient()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(cyTube)
                .preferredColorScheme(.dark)
        }
        .modelContainer(AppStore.container)
    }
}