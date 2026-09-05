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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val NortechBlue = Color(0xFF123D74)
val NortechOrange = Color(0xFFF28C00)
val NortechScheme = lightColorScheme(
    primary = NortechOrange,
    onPrimary = Color.White,
    secondary = NortechBlue,
    onSecondary = Color.White,
    background = Color.White,
    surface = Color.White,
    surfaceVariant = Color(0xFFF4F7FB),
    outline = Color(0xFFD7DFEA)
)
const val WHATSAPP = "91 99181-5138"
const val WHATSAPP_URL = "https://wa.me/5591991815138"
const val APP_STATUS = "VERSÃO BETA"

fun parseNumber(s: String): Double {
    val clean = s.trim().replace("R$", "").replace(" ", "")
    return if (clean.contains(',')) clean.replace(".", "").replace(',', '.').toDouble() else clean.toDouble()
}
fun fmt(v: Double, decimals: Int = 2): String = "% .${decimals}f".format(Locale("pt", "BR"), v).trim()

fun loadLogoBitmap(context: Context): Bitmap? = try {
    val encoded = context.resources.openRawResource(R.raw.nortech_logo_b64)
        .bufferedReader().use { it.readText().trim() }
    val bytes = Base64.decode(encoded, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
} catch (_: Exception) { null }

@Composable
fun BrandHeader() {
    val context = LocalContext.current
    val logo = remember { loadLogoBitmap(context) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        if (logo != null) {
            Image(
                bitmap = logo.asImageBitmap(),
                contentDescription = "Logo oficial NORTECH",
                modifier = Modifier.fillMaxWidth().height(112.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Text("NORTECH", color = NortechBlue, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineLarge)
            Text("SERVIÇOS E COMÉRCIO LTDA", color = NortechOrange, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun NumField(label: String, value: String, change: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = change,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

@Composable
fun TextFieldN(label: String, value: String, change: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = change, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
}

@Composable
fun ResultLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(10.dp))
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, color = NortechBlue, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

fun createTextReport(context: Context, title: String, lines: List<String>): Uri {
    val doc = PdfDocument()
    var pageNo = 1
    var page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNo).create())
    var canvas = page.canvas
    val blue = Paint().apply { color = android.graphics.Color.rgb(18,61,116); textSize = 17f; typeface = Typeface.DEFAULT_BOLD }
    val orange = Paint().apply { color = android.graphics.Color.rgb(242,140,0); textSize = 10f; typeface = Typeface.DEFAULT_BOLD }
    val body = Paint().apply { color = android.graphics.Color.DKGRAY; textSize = 9.5f }
    val small = Paint(body).apply { textSize = 8f }
    val beta = Paint().apply {
        color = android.graphics.Color.rgb(180, 180, 180)
        alpha = 55
        textSize = 52f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    fun drawBetaWatermark() {
        canvas.save()
        canvas.rotate(-32f, 297.5f, 421f)
        canvas.drawText(APP_STATUS, 297.5f, 421f, beta)
        canvas.restore()
    }

    fun header(): Float {
        drawBetaWatermark()
        var y = 28f
        loadLogoBitmap(context)?.let { logo ->
            val w = 220f
            val h = w * logo.height.toFloat() / logo.width.toFloat()
            canvas.drawBitmap(logo, null, RectF(38f, y, 38f + w, y + h), null)
            y += h + 18f
        }
        canvas.drawText(title, 38f, y, blue)
        y += 22f
        val date = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR")).format(Date())
        canvas.drawText("Emitido em: $date", 38f, y, small)
        canvas.drawText(APP_STATUS, 500f, y, orange)
        return y + 20f
    }
    fun footer() {
        canvas.drawText("NORTECH • $APP_STATUS • Desenvolvido por Ezequiel Paixão • WhatsApp $WHATSAPP", 38f, 810f, orange)
        canvas.drawText("Página $pageNo", 515f, 810f, small)
    }
    var y = header()
    for (raw in lines) {
        val chunks = wrapText(raw, 90)
        for (line in chunks) {
            if (y > 785f) {
                footer(); doc.finishPage(page)
                pageNo++
                page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNo).create())
                canvas = page.canvas
                y = header()
            }
            canvas.drawText(line, 38f, y, body)
            y += 14f
        }
        y += 2f
    }
    footer(); doc.finishPage(page)
    val dir = File(context.cacheDir, "reports").apply { mkdirs() }
    val file = File(dir, "NORTECH_${title.replace(Regex("[^A-Za-z0-9]+"), "_")}_${System.currentTimeMillis()}.pdf")
    FileOutputStream(file).use { doc.writeTo(it) }
    doc.close()
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun wrapText(text: String, max: Int): List<String> {
    if (text.length <= max) return listOf(text)
    val out = mutableListOf<String>()
    var rest = text
    while (rest.length > max) {
        var cut = rest.lastIndexOf(' ', max)
        if (cut <= 0) cut = max
        out += rest.substring(0, cut)
        rest = rest.substring(cut).trimStart()
    }
    if (rest.isNotEmpty()) out += rest
    return out
}

fun sharePdf(context: Context, uri: Uri, chooserTitle: String = "Compartilhar relatório NORTECH") {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}
