package com.example.pomodoro.features.areas.presentation

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pomodoro.features.areas.data.Item
import com.example.pomodoro.features.areas.data.ItemKind
import com.example.pomodoro.features.areas.data.ItemMark
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
                title = { Text(area?.name ?: "Área", fontWeight = FontWeight.Bold) },
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
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AreaFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { viewModel.setFilter(option) },
                        label = {
                            Text(
                                when (option) {
                                    AreaFilter.TODO -> "Todo"
                                    AreaFilter.IMPORTANTES -> "Importantes"
                                    AreaFilter.ENTREGAS -> "Entregas"
                                }
                            )
                        }
                    )
                }
            }

            if (items.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Nada por aquí todavía", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Añade archivos con el botón, o comparte cualquier cosa desde otra " +
                            "app y elige esta área.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            onToggleMark = { viewModel.cycleMark(item) },
                            onDelete = { viewModel.deleteItem(item) }
                        )
                    }
                }
            }
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

    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = when (item.kind) {
                    ItemKind.FOTO -> Icons.Default.Image
                    ItemKind.PDF -> Icons.Default.PictureAsPdf
                    ItemKind.VIDEO -> Icons.Default.Movie
                    ItemKind.AUDIO -> Icons.Default.Audiotrack
                    ItemKind.ENLACE -> Icons.Default.Link
                    ItemKind.NOTA -> Icons.Default.Notes
                    ItemKind.GENERADO -> Icons.Default.AutoAwesome
                    ItemKind.ARCHIVO -> Icons.Default.InsertDriveFile
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
                Text(
                    buildString {
                        append(dateFormat.format(Date(item.createdAt)))
                        item.sizeBytes?.let { append(" · ${formatSize(it)}") }
                        if (item.isExternalReference) append(" · enlazado")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (item.mark != ItemMark.NINGUNA) {
                AssistChip(
                    onClick = onToggleMark,
                    label = {
                        Text(if (item.mark == ItemMark.IMPORTANTE) "Importante" else "Entrega")
                    }
                )
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
                        onClick = { menuOpen = false; onToggleMark() }
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar") },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }
        }
    }
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

private val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

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
