@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cuidadopet.ui.screens.documents

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuidadopet.R
import com.cuidadopet.data.db.entity.PetDocumentEntity
import com.cuidadopet.domain.PetDocumentRules
import com.cuidadopet.ui.utils.adaptiveHorizontalPadding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Rascunho do formulário: novo documento (uri preenchida) ou edição (existing preenchido)
private data class DocumentDraft(
    val uri: Uri?,
    val existing: PetDocumentEntity?,
    val title: String,
    // Início do dia local
    val date: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDocumentsScreen(
    petId: Long,
    onNavigateBack: () -> Unit,
    viewModel: PetDocumentsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(petId) { viewModel.setPetId(petId) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    var draft by remember { mutableStateOf<DocumentDraft?>(null) }
    var deleteTarget by remember { mutableStateOf<PetDocumentEntity?>(null) }
    var copyTarget by remember { mutableStateOf<PetDocumentEntity?>(null) }
    var fromPickerOpen by remember { mutableStateOf(false) }
    var toPickerOpen by remember { mutableStateOf(false) }

    // Escolha do PDF no seletor de arquivos do Android (só PDF)
    val addPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                draft = DocumentDraft(
                    uri = uri,
                    existing = null,
                    title = viewModel.suggestTitle(uri),
                    date = PetDocumentRules.localStartOfDay(System.currentTimeMillis())
                )
            }
        }
    }

    // "Salvar como" para baixar uma cópia do PDF
    val savePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val doc = copyTarget
        copyTarget = null
        if (uri != null && doc != null) viewModel.saveCopy(doc, uri)
    }

    draft?.let { current ->
        DocumentFormDialog(
            draft = current,
            onConfirm = { title, date ->
                val existing = current.existing
                if (existing != null) {
                    viewModel.updateDocument(existing, title, date)
                } else if (current.uri != null) {
                    viewModel.addDocument(current.uri, title, date)
                }
                draft = null
            },
            onDismiss = { draft = null }
        )
    }

    deleteTarget?.let { doc ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.docs_delete_title)) },
            text = { Text(stringResource(R.string.docs_delete_msg, doc.title)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDocument(doc)
                    deleteTarget = null
                }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (fromPickerOpen) {
        DocumentDatePickerDialog(
            initialLocalDate = state.filter.fromDate,
            onPicked = { viewModel.onFromDateChange(it) },
            onDismiss = { fromPickerOpen = false }
        )
    }
    if (toPickerOpen) {
        DocumentDatePickerDialog(
            initialLocalDate = state.filter.toDate,
            onPicked = { viewModel.onToDateChange(it) },
            onDismiss = { toPickerOpen = false }
        )
    }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.docs_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { addPicker.launch(arrayOf("application/pdf")) }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.docs_add_cd))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = adaptiveHorizontalPadding()),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = state.filter.query,
                    onValueChange = { viewModel.onQueryChange(it) },
                    label = { Text(stringResource(R.string.docs_search_hint)) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.filter.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.action_cancel))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { fromPickerOpen = true }, modifier = Modifier.weight(1f)) {
                        Text(
                            state.filter.fromDate?.let {
                                stringResource(R.string.docs_filter_from, dateFormat.format(Date(it)))
                            } ?: stringResource(R.string.docs_filter_from, stringResource(R.string.docs_filter_unset)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(onClick = { toPickerOpen = true }, modifier = Modifier.weight(1f)) {
                        Text(
                            state.filter.toDate?.let {
                                stringResource(R.string.docs_filter_to, dateFormat.format(Date(it)))
                            } ?: stringResource(R.string.docs_filter_to, stringResource(R.string.docs_filter_unset)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                val from = state.filter.fromDate
                val to = state.filter.toDate
                if (from != null && to != null && from > to) {
                    Text(
                        stringResource(R.string.docs_date_range_invalid),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (state.filter.isActive) {
                    TextButton(onClick = { viewModel.clearFilters() }) {
                        Text(stringResource(R.string.docs_filter_clear))
                    }
                }
            }

            if (state.isBusy) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                }
            }

            if (state.documents.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (state.totalCount == 0) {
                            Text(
                                stringResource(R.string.docs_empty_title),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                stringResource(R.string.docs_empty_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                stringResource(R.string.docs_empty_filtered),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(state.documents, key = { it.document.id }) { item ->
                val doc = item.document
                val missing = item.fileMissing
                DocumentCard(
                    document = doc,
                    fileMissing = missing,
                    dateText = dateFormat.format(Date(doc.documentDate)),
                    onOpen = {
                        if (missing) viewModel.notify(R.string.docs_file_missing)
                        else openPdf(context, viewModel, doc) { viewModel.notify(R.string.docs_error_no_viewer) }
                    },
                    onShare = {
                        if (missing) viewModel.notify(R.string.docs_file_missing)
                        else sharePdf(context, viewModel, doc)
                    },
                    onDownload = {
                        if (missing) {
                            viewModel.notify(R.string.docs_file_missing)
                        } else {
                            copyTarget = doc
                            savePicker.launch(suggestedFileName(doc))
                        }
                    },
                    onEdit = {
                        draft = DocumentDraft(
                            uri = null,
                            existing = doc,
                            title = doc.title,
                            date = doc.documentDate
                        )
                    },
                    onDelete = { deleteTarget = doc }
                )
            }
        }
    }
}

@Composable
private fun DocumentCard(
    document: PetDocumentEntity,
    fileMissing: Boolean,
    dateText: String,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.docs_date_label, dateText) +
                            " · " + PetDocumentRules.formatMegabytes(document.fileSizeBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (fileMissing) {
                        Text(
                            text = stringResource(R.string.docs_file_missing_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDownload) {
                    Icon(Icons.Default.Download, contentDescription = stringResource(R.string.docs_action_download_cd))
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = stringResource(R.string.docs_action_share_cd))
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.docs_action_edit_cd))
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.docs_action_delete_cd),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// Formulário para adicionar ou editar título e data
@Composable
private fun DocumentFormDialog(
    draft: DocumentDraft,
    onConfirm: (title: String, date: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(draft.title) }
    var date by remember { mutableStateOf(draft.date) }
    var pickerOpen by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (draft.existing == null) R.string.docs_dialog_add_title else R.string.docs_dialog_edit_title
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.docs_field_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = { pickerOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.docs_field_date, dateFormat.format(Date(date))))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title.trim(), date) },
                enabled = title.isNotBlank()
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )

    if (pickerOpen) {
        DocumentDatePickerDialog(
            initialLocalDate = date,
            onPicked = { date = it },
            onDismiss = { pickerOpen = false }
        )
    }
}

// Seletor de data. Devolve o início do dia escolhido, no horário local.
@Composable
private fun DocumentDatePickerDialog(
    initialLocalDate: Long?,
    onPicked: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialLocalDate?.let { PetDocumentRules.localStartOfDayToUtcMillis(it) }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { onPicked(PetDocumentRules.utcMillisToLocalStartOfDay(it)) }
                onDismiss()
            }) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    ) {
        DatePicker(state = pickerState)
    }
}

// Abre o PDF no leitor do aparelho. Se não houver app, avisa.
private fun openPdf(
    context: android.content.Context,
    viewModel: PetDocumentsViewModel,
    document: PetDocumentEntity,
    onNoViewer: () -> Unit
) {
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(viewModel.uriFor(document), "application/pdf")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        onNoViewer()
    }
}

// Envia o PDF para outro app (WhatsApp, e-mail, Drive...)
private fun sharePdf(
    context: android.content.Context,
    viewModel: PetDocumentsViewModel,
    document: PetDocumentEntity
) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("application/pdf")
        .putExtra(Intent.EXTRA_STREAM, viewModel.uriFor(document))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, context.getString(R.string.docs_share_chooser)))
}

// Nome sugerido ao salvar a cópia: título sem caracteres proibidos em nomes de arquivo
private fun suggestedFileName(document: PetDocumentEntity): String =
    document.title.replace(Regex("[\\\\/:*?\"<>|]"), "_") + ".pdf"
