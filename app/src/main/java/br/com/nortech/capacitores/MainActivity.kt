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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val previous = prefs.getInt(PREF_ACCESS_COUNT, 0)
        val locked = previous >= MAX_BETA_ACCESSES
        val used = if (locked) previous else previous + 1
        if (!locked) prefs.edit().putInt(PREF_ACCESS_COUNT, used).apply()
        val remaining = (MAX_BETA_ACCESSES - used).coerceAtLeast(0)

        setContent {
            MaterialTheme(colorScheme = NortechScheme, typography = NortechTypography) {
                Surface(Modifier.fillMaxSize(), color = AppBackground) {
                    if (locked) BetaLockedScreen() else KpElectricalToolsApp(remaining)
                }
            }
        }
    }
}

enum class Screen { HOME, CAPACITOR, TRANSFORMER, CABLE, DROP, DEMAND, GENERATOR, INVOICE, ENERGY }

data class CalcResult(val qc:Double,val commercial:Double,val current:Double,val breaker:Int,val ct:Int,val fixed:Double?,val tuning:Double?,val stages:List<Double>)

@Composable
fun KpElectricalToolsApp(remainingAccesses: Int) {
    var showSplash by remember { mutableStateOf(true) }
    if (showSplash) {
        LaunchedEffect(Unit) { delay(2200); showSplash = false }
        BetaSplashScreen(remainingAccesses)
        return
    }

    var screen by remember { mutableStateOf(Screen.HOME) }
    when(screen){
        Screen.HOME -> HomeScreen(remainingAccesses) { screen=it }
        Screen.CAPACITOR -> CapacitorScreen { screen=Screen.HOME }
        Screen.TRANSFORMER -> TransformerScreenV9 { screen=Screen.HOME }
        Screen.CABLE -> CableScreen { screen=Screen.HOME }
        Screen.DROP -> VoltageDropScreen { screen=Screen.HOME }
        Screen.DEMAND -> DemandScreen { screen=Screen.HOME }
        Screen.GENERATOR -> GeneratorScreen { screen=Screen.HOME }
        Screen.INVOICE -> InvoiceAnalysisScreen { screen=Screen.HOME }
        Screen.ENERGY -> EnergyAnalysisScreen { screen=Screen.HOME }
    }
}

