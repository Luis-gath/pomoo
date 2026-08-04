package com.example.pomodoro.features.tasks.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.pomodoro.features.tasks.domain.ImportedStudyPlan
import com.example.pomodoro.features.tasks.domain.StudyPlanCodeCodec
import com.example.pomodoro.features.tasks.domain.StudyScheduleShareFormatter
import com.example.pomodoro.features.tasks.domain.StudyHabitPlan
import com.example.pomodoro.features.tasks.domain.WeeklyScheduleTemplate
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class StudyPlanShareContent(
    val title: String,
    val summary: String,
    val code: String
)

@Composable
fun StudyPlanShareDialog(
    content: StudyPlanShareContent,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var qrBitmap by remember(content.code) { mutableStateOf<Bitmap?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(content.code) {
        qrBitmap = withContext(Dispatchers.Default) { createQrBitmap(content.code, 720) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Compartir plan", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "La otra persona puede escanear este QR desde Pomodoro o pegar el código.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = androidx.compose.ui.graphics.Color.White
                ) {
                    val bitmap = qrBitmap
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Código QR del plan de estudio",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .padding(14.dp)
                        )
                    } else {
                        Spacer(Modifier.fillMaxWidth().aspectRatio(1f))
                    }
                }

                Text(content.title, style = MaterialTheme.typography.titleSmall)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = content.code,
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            copyPlanCode(context, content.code)
                            message = "Código copiado"
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Copiar")
                    }
                    OutlinedButton(
                        onClick = {
                            message = sharePlanCode(context, content)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Código")
                    }
                }

                Button(
                    onClick = {
                        message = qrBitmap?.let { sharePlanQr(context, content, it) }
                            ?: "El QR todavía se está preparando"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Compartir QR")
                }

                message?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Listo") } }
    )
}

@Composable
fun StudyPlanImportDialog(
    onDismiss: () -> Unit,
    onApplySchedule: (WeeklyScheduleTemplate, Int, String, Boolean) -> Unit,
    onApplyHabit: (StudyHabitPlan) -> Unit
) {
    val context = LocalContext.current
    var code by remember { mutableStateOf("") }
    var imported by remember { mutableStateOf<ImportedStudyPlan?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun parse(candidate: String) {
        code = candidate
        StudyPlanCodeCodec.decode(candidate)
            .onSuccess {
                imported = it
                error = null
            }
            .onFailure {
                imported = null
                error = it.message ?: "No se pudo leer el plan"
            }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Importar plan", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Escanea el QR desde otro teléfono o pega el código que recibiste. Nada se crea sin tu confirmación.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        startPlanQrScanner(
                            context = context,
                            onResult = ::parse,
                            onError = { error = it }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Escanear QR")
                }

                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it
                        imported = null
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Código del plan") },
                    minLines = 2,
                    maxLines = 4,
                    textStyle = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val pasted = clipboard.primaryClip
                                ?.getItemAt(0)
                                ?.coerceToText(context)
                                ?.toString()
                                .orEmpty()
                            if (pasted.isBlank()) error = "El portapapeles está vacío" else parse(pasted)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Pegar")
                    }
                    OutlinedButton(
                        onClick = { parse(code) },
                        enabled = code.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Revisar")
                    }
                }

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                imported?.let { plan -> ImportedPlanPreview(plan) }
            }
        },
        confirmButton = {
            val plan = imported
            Button(
                onClick = {
                    when (plan) {
                        is ImportedStudyPlan.Weekly -> onApplySchedule(
                            plan.template,
                            plan.weeks,
                            plan.courseOrProject,
                            plan.withReminders
                        )
                        is ImportedStudyPlan.Habit -> onApplyHabit(plan.plan)
                        null -> Unit
                    }
                },
                enabled = plan != null
            ) {
                Text(if (plan is ImportedStudyPlan.Habit) "Crear hábito" else "Aplicar horario")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun ImportedPlanPreview(plan: ImportedStudyPlan) {
    val (title, summary) = when (plan) {
        is ImportedStudyPlan.Weekly -> "Horario recibido" to StudyScheduleShareFormatter.weekly(
            plan.template,
            plan.weeks,
            plan.courseOrProject,
            plan.withReminders
        )
        is ImportedStudyPlan.Habit -> "Hábito recibido" to StudyScheduleShareFormatter.habit(plan.plan)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                summary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 9,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun createQrBitmap(value: String, size: Int): Bitmap {
    val matrix = QRCodeWriter().encode(
        value,
        BarcodeFormat.QR_CODE,
        size,
        size,
        mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1
        )
    )
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        for (x in 0 until size) {
            pixels[y * size + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
        }
    }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}

private fun copyPlanCode(context: Context, code: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Código de horario Pomodoro", code))
}

private fun sharePlanCode(context: Context, content: StudyPlanShareContent): String? = runCatching {
    val text = "${content.summary}\n\nCódigo para importar en Pomodoro:\n${content.code}"
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, content.title)
                putExtra(Intent.EXTRA_TEXT, text)
            },
            "Compartir código del plan"
        )
    )
}.exceptionOrNull()?.let { "No se pudo compartir el código" }

private fun sharePlanQr(
    context: Context,
    content: StudyPlanShareContent,
    bitmap: Bitmap
): String? = runCatching {
    val directory = File(context.cacheDir, "shared_plans").apply { mkdirs() }
    val file = File(directory, "pomodoro-plan.png")
    FileOutputStream(file).use { stream -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val fallbackText = "${content.summary}\n\nCódigo para importar en Pomodoro:\n${content.code}"

    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, fallbackText)
                clipData = ClipData.newUri(context.contentResolver, content.title, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            "Compartir QR del plan"
        )
    )
}.exceptionOrNull()?.let { "No se pudo compartir el QR" }

private fun startPlanQrScanner(
    context: Context,
    onResult: (String) -> Unit,
    onError: (String) -> Unit
) {
    val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .enableAutoZoom()
        .build()

    GmsBarcodeScanning.getClient(context, options)
        .startScan()
        .addOnSuccessListener { barcode ->
            val value = barcode.rawValue
            if (value.isNullOrBlank()) onError("El QR no contiene un plan") else onResult(value)
        }
        .addOnFailureListener { onError("No se pudo abrir el escáner. También puedes pegar el código.") }
}
