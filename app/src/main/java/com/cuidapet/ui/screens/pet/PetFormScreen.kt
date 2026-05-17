package com.cuidadopet.ui.screens.pet

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import java.io.File
import com.cuidadopet.ui.utils.adaptiveHorizontalPadding

private val speciesOptions = listOf(
    "DOG"     to "Cachorro",
    "CAT"     to "Gato",
    "RABBIT"  to "Coelho",
    "BIRD"    to "Pássaro",
    "HAMSTER" to "Hamster",
    "TURTLE"  to "Tartaruga",
    "FISH"    to "Peixe",
    "OTHER"   to "Outro"
)

private val clinicalStateLabels = mapOf(
    "ACTIVE_TREATMENT" to "Tratamento ativo",
    "CHRONIC_DISEASE"  to "Doença crônica",
    "POST_SURGICAL"    to "Pós-cirúrgico",
    "RECOVERY"         to "Recuperação",
    "PREVENTIVE"       to "Monitoramento preventivo"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetFormScreen(
    petId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: PetFormViewModel = hiltViewModel()
) {
    val uiState           = viewModel.uiState.collectAsStateWithLifecycle().value
    val snackbarHostState = remember { SnackbarHostState() }
    val context           = LocalContext.current
    val dateFmt           = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR")) }

    LaunchedEffect(petId) { if (petId != null) viewModel.loadPet(petId) }
    LaunchedEffect(uiState.isSaved) { if (uiState.isSaved) onNavigateBack() }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }

    var showBirthDatePicker by remember { mutableStateOf(false) }

    if (showBirthDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.birthDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showBirthDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onBirthDateChange(pickerState.selectedDateMillis)
                    showBirthDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showBirthDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    // ── Lançadores para galeria e câmera ──────────────────────────────────────

    // Seletor de foto da galeria — PickVisualMedia é a API moderna (API 21+)
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { viewModel.onPhotoFromGallery(it) } }

    // Referência ao arquivo temporário criado antes de lançar a câmera
    // var é necessário porque criamos um novo arquivo a cada vez que o botão é pressionado
    var cameraFile by remember { mutableStateOf<File?>(null) }

    // TakePicture salva a foto no URI fornecido e retorna true/false de sucesso
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) cameraFile?.absolutePath?.let { viewModel.onPhotoFromCamera(it) }
    }

    // Controla a exibição do diálogo "Galeria ou Câmera?"
    var showPhotoPickerDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (petId == null) "Novo pet" else "Editar pet") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
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
                .padding(horizontal = adaptiveHorizontalPadding(), vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Foto do pet ────────────────────────────────────────────────────
            SectionTitle("Foto do pet")

            Box(
                modifier          = Modifier.fillMaxWidth(),
                contentAlignment  = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Container externo — o clip do círculo fica no Box interno,
                    // assim o overlay de câmera não é cortado pela máscara circular
                    Box(modifier = Modifier.size(96.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable { showPhotoPickerDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.photoPath != null) {
                                AsyncImage(
                                    model              = File(uiState.photoPath),
                                    contentDescription = "Foto de ${uiState.name}",
                                    contentScale       = ContentScale.Crop,
                                    modifier           = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    Icons.Default.Pets,
                                    contentDescription = "Adicionar foto",
                                    modifier           = Modifier.size(40.dp),
                                    tint               = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        // Overlay fora do círculo clipeado — não sofre corte
                        Box(
                            modifier         = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AddAPhoto,
                                contentDescription = null,
                                modifier           = Modifier.size(16.dp),
                                tint               = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Text(
                        "Toque para alterar",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Informações básicas ────────────────────────────────────────────
            SectionTitle("Informações básicas")

            OutlinedTextField(
                value         = uiState.name,
                onValueChange = viewModel::onNameChange,
                label         = { Text("Nome do pet *") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true
            )

            var speciesExpanded by remember { mutableStateOf(false) }
            val selectedSpeciesLabel = speciesOptions.firstOrNull { it.first == uiState.species }?.second ?: ""
            ExposedDropdownMenuBox(
                expanded = speciesExpanded,
                onExpandedChange = { speciesExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedSpeciesLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Espécie *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = speciesExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = speciesExpanded,
                    onDismissRequest = { speciesExpanded = false }
                ) {
                    speciesOptions.forEach { (code, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                viewModel.onSpeciesChange(code)
                                speciesExpanded = false
                            }
                        )
                    }
                }
            }

            if (uiState.species == "OTHER") {
                OutlinedTextField(
                    value         = uiState.customSpecies,
                    onValueChange = { if (it.length <= 50) viewModel.onCustomSpeciesChange(it) },
                    label         = { Text("Qual espécie? *") },
                    placeholder   = { Text("Ex: Chinchila, Furão, Porquinho-da-índia...") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    supportingText = { Text("${uiState.customSpecies.length}/50") }
                )
            }

            OutlinedTextField(
                value         = uiState.breed,
                onValueChange = viewModel::onBreedChange,
                label         = { Text("Raça (opcional)") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true
            )

            // ── Data de nascimento ─────────────────────────────────────────────
            SectionTitle("Data de nascimento (opcional)")
            Text(
                "Pode ser uma data aproximada — especialmente para pets resgatados.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val birthDateMs = uiState.birthDate
            if (birthDateMs == null) {
                OutlinedButton(
                    onClick  = { showBirthDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Informar data de nascimento")
                }
            } else {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick  = { showBirthDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Nascimento: ${dateFmt.format(Date(birthDateMs))}")
                    }
                    TextButton(onClick = { viewModel.onBirthDateChange(null) }) {
                        Text("Remover")
                    }
                }
                Text(
                    "Idade: ${calculatePetAge(birthDateMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            OutlinedTextField(
                value         = uiState.weightKg,
                onValueChange = viewModel::onWeightChange,
                label         = { Text("Peso atual (kg) *") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                suffix        = { Text("kg") }
            )

            Text("Sexo *", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = uiState.sex == "MALE",
                    onClick = { viewModel.onSexChange("MALE") }, label = { Text("Macho") })
                FilterChip(selected = uiState.sex == "FEMALE",
                    onClick = { viewModel.onSexChange("FEMALE") }, label = { Text("Fêmea") })
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = uiState.isNeutered, onCheckedChange = viewModel::onNeuteredChange)
                Text("Castrado(a)", style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 4.dp))
            }

            // ── Estados clínicos ───────────────────────────────────────────────
            Spacer(Modifier.height(4.dp))
            SectionTitle("Estado clínico atual")
            Text("Selecione todos que se aplicam:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            clinicalStateLabels.forEach { (key, label) ->
                FilterChip(
                    selected = key in uiState.clinicalStates,
                    onClick  = { viewModel.onClinicalStateToggle(key) },
                    label    = { Text(label) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(4.dp))
            DisclaimerText()

            Spacer(Modifier.height(8.dp))
            Button(
                onClick  = { viewModel.savePet(existingPetId = petId) },
                modifier = Modifier.fillMaxWidth(),
                enabled  = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (petId == null) "Cadastrar pet" else "Salvar alterações")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // Diálogo para escolher entre galeria e câmera
    if (showPhotoPickerDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoPickerDialog = false },
            title            = { Text("Foto do pet") },
            text             = { Text("Como você quer adicionar a foto?") },
            confirmButton    = {
                Button(onClick = {
                    showPhotoPickerDialog = false
                    // PickVisualMedia.ImageOnly filtra somente imagens
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }) {
                    Icon(Icons.Default.Photo, null, modifier = Modifier.size(18.dp))
                    Text("  Galeria")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPhotoPickerDialog = false
                    // Cria o arquivo onde a câmera vai salvar, depois lança o app de câmera
                    val file = viewModel.createCameraFile()
                    cameraFile = file
                    // FileProvider.getUriForFile gera uma Uri "content://" que a câmera pode usar
                    // sem precisar de permissão de acesso ao armazenamento
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    cameraLauncher.launch(uri)
                }) {
                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(18.dp))
                    Text("  Câmera")
                }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

private fun calculatePetAge(birthDateMs: Long): String {
    val birth = Calendar.getInstance().also { it.timeInMillis = birthDateMs }
    val today = Calendar.getInstance()
    var years  = today.get(Calendar.YEAR)  - birth.get(Calendar.YEAR)
    var months = today.get(Calendar.MONTH) - birth.get(Calendar.MONTH)
    if (months < 0) { years--; months += 12 }
    if (today.get(Calendar.DAY_OF_MONTH) < birth.get(Calendar.DAY_OF_MONTH)) {
        if (months == 0) { years--; months = 11 } else months--
    }
    return when {
        years > 0 && months > 0 -> "$years ${if (years == 1) "ano" else "anos"} e $months ${if (months == 1) "mês" else "meses"}"
        years > 0               -> "$years ${if (years == 1) "ano" else "anos"}"
        months > 0              -> "$months ${if (months == 1) "mês" else "meses"}"
        else                    -> "menos de 1 mês"
    }
}

@Composable
private fun DisclaimerText() {
    Text(
        "⚠ Este app é uma ferramenta de organização e não substitui " +
            "a consulta veterinária. Sempre siga as orientações do seu médico veterinário.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
