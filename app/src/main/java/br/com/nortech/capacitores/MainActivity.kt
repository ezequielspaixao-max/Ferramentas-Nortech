package br.com.nortech.capacitores

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = NortechScheme, typography = NortechTypography) {
                Surface(Modifier.fillMaxSize()) { NortechApp() }
            }
        }
    }
}

enum class Screen { HOME, CAPACITOR, TRANSFORMER, CABLE, DROP, DEMAND, GENERATOR, INVOICE, ENERGY }

data class CalcResult(val qc:Double,val commercial:Double,val current:Double,val breaker:Int,val ct:Int,val fixed:Double?,val tuning:Double?,val stages:List<Double>)

@Composable
fun NortechApp() {
    var showSplash by remember { mutableStateOf(true) }
    if (showSplash) {
        LaunchedEffect(Unit) {
            delay(2200)
            showSplash = false
        }
        BetaSplashScreen()
        return
    }

    var screen by remember { mutableStateOf(Screen.HOME) }
    when(screen){
        Screen.HOME -> HomeScreen { screen=it }
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
private fun BetaSplashScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BrandHeader()
        Spacer(Modifier.height(18.dp))
        Text(
            "NORTECH FERRAMENTAS ELÉTRICAS",
            style = MaterialTheme.typography.headlineMedium,
            color = NortechBlue,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Surface(color = NortechOrange, shape = MaterialTheme.shapes.large) {
            Text(
                "VERSÃO BETA",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 11.dp),
                color = androidx.compose.ui.graphics.Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Versão em validação técnica. Confira os resultados antes do uso em projeto executivo.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HomeScreen(open:(Screen)->Unit){
    val context=LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement=Arrangement.spacedBy(14.dp)){
        BrandHeader()
        Text("Ferramentas Elétricas • v10 BETA", style=MaterialTheme.typography.headlineMedium, color=NortechBlue)
        Text("Suite técnica NORTECH para cálculos, análise de faturas e relatórios de campo.", style = MaterialTheme.typography.bodyLarge)
        Surface(color = NortechOrange.copy(alpha = 0.10f), shape = MaterialTheme.shapes.medium) {
            Text("VERSÃO BETA • uso em validação técnica", modifier = Modifier.fillMaxWidth().padding(12.dp), color = NortechOrange, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
        }
        Button(onClick={context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(WHATSAPP_URL)))},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=NortechOrange)){Text("WhatsApp: $WHATSAPP", style = MaterialTheme.typography.labelLarge)}
        menu("⚡ Banco de Capacitores","Correção de FP, estágios, proteção e PDF"){open(Screen.CAPACITOR)}
        menu("🔌 Transformadores","Correntes, carregamento e capacidade disponível"){open(Screen.TRANSFORMER)}
        menu("🧵 Cabos","Seção, corrente, queda e disjuntor preliminar"){open(Screen.CABLE)}
        menu("📉 Queda de Tensão","Cálculo em V e %"){open(Screen.DROP)}
        menu("📊 Demanda","kW, kVA, corrente e transformador sugerido"){open(Screen.DEMAND)}
        menu("⚙️ Geradores","Carga, reserva e partida de motores"){open(Screen.GENERATOR)}
        menu("🧾 Análise de Faturas","Upload de PDFs/imagens, OCR e relatório"){open(Screen.INVOICE)}
        menu("📈 Análise de Energia","Tensão, corrente, FP, frequência e DHT"){open(Screen.ENERGY)}
        HorizontalDivider()
        Text("Desenvolvido por Ezequiel Paixão",color=NortechBlue,style = MaterialTheme.typography.titleSmall)
        OutlinedButton(
            onClick = { (context as? ComponentActivity)?.finish() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("SAIR DO APLICATIVO", style = MaterialTheme.typography.labelLarge) }
    }
}

@Composable
private fun menu(t:String,s:String,a:()->Unit){
    ElevatedCard(Modifier.fillMaxWidth()){
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text(t, style = MaterialTheme.typography.titleLarge, color=NortechBlue)
            Text(s, style = MaterialTheme.typography.bodyMedium)
            Button(onClick=a,modifier=Modifier.fillMaxWidth()){Text("ABRIR MÓDULO", style = MaterialTheme.typography.labelLarge)}
        }
    }
}

@Composable
private fun CapacitorScreen(onBack:()->Unit){
    val context=LocalContext.current
    var client by remember{mutableStateOf("")};var installation by remember{mutableStateOf("")};var power by remember{mutableStateOf("500")};var fp1 by remember{mutableStateOf("0,80")};var fp2 by remember{mutableStateOf("0,98")};var voltage by remember{mutableStateOf("380")};var channels by remember{mutableStateOf(12)};var controller by remember{mutableStateOf("Trifásico")};var detuned by remember{mutableStateOf(false)};var reactor by remember{mutableStateOf("7")};var transformer by remember{mutableStateOf("750")};var fixedPct by remember{mutableStateOf("2")};var result by remember{mutableStateOf<CalcResult?>(null)};var error by remember{mutableStateOf("")}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        OutlinedButton(onClick=onBack, modifier = Modifier.fillMaxWidth()){Text("VOLTAR AO PAINEL", style = MaterialTheme.typography.labelLarge)}
        BrandHeader();Text("Banco de Capacitores",style=MaterialTheme.typography.headlineMedium,color=NortechBlue)
        TextFieldN("Cliente",client){client=it};TextFieldN("Instalação / QGBT / Transformador",installation){installation=it};NumField("Potência ativa (kW)",power){power=it};NumField("FP atual",fp1){fp1=it};NumField("FP desejado",fp2){fp2=it};NumField("Tensão trifásica (V)",voltage){voltage=it}
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(controller=="Monofásico",{controller="Monofásico"},{Text("Monofásico")});FilterChip(controller=="Trifásico",{controller="Trifásico"},{Text("Trifásico")})}
        Text("Canais",style = MaterialTheme.typography.titleSmall);Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){listOf(6,8,12,16,24).forEach{n->FilterChip(channels==n,{channels=n},{Text("$n")})}}
        Row(verticalAlignment = Alignment.CenterVertically){Checkbox(detuned,{detuned=it});Text("Banco dessintonizado / reatores", style = MaterialTheme.typography.bodyMedium)};if(detuned)NumField("Reator (%)",reactor){reactor=it};NumField("Transformador (kVA)",transformer){transformer=it};NumField("Capacitor fixo (%)",fixedPct){fixedPct=it}
        Button(onClick={runCatching{val p=parseNumber(power);val a=parseNumber(fp1);val b=parseNumber(fp2);val v=parseNumber(voltage);require(b>a&&a in 0.01..1.0&&b<=1.0);val qc=p*(tan(acos(a))-tan(acos(b)));val commercial=ceil(qc/5.0)*5.0;val current=commercial*1000/(sqrt(3.0)*v);val breaker=nextStd(current*1.5);val loadCurrent=p*1000/(sqrt(3.0)*v*a);val ct=nextStd(loadCurrent*1.1);val fixed=transformer.takeIf{it.isNotBlank()}?.let{parseNumber(it)*parseNumber(fixedPct)/100};val tuning=if(detuned)60/sqrt(parseNumber(reactor)/100) else null;val units=round(commercial/5).toInt();val base=units/channels;val rem=units%channels;val stages=(0 until channels).map{(base+if(it<rem)1 else 0)*5.0}.filter{it>0};result=CalcResult(qc,commercial,current,breaker,ct,fixed,tuning,stages);error=""}.onFailure{error="Revise os valores informados."}},modifier=Modifier.fillMaxWidth()){Text("CALCULAR", style = MaterialTheme.typography.labelLarge)}
        if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        result?.let{r->SectionCard("Resultado"){ResultLine("Banco calculado","${fmt(r.qc)} kvar");ResultLine("Banco comercial","${fmt(r.commercial,0)} kvar");ResultLine("Corrente","${fmt(r.current,1)} A");ResultLine("Proteção preliminar","${r.breaker} A");ResultLine("TC preliminar","${r.ct}/5 A");r.fixed?.let{ResultLine("Capacitor fixo","${fmt(it,1)} kvar")};r.tuning?.let{ResultLine("Frequência de sintonia","${fmt(it,1)} Hz")};Text("Estágios: "+r.stages.mapIndexed{i,k->"E${i+1} ${fmt(k,0)} kvar"}.joinToString(" | "), style = MaterialTheme.typography.bodyMedium)};Button(onClick={val lines=buildList{add("Cliente: $client");add("Instalação: $installation");add("Potência: $power kW; FP $fp1 → $fp2; tensão $voltage V");add("Banco calculado: ${fmt(r.qc)} kvar");add("Banco comercial: ${fmt(r.commercial,0)} kvar");add("Corrente: ${fmt(r.current,1)} A; proteção: ${r.breaker} A; TC: ${r.ct}/5 A");add("Estágios: "+r.stages.mapIndexed{i,k->"E${i+1} ${fmt(k,0)} kvar"}.joinToString(" | "));add("Pré-dimensionamento: validar curto-circuito, seletividade, fabricante, temperatura e harmônicos.")};sharePdf(context,createTextReport(context,"Banco de Capacitores",lines))},modifier=Modifier.fillMaxWidth()){Text("GERAR / COMPARTILHAR PDF", style = MaterialTheme.typography.labelLarge)}}
    }
}

private fun nextStd(v:Double):Int{val values=listOf(6,10,16,20,25,32,40,50,63,80,100,125,160,200,250,315,400,500,630,800,1000,1250,1600,2000);return values.firstOrNull{it>=v}?:ceil(v/100).toInt()*100}
