package br.com.nortech.capacitores

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.*

private const val MAX_BETA_ACCESSES = 30
private const val PREFS_NAME = "kp_electrical_tools_beta"
private const val PREF_ACCESS_COUNT = "access_count"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(colorScheme = NortechScheme, typography = NortechTypography) {
                Surface(Modifier.fillMaxSize(), color = AppBackground) {
                    KpElectricalToolsApp(MAX_BETA_ACCESSES)
                }
            }
        }
    }
}

enum class Screen { HOME, CAPACITOR, TRANSFORMER, CABLE, DROP, DEMAND, GENERATOR, INVOICE, ENERGY }

data class CalcResult(
    val qc: Double,
    val commercial: Double,
    val current: Double,
    val breaker: Int,
    val ct: Int,
    val fixed: Double?,
    val tuning: Double?,
    val stages: List<Double>
)

@Composable
fun KpElectricalToolsApp(remainingAccesses: Int) {
    var showSplash by remember { mutableStateOf(true) }
    if (showSplash) {
        LaunchedEffect(Unit) { delay(2200); showSplash = false }
        BetaSplashScreen(remainingAccesses)
        return
    }

    var screen by remember { mutableStateOf(Screen.HOME) }
    when (screen) {
        Screen.HOME -> HomeScreen(remainingAccesses) { screen = it }
        Screen.CAPACITOR -> CapacitorScreen { screen = Screen.HOME }
        Screen.TRANSFORMER -> TransformerScreenV9 { screen = Screen.HOME }
        Screen.CABLE -> CableScreen { screen = Screen.HOME }
        Screen.DROP -> VoltageDropScreen { screen = Screen.HOME }
        Screen.DEMAND -> DemandScreen { screen = Screen.HOME }
        Screen.GENERATOR -> GeneratorScreen { screen = Screen.HOME }
        Screen.INVOICE -> InvoiceAnalysisScreen { screen = Screen.HOME }
        Screen.ENERGY -> EnergyAnalysisScreen { screen = Screen.HOME }
    }
}

@Composable
private fun KpMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF0B7FCC), Color(0xFF0A2743)))),
        contentAlignment = Alignment.Center
    ) {
        Text("KP", color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
        Text(
            "⚡",
            color = NortechOrange,
            modifier = Modifier.align(Alignment.BottomEnd).padding(7.dp),
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun BetaSplashScreen(remainingAccesses: Int) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF061A2D), Color(0xFF0A3355), Color(0xFF04131F))))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            KpMark(Modifier.size(128.dp))
            Spacer(Modifier.height(24.dp))
            Text(APP_NAME, style = MaterialTheme.typography.headlineLarge, color = Color.White, textAlign = TextAlign.Center)
            Text(APP_SUBTITLE, style = MaterialTheme.typography.titleMedium, color = NortechCyan)
            Spacer(Modifier.height(12.dp))
            Text(APP_SLOGAN, style = MaterialTheme.typography.bodyLarge, color = AppTextMuted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(28.dp))
            Surface(color = NortechOrange, shape = RoundedCornerShape(50)) {
                Text("VERSÃO v1.2 • SEM LIMITAÇÃO", modifier = Modifier.padding(horizontal = 22.dp, vertical = 9.dp), color = Color.White, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Uso sem limitação de acessos",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(34.dp))
            BrandHeader(compact = true)
        }
    }
}

@Composable
private fun BetaLockedScreen() {
    val context = LocalContext.current
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF061A2D), Color(0xFF0B2D4A), Color(0xFF04131F))))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            KpMark(Modifier.size(104.dp))
            Spacer(Modifier.height(20.dp))
            Text(APP_NAME, style = MaterialTheme.typography.headlineMedium, color = Color.White, textAlign = TextAlign.Center)
            Text("Versão BETA encerrada", style = MaterialTheme.typography.titleLarge, color = NortechOrange)
            Spacer(Modifier.height(12.dp))
            Text(
                "Este período de testes atingiu o limite de $MAX_BETA_ACCESSES acessos. Entre em contato com a NORTECH para liberação.",
                style = MaterialTheme.typography.bodyLarge,
                color = AppTextMuted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(WHATSAPP_URL))) },
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) { Text("FALAR COM A NORTECH") }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { (context as? ComponentActivity)?.finish() },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("FECHAR APLICATIVO") }
        }
    }
}

