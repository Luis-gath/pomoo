package com.example.pomodoro.features.areas.presentation

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pomodoro.features.areas.data.Item
import com.example.pomodoro.features.areas.data.ItemKind
import com.example.pomodoro.features.areas.data.ItemMark
import com.example.pomodoro.shared.ui.components.PickerDateUtils
import com.example.pomodoro.shared.ui.components.PomodoroDatePickerDialog
import com.example.pomodoro.shared.ui.components.PomodoroTimePickerDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreaDetailScreen(
    onBack: () -> Unit,
    viewModel: AreaDetailViewModel = hiltViewModel()
) {
    val area by viewModel.area.collectAsState()
    val allItems by viewModel.allItems.collectAsState()
    val items by viewModel.items.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val message by viewModel.message.collectAsState()

    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var showAddMenu by remember { mutableStateOf(false) }
    var showTextDialog by remember { mutableStateOf<TextEntry?>(null) }
    var noteBeingRead by remember { mutableStateOf<Item?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(actionError) {
        actionError?.let {
            snackbar.showSnackbar(it)
            actionError = null
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    // Archivos sueltos: el selector del sistema concede permiso permanente.
    val filesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> viewModel.addFiles(uris) }

    // Carpeta entera: Android no deja compartir carpetas, pero sí concedernos acceso al árbol.
    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            val children = listFilesIn(context, treeUri)
            viewModel.addFolder(treeUri, children)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(area?.name ?: "Área", fontWeight = FontWeight.Bold)
                        area?.let { current ->
                            Text(
                                text = when (current.type) {
                                    com.example.pomodoro.features.areas.data.AreaType.CURSO -> "Curso"
                                    com.example.pomodoro.features.areas.data.AreaType.HABILIDAD -> "Habilidad"
                                    com.example.pomodoro.features.areas.data.AreaType.PROYECTO -> "Proyecto"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    if (items.isNotEmpty()) {
                        IconButton(
                            onClick = { actionError = ItemActions.share(context, items) }
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Enviar material a otra app"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Box {
                ExtendedFloatingActionButton(
                    onClick = { showAddMenu = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Añadir") }
                )
                DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Archivos") },
                        leadingIcon = { Icon(Icons.Default.AttachFile, null) },
                        onClick = {
                            showAddMenu = false
                            filesLauncher.launch(arrayOf("*/*"))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Una carpeta entera") },
                        leadingIcon = { Icon(Icons.Default.Folder, null) },
                        onClick = {
                            showAddMenu = false
                            folderLauncher.launch(null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Enlace") },
                        leadingIcon = { Icon(Icons.Default.Link, null) },
                        onClick = {
                            showAddMenu = false
                            showTextDialog = TextEntry.LINK
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Nota") },
                        leadingIcon = { Icon(Icons.Default.Notes, null) },
                        onClick = {
                            showAddMenu = false
                            showTextDialog = TextEntry.NOTE
                        }
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            AreaDetailSummary(
                total = allItems.size,
                important = allItems.count { it.mark == ItemMark.IMPORTANTE },
                deliveries = allItems.count { it.mark == ItemMark.ENTREGA },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AreaFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { viewModel.setFilter(option) },
                        label = {
                            Text(
                                when (option) {
                                    AreaFilter.TODO -> "Todo (${allItems.size})"
                                    AreaFilter.IMPORTANTES ->
                                        "Importantes (${allItems.count { it.mark == ItemMark.IMPORTANTE }})"
                                    AreaFilter.ENTREGAS ->
                                        "Entregas (${allItems.count { it.mark == ItemMark.ENTREGA }})"
                                }
                            )
                        }
                    )
                }
            }

            if (items.isEmpty()) {
                EmptyAreaContent(filter = filter, hasAnyItems = allItems.isNotEmpty())
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        ItemRow(
                            item = item,
                            onOpen = {
                                if (item.kind == ItemKind.NOTA) {
                                    noteBeingRead = item
                                } else {
                                    actionError = ItemActions.open(context, item)
                                }
                            },
                            onShare = { actionError = ItemActions.share(context, listOf(item)) },
                            onToggleMark = { viewModel.requestMarkChange(item) },
                            onDelete = { viewModel.deleteItem(item) }
                        )
                    }
                }
            }
        }
    }

    // Al llegar a ENTREGA se pide fecha y hora en dos pasos: primero el día (Material 3
    // devuelve medianoche UTC), luego la hora local, que se combinan con PickerDateUtils.
    val pendingDueDate by viewModel.pendingDueDateFor.collectAsState()
    var chosenDateUtc by remember { mutableStateOf<Long?>(null) }

    pendingDueDate?.let { item ->
        val base = item.dueAt ?: System.currentTimeMillis()
        if (chosenDateUtc == null) {
            PomodoroDatePickerDialog(
                initialDateMillis = base,
                onDismiss = { viewModel.dismissDueDatePicker() },
                onConfirm = { utcDateMillis -> chosenDateUtc = utcDateMillis },
                confirmText = "Siguiente"
            )
        } else {
            PomodoroTimePickerDialog(
                initialHour = PickerDateUtils.hourOf(base),
                initialMinute = PickerDateUtils.minuteOf(base),
                onDismiss = {
                    chosenDateUtc = null
                    viewModel.dismissDueDatePicker()
                },
                onConfirm = { hour, minute ->
                    val dueAt = PickerDateUtils.combineDateAndTime(chosenDateUtc!!, hour, minute)
                    chosenDateUtc = null
                    viewModel.confirmDueDate(item, dueAt)
                }
            )
        }
    }

    // Las notas no salen a otra app: se leen aquí mismo.
    noteBeingRead?.let { note ->
        AlertDialog(
            onDismissRequest = { noteBeingRead = null },
            title = { Text(note.title) },
            text = { Text(note.content) },
            confirmButton = {
                TextButton(onClick = { noteBeingRead = null }) { Text("Cerrar") }
            }
        )
    }

    showTextDialog?.let { entry ->
        TextEntryDialog(
            entry = entry,
            onDismiss = { showTextDialog = null },
            onConfirm = { text ->
                if (entry == TextEntry.LINK) viewModel.addLink(text) else viewModel.addNote(text)
                showTextDialog = null
            }
        )
    }
}

private enum class TextEntry { LINK, NOTE }

@Composable
private fun AreaDetailSummary(
    total: Int,
    important: Int,
    deliveries: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DetailMetric(total, "Materiales", Icons.Default.InsertDriveFile, Modifier.weight(1f))
            DetailMetric(important, "Importantes", Icons.Default.Star, Modifier.weight(1f))
            DetailMetric(
                deliveries,
                "Entregas",
                Icons.Default.AssignmentLate,
                Modifier.weight(1f),
                alert = deliveries > 0
            )
        }
    }
}

@Composable
private fun DetailMetric(
    value: Int,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    alert: Boolean = false
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (alert) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                value.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (alert) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
        )
    }
}

@Composable
private fun EmptyAreaContent(filter: AreaFilter, hasAnyItems: Boolean) {
    val filtered = hasAnyItems && filter != AreaFilter.TODO
    val title = if (filtered) {
        when (filter) {
            AreaFilter.IMPORTANTES -> "No hay material importante"
            AreaFilter.ENTREGAS -> "No hay entregas"
            AreaFilter.TODO -> "Nada por aquí todavía"
        }
    } else {
        "Añade tu primer material"
    }
    val description = if (filtered) {
        "Cambia el filtro para consultar el resto del material."
    } else {
        "Añade archivos, enlaces o notas; también puedes compartir contenido desde otra aplicación."
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Icon(
                imageVector = if (filtered) Icons.Default.FilterAltOff else Icons.Default.NoteAdd,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(18.dp).size(30.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(7.dp))
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemRow(
    item: Item,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onToggleMark: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ItemPreview(item = item, modifier = Modifier.size(54.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                Text(
                    buildString {
                        append(formatItemDate(item.createdAt))
                        item.sizeBytes?.let { append(" · ${formatSize(it)}") }
                        if (item.isExternalReference) append(" · enlace externo")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.mark != ItemMark.NINGUNA) {
                    Spacer(Modifier.height(5.dp))
                    val markColor = if (item.mark == ItemMark.ENTREGA) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    }
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onToggleMark)
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            if (item.mark == ItemMark.IMPORTANTE) Icons.Default.Star
                            else Icons.Default.AssignmentLate,
                            contentDescription = null,
                            tint = markColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            if (item.mark == ItemMark.IMPORTANTE) "Importante" else "Entrega",
                            style = MaterialTheme.typography.labelSmall,
                            color = markColor
                        )
                    }
                }
            }

            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Enviar a otra app") },
                        leadingIcon = { Icon(Icons.Default.Share, null) },
                        onClick = { menuOpen = false; onShare() }
                    )
                    DropdownMenuItem(
                        text = { Text("Cambiar marca") },
                        leadingIcon = { Icon(Icons.Default.Label, null) },
                        onClick = { menuOpen = false; onToggleMark() }
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }
        }
    }
}

internal fun itemKindIcon(kind: ItemKind) = when (kind) {
    ItemKind.FOTO -> Icons.Default.Image
    ItemKind.PDF -> Icons.Default.PictureAsPdf
    ItemKind.VIDEO -> Icons.Default.Movie
    ItemKind.AUDIO -> Icons.Default.Audiotrack
    ItemKind.ENLACE -> Icons.Default.Link
    ItemKind.NOTA -> Icons.Default.Notes
    ItemKind.GENERADO -> Icons.Default.AutoAwesome
    ItemKind.ARCHIVO -> Icons.Default.InsertDriveFile
}

@Composable
private fun TextEntryDialog(
    entry: TextEntry,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val isLink = entry == TextEntry.LINK

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isLink) "Guardar enlace" else "Nueva nota") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(if (isLink) "https://…" else "Escribe aquí") },
                singleLine = isLink,
                minLines = if (isLink) 1 else 4,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) {
                Text("Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

private fun formatItemDate(timestamp: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(timestamp))

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1024 -> "%d KB".format(bytes / 1024)
    else -> "$bytes B"
}

/** Lista los archivos directos de una carpeta concedida con OpenDocumentTree. */
private fun listFilesIn(context: android.content.Context, treeUri: Uri): List<Uri> {
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
        treeUri,
        DocumentsContract.getTreeDocumentId(treeUri)
    )
    val result = mutableListOf<Uri>()

    runCatching {
        context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val documentId = cursor.getString(0)
                val mime = cursor.getString(1)
                // Las subcarpetas se ignoran: la organización va por área, no por árbol.
                if (mime != DocumentsContract.Document.MIME_TYPE_DIR) {
                    result += DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                }
            }
        }
    }

    return result
}
