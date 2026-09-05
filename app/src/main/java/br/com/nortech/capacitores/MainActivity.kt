package br.com.nortech.capacitores

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = NortechScheme) {
                Surface(Modifier.fillMaxSize()) { NortechApp() }
            }
        }
    }
}

private val NortechBlue = Color(0xFF123D74)
private val NortechOrange = Color(0xFFF28C00)
private val NortechScheme = lightColorScheme(
    primary = NortechOrange,
    onPrimary = Color.White,
    secondary = NortechBlue,
    onSecondary = Color.White,
    background = Color.White,
    surface = Color.White,
    surfaceVariant = Color(0xFFF4F7FB),
    outline = Color(0xFFD7DFEA)
)
private const val WHATSAPP = "91 99181-5138"
private const val WHATSAPP_URL = "https://wa.me/5591991815138"

enum class Screen { HOME, CAPACITOR, TRANSFORMER, OTHER }

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
fun NortechApp() {
    var screen by remember { mutableStateOf(Screen.HOME) }
    when (screen) {
        Screen.HOME -> HomeScreen { screen = it }
        Screen.CAPACITOR -> CapacitorScreen { screen = Screen.HOME }
        Screen.TRANSFORMER -> TransformerScreen { screen = Screen.HOME }
        Screen.OTHER -> PlaceholderScreen { screen = Screen.HOME }
    }
}

@Composable
private fun BrandHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("NORTECH", color = NortechBlue, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineLarge)
        Text("SERVIÇOS E COMÉRCIO LTDA", color = NortechOrange, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HomeScreen(open: (Screen) -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BrandHeader()
        Text("Ferramentas Elétricas • v7", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = NortechBlue)
        Text("Painel técnico para cálculos elétricos, pré-dimensionamento e memoriais de campo.")
        Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(WHATSAPP_URL))) }, modifier = Modifier.fillMaxWidth()) {
            Text("WhatsApp: $WHATSAPP")
        }
        MenuCard("⚡ Banco de Capacitores", "Correção de FP, canais, controlador e PDF") { open(Screen.CAPACITOR) }
        MenuCard("🔌 Transformadores", "Correntes primária e secundária") { open(Screen.TRANSFORMER) }
        MenuCard("🧵 Cabos", "Pré-dimensionamento") { open(Screen.OTHER) }
        MenuCard("📉 Queda de Tensão", "Cálculo trifásico") { open(Screen.OTHER) }
        MenuCard("📊 Demanda", "Demanda em kW e kVA") { open(Screen.OTHER) }
        MenuCard("⚙️ Geradores", "Potência aparente e reserva") { open(Screen.OTHER) }
        MenuCard("🧾 Análise de Fatura", "Módulo em evolução") { open(Screen.OTHER) }
        Text("Desenvolvido por Ezequiel Paixão", color = NortechBlue, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MenuCard(title: String, subtitle: String, action: () -> Unit) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle)
            Button(onClick = action, modifier = Modifier.fillMaxWidth()) { Text("ABRIR") }
        }
    }
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
    var controller by remember { mutableStateOf("Trifásico") }
    var channels by remember { mutableStateOf(12) }
    var detuned by remember { mutableStateOf(false) }
    var reactor by remember { mutableStateOf("7") }
    var transformer by remember { mutableStateOf("750") }
    var fixedPercent by remember { mutableStateOf("2") }
    var result by remember { mutableStateOf<CalcResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← Voltar ao painel") }
        BrandHeader()
        Text("Banco de Capacitores", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = NortechBlue)
        Field("Cliente", client) { client = it }
        Field("Instalação / QGBT / Transformador", installation) { installation = it }
        NumField("Potência ativa da carga (kW)", power) { power = it }
        NumField("Fator de potência atual", fp1) { fp1 = it }
        NumField("Fator de potência desejado", fp2) { fp2 = it }
        NumField("Tensão trifásica (V)", voltage) { voltage = it }

        Text("Tipo de controlador", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = controller == "Monofásico", onClick = { controller = "Monofásico" }, label = { Text("Monofásico") })
            FilterChip(selected = controller == "Trifásico", onClick = { controller = "Trifásico" }, label = { Text("Trifásico") })
        }

        Text("Número de canais", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(6, 8, 12, 16, 24).forEach { n ->
                FilterChip(selected = channels == n, onClick = { channels = n }, label = { Text(n.toString()) })
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = detuned, onCheckedChange = { detuned = it })
            Text("Banco dessintonizado / com reatores")
        }
        if (detuned) NumField("Fator do reator (%)", reactor) { reactor = it }
        NumField("Potência do transformador (kVA) - opcional", transformer) { transformer = it }
        NumField("Percentual para capacitor fixo (%)", fixedPercent) { fixedPercent = it }

        Button(onClick = {
            try {
                val p = parse(power)
                val a = parse(fp1)
                val b = parse(fp2)
                val v = parse(voltage)
                require(p > 0 && v > 0 && a in 0.01..1.0 && b in 0.01..1.0 && b > a)
                val qc = p * (tan(acos(a)) - tan(acos(b)))
                val commercial = ceil(qc / 5.0) * 5.0
                val current = commercial * 1000.0 / (sqrt(3.0) * v)
                val breaker = nextStandard(current * 1.5)
                val loadCurrent = p * 1000.0 / (sqrt(3.0) * v * a)
                val ct = nextStandard(loadCurrent * 1.10)
                val fixed = transformer.takeIf { it.isNotBlank() }?.let { parse(it) * parse(fixedPercent) / 100.0 }
                val tuning = if (detuned) 60.0 / sqrt(parse(reactor) / 100.0) else null
                val units = round(commercial / 5.0).toInt()
                val base = units / channels
                val rem = units % channels
                val stages = (0 until channels).map { (base + if (it < rem) 1 else 0) * 5.0 }.filter { it > 0 }
                result = CalcResult(qc, commercial, current, breaker, ct, fixed, tuning, stages)
                error = null
            } catch (e: Exception) {
                error = "Revise os valores informados. O FP desejado deve ser maior que o FP atual."
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("CALCULAR") }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        result?.let { r ->
            ResultCard(r)
            Button(onClick = {
                val uri = createPdf(context, client, installation, controller, channels, detuned, r, power, fp1, fp2, voltage)
                sharePdf(context, uri)
            }, modifier = Modifier.fillMaxWidth()) { Text("GERAR / COMPARTILHAR PDF") }
        }
    }
}

