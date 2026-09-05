import SwiftUI

@main
struct KPElectricalToolsApp: App {
    @AppStorage("kp_beta_access_count") private var accessCount = 0
    private let maxAccesses = 30

    var body: some Scene {
        WindowGroup {
            if accessCount >= maxAccesses {
                BetaLockedView()
            } else {
                RootView(remaining: maxAccesses - accessCount - 1)
                    .onAppear {
                        if accessCount < maxAccesses { accessCount += 1 }
                    }
            }
        }
    }
}

private enum KPColor {
    static let navy = Color(red: 5/255, green: 24/255, blue: 43/255)
    static let panel = Color(red: 8/255, green: 37/255, blue: 63/255)
    static let blue = Color(red: 18/255, green: 61/255, blue: 116/255)
    static let cyan = Color(red: 0/255, green: 193/255, blue: 224/255)
    static let orange = Color(red: 242/255, green: 140/255, blue: 0/255)
}

struct RootView: View {
    let remaining: Int
    @State private var showSplash = true

    var body: some View {
        Group {
            if showSplash {
                SplashView(remaining: remaining)
            } else {
                HomeView(remaining: remaining)
            }
        }
        .preferredColorScheme(.dark)
        .task {
            try? await Task.sleep(for: .seconds(1.8))
            withAnimation(.easeInOut(duration: 0.35)) { showSplash = false }
        }
    }
}

struct SplashView: View {
    let remaining: Int
    var body: some View {
        ZStack {
            LinearGradient(colors: [KPColor.navy, KPColor.panel, .black], startPoint: .topLeading, endPoint: .bottomTrailing)
                .ignoresSafeArea()
            VStack(spacing: 22) {
                KPLogoMark(size: 118)
                VStack(spacing: 5) {
                    Text("KP Electrical Tools").font(.system(size: 31, weight: .bold, design: .rounded))
                    Text("by NORTECH").font(.headline).foregroundStyle(KPColor.cyan)
                    Text("Precisão elétrica para decisões melhores.").font(.subheadline).foregroundStyle(.white.opacity(0.78))
                }
                Text("VERSÃO BETA v1.0 iOS")
                    .font(.caption.bold()).padding(.horizontal, 18).padding(.vertical, 9)
                    .background(KPColor.orange, in: Capsule())
                Text(remaining == 0 ? "Último acesso liberado" : "Acessos restantes: \(remaining) de 30")
                    .font(.footnote.weight(.semibold)).foregroundStyle(.white.opacity(0.8))
            }
            .multilineTextAlignment(.center).padding(30)
        }
    }
}

struct HomeView: View {
    let remaining: Int
    private let modules: [(String, String, String)] = [
        ("bolt.circle.fill", "Banco de Capacitores", "Correção de FP e dimensionamento"),
        ("shippingbox.fill", "Transformadores", "Corrente, carga e capacidade"),
        ("cable.connector", "Cabos", "Seção, corrente e proteção"),
        ("arrow.down.right.circle.fill", "Queda de Tensão", "Cálculo em V e %"),
        ("chart.bar.fill", "Demanda", "kW, kVA e demanda"),
        ("gearshape.2.fill", "Geradores", "Carga e reserva"),
        ("doc.text.magnifyingglass", "Análise de Faturas", "PDF, imagem e relatório"),
        ("waveform.path.ecg", "Análise de Energia", "Medições, planilhas e gráficos")
    ]

