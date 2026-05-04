package com.cuidadopet.ui.screens.health

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.cuidadopet.data.db.entity.HealthPhotoEntity
import com.cuidadopet.ui.utils.adaptiveHorizontalPadding
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HealthEntryFormScreen(
    petId: Long,
    entryId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: HealthEntryFormViewModel = hiltViewModel()
) {
    LaunchedEffect(petId, entryId) { viewModel.loadEntry(petId, entryId) }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { viewModel.addPhoto(it) } }
    var fullScreenPhoto by remember { mutableStateOf<HealthPhotoEntity?>(null) }

    LaunchedEffect(state.isSaved) { if (state.isSaved) onNavigateBack() }
    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }

    val title = if (entryId == null) "Nova entrada no diário" else "Editar entrada"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = adaptiveHorizontalPadding())
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Comportamento ───────────────────────────────────────────
            ChipSelector(
                title = "Comportamento geral",
                options = listOf(
                    "NORMAL"    to "Normal, ativo",
                    "LETHARGIC" to "Apático",
                    "AGITATED"  to "Agitado",
                    "SLEEPY"    to "Sonolento"
                ),
                selected = state.behavior,
                onSelect = viewModel::updateBehavior
            )

            // ── Fezes ──────────────────────────────────────────────────
            ChipSelector(
                title = "Fezes",
                options = listOf(
                    "NORMAL"   to "Normal",
                    "SOFT"     to "Amolecidas",
                    "DIARRHEA" to "Diarreia",
                    "ABSENT"   to "Não evacuou",
                    "BLOOD"    to "Com sangue ⚠️"
                ),
                selected = state.fecesStatus,
                onSelect = viewModel::updateFeces
            )

            // ── Urina ──────────────────────────────────────────────────
            ChipSelector(
                title = "Urina",
                options = listOf(
                    "NORMAL"    to "Normal",
                    "INCREASED" to "Aumentada",
                    "REDUCED"   to "Reduzida",
                    "ABSENT"    to "Não urinou",
                    "BLOOD"     to "Com sangue ⚠️"
                ),
                selected = state.urineStatus,
                onSelect = viewModel::updateUrine
            )

            // ── Vômitos ────────────────────────────────────────────────
            OutlinedTextField(
                value = state.vomitCount,
                onValueChange = { viewModel.updateVomitCount(it.filter { c -> c.isDigit() }.take(2)) },
                label = { Text("Número de episódios de vômito") },
                placeholder = { Text("0 = não vomitou") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            // ── Mobilidade ────────────────────────────────────────────
            ChipSelector(
                title = "Mobilidade",
                options = listOf(
                    "NORMAL"   to "Normal",
                    "REDUCED"  to "Reduzida",
                    "IMMOBILE" to "Imóvel"
                ),
                selected = state.mobility,
                onSelect = viewModel::updateMobility
            )

            // ── Sinais de dor ─────────────────────────────────────────
            ChipSelector(
                title = "Sinais de dor",
                options = listOf(
                    "NONE"     to "Sem sinais",
                    "APPARENT" to "Aparente",
                    "EVIDENT"  to "Evidente"
                ),
                selected = state.painSigns,
                onSelect = viewModel::updatePainSigns
            )

            // ── Observações livres ────────────────────────────────────
            OutlinedTextField(
                value = state.observations,
                onValueChange = viewModel::updateObservations,
                label = { Text("Observações (campo livre)") },
                placeholder = { Text("Ex: dormiu o dia todo, não quis brincar...") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 5
            )

            // ── Fotos ─────────────────────────────────────────────────
            Text("Fotos da observação", style = MaterialTheme.typography.titleSmall)
            if (state.photos.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.photos.forEach { photo ->
                        Box(modifier = Modifier
                            .size(88.dp)
                            .clickable { fullScreenPhoto = photo }
                        ) {
                            AsyncImage(
                                model = File(photo.filePath),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { viewModel.deletePhoto(photo) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(MaterialTheme.colorScheme.error, CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Excluir foto",
                                    tint = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Adicionar foto")
            }

            // ── Aviso ─────────────────────────────────────────────────
            Text(
                "Este diário é para observação do tutor. Não substitui avaliação veterinária.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ── Salvar ────────────────────────────────────────────────
            Button(
                onClick = { viewModel.save(petId, entryId) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp).width(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Salvar entrada")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    fullScreenPhoto?.let { photo ->
        Dialog(
            onDismissRequest = { fullScreenPhoto = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
            ) {
                AsyncImage(
                    model = File(photo.filePath),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { fullScreenPhoto = null },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                }
                IconButton(
                    onClick = {
                        val file = File(photo.filePath)
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/jpeg"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Compartilhar foto"))
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Compartilhar", tint = Color.White)
                }
            }
        }
    }
}

// Seletor de chip reutilizável — toque no chip selecionado para desmarcar (nullable)
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipSelector(
    title: String,
    options: List<Pair<String, String>>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        // FlowRow quebra automaticamente para a próxima linha quando não cabe
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (key, label) ->
                FilterChip(
                    selected = selected == key,
                    // Tocar no chip já selecionado o desmarca (volta a null)
                    onClick = { onSelect(if (selected == key) null else key) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}
