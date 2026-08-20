import SwiftUI
import Speech
import AVFAudio

// Voice-to-Chat — Port von VoiceToChat.kt. SFSpeechRecognizer + AVAudioEngine.
// Fallback: simulierte Phrase bei nicht verfügbar/Fehler (wie Android "Hacking
// into the mainframe..."). ponytail: 1-Min-Limit pro Task; Tap toggelt Start/Stop.

@Observable
final class VoiceController {
    var isListening = false
    private var recognizer: SFSpeechRecognizer?
    private let engine = AVAudioEngine()
    private var request: SFSpeechAudioBufferRecognitionRequest?
    private var task: SFSpeechRecognitionTask?
    private var onResult: ((String) -> Void)?

    private let simulated = [
        "Synthesizer frequency is fully synchronized! 👽",
        "This matrix falling code is gorgeous.",
        "Mike's CyTube App runs flawless native Swift!",
        "Who is watching the Akira anime showing tonight?",
        "CORS errors defeated by native iOS WebSockets! 🤘"
    ]

    func toggle(onResult: @escaping (String) -> Void) {
        self.onResult = onResult
        isListening ? stop() : requestAndStart()
    }

    private func requestAndStart() {
        SFSpeechRecognizer.requestAuthorization { speechAuth in
            AVAudioApplication.requestRecordPermission { micAuth in
                guard speechAuth == .authorized, micAuth else {
                    DispatchQueue.main.async { self.fallback("🎤 Mic/speech permission denied") }
                    return
                }
                DispatchQueue.main.async { self.begin() }
            }
        }
    }

    private func begin() {
        recognizer = SFSpeechRecognizer()
        guard let r = recognizer, r.isAvailable else {
            fallback(simulated.randomElement() ?? "🎤 Voice unavailable")
            return
        }
        let req = SFSpeechAudioBufferRecognitionRequest()
        req.shouldReportPartialResults = false
        request = req

        let fmt = engine.inputNode.outputFormat(forBus: 0)
        engine.inputNode.installTap(onBus: 0, bufferSize: 4096, format: fmt) { buf, _ in
            req.append(buf)
        }
        do {
            engine.prepare()
            try engine.start()
        } catch {
            fallback("🎤 Mic error: \(error.localizedDescription)")
            return
        }
        isListening = true

        task = r.recognitionTask(with: req) { [weak self] res, err in
            guard let self else { return }
            if let _ = err {
                // ponytail: any recognizer error → gentle simulated fallback like Android
                self.stop()
                self.fallback("Hacking into the mainframe...")
                return
            }
            if let res = res, res.isFinal {
                let text = res.bestTranscription.formattedString
                if !text.isEmpty { self.fallback(text) }
                self.stop()
            }
        }
    }

    private func fallback(_ s: String) {
        DispatchQueue.main.async {
            self.onResult?(s)
            self.isListening = false
        }
    }

    func stop() {
        engine.inputNode.removeTap(onBus: 0)
        engine.stop()
        request?.endAudio()
        task?.cancel()
        task = nil; request = nil
        isListening = false
    }
}

struct VoiceToChatButton: View {
    let palette: CyTubeColors
    let onResult: (String) -> Void
    @State private var controller = VoiceController()

    var body: some View {
        Button { controller.toggle(onResult: onResult) } label: {
            Image(systemName: controller.isListening ? "mic.fill" : "mic")
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(controller.isListening ? palette.accent : palette.primary)
                .frame(width: 46, height: 46)
                .background(
                    controller.isListening ? palette.primary.opacity(0.25) : Color.black.opacity(0.5)
                )
                .overlay(
                    Circle().stroke(controller.isListening ? palette.accent : palette.primary.opacity(0.6),
                                     lineWidth: controller.isListening ? 3 : 2)
                )
                .clipShape(Circle())
                .scaleEffect(controller.isListening ? 1.25 : 1.0)
                .animation(.easeInOut(duration: 0.8).repeatForever(autoreverses: true),
                           value: controller.isListening)
        }
        .buttonStyle(.plain)
    }
}