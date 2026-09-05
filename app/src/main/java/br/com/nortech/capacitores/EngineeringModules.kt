package br.com.nortech.capacitores

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.*

private val cableSections = listOf(1.5,2.5,4.0,6.0,10.0,16.0,25.0,35.0,50.0,70.0,95.0,120.0,150.0,185.0,240.0,300.0)
private val trafoStandards = listOf(45,75,112,150,225,300,500,750,1000,1500,2000,2500,3000)
private val generatorStandards = listOf(30,40,55,75,100,125,150,180,200,250,300,350,400,450,500,625,750,1000,1250,1500,2000,2500)

@Composable
fun CableScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var power by remember { mutableStateOf("75") }
    var voltage by remember { mutableStateOf("380") }
    var fp by remember { mutableStateOf("0,90") }
    var length by remember { mutableStateOf("50") }
    var maxDrop by remember { mutableStateOf("4") }
    var threePhase by remember { mutableStateOf(true) }
    var copper by remember { mutableStateOf(true) }
    var result by remember { mutableStateOf<List<String>>(emptyList()) }
    ModuleScaffold("Dimensionamento de Cabos", onBack) {
        NumField("Potência ativa (kW)", power) { power = it }
        NumField("Tensão (V)", voltage) { voltage = it }
        NumField("Fator de potência", fp) { fp = it }
        NumField("Comprimento do circuito (m)", length) { length = it }
        NumField("Queda máxima admissível (%)", maxDrop) { maxDrop = it }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = threePhase, onClick = { threePhase = true }, label = { Text("Trifásico") })
            FilterChip(selected = !threePhase, onClick = { threePhase = false }, label = { Text("Monofásico") })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = copper, onClick = { copper = true }, label = { Text("Cobre") })
            FilterChip(selected = !copper, onClick = { copper = false }, label = { Text("Alumínio") })
        }
        Button(onClick = {
            runCatching {
                val p = parseNumber(power) * 1000.0
                val v = parseNumber(voltage)
                val f = parseNumber(fp)
                val l = parseNumber(length)
                val dropPct = parseNumber(maxDrop)
                require(p > 0 && v > 0 && f in 0.1..1.0 && l > 0 && dropPct > 0)
                val current = if (threePhase) p / (sqrt(3.0) * v * f) else p / (v * f)
                val rho = if (copper) 0.0175 else 0.0282
                val allowedV = v * dropPct / 100.0
                val calcS = if (threePhase) sqrt(3.0) * rho * l * current / allowedV else 2.0 * rho * l * current / allowedV
                val ampacityS = ampacitySection(current, copper)
                val dropSection = cableSections.firstOrNull { it >= calcS } ?: cableSections.last()
                val chosen = maxOf(dropSection, ampacityS)
                val actualDrop = if (threePhase) sqrt(3.0) * rho * l * current / chosen else 2.0 * rho * l * current / chosen
                val pct = actualDrop / v * 100.0
                val breaker = nextBreaker(current * 1.20)
                result = listOf(
                    "Corrente de projeto: ${fmt(current,1)} A",
                    "Seção mínima pela queda: ${fmt(calcS,2)} mm²",
                    "Seção comercial sugerida: ${fmt(chosen,1)} mm²",
                    "Queda estimada: ${fmt(actualDrop,2)} V (${fmt(pct,2)}%)",
                    "Disjuntor preliminar: $breaker A",
                    "Material: ${if (copper) "cobre" else "alumínio"}; sistema: ${if (threePhase) "trifásico" else "monofásico"}",
                    "Pré-dimensionamento. Confirmar método de instalação, temperatura, agrupamento, isolação, curto-circuito e critérios da NBR 5410/NBR 14039."
                )
            }.onFailure { result = listOf("Revise os valores informados.") }
        }, modifier = Modifier.fillMaxWidth()) { Text("CALCULAR") }
        ResultAndReport("Dimensionamento de Cabos", result)
    }
}

