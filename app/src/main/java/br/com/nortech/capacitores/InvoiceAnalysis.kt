package br.com.nortech.capacitores

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

@Stable
data class InvoiceData(
    val fileName: String,
    val reference: String = "",
    val consumptionKwh: String = "",
    val demandKw: String = "",
    val powerFactor: String = "",
    val totalValue: String = "",
    val reactiveKvarh: String = "",
    val rawText: String = ""
)

@Composable
fun InvoiceAnalysisScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val invoices = remember { mutableStateListOf<InvoiceData>() }
    var loading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var client by remember { mutableStateOf("") }
    var transformerKva by remember { mutableStateOf("") }
    var freeMarket by remember { mutableStateOf(false) }
    var aclPrice by remember { mutableStateOf("") }
    var hasGenerator by remember { mutableStateOf(false) }
    var hasPv by remember { mutableStateOf(false) }
    var pvKw by remember { mutableStateOf("") }
    var targetFp by remember { mutableStateOf("0,98") }
    var analysis by remember { mutableStateOf<List<String>>(emptyList()) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            loading = true
            status = "Lendo ${uris.size} arquivo(s)..."
            extractInvoices(context, uris) { data ->
                invoices.clear(); invoices.addAll(data)
                loading = false
                status = "${data.size} fatura(s) carregada(s). Confira os dados antes de analisar."
            }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← Voltar ao painel") }
        BrandHeader()
        Text("Análise de Faturas", style = MaterialTheme.typography.headlineSmall, color = NortechBlue, fontWeight = FontWeight.Bold)
        Text("Selecione uma ou várias faturas em PDF, JPG ou PNG. PDFs com texto serão lidos diretamente; imagens usam OCR no aparelho.")
        Button(onClick = { picker.launch(arrayOf("application/pdf", "image/*")) }, modifier = Modifier.fillMaxWidth(), enabled = !loading) {
            Text(if (loading) "LENDO FATURAS..." else "SELECIONAR FATURAS")
        }
        if (status.isNotBlank()) Text(status, color = NortechBlue)

        if (invoices.isNotEmpty()) {
            SectionCard("Dados da instalação") {
                TextFieldN("Cliente", client) { client = it }
                NumField("Transformadores instalados (kVA) - opcional", transformerKva) { transformerKva = it }
                Row { Checkbox(freeMarket, { freeMarket = it }); Text("Mercado Livre (ACL)") }
                if (freeMarket) NumField("Preço contratado de energia (R$/MWh) - opcional", aclPrice) { aclPrice = it }
                Row { Checkbox(hasGenerator, { hasGenerator = it }); Text("Possui grupo gerador") }
                Row { Checkbox(hasPv, { hasPv = it }); Text("Possui geração fotovoltaica") }
                if (hasPv) NumField("Potência fotovoltaica (kW)", pvKw) { pvKw = it }
                NumField("FP alvo para simulação", targetFp) { targetFp = it }
            }

            Text("Conferência dos dados identificados", fontWeight = FontWeight.Bold, color = NortechBlue)
            invoices.forEachIndexed { index, item ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(item.fileName, fontWeight = FontWeight.Bold)
                        TextFieldN("Referência", item.reference) { invoices[index] = item.copy(reference = it) }
                        NumField("Consumo (kWh)", item.consumptionKwh) { invoices[index] = item.copy(consumptionKwh = it) }
                        NumField("Demanda medida (kW)", item.demandKw) { invoices[index] = item.copy(demandKw = it) }
                        NumField("Fator de potência", item.powerFactor) { invoices[index] = item.copy(powerFactor = it) }
                        NumField("Energia reativa (kvarh)", item.reactiveKvarh) { invoices[index] = item.copy(reactiveKvarh = it) }
                        NumField("Valor total da fatura (R$)", item.totalValue) { invoices[index] = item.copy(totalValue = it) }
                        if (item.rawText.isNotBlank()) {
                            Text("Texto lido: ${item.rawText.take(220).replace('\n',' ')}${if (item.rawText.length>220) "..." else ""}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Button(onClick = {
                analysis = analyzeInvoices(invoices.toList(), transformerKva, targetFp, freeMarket, aclPrice, hasGenerator, hasPv, pvKw)
            }, modifier = Modifier.fillMaxWidth()) { Text("ANALISAR FATURAS") }
        }

        if (analysis.isNotEmpty()) {
            SectionCard("Resultado da análise") { analysis.forEach { Text(it) } }
            Button(onClick = {
                val header = buildList {
                    add("Cliente: ${client.ifBlank { "não informado" }}")
                    add("Faturas analisadas: ${invoices.size}")
                    add("Mercado: ${if (freeMarket) "Livre (ACL)" else "Cativo / não informado"}")
                    if (transformerKva.isNotBlank()) add("Transformadores instalados: $transformerKva kVA")
                    if (hasGenerator) add("Grupo gerador: SIM")
                    if (hasPv) add("Geração fotovoltaica: SIM${if(pvKw.isNotBlank()) " - $pvKw kW" else ""}")
                    add("")
                    addAll(analysis)
                }
                sharePdf(context, createTextReport(context, "Análise de Faturas", header))
            }, modifier = Modifier.fillMaxWidth()) { Text("GERAR RELATÓRIO NORTECH") }
        }
    }
}

