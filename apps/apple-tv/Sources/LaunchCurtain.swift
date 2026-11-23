import SwiftUI

@MainActor
final class LaunchCurtain: ObservableObject {
    private static let sidebarSettleTime: Duration = .milliseconds(450)
    private static let longestWait: Duration = .seconds(3)

    @Published private(set) var isLifted = false
    private var focusLift: Task<Void, Never>?
    private var fallbackLift: Task<Void, Never>?

    func liftOnceContentHasFocus() {
        guard !isLifted, focusLift == nil else { return }
        focusLift = lift(after: Self.sidebarSettleTime)
    }

    func liftIfContentNeverTakesFocus() {
        guard !isLifted, fallbackLift == nil else { return }
        fallbackLift = lift(after: Self.longestWait)
    }

    func liftNow() {
        isLifted = true
    }

    private func lift(after delay: Duration) -> Task<Void, Never> {
        Task { @MainActor [weak self] in
            try? await Task.sleep(for: delay)
            self?.isLifted = true
        }
    }
}

struct LaunchCurtainCover: View {
    @ObservedObject var curtain: LaunchCurtain

    var body: some View {
        ZStack {
            if !curtain.isLifted {
                Color.black
                    .ignoresSafeArea()
                    .allowsHitTesting(false)
                    .accessibilityHidden(true)
                    .transition(.opacity)
            }
        }
        .animation(.easeOut(duration: 0.35), value: curtain.isLifted)
    }
}