@Composable
fun VoltageDropScreen(onBack: () -> Unit) {
    var current by remember { mutableStateOf("100") }
    var voltage by remember { mutableStateOf("380") }
    var length by remember { mutableStateOf("80") }
    var section by remember { mutableStateOf("35") }
    var threePhase by remember { mutableStateOf(true) }
    var copper by remember { mutableStateOf(true) }
    var limit by remember { mutableStateOf("4") }
    var result by remember { mutableStateOf<List<String>>(emptyList()) }
    ModuleScaffold("Queda de Tensão", onBack) {
        NumField("Corrente (A)", current) { current = it }
        NumField("Tensão nominal (V)", voltage) { voltage = it }
        NumField("Comprimento (m)", length) { length = it }
        NumField("Seção do condutor (mm²)", section) { section = it }
        NumField("Limite adotado (%)", limit) { limit = it }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = threePhase, onClick = { threePhase = true }, label = { Text("Trifásico") })
            FilterChip(selected = !threePhase, onClick = { threePhase = false }, label = { Text("Monofásico") })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = copper, onClick = { copper = true }, label = { Text("Cobre") })
            FilterChip(selected = !copper, onClick = { copper = false }, label = { Text("Alumínio") })
        }
        Button(onClick = {
            runCatching {
                val i = parseNumber(current); val v = parseNumber(voltage); val l = parseNumber(length); val s = parseNumber(section); val lim = parseNumber(limit)
                val rho = if (copper) 0.0175 else 0.0282
                val dv = if (threePhase) sqrt(3.0)*rho*l*i/s else 2.0*rho*l*i/s
                val pct = dv/v*100.0
                result = listOf(
                    "Queda de tensão: ${fmt(dv,2)} V",
                    "Queda percentual: ${fmt(pct,2)}%",
                    "Limite informado: ${fmt(lim,2)}%",
                    "Situação: ${if (pct <= lim) "DENTRO DO LIMITE INFORMADO" else "ACIMA DO LIMITE INFORMADO"}",
                    "Cálculo resistivo preliminar; em circuitos relevantes considerar reatância, temperatura de operação e parâmetros do cabo."
                )
            }.onFailure { result = listOf("Revise os valores informados.") }
        }, modifier = Modifier.fillMaxWidth()) { Text("CALCULAR") }
        ResultAndReport("Queda de Tensão", result)
    }
}

@Composable
fun DemandScreen(onBack: () -> Unit) {
    var installed by remember { mutableStateOf("500") }
    var demandFactor by remember { mutableStateOf("70") }
    var simult by remember { mutableStateOf("90") }
    var fp by remember { mutableStateOf("0,92") }
    var voltage by remember { mutableStateOf("380") }
    var result by remember { mutableStateOf<List<String>>(emptyList()) }
    ModuleScaffold("Demanda", onBack) {
        NumField("Potência instalada (kW)", installed) { installed = it }
        NumField("Fator de demanda (%)", demandFactor) { demandFactor = it }
        NumField("Simultaneidade (%)", simult) { simult = it }
        NumField("Fator de potência", fp) { fp = it }
        NumField("Tensão trifásica (V)", voltage) { voltage = it }
        Button(onClick = {
            runCatching {
                val p = parseNumber(installed); val fd = parseNumber(demandFactor)/100.0; val fs = parseNumber(simult)/100.0; val f = parseNumber(fp); val v = parseNumber(voltage)
                val kw = p*fd*fs
                val kva = kw/f
                val i = kva*1000.0/(sqrt(3.0)*v)
                val trafo = trafoStandards.firstOrNull { it >= kva*1.10 } ?: trafoStandards.last()
                result = listOf(
                    "Demanda ativa calculada: ${fmt(kw,2)} kW",
                    "Demanda aparente: ${fmt(kva,2)} kVA",
                    "Corrente correspondente: ${fmt(i,1)} A",
                    "Transformador comercial sugerido com margem aproximada de 10%: $trafo kVA",
                    "Confirmar perfil de carga, partidas, expansão, simultaneidade real e critérios da concessionária."
                )
            }.onFailure { result = listOf("Revise os valores informados.") }
        }, modifier = Modifier.fillMaxWidth()) { Text("CALCULAR") }
        ResultAndReport("Cálculo de Demanda", result)
    }
}

@Composable
fun GeneratorScreen(onBack: () -> Unit) {
    var load by remember { mutableStateOf("300") }
    var fp by remember { mutableStateOf("0,80") }
    var reserve by remember { mutableStateOf("20") }
    var largestMotor by remember { mutableStateOf("75") }
    var startFactor by remember { mutableStateOf("6") }
    var voltage by remember { mutableStateOf("380") }
    var result by remember { mutableStateOf<List<String>>(emptyList()) }
    ModuleScaffold("Geradores", onBack) {
        NumField("Carga contínua (kW)", load) { load = it }
        NumField("Fator de potência", fp) { fp = it }
        NumField("Margem de reserva (%)", reserve) { reserve = it }
        NumField("Maior motor (kW)", largestMotor) { largestMotor = it }
        NumField("Fator de corrente de partida do motor", startFactor) { startFactor = it }
        NumField("Tensão do gerador (V)", voltage) { voltage = it }
        Button(onClick = {
            runCatching {
                val p = parseNumber(load); val f = parseNumber(fp); val r = parseNumber(reserve)/100.0; val m = parseNumber(largestMotor); val sf = parseNumber(startFactor); val v = parseNumber(voltage)
                val baseKva = p/f*(1+r)
                val motorRatedKva = if (m>0) m/0.88/0.85 else 0.0
                val startKva = motorRatedKva*sf
                val transientSuggestion = max(baseKva, (p-m.coerceAtMost(p))/f + startKva*0.35)
                val suggested = generatorStandards.firstOrNull { it >= transientSuggestion } ?: generatorStandards.last()
                val current = suggested*1000.0/(sqrt(3.0)*v)
                result = listOf(
                    "Potência contínua com reserva: ${fmt(baseKva,1)} kVA",
                    "kVA de partida estimado do maior motor: ${fmt(startKva,1)} kVA",
                    "Critério combinado preliminar: ${fmt(transientSuggestion,1)} kVA",
                    "Gerador comercial sugerido: $suggested kVA",
                    "Corrente nominal aproximada a ${fmt(v,0)} V: ${fmt(current,1)} A",
                    "Dimensionamento preliminar: validar método de partida, curva de carga, degrau de carga, alternador, AVR, temperatura, altitude e especificação Prime/Standby do fabricante."
                )
            }.onFailure { result = listOf("Revise os valores informados.") }
        }, modifier = Modifier.fillMaxWidth()) { Text("CALCULAR") }
        ResultAndReport("Dimensionamento de Gerador", result)
    }
}