private fun analyzeInvoices(
    invoices: List<InvoiceData>,
    transformerKva: String,
    targetFpText: String,
    freeMarket: Boolean,
    aclPrice: String,
    hasGenerator: Boolean,
    hasPv: Boolean,
    pvKw: String
): List<String> {
    val valid = invoices.mapNotNull { inv ->
        val kwh = inv.consumptionKwh.toPtDoubleOrNull()
        val total = inv.totalValue.toPtDoubleOrNull()
        if (kwh != null || total != null) Triple(inv, kwh ?: 0.0, total ?: 0.0) else null
    }
    if (valid.isEmpty()) return listOf("Não há dados suficientes. Confira consumo e valor das faturas.")
    val sumKwh = valid.sumOf { it.second }
    val sumValue = valid.sumOf { it.third }
    val avgKwh = sumKwh / valid.size
    val avgValue = sumValue / valid.size
    val avgCost = if (sumKwh > 0) sumValue / sumKwh else 0.0
    val demands = invoices.mapNotNull { it.demandKw.toPtDoubleOrNull() }
    val fps = invoices.mapNotNull { it.powerFactor.toPtDoubleOrNull() }.filter { it in 0.01..1.0 }
    val reactives = invoices.mapNotNull { it.reactiveKvarh.toPtDoubleOrNull() }
    val maxDemand = demands.maxOrNull()
    val minDemand = demands.minOrNull()
    val avgFp = fps.takeIf { it.isNotEmpty() }?.average()
    val targetFp = targetFpText.toPtDoubleOrNull()?.coerceIn(0.01,1.0) ?: 0.98
    val fp092EquivalentLoss = if (avgFp != null && avgFp < 0.92 && sumKwh > 0) sumKwh * (0.92 / avgFp - 1.0) else 0.0
    val fpTargetEquivalentGain = if (avgFp != null && avgFp < targetFp && sumKwh > 0) sumKwh * (targetFp / avgFp - 1.0) else 0.0
    val estimatedValue092 = fp092EquivalentLoss * avgCost
    val estimatedValueTarget = fpTargetEquivalentGain * avgCost
    return buildList {
        add("Período analisado: ${invoices.size} fatura(s).")
        add("Consumo total identificado: ${fmt(sumKwh,0)} kWh; média mensal: ${fmt(avgKwh,0)} kWh.")
        add("Valor total identificado: R$ ${fmt(sumValue,2)}; média mensal: R$ ${fmt(avgValue,2)}.")
        if (avgCost > 0) add("Custo médio global identificado: R$ ${fmt(avgCost,4)}/kWh.")
        if (maxDemand != null) add("Demanda máxima identificada: ${fmt(maxDemand,1)} kW; mínima: ${fmt(minDemand ?: maxDemand,1)} kW.")
        if (avgFp != null) {
            add("Fator de potência médio informado/lido: ${fmt(avgFp,3)}.")
            add("Simulação equivalente até FP 0,92: ${fmt(fp092EquivalentLoss,0)} kWh-equivalentes; impacto estimado pelo custo médio: R$ ${fmt(estimatedValue092,2)}.")
            add("Simulação equivalente até FP ${fmt(targetFp,2)}: ${fmt(fpTargetEquivalentGain,0)} kWh-equivalentes; impacto estimado pelo custo médio: R$ ${fmt(estimatedValueTarget,2)}.")
            add("A simulação de FP é indicativa e não substitui a memória tarifária da distribuidora; a cobrança real de energia/demanda reativa deve ser obtida dos campos tarifários da fatura.")
        }
        if (reactives.isNotEmpty()) add("Energia reativa total informada/lida: ${fmt(reactives.sum(),0)} kvarh.")
        transformerKva.toPtDoubleOrNull()?.let { kva -> if (maxDemand != null) add("Relação demanda máxima / potência de transformadores: ${fmt(maxDemand/kva*100.0,1)}% (comparação kW/kVA apenas indicativa).") }
        if (freeMarket) add("Mercado Livre informado.${aclPrice.toPtDoubleOrNull()?.let { " Preço ACL informado: R$ ${fmt(it,2)}/MWh." } ?: ""}")
        if (hasGenerator) add("A instalação possui grupo gerador; avaliar separadamente operação em horário de ponta, transferência e reflexos na demanda.")
        if (hasPv) add("A instalação possui geração fotovoltaica${pvKw.toPtDoubleOrNull()?.let { " de ${fmt(it,1)} kW" } ?: ""}; avaliar autoconsumo, zero-grid/compensação e efeito na curva de demanda.")
        add("Recomendação: conferir demanda contratada versus medida, ultrapassagens, energia reativa, modalidade tarifária, impostos, ponta/fora ponta e histórico de pelo menos 12 meses quando disponíveis.")
    }
}