@Composable
private fun HomeScreen(remainingAccesses: Int, open: (Screen) -> Unit) {
    val context = LocalContext.current
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF061A2D), Color(0xFF0A2945), Color(0xFF061A2D))))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KpMark(Modifier.size(58.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Olá, Engenheiro!", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text("Ferramentas para um futuro mais eficiente.", style = MaterialTheme.typography.bodySmall, color = AppTextMuted)
                }
                Surface(color = NortechOrange.copy(alpha = 0.16f), shape = RoundedCornerShape(12.dp)) {
                    Text("v1.2", modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = NortechOrange, style = MaterialTheme.typography.labelMedium)
                }
            }

            Text(APP_NAME, style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text(APP_SLOGAN, style = MaterialTheme.typography.bodyMedium, color = AppTextMuted)

            Surface(color = Color(0xFF0B3557), shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Licença", color = NortechCyan, style = MaterialTheme.typography.titleSmall)
                    Text("Sem limitação", color = Color.White, style = MaterialTheme.typography.titleSmall)
                }
            }

            ModuleRow(
                left = ModuleSpec("⚡", "Banco de\nCapacitores", Color(0xFF17A8E5), Screen.CAPACITOR),
                right = ModuleSpec("▣", "Transformadores", Color(0xFF5A91D9), Screen.TRANSFORMER),
                open = open
            )
            ModuleRow(
                left = ModuleSpec("◉", "Cabos", Color(0xFF2AA9EB), Screen.CABLE),
                right = ModuleSpec("↯", "Queda de Tensão", NortechOrange, Screen.DROP),
                open = open
            )
            ModuleRow(
                left = ModuleSpec("▥", "Demanda", Color(0xFF9A75E8), Screen.DEMAND),
                right = ModuleSpec("⚙", "Geradores", Color(0xFF35D47B), Screen.GENERATOR),
                open = open
            )
            ModuleRow(
                left = ModuleSpec("▤", "Análise de Faturas", Color(0xFF4A8BE9), Screen.INVOICE),
                right = ModuleSpec("▥", "Análise de Energia", Color(0xFF3AD58D), Screen.ENERGY),
                open = open
            )

            Surface(color = Color(0xFF0A2238), shape = RoundedCornerShape(18.dp), tonalElevation = 4.dp) {
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    BottomItem("⌂", "Início", true)
                    BottomItem("↻", "Histórico", false)
                    BottomItem("?", "Ajuda", false)
                    BottomItem("⚙", "Config.", false)
                }
            }

            BrandHeader(compact = true)
            Text("Desenvolvido por Ezequiel Paixão", color = AppTextMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

            OutlinedButton(
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(WHATSAPP_URL))) },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("WhatsApp NORTECH • $WHATSAPP") }
            OutlinedButton(
                onClick = { (context as? ComponentActivity)?.finish() },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("SAIR DO APLICATIVO") }
        }
    }
}

private data class ModuleSpec(val icon: String, val title: String, val accent: Color, val screen: Screen)

@Composable
private fun ModuleRow(left: ModuleSpec, right: ModuleSpec, open: (Screen) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ModuleCard(left, Modifier.weight(1f)) { open(left.screen) }
        ModuleCard(right, Modifier.weight(1f)) { open(right.screen) }
    }
}

