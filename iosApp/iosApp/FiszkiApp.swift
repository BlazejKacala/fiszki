import SwiftUI
import FiszkiShared

@main
struct FiszkiApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeScreen()
                .ignoresSafeArea()
                .preferredColorScheme(.light)
        }
    }
}

struct ComposeScreen: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