private fun extractInvoices(context: Context, uris: List<Uri>, done: (List<InvoiceData>) -> Unit) {
    PDFBoxResourceLoader.init(context.applicationContext)
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val remaining = AtomicInteger(uris.size)
    val results = java.util.Collections.synchronizedList(mutableListOf<InvoiceData>())
    val handler = Handler(Looper.getMainLooper())
    fun finishOne(data: InvoiceData) {
        results += data
        if (remaining.decrementAndGet() == 0) {
            recognizer.close()
            handler.post { done(results.sortedBy { it.fileName }) }
        }
    }
    uris.forEach { uri ->
        val name = displayName(context, uri)
        val mime = context.contentResolver.getType(uri).orEmpty()
        if (mime == "application/pdf" || name.endsWith(".pdf", true)) {
            Thread {
                val text = runCatching {
                    context.contentResolver.openInputStream(uri).use { input ->
                        requireNotNull(input)
                        PDDocument.load(input).use { doc -> PDFTextStripper().getText(doc) }
                    }
                }.getOrElse { "" }
                finishOne(parseInvoice(name, text))
            }.start()
        } else {
            runCatching { InputImage.fromFilePath(context, uri) }.onSuccess { image ->
                recognizer.process(image)
                    .addOnSuccessListener { result -> finishOne(parseInvoice(name, result.text)) }
                    .addOnFailureListener { finishOne(parseInvoice(name, "")) }
            }.onFailure { finishOne(parseInvoice(name, "")) }
        }
    }
}

private fun parseInvoice(fileName: String, text: String): InvoiceData {
    val clean = text.replace('\u00A0',' ')
    fun first(patterns: List<Regex>): String {
        for (r in patterns) {
            val m = r.find(clean)
            if (m != null) return m.groupValues.getOrNull(1)?.trim().orEmpty()
        }
        return ""
    }
    val reference = first(listOf(
        Regex("(?i)(?:m[eê]s\s*de\s*refer[eê]ncia|refer[eê]ncia|m[eê]s/ano)\s*[:\-]?\s*(\d{2}/\d{4})"),
        Regex("\b(0[1-9]|1[0-2])/20\d{2}\b")
    ))
    val consumption = first(listOf(
        Regex("(?i)(?:consumo(?:\s+ativo)?|energia\s+ativa)[^\n]{0,45}?(\d[\d\.]*[,]?\d*)\s*kwh"),
        Regex("(?i)(\d[\d\.]*[,]?\d*)\s*kwh")
    ))
    val demand = first(listOf(
        Regex("(?i)(?:demanda\s+(?:medida|faturada|registrada)|demanda)[^\n]{0,45}?(\d[\d\.]*[,]?\d*)\s*kw")
    ))
    val fp = first(listOf(
        Regex("(?i)(?:fator\s+de\s+pot[eê]ncia|fp)\s*[:\-]?\s*(0[,\.]\d{2,4}|1[,\.]?0*)")
    ))
    val reactive = first(listOf(
        Regex("(?i)(?:energia\s+reativa|reativ[ao])[^\n]{0,45}?(\d[\d\.]*[,]?\d*)\s*kvarh")
    ))
    val total = first(listOf(
        Regex("(?i)(?:valor\s+total|total\s+a\s+pagar|total\s+da\s+fatura)[^\n]{0,35}?R?\$?\s*(\d[\d\.]*,\d{2})"),
        Regex("R\$\s*(\d[\d\.]*,\d{2})")
    ))
    return InvoiceData(fileName, reference, consumption, demand, fp, total, reactive, clean.take(12000))
}

private fun displayName(context: Context, uri: Uri): String {
    var name = "fatura"
    val cursor: Cursor? = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
    cursor?.use { if (it.moveToFirst()) name = it.getString(0) ?: name }
    return name
}

private fun String.toPtDoubleOrNull(): Double? {
    val s = trim().replace("R$", "").replace(" ", "")
    if (s.isBlank()) return null
    return if (s.contains(',')) s.replace(".", "").replace(',', '.').toDoubleOrNull() else s.toDoubleOrNull()
}