@Composable
private fun ModuleCard(spec: ModuleSpec, modifier: Modifier = Modifier, action: () -> Unit) {
    ElevatedCard(
        modifier = modifier.height(132.dp).clickable(onClick = action),
        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF0C3555)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(13.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(spec.icon, color = spec.accent, style = MaterialTheme.typography.headlineMedium)
            Text(spec.title, color = Color.White, style = MaterialTheme.typography.titleSmall)
            Text("Abrir  ›", color = AppTextMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun BottomItem(icon: String, label: String, selected: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, color = if (selected) NortechOrange else AppTextMuted, style = MaterialTheme.typography.titleMedium)
        Text(label, color = if (selected) NortechOrange else AppTextMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ModuleBackButton(onBack: () -> Unit) {
    OutlinedButton(onClick = onBack, modifier = Modifier.height(48.dp)) { Text("←  VOLTAR") }
}

@Composable
private fun CapacitorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var client by remember { mutableStateOf("") }
    var installation by remember { mutableStateOf("") }
    var power by remember { mutableStateOf("500") }
    var fp1 by remember { mutableStateOf("0,80") }
    var fp2 by remember { mutableStateOf("0,98") }
    var voltage by remember { mutableStateOf("380") }
    var channels by remember { mutableStateOf(12) }
    var controller by remember { mutableStateOf("Trifásico") }
    var detuned by remember { mutableStateOf(false) }
    var reactor by remember { mutableStateOf("7") }
    var transformer by remember { mutableStateOf("750") }
    var fixedPct by remember { mutableStateOf("2") }
    var result by remember { mutableStateOf<CalcResult?>(null) }
    var error by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF061A2D), Color(0xFF0A2945))))) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModuleBackButton(onBack)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = NortechCyan.copy(alpha = 0.14f), shape = RoundedCornerShape(14.dp)) {
                    Text("⚡", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.headlineMedium)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Banco de Capacitores", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text("Dimensionamento e correção do fator de potência", color = AppTextMuted, style = MaterialTheme.typography.bodySmall)
                }
            }

            SectionCard("Dados do projeto") {
                TextFieldN("Cliente", client) { client = it }
                TextFieldN("Instalação / QGBT / Transformador", installation) { installation = it }
                NumField("Potência ativa (kW)", power) { power = it }
                NumField("FP atual", fp1) { fp1 = it }
                NumField("FP desejado", fp2) { fp2 = it }
                NumField("Tensão trifásica (V)", voltage) { voltage = it }
            }

            SectionCard("Configuração") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(controller == "Monofásico", { controller = "Monofásico" }, { Text("Monofásico") })
                    FilterChip(controller == "Trifásico", { controller = "Trifásico" }, { Text("Trifásico") })
                }
                Text("Número de canais", style = MaterialTheme.typography.titleSmall, color = AppTextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf(6, 8, 12, 16, 24).forEach { n -> FilterChip(channels == n, { channels = n }, { Text("$n") }) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(detuned, { detuned = it })
                    Text("Banco dessintonizado / reatores", style = MaterialTheme.typography.bodyMedium)
                }
                if (detuned) NumField("Reator (%)", reactor) { reactor = it }
                NumField("Transformador (kVA)", transformer) { transformer = it }
                NumField("Capacitor fixo (%)", fixedPct) { fixedPct = it }
            }

            Button(
                onClick = {
                    runCatching {
                        val p = parseNumber(power)
                        val a = parseNumber(fp1)
                        val b = parseNumber(fp2)
                        val v = parseNumber(voltage)
                        require(b > a && a in 0.01..1.0 && b <= 1.0)
                        val qc = p * (tan(acos(a)) - tan(acos(b)))
                        val commercial = ceil(qc / 5.0) * 5.0
                        val current = commercial * 1000 / (sqrt(3.0) * v)
                        val breaker = nextStd(current * 1.5)
                        val loadCurrent = p * 1000 / (sqrt(3.0) * v * a)
                        val ct = nextStd(loadCurrent * 1.1)
                        val fixed = transformer.takeIf { it.isNotBlank() }?.let { parseNumber(it) * parseNumber(fixedPct) / 100 }
                        val tuning = if (detuned) 60 / sqrt(parseNumber(reactor) / 100) else null
                        val units = round(commercial / 5).toInt()
                        val base = units / channels
                        val rem = units % channels
                        val stages = (0 until channels).map { (base + if (it < rem) 1 else 0) * 5.0 }.filter { it > 0 }
                        result = CalcResult(qc, commercial, current, breaker, ct, fixed, tuning, stages)
                        error = ""
                    }.onFailure { error = "Revise os valores informados." }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) { Text("CALCULAR BANCO") }

            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)

            result?.let { r ->
                SectionCard("Resultado") {
                    ResultLine("Banco calculado", "${fmt(r.qc)} kvar")
                    ResultLine("Banco comercial", "${fmt(r.commercial, 0)} kvar")
                    ResultLine("Corrente", "${fmt(r.current, 1)} A")
                    ResultLine("Proteção preliminar", "${r.breaker} A")
                    ResultLine("TC preliminar", "${r.ct}/5 A")
                    r.fixed?.let { ResultLine("Capacitor fixo", "${fmt(it, 1)} kvar") }
                    r.tuning?.let { ResultLine("Frequência de sintonia", "${fmt(it, 1)} Hz") }
                    Text("Estágios: " + r.stages.mapIndexed { i, k -> "E${i + 1} ${fmt(k, 0)} kvar" }.joinToString(" | "), color = AppTextMuted)
                }
                Button(
                    onClick = {
                        val lines = buildList {
                            add("IDENTIFICAÇÃO:")
                            add("Cliente: $client")
                            add("Instalação: $installation")
                            add("DADOS DE ENTRADA:")
                            add("Potência: $power kW; FP $fp1 → $fp2; tensão $voltage V")
                            add("RESULTADOS:")
                            add("Banco calculado: ${fmt(r.qc)} kvar")
                            add("Banco comercial: ${fmt(r.commercial, 0)} kvar")
                            add("Corrente: ${fmt(r.current, 1)} A; proteção: ${r.breaker} A; TC: ${r.ct}/5 A")
                            add("Estágios: " + r.stages.mapIndexed { i, k -> "E${i + 1} ${fmt(k, 0)} kvar" }.joinToString(" | "))
                            add("RECOMENDAÇÕES:")
                            add("Pré-dimensionamento: validar curto-circuito, seletividade, fabricante, temperatura e harmônicos.")
                        }
                        sharePdf(context, createTextReport(context, "Banco de Capacitores", lines))
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("GERAR / COMPARTILHAR PDF") }
            }
        }
    }
}

private fun nextStd(v: Double): Int {
    val values = listOf(6, 10, 16, 20, 25, 32, 40, 50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630, 800, 1000, 1250, 1600, 2000)
    return values.firstOrNull { it >= v } ?: ceil(v / 100).toInt() * 100
}
