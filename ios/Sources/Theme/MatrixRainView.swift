import SwiftUI

// MatrixRain — Port von MatrixRainCanvas.kt (Compose Canvas).
// SwiftUI GeometryReader + Canvas + Timer.publish (30 FPS). Spalten fallen,
// chars shiften (10%), Leader glüht in accent, Trail fadet in primary.
// ponytail: ~numCols * ~15 chars = bis zu ~600 Text-Draws/Frame; rasterize falls FPS fällt.
struct MatrixRainView: View {
    let palette: CyTubeColors
    var opacity: Double = 0.15

    @State private var columns: [RainCol] = []
    @State private var size: CGSize = .zero
    private let fontSize: CGFloat = 14
    private let spacing: CGFloat = 1.2
    private let pool: [Character] = Array(
        "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜﾝ*#$@&%+-="
    )

    var body: some View {
        GeometryReader { geo in
            Canvas { ctx, _ in
                for col in columns {
                    for (i, ch) in col.chars.enumerated() {
                        let y = col.y - CGFloat(i) * col.size
                        guard y > -col.size && y < geo.size.height + col.size else { continue }
                        let isLeader = i == col.chars.count - 1
                        let ratio = Double(i) / Double(max(1, col.chars.count))
                        let alpha: Double = isLeader ? 1.0 : min(1.0, max(0.06, ratio))
                        let color = isLeader ? palette.accent : palette.primary.opacity(alpha)
                        ctx.draw(Text(String(ch)).font(.system(size: col.size, design: .monospaced))
                                  .foregroundColor(color),
                                  at: CGPoint(x: col.x, y: y))
                    }
                }
            }
            .onAppear { initCols(geo.size) }
            .onChange(of: geo.size) { _, new in initCols(new) }
        }
        .onReceive(Timer.publish(every: 0.033, on: .main, in: .common).autoconnect()) { _ in advance() }
    }

    private func initCols(_ sz: CGSize) {
        guard sz.width > 0 else { return }
        let num = max(1, Int(sz.width / (fontSize * spacing)))
        columns = (0..<num).map { i in
            RainCol(
                x: CGFloat(i) * fontSize * spacing,
                y: Double.random(in: -sz.height ... 0),
                speed: Double.random(in: 4 ... 12),
                chars: (0..<Int.random(in: 8...22)).map { _ in pool.randomElement()! },
                size: fontSize
            )
        }
        size = sz
    }

    private func advance() {
        guard !columns.isEmpty else { return }
        for i in columns.indices {
            columns[i].y += columns[i].speed
            let colH = CGFloat(columns[i].chars.count) * columns[i].size
            if columns[i].y - colH > size.height {
                columns[i].y = Double.random(in: -150 ... -50)
            }
            if Double.random(in: 0..<1) < 0.1 {
                let idx = Int.random(in: 0..<columns[i].chars.count)
                columns[i].chars[idx] = pool.randomElement()!
            }
        }
    }
}

private struct RainCol {
    var x: CGFloat
    var y: CGFloat
    var speed: CGFloat
    var chars: [Character]
    let size: CGFloat
}