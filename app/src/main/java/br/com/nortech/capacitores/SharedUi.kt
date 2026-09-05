package br.com.nortech.capacitores

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val NortechBlue = Color(0xFF123D74)
val NortechNavy = Color(0xFF071C2F)
val NortechOrange = Color(0xFFF28C00)
val AppBackground = Color(0xFFEAF0F6)
val AppSurface = Color(0xFFF7F9FC)

val NortechScheme = lightColorScheme(
    primary = NortechOrange,
    onPrimary = Color.White,
    secondary = NortechBlue,
    onSecondary = Color.White,
    background = AppBackground,
    surface = AppSurface,
    surfaceVariant = Color(0xFFDDE7F1),
    onSurface = Color(0xFF172433),
    outline = Color(0xFFB7C6D6)
)

val NortechTypography = Typography(
    displaySmall = TextStyle(FontFamily.SansSerif, FontWeight.ExtraBold, 30.sp, 36.sp),
    headlineLarge = TextStyle(FontFamily.SansSerif, FontWeight.ExtraBold, 28.sp, 34.sp),
    headlineMedium = TextStyle(FontFamily.SansSerif, FontWeight.Bold, 24.sp, 30.sp),
    headlineSmall = TextStyle(FontFamily.SansSerif, FontWeight.Bold, 21.sp, 27.sp),
    titleLarge = TextStyle(FontFamily.SansSerif, FontWeight.Bold, 19.sp, 25.sp),
    titleMedium = TextStyle(FontFamily.SansSerif, FontWeight.SemiBold, 17.sp, 23.sp),
    titleSmall = TextStyle(FontFamily.SansSerif, FontWeight.SemiBold, 15.sp, 21.sp),
    bodyLarge = TextStyle(FontFamily.SansSerif, FontWeight.Normal, 16.sp, 23.sp),
    bodyMedium = TextStyle(FontFamily.SansSerif, FontWeight.Normal, 15.sp, 21.sp),
    bodySmall = TextStyle(FontFamily.SansSerif, FontWeight.Normal, 13.sp, 18.sp),
    labelLarge = TextStyle(FontFamily.SansSerif, FontWeight.Bold, 15.sp, 20.sp, 0.2.sp),
    labelMedium = TextStyle(FontFamily.SansSerif, FontWeight.SemiBold, 13.sp, 18.sp)
)

const val APP_NAME = "KP Electrical Tools"
const val APP_SUBTITLE = "by NORTECH"
const val APP_SLOGAN = "Precisão elétrica para decisões melhores."
const val APP_STATUS = "VERSÃO BETA v1.0"
const val WHATSAPP = "91 99181-5138"
const val WHATSAPP_URL = "https://wa.me/5591991815138"

fun parseNumber(s: String): Double {
    val clean = s.trim().replace("R$", "").replace(" ", "")
    return if (clean.contains(',')) clean.replace(".", "").replace(',', '.').toDouble() else clean.toDouble()
}
fun fmt(v: Double, decimals: Int = 2): String = "% .${decimals}f".format(Locale("pt", "BR"), v).trim()