@Composable
private fun ResultCard(r: CalcResult) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Resultado", color = NortechBlue, fontWeight = FontWeight.Bold)
            ResultLine("Banco calculado", "%.2f kvar".format(r.qc))
            ResultLine("Banco comercial", "%.0f kvar".format(r.commercial))
            ResultLine("Corrente do banco", "%.1f A".format(r.current))
            ResultLine("Proteção geral sugerida*", "${r.breaker} A")
            ResultLine("TC sugerido*", "${r.ct}/5 A")
            r.fixed?.let { ResultLine("Capacitor fixo trafo", "%.2f kvar".format(it)) }
            r.tuning?.let { ResultLine("Frequência de sintonia", "%.1f Hz".format(it)) }
            Text("Estágios (${r.stages.size} usados):", fontWeight = FontWeight.Bold)
            r.stages.forEachIndexed { i, kvar -> Text("E${i + 1}: %.0f kvar".format(kvar)) }
            Text("* Pré-dimensionamento: validar fabricante, curto-circuito, seletividade, temperatura e harmônicos.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TransformerScreen(onBack: () -> Unit) {
    var kva by remember { mutableStateOf("750") }
    var vp by remember { mutableStateOf("13800") }
    var vs by remember { mutableStateOf("380") }
    var result by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← Voltar") }
        BrandHeader()
        Text("Transformadores", style = MaterialTheme.typography.headlineSmall, color = NortechBlue, fontWeight = FontWeight.Bold)
        NumField("Potência (kVA)", kva) { kva = it }
        NumField("Tensão primária (V)", vp) { vp = it }
        NumField("Tensão secundária (V)", vs) { vs = it }
        Button(onClick = {
            val s = parse(kva) * 1000.0
            result = "Primário: %.2f A\nSecundário: %.2f A".format(s / (sqrt(3.0) * parse(vp)), s / (sqrt(3.0) * parse(vs)))
        }) { Text("CALCULAR") }
        if (result.isNotBlank()) Text(result)
    }
}