@Composable
private fun BetaSplashScreen(remainingAccesses: Int) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF061A2D), Color(0xFF0D3557), Color(0xFF071C2F)))
        ).statusBarsPadding().navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(color = Color.White.copy(alpha = 0.96f), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    BrandHeader()
                    Text(APP_NAME, style = MaterialTheme.typography.headlineLarge, color = NortechNavy, textAlign = TextAlign.Center)
                    Text(APP_SUBTITLE, style = MaterialTheme.typography.titleMedium, color = NortechBlue)
                    Spacer(Modifier.height(8.dp))
                    Text(APP_SLOGAN, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF455565), textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(22.dp))
            Surface(color = NortechOrange, shape = RoundedCornerShape(50)) {
                Text(APP_STATUS, modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp), color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                if (remainingAccesses == 0) "Último acesso liberado nesta versão BETA" else "Acessos restantes: $remainingAccesses de $MAX_BETA_ACCESSES",
                color = Color.White, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BetaLockedScreen() {
    val context = LocalContext.current
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF061A2D), Color(0xFF0D3557), Color(0xFF071C2F))))
            .statusBarsPadding().navigationBarsPadding()
    ) {
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(color = Color.White, shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    BrandHeader(compact = true)
                    Text(APP_NAME, style = MaterialTheme.typography.headlineMedium, color = NortechNavy, textAlign = TextAlign.Center)
                    Text("Versão BETA encerrada", style = MaterialTheme.typography.titleLarge, color = NortechOrange)
                    Text(
                        "Este período de testes atingiu o limite de $MAX_BETA_ACCESSES acessos. Entre em contato com a NORTECH para liberação.",
                        style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(WHATSAPP_URL))) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("FALAR COM A NORTECH") }
                    OutlinedButton(
                        onClick = { (context as? ComponentActivity)?.finish() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("FECHAR APLICATIVO") }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(remainingAccesses: Int, open:(Screen)->Unit){
    val context=LocalContext.current
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF071C2F), Color(0xFF0B2A46), Color(0xFFEAF0F6)))
        )
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(18.dp),
            verticalArrangement=Arrangement.spacedBy(12.dp)
        ) {
            Surface(color = Color.White.copy(alpha = 0.97f), shape = RoundedCornerShape(22.dp), tonalElevation = 6.dp) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    BrandHeader(compact = true)
                    Text(APP_NAME, style=MaterialTheme.typography.headlineMedium, color=NortechNavy, textAlign = TextAlign.Center)
                    Text(APP_SUBTITLE, style = MaterialTheme.typography.titleSmall, color=NortechBlue)
                    Text(APP_SLOGAN, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF566575), textAlign = TextAlign.Center)
                }
            }

            Surface(color = NortechOrange.copy(alpha = 0.96f), shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("BETA v1.0", color=Color.White, style=MaterialTheme.typography.titleSmall)
                    Text(
                        if (remainingAccesses == 0) "Último acesso" else "$remainingAccesses acessos restantes",
                        color=Color.White, style=MaterialTheme.typography.titleSmall
                    )
                }
            }

            Text("Módulos técnicos", color=Color.White, style=MaterialTheme.typography.titleLarge)
            menu("⚡", "Banco de Capacitores","Correção de FP, estágios, proteção e relatório"){open(Screen.CAPACITOR)}
            menu("🔌", "Transformadores","Correntes, carregamento e capacidade disponível"){open(Screen.TRANSFORMER)}
            menu("🧵", "Cabos","Seção, corrente, queda e proteção preliminar"){open(Screen.CABLE)}
            menu("📉", "Queda de Tensão","Cálculo em volts e percentual"){open(Screen.DROP)}
            menu("📊", "Demanda","kW, kVA, corrente e transformador sugerido"){open(Screen.DEMAND)}
            menu("⚙️", "Geradores","Carga, reserva e partida de motores"){open(Screen.GENERATOR)}
            menu("🧾", "Análise de Faturas","Upload de PDFs/imagens, OCR e relatório"){open(Screen.INVOICE)}
            menu("📈", "Análise de Energia","Dados elétricos, arquivos e relatório técnico"){open(Screen.ENERGY)}

            Surface(color = Color.White.copy(alpha = 0.95f), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Suporte", color=NortechNavy, style=MaterialTheme.typography.titleMedium)
                    OutlinedButton(
                        onClick={context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(WHATSAPP_URL)))},
                        modifier=Modifier.fillMaxWidth()
                    ){Text("WhatsApp NORTECH • $WHATSAPP")}
                    Text("Desenvolvido por Ezequiel Paixão",color=NortechBlue,style = MaterialTheme.typography.titleSmall)
                    OutlinedButton(
                        onClick = { (context as? ComponentActivity)?.finish() },
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) { Text("SAIR DO APLICATIVO") }
                }
            }
        }
    }
}