    var body: some View {
        NavigationStack {
            ZStack {
                LinearGradient(colors: [KPColor.navy, KPColor.panel, KPColor.navy], startPoint: .top, endPoint: .bottom)
                    .ignoresSafeArea()
                ScrollView {
                    VStack(spacing: 18) {
                        HStack(spacing: 14) {
                            KPLogoMark(size: 66)
                            VStack(alignment: .leading, spacing: 2) {
                                Text("KP Electrical Tools").font(.title2.bold())
                                Text("by NORTECH").font(.subheadline.weight(.semibold)).foregroundStyle(KPColor.cyan)
                                Text("Precisão elétrica para decisões melhores.").font(.caption).foregroundStyle(.white.opacity(0.65))
                            }
                            Spacer()
                        }
                        .padding(.top, 8)

                        HStack {
                            Label("BETA iOS", systemImage: "testtube.2")
                            Spacer()
                            Text(remaining == 0 ? "Último acesso" : "\(remaining) acessos restantes")
                        }
                        .font(.caption.bold()).padding(12)
                        .background(KPColor.orange.opacity(0.95), in: RoundedRectangle(cornerRadius: 14))

                        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                            ForEach(Array(modules.enumerated()), id: \.offset) { index, item in
                                NavigationLink {
                                    destination(index)
                                } label: {
                                    ModuleCard(symbol: item.0, title: item.1, subtitle: item.2)
                                }
                                .buttonStyle(.plain)
                            }
                        }

                        VStack(spacing: 8) {
                            Text("NORTECH SERVIÇOS E COMERCIO LTDA").font(.caption.bold()).foregroundStyle(.white.opacity(0.75))
                            Text("Desenvolvido por Ezequiel Paixão").font(.caption2).foregroundStyle(.white.opacity(0.5))
                        }.padding(.vertical, 12)
                    }
                    .padding(.horizontal, 16)
                }
            }
            .toolbar(.hidden, for: .navigationBar)
        }
    }

    @ViewBuilder private func destination(_ index: Int) -> some View {
        switch index {
        case 0: CapacitorView()
        case 6: ImportView(title: "Análise de Faturas", description: "Importação de PDF e imagens para leitura e conferência dos dados da fatura.", formats: "PDF • JPG • PNG")
        case 7: ImportView(title: "Análise de Energia", description: "Importação de medições e planilhas para análise elétrica e geração de relatório.", formats: "CSV • XLSX • PDF • TXT")
        default: GenericModuleView(title: modules[index].1, symbol: modules[index].0)
        }
    }
}

struct ModuleCard: View {
    let symbol: String; let title: String; let subtitle: String
    var body: some View {
        VStack(alignment: .leading, spacing: 9) {
            Image(systemName: symbol).font(.system(size: 27, weight: .semibold)).foregroundStyle(KPColor.cyan)
            Text(title).font(.subheadline.bold()).foregroundStyle(.white).multilineTextAlignment(.leading)
            Text(subtitle).font(.caption2).foregroundStyle(.white.opacity(0.58)).lineLimit(2)
            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity, minHeight: 126, alignment: .topLeading)
        .padding(14)
        .background(
            LinearGradient(colors: [Color.white.opacity(0.10), KPColor.blue.opacity(0.22)], startPoint: .topLeading, endPoint: .bottomTrailing),
            in: RoundedRectangle(cornerRadius: 18)
        )
        .overlay(RoundedRectangle(cornerRadius: 18).stroke(KPColor.cyan.opacity(0.18), lineWidth: 1))
    }
}

struct CapacitorView: View {
    @State private var power = "500"
    @State private var fpCurrent = "0,80"
    @State private var fpTarget = "0,98"
    @State private var voltage = "380"
    @State private var result: Double?

