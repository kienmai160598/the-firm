import SwiftUI

struct PlaceholderView: View {
    let title: String

    var body: some View {
        VStack(spacing: 16) {
            Text(title)
                .font(.title2)
            Text("Coming in a later slice")
                .foregroundStyle(.secondary)
        }
    }
}