@Composable
fun TransformerScreenV9(onBack: () -> Unit) {
    var kva by remember { mutableStateOf("750") }
    var vp by remember { mutableStateOf("13800") }
    var vs by remember { mutableStateOf("380") }
    var loadKw by remember { mutableStateOf("500") }
    var fp by remember { mutableStateOf("0,92") }
    var fixedPct by remember { mutableStateOf("2") }
    var result by remember { mutableStateOf<List<String>>(emptyList()) }
    ModuleScaffold("Transformadores", onBack) {
        NumField("Potência do transformador (kVA)", kva) { kva = it }
        NumField("Tensão primária (V)", vp) { vp = it }
        NumField("Tensão secundária (V)", vs) { vs = it }
        NumField("Carga ativa atual (kW)", loadKw) { loadKw = it }
        NumField("Fator de potência da carga", fp) { fp = it }
        NumField("Capacitor fixo a vazio (%)", fixedPct) { fixedPct = it }
        Button(onClick = {
            runCatching {
                val s = parseNumber(kva); val pV = parseNumber(vp); val sV = parseNumber(vs); val p = parseNumber(loadKw); val f = parseNumber(fp)
                val ip = s*1000.0/(sqrt(3.0)*pV); val isec = s*1000.0/(sqrt(3.0)*sV)
                val loadKva = p/f; val loading = loadKva/s*100.0; val available = max(0.0,s-loadKva)
                val fixed = s*parseNumber(fixedPct)/100.0
                result = listOf(
                    "Corrente nominal primária: ${fmt(ip,2)} A",
                    "Corrente nominal secundária: ${fmt(isec,1)} A",
                    "Relação de transformação: ${fmt(pV/sV,2)} : 1",
                    "Carga aparente atual: ${fmt(loadKva,1)} kVA",
                    "Carregamento do transformador: ${fmt(loading,1)}%",
                    "Capacidade aparente disponível: ${fmt(available,1)} kVA",
                    "Capacitor fixo preliminar a vazio: ${fmt(fixed,1)} kvar",
                    "O capacitor fixo deve ser validado com dados de magnetização/perdas e recomendação do fabricante."
                )
            }.onFailure { result = listOf("Revise os valores informados.") }
        }, modifier = Modifier.fillMaxWidth()) { Text("CALCULAR") }
        ResultAndReport("Transformadores", result)
    }
}