fun loadLogoBitmap(context: Context): Bitmap? = try {
    val encoded = context.resources.openRawResource(R.raw.nortech_logo_b64).bufferedReader().use { it.readText().trim() }
    val bytes = Base64.decode(encoded, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
} catch (_: Exception) { null }

@Composable
fun BrandHeader(compact: Boolean = false) {
    val context = LocalContext.current
    val logo = remember { loadLogoBitmap(context) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        if (logo != null) {
            Image(
                bitmap = logo.asImageBitmap(),
                contentDescription = "Logo oficial NORTECH",
                modifier = Modifier.fillMaxWidth().height(if (compact) 76.dp else 104.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Text("NORTECH", color = NortechBlue, style = MaterialTheme.typography.headlineLarge)
        }
    }
}

@Composable
fun NumField(label: String, value: String, change: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = change,
        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        textStyle = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

@Composable
fun TextFieldN(label: String, value: String, change: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = change,
        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        textStyle = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth(), singleLine = true
    )
}

@Composable
fun ResultLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(10.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, color = NortechBlue)
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = AppSurface)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(title, color = NortechBlue, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

fun createTextReport(context: Context, title: String, lines: List<String>): Uri {
    val doc = PdfDocument()
    var pageNo = 1
    var page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNo).create())
    var canvas = page.canvas
    val blue = Paint().apply { color = android.graphics.Color.rgb(18,61,116); textSize = 17f; typeface = Typeface.create("sans-serif", Typeface.BOLD) }
    val orange = Paint().apply { color = android.graphics.Color.rgb(242,140,0); textSize = 9.5f; typeface = Typeface.create("sans-serif", Typeface.BOLD) }
    val body = Paint().apply { color = android.graphics.Color.rgb(45,55,65); textSize = 10.2f; typeface = Typeface.create("sans-serif", Typeface.NORMAL) }
    val small = Paint(body).apply { textSize = 8.2f }
    val linePaint = Paint().apply { color = android.graphics.Color.rgb(190,200,212); style = Paint.Style.STROKE; strokeWidth = 0.8f }
    val beta = Paint().apply {
        color = android.graphics.Color.rgb(150, 158, 168); alpha = 42; textSize = 48f
        typeface = Typeface.create("sans-serif", Typeface.BOLD); textAlign = Paint.Align.CENTER
    }

    fun watermark() {
        canvas.save(); canvas.rotate(-32f, 297.5f, 421f); canvas.drawText(APP_STATUS, 297.5f, 421f, beta); canvas.restore()
    }
    fun header(): Float {
        watermark()
        canvas.drawRect(28f, 22f, 567f, 818f, linePaint)
        var y = 34f
        loadLogoBitmap(context)?.let { logo ->
            val w = 145f; val h = w * logo.height.toFloat() / logo.width.toFloat()
            canvas.drawBitmap(logo, null, RectF(38f, y, 38f + w, y + h), null); y += h + 10f
        }
        canvas.drawText(APP_NAME, 390f, 48f, blue)
        canvas.drawText(APP_SUBTITLE, 450f, 63f, orange)
        canvas.drawLine(38f, y, 557f, y, linePaint); y += 22f
        canvas.drawText(title.uppercase(Locale("pt", "BR")), 38f, y, blue); y += 21f
        val date = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR")).format(Date())
        canvas.drawText("Data e hora de emissão: $date", 38f, y, small)
        canvas.drawText(APP_STATUS, 474f, y, orange)
        return y + 20f
    }
    fun footer() {
        canvas.drawLine(38f, 792f, 557f, 792f, linePaint)
        canvas.drawText("$APP_NAME | NORTECH | $APP_STATUS", 38f, 807f, orange)
        canvas.drawText("Desenvolvido por Ezequiel Paixão | WhatsApp $WHATSAPP", 38f, 819f, small)
        canvas.drawText("Pág. $pageNo", 520f, 819f, small)
    }
    var y = header()
    for (raw in lines) {
        val isSection = raw.endsWith(":") && raw.length < 55
        val chunks = wrapText(raw, 82)
        for (line in chunks) {
            if (y > 775f) {
                footer(); doc.finishPage(page); pageNo++
                page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNo).create()); canvas = page.canvas; y = header()
            }
            if (isSection) {
                canvas.drawRect(38f, y - 12f, 557f, y + 5f, Paint().apply { color = android.graphics.Color.rgb(235,241,247) })
                canvas.drawText(line, 43f, y, Paint(blue).apply { textSize = 10.5f })
            } else canvas.drawText(line, 43f, y, body)
            y += 15f
        }
        y += 3f
    }
    footer(); doc.finishPage(page)
    val dir = File(context.cacheDir, "reports").apply { mkdirs() }
    val file = File(dir, "KP_ET_${title.replace(Regex("[^A-Za-z0-9]+"), "_")}_${System.currentTimeMillis()}.pdf")
    FileOutputStream(file).use { doc.writeTo(it) }; doc.close()
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun wrapText(text: String, max: Int): List<String> {
    if (text.length <= max) return listOf(text)
    val out = mutableListOf<String>(); var rest = text
    while (rest.length > max) {
        var cut = rest.lastIndexOf(' ', max); if (cut <= 0) cut = max
        out += rest.substring(0, cut); rest = rest.substring(cut).trimStart()
    }
    if (rest.isNotEmpty()) out += rest
    return out
}

fun sharePdf(context: Context, uri: Uri, chooserTitle: String = "Compartilhar relatório KP Electrical Tools") {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}
