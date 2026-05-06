import SwiftUI

struct ContentView: View {
    @StateObject private var vm = PaymentViewModel()

    var body: some View {
        VStack(spacing: 32) {
            VStack(spacing: 8) {
                if vm.state.isLoading {
                    ProgressView()
                        .scaleEffect(1.5)
                }
                Text(vm.state.label)
                    .font(.title2.weight(.semibold))
                    .multilineTextAlignment(.center)
                    .foregroundStyle(labelColor)
            }
            .frame(maxHeight: 80)

            VStack(spacing: 16) {
                Button("Initialize Terminal") {
                    vm.initialize()
                }
                .buttonStyle(.borderedProminent)
                .disabled(vm.state.isLoading)

                Button("Charge $100") {
                    vm.startTransaction(amount: 100.0)
                }
                .buttonStyle(.borderedProminent)
                .tint(.green)
                .disabled(vm.state.isLoading)
            }
        }
        .padding(32)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var labelColor: Color {
        switch vm.state {
        case .success:                       return .green
        case .failedNotInitialized,
             .failedLocalSecurity,
             .failedBackend:                 return .red
        default:                             return .primary
        }
    }
}

#Preview {
    ContentView()
}