@Composable
fun EnergyAnalysisScreen(onBack: () -> Unit) {
    var va by remember { mutableStateOf("380") }; var vb by remember { mutableStateOf("380") }; var vc by remember { mutableStateOf("380") }
    var ia by remember { mutableStateOf("100") }; var ib by remember { mutableStateOf("100") }; var ic by remember { mutableStateOf("100") }
    var fp by remember { mutableStateOf("0,95") }; var freq by remember { mutableStateOf("60") }
    var thdv by remember { mutableStateOf("3") }; var thdi by remember { mutableStateOf("10") }
    var vLimit by remember { mutableStateOf("5") }; var unbLimit by remember { mutableStateOf("2") }; var thdvLimit by remember { mutableStateOf("8") }
    var nominal by remember { mutableStateOf("380") }
    var result by remember { mutableStateOf<List<String>>(emptyList()) }
    ModuleScaffold("Análise de Energia", onBack) {
        Text("Entrada manual de grandezas medidas", fontWeight = FontWeight.Bold)
        NumField("Tensão A (V)", va) { va=it }; NumField("Tensão B (V)", vb) { vb=it }; NumField("Tensão C (V)", vc) { vc=it }
        NumField("Corrente A (A)", ia) { ia=it }; NumField("Corrente B (A)", ib) { ib=it }; NumField("Corrente C (A)", ic) { ic=it }
        NumField("Fator de potência", fp) { fp=it }; NumField("Frequência (Hz)", freq) { freq=it }
        NumField("DHT tensão (%)", thdv) { thdv=it }; NumField("DHT corrente (%)", thdi) { thdi=it }
        Text("Limites preliminares configuráveis", fontWeight = FontWeight.Bold)
        NumField("Tensão nominal (V)", nominal) { nominal=it }; NumField("Desvio de tensão limite (%)", vLimit) { vLimit=it }
        NumField("Desequilíbrio limite (%)", unbLimit) { unbLimit=it }; NumField("DHT-V limite (%)", thdvLimit) { thdvLimit=it }
        Button(onClick = {
            runCatching {
                val volts=listOf(parseNumber(va),parseNumber(vb),parseNumber(vc)); val amps=listOf(parseNumber(ia),parseNumber(ib),parseNumber(ic))
                val vavg=volts.average(); val iavg=amps.average(); val unb=(volts.maxOrNull()!!-volts.minOrNull()!!)/vavg*100.0
                val dev=abs(vavg-parseNumber(nominal))/parseNumber(nominal)*100.0
                val p3=sqrt(3.0)*vavg*iavg*parseNumber(fp)/1000.0
                result = listOf(
                    "Tensão média: ${fmt(vavg,1)} V; desvio médio: ${fmt(dev,2)}% (${if(dev<=parseNumber(vLimit)) "OK pelo limite informado" else "ALERTA"})",
                    "Corrente média: ${fmt(iavg,1)} A",
                    "Desequilíbrio simplificado de tensão: ${fmt(unb,2)}% (${if(unb<=parseNumber(unbLimit)) "OK pelo limite informado" else "ALERTA"})",
                    "Potência ativa trifásica aproximada: ${fmt(p3,1)} kW",
                    "Fator de potência informado: ${fmt(parseNumber(fp),3)}",
                    "Frequência informada: ${fmt(parseNumber(freq),2)} Hz",
                    "DHT-V: ${fmt(parseNumber(thdv),2)}% (${if(parseNumber(thdv)<=parseNumber(thdvLimit)) "OK pelo limite informado" else "ALERTA"})",
                    "DHT-I: ${fmt(parseNumber(thdi),2)}% (interpretar conforme corrente de referência e critérios aplicáveis)",
                    "Esta tela é análise preliminar. Para relatório de qualidade de energia, utilizar séries temporais, eventos, harmônicos por ordem, VTCD e limites normativos aplicáveis ao ponto de medição."
                )
            }.onFailure { result = listOf("Revise os valores informados.") }
        }, modifier = Modifier.fillMaxWidth()) { Text("ANALISAR") }
        ResultAndReport("Análise de Energia", result)
    }
}

@Composable
private fun ModuleScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← Voltar ao painel") }
        BrandHeader()
        Text(title, style = MaterialTheme.typography.headlineSmall, color = NortechBlue, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun ResultAndReport(title: String, result: List<String>) {
    val context = LocalContext.current
    if (result.isNotEmpty()) {
        SectionCard("Resultado") { result.forEach { Text(it) } }
        if (result.none { it.startsWith("Revise") }) {
            Button(onClick = { sharePdf(context, createTextReport(context, title, result)) }, modifier = Modifier.fillMaxWidth()) { Text("GERAR / COMPARTILHAR PDF") }
        }
    }
}

private fun ampacitySection(current: Double, copper: Boolean): Double {
    val approx = if (copper) listOf(1.5 to 15.0,2.5 to 21.0,4.0 to 28.0,6.0 to 36.0,10.0 to 50.0,16.0 to 68.0,25.0 to 89.0,35.0 to 110.0,50.0 to 134.0,70.0 to 171.0,95.0 to 207.0,120.0 to 239.0,150.0 to 272.0,185.0 to 310.0,240.0 to 364.0,300.0 to 419.0)
    else listOf(2.5 to 16.0,4.0 to 22.0,6.0 to 28.0,10.0 to 39.0,16.0 to 53.0,25.0 to 70.0,35.0 to 86.0,50.0 to 104.0,70.0 to 133.0,95.0 to 161.0,120.0 to 186.0,150.0 to 211.0,185.0 to 241.0,240.0 to 283.0,300.0 to 326.0)
    return approx.firstOrNull { it.second >= current*1.05 }?.first ?: approx.last().first
}

private fun nextBreaker(v: Double): Int {
    val values=listOf(6,10,16,20,25,32,40,50,63,80,100,125,160,200,250,315,400,500,630,800,1000,1250,1600,2000)
    return values.firstOrNull { it>=v } ?: ceil(v/100.0).toInt()*100
}