@Composable
private fun PlaceholderScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← Voltar") }
        BrandHeader()
        Text("Módulo preparado", style = MaterialTheme.typography.headlineSmall, color = NortechBlue, fontWeight = FontWeight.Bold)
        Text("Este módulo será ampliado nas próximas versões do aplicativo.")
    }
}

@Composable
private fun Field(label: String, value: String, change: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = change, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
}

@Composable
private fun NumField(label: String, value: String, change: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = change, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
}

@Composable
private fun ResultLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Bold)
    }
}

private fun parse(s: String) = s.trim().replace(',', '.').toDouble()
private fun nextStandard(v: Double): Int {
    val values = listOf(6,10,16,20,25,32,40,50,63,80,100,125,160,200,250,315,400,500,630,800,1000,1250,1600,2000)
    return values.firstOrNull { it >= v } ?: ceil(v / 100.0).toInt() * 100
}

private fun createPdf(context: Context, client: String, installation: String, controller: String, channels: Int, detuned: Boolean, r: CalcResult, p: String, fp1: String, fp2: String, v: String): Uri {
    val doc = PdfDocument()
    val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
    val c = page.canvas
    val blue = Paint().apply { color = android.graphics.Color.rgb(18,61,116); textSize = 18f; typeface = Typeface.DEFAULT_BOLD }
    val orange = Paint().apply { color = android.graphics.Color.rgb(242,140,0); textSize = 12f; typeface = Typeface.DEFAULT_BOLD }
    val body = Paint().apply { color = android.graphics.Color.DKGRAY; textSize = 10f }
    val bold = Paint(body).apply { typeface = Typeface.DEFAULT_BOLD }
    var y = 55f
    c.drawText("NORTECH", 40f, y, blue); y += 20f
    c.drawText("SERVIÇOS E COMÉRCIO LTDA", 40f, y, orange); y += 34f
    c.drawText("MEMORIAL DE CÁLCULO - BANCO DE CAPACITORES", 40f, y, blue); y += 24f
    val created = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR")).format(Date())
    val lines = listOf(
        "Data e hora de criação: $created",
        "Cliente: $client",
        "Instalação: $installation",
        "Potência ativa: $p kW",
        "FP atual: $fp1",
        "FP desejado: $fp2",
        "Tensão: $v V",
        "Controlador: $controller - $channels canais",
        "Banco dessintonizado: ${if (detuned) "SIM" else "NÃO"}",
        "Banco calculado: %.2f kvar".format(r.qc),
        "Banco comercial: %.0f kvar".format(r.commercial),
        "Corrente do banco: %.1f A".format(r.current),
        "Proteção sugerida*: ${r.breaker} A",
        "TC sugerido*: ${r.ct}/5 A"
    )
    lines.forEach { c.drawText(it, 40f, y, if (it.startsWith("Banco comercial")) bold else body); y += 16f }
    y += 8f
    c.drawText("Estágios", 40f, y, blue); y += 18f
    r.stages.forEachIndexed { i, kvar -> c.drawText("E${i+1}: %.0f kvar".format(kvar), 50f, y, body); y += 14f }
    y += 12f
    c.drawText("* Pré-dimensionamento. Validar projeto, fabricante, curto-circuito, seletividade e harmônicos.", 40f, y, body)
    c.drawText("Desenvolvido por Ezequiel Paixão", 40f, 790f, blue)
    c.drawText("WhatsApp: $WHATSAPP | $WHATSAPP_URL", 40f, 810f, body)
    doc.finishPage(page)
    val dir = File(context.cacheDir, "reports").apply { mkdirs() }
    val file = File(dir, "NORTECH_Banco_${System.currentTimeMillis()}.pdf")
    FileOutputStream(file).use { doc.writeTo(it) }
    doc.close()
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun sharePdf(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartilhar relatório NORTECH"))
}