@Composable
private fun menu(icon:String, title:String, subtitle:String, action:()->Unit){
    ElevatedCard(
        modifier=Modifier.fillMaxWidth().clickable(onClick = action),
        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFF7F9FC)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = NortechBlue.copy(alpha = 0.10f), shape = RoundedCornerShape(14.dp)) {
                Text(icon, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color=NortechNavy)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF5A6876))
            }
            Text("›", color = NortechOrange, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun ModuleBackButton(onBack:()->Unit) {
    OutlinedButton(onClick=onBack, modifier = Modifier.height(48.dp)) { Text("←  VOLTAR") }
}

@Composable
private fun CapacitorScreen(onBack:()->Unit){
    val context=LocalContext.current
    var client by remember{mutableStateOf("")};var installation by remember{mutableStateOf("")};var power by remember{mutableStateOf("500")};var fp1 by remember{mutableStateOf("0,80")};var fp2 by remember{mutableStateOf("0,98")};var voltage by remember{mutableStateOf("380")};var channels by remember{mutableStateOf(12)};var controller by remember{mutableStateOf("Trifásico")};var detuned by remember{mutableStateOf(false)};var reactor by remember{mutableStateOf("7")};var transformer by remember{mutableStateOf("750")};var fixedPct by remember{mutableStateOf("2")};var result by remember{mutableStateOf<CalcResult?>(null)};var error by remember{mutableStateOf("")}
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(18.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)
    ){
        ModuleBackButton(onBack)
        BrandHeader(compact = true)
        Text("Banco de Capacitores",style=MaterialTheme.typography.headlineMedium,color=NortechNavy)
        TextFieldN("Cliente",client){client=it};TextFieldN("Instalação / QGBT / Transformador",installation){installation=it};NumField("Potência ativa (kW)",power){power=it};NumField("FP atual",fp1){fp1=it};NumField("FP desejado",fp2){fp2=it};NumField("Tensão trifásica (V)",voltage){voltage=it}
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(controller=="Monofásico",{controller="Monofásico"},{Text("Monofásico")});FilterChip(controller=="Trifásico",{controller="Trifásico"},{Text("Trifásico")})}
        Text("Canais",style = MaterialTheme.typography.titleSmall);Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){listOf(6,8,12,16,24).forEach{n->FilterChip(channels==n,{channels=n},{Text("$n")})}}
        Row(verticalAlignment = Alignment.CenterVertically){Checkbox(detuned,{detuned=it});Text("Banco dessintonizado / reatores", style = MaterialTheme.typography.bodyMedium)};if(detuned)NumField("Reator (%)",reactor){reactor=it};NumField("Transformador (kVA)",transformer){transformer=it};NumField("Capacitor fixo (%)",fixedPct){fixedPct=it}
        Button(onClick={runCatching{val p=parseNumber(power);val a=parseNumber(fp1);val b=parseNumber(fp2);val v=parseNumber(voltage);require(b>a&&a in 0.01..1.0&&b<=1.0);val qc=p*(tan(acos(a))-tan(acos(b)));val commercial=ceil(qc/5.0)*5.0;val current=commercial*1000/(sqrt(3.0)*v);val breaker=nextStd(current*1.5);val loadCurrent=p*1000/(sqrt(3.0)*v*a);val ct=nextStd(loadCurrent*1.1);val fixed=transformer.takeIf{it.isNotBlank()}?.let{parseNumber(it)*parseNumber(fixedPct)/100};val tuning=if(detuned)60/sqrt(parseNumber(reactor)/100) else null;val units=round(commercial/5).toInt();val base=units/channels;val rem=units%channels;val stages=(0 until channels).map{(base+if(it<rem)1 else 0)*5.0}.filter{it>0};result=CalcResult(qc,commercial,current,breaker,ct,fixed,tuning,stages);error=""}.onFailure{error="Revise os valores informados."}},modifier=Modifier.fillMaxWidth().height(52.dp)){Text("CALCULAR")}
        if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        result?.let{r->SectionCard("Resultado"){ResultLine("Banco calculado","${fmt(r.qc)} kvar");ResultLine("Banco comercial","${fmt(r.commercial,0)} kvar");ResultLine("Corrente","${fmt(r.current,1)} A");ResultLine("Proteção preliminar","${r.breaker} A");ResultLine("TC preliminar","${r.ct}/5 A");r.fixed?.let{ResultLine("Capacitor fixo","${fmt(it,1)} kvar")};r.tuning?.let{ResultLine("Frequência de sintonia","${fmt(it,1)} Hz")};Text("Estágios: "+r.stages.mapIndexed{i,k->"E${i+1} ${fmt(k,0)} kvar"}.joinToString(" | "), style = MaterialTheme.typography.bodyMedium)};Button(onClick={val lines=buildList{add("IDENTIFICAÇÃO:");add("Cliente: $client");add("Instalação: $installation");add("DADOS DE ENTRADA:");add("Potência: $power kW; FP $fp1 → $fp2; tensão $voltage V");add("RESULTADOS:");add("Banco calculado: ${fmt(r.qc)} kvar");add("Banco comercial: ${fmt(r.commercial,0)} kvar");add("Corrente: ${fmt(r.current,1)} A; proteção: ${r.breaker} A; TC: ${r.ct}/5 A");add("Estágios: "+r.stages.mapIndexed{i,k->"E${i+1} ${fmt(k,0)} kvar"}.joinToString(" | "));add("RECOMENDAÇÕES:");add("Pré-dimensionamento: validar curto-circuito, seletividade, fabricante, temperatura e harmônicos.")};sharePdf(context,createTextReport(context,"Banco de Capacitores",lines))},modifier=Modifier.fillMaxWidth().height(52.dp)){Text("GERAR / COMPARTILHAR PDF")}}
    }
}

private fun nextStd(v:Double):Int{val values=listOf(6,10,16,20,25,32,40,50,63,80,100,125,160,200,250,315,400,500,630,800,1000,1250,1600,2000);return values.firstOrNull{it>=v}?:ceil(v/100).toInt()*100}