    var body: some View {
        Form {
            Section("Dados de entrada") {
                field("Potência ativa (kW)", text: $power)
                field("FP atual", text: $fpCurrent)
                field("FP desejado", text: $fpTarget)
                field("Tensão trifásica (V)", text: $voltage)
            }
            Section {
                Button("CALCULAR BANCO") { calculate() }
                    .frame(maxWidth: .infinity).font(.headline).foregroundStyle(.white)
            }
            if let result {
                Section("Resultado") {
                    HStack { Text("Banco calculado"); Spacer(); Text(String(format: "%.2f kvar", result)).bold().foregroundStyle(KPColor.cyan) }
                    HStack { Text("Banco comercial"); Spacer(); Text(String(format: "%.0f kvar", ceil(result / 5) * 5)).bold().foregroundStyle(KPColor.orange) }
                    Text("Qc = P × [tan(arccos FP₁) − tan(arccos FP₂)]").font(.caption).foregroundStyle(.secondary)
                }
            }
        }
        .scrollContentBackground(.hidden)
        .background(KPColor.navy)
        .navigationTitle("Banco de Capacitores")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func field(_ label: String, text: Binding<String>) -> some View {
        TextField(label, text: text).keyboardType(.decimalPad)
    }
    private func number(_ s: String) -> Double? { Double(s.replacingOccurrences(of: ",", with: ".")) }
    private func calculate() {
        guard let p = number(power), let a = number(fpCurrent), let b = number(fpTarget), a > 0, a < b, b <= 1 else { result = nil; return }
        result = p * (tan(acos(a)) - tan(acos(b)))
    }
}

struct ImportView: View {
    let title: String; let description: String; let formats: String
    @State private var importer = false
    @State private var selected = "Nenhum arquivo selecionado"
    var body: some View {
        ZStack {
            KPColor.navy.ignoresSafeArea()
            VStack(spacing: 22) {
                Image(systemName: "square.and.arrow.down.fill").font(.system(size: 46)).foregroundStyle(KPColor.cyan)
                Text(title).font(.title2.bold())
                Text(description).multilineTextAlignment(.center).foregroundStyle(.white.opacity(0.7))
                Text(formats).font(.caption.bold()).foregroundStyle(KPColor.orange)
                Button("SELECIONAR ARQUIVO") { importer = true }
                    .buttonStyle(.borderedProminent).tint(KPColor.orange)
                Text(selected).font(.caption).foregroundStyle(.white.opacity(0.55))
                Spacer()
            }.padding(26)
        }
        .fileImporter(isPresented: $importer, allowedContentTypes: [.data], allowsMultipleSelection: false) { result in
            if case let .success(urls) = result, let url = urls.first { selected = url.lastPathComponent }
        }
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct GenericModuleView: View {
    let title: String; let symbol: String
    var body: some View {
        ZStack {
            KPColor.navy.ignoresSafeArea()
            VStack(spacing: 18) {
                Image(systemName: symbol).font(.system(size: 52)).foregroundStyle(KPColor.cyan)
                Text(title).font(.title2.bold())
                Text("Módulo iOS em estruturação para manter os mesmos cálculos e relatórios da versão Android.")
                    .multilineTextAlignment(.center).foregroundStyle(.white.opacity(0.65))
            }.padding(30)
        }
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct BetaLockedView: View {
    var body: some View {
        ZStack {
            KPColor.navy.ignoresSafeArea()
            VStack(spacing: 20) {
                KPLogoMark(size: 92)
                Text("Versão BETA encerrada").font(.title.bold())
                Text("Esta versão atingiu o limite de 30 acessos. Entre em contato com a NORTECH para liberação.")
                    .multilineTextAlignment(.center).foregroundStyle(.white.opacity(0.7))
                Link("FALAR COM A NORTECH", destination: URL(string: "https://wa.me/5591991815138")!)
                    .font(.headline).padding(.horizontal, 22).padding(.vertical, 12)
                    .background(KPColor.orange, in: Capsule()).foregroundStyle(.white)
            }.padding(30)
        }
        .preferredColorScheme(.dark)
    }
}

struct KPLogoMark: View {
    let size: CGFloat
    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: size * 0.22).fill(Color.white.opacity(0.05))
            Circle().stroke(KPColor.cyan, lineWidth: size * 0.07).padding(size * 0.12)
            Circle().trim(from: 0.69, to: 0.94).stroke(KPColor.orange, style: StrokeStyle(lineWidth: size * 0.08, lineCap: .round)).rotationEffect(.degrees(-20)).padding(size * 0.12)
            Text("KP").font(.system(size: size * 0.34, weight: .black, design: .rounded)).foregroundStyle(.white)
            Image(systemName: "bolt.fill").font(.system(size: size * 0.16, weight: .bold)).foregroundStyle(KPColor.orange).offset(x: size * 0.24, y: -size * 0.22)
        }
        .frame(width: size, height: size)
    }
}
