package com.cuidadopet.ui.screens.pet

import android.content.Context
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
import androidx.compose.material3.SelectableDates
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
import androidx.compose.ui.res.stringResource
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
import com.cuidadopet.R
import com.cuidadopet.ui.utils.adaptiveHorizontalPadding

private val speciesOptions = listOf(
    "DOG"     to R.string.species_dog,
    "CAT"     to R.string.species_cat,
    "RABBIT"  to R.string.species_rabbit,
    "BIRD"    to R.string.species_bird,
    "HAMSTER" to R.string.species_hamster,
    "TURTLE"  to R.string.species_turtle,
    "FISH"    to R.string.species_fish,
    "OTHER"   to R.string.species_other
)

private val clinicalStateLabels = mapOf(
    "ACTIVE_TREATMENT" to R.string.pet_form_clinical_active_treatment,
    "CHRONIC_DISEASE"  to R.string.pet_form_clinical_chronic,
    "POST_SURGICAL"    to R.string.pet_form_clinical_post_surgical,
    "RECOVERY"         to R.string.pet_form_clinical_recovery,
    "PREVENTIVE"       to R.string.pet_form_clinical_preventive
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
            initialSelectedDateMillis = uiState.birthDate ?: System.currentTimeMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    utcTimeMillis <= System.currentTimeMillis()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showBirthDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onBirthDateChange(pickerState.selectedDateMillis)
                    showBirthDatePicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showBirthDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { viewModel.onPhotoFromGallery(it) } }

    var cameraFile by remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) cameraFile?.absolutePath?.let { viewModel.onPhotoFromCamera(it) }
    }

    var showPhotoPickerDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (petId == null) R.string.pet_form_new_title else R.string.pet_form_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back),
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState, modifier = Modifier.imePadding()) }
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

            SectionTitle(stringResource(R.string.pet_form_photo_section))

            Box(
                modifier         = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                                    contentDescription = stringResource(R.string.pet_form_photo_add_cd),
                                    modifier           = Modifier.size(40.dp),
                                    tint               = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
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
                        stringResource(R.string.pet_form_photo_tap_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SectionTitle(stringResource(R.string.pet_form_basic_section))

            OutlinedTextField(
                value         = uiState.name,
                onValueChange = viewModel::onNameChange,
                label         = { Text(stringResource(R.string.pet_form_name_label)) },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true
            )

            var speciesExpanded by remember { mutableStateOf(false) }
            val selectedSpeciesLabel = when {
                uiState.species == "OTHER" && uiState.customSpecies.isNotBlank() -> uiState.customSpecies
                else -> speciesOptions.firstOrNull { it.first == uiState.species }?.second?.let { stringResource(it) } ?: ""
            }
            ExposedDropdownMenuBox(
                expanded = speciesExpanded,
                onExpandedChange = { speciesExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedSpeciesLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.pet_form_species_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = speciesExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = speciesExpanded,
                    onDismissRequest = { speciesExpanded = false }
                ) {
                    speciesOptions.forEach { (code, labelRes) ->
                        DropdownMenuItem(
                            text = { Text(stringResource(labelRes)) },
                            onClick = {
                                if (code == "OTHER") viewModel.onCustomSpeciesChange("")
                                viewModel.onSpeciesChange(code)
                                speciesExpanded = false
                            }
                        )
                    }
                }
            }

            if (uiState.species == "OTHER" && uiState.customSpecies.isBlank()) {
                OutlinedTextField(
                    value         = uiState.customSpecies,
                    onValueChange = { if (it.length <= 50) viewModel.onCustomSpeciesChange(it) },
                    label         = { Text(stringResource(R.string.pet_form_custom_species_label)) },
                    placeholder   = { Text(stringResource(R.string.pet_form_custom_species_hint)) },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    supportingText = { Text("${uiState.customSpecies.length}/50") }
                )
            }

            OutlinedTextField(
                value         = uiState.breed,
                onValueChange = viewModel::onBreedChange,
                label         = { Text(stringResource(R.string.pet_form_breed_label)) },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true
            )

            SectionTitle(stringResource(R.string.pet_form_birthdate_section))
            Text(
                stringResource(R.string.pet_form_birthdate_approx),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val birthDateMs = uiState.birthDate
            if (birthDateMs == null) {
                OutlinedButton(
                    onClick  = { showBirthDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.pet_form_birthdate_set_btn))
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
                        Text(stringResource(R.string.pet_form_birthdate_display, dateFmt.format(Date(birthDateMs))))
                    }
                    TextButton(onClick = { viewModel.onBirthDateChange(null) }) {
                        Text(stringResource(R.string.pet_form_birthdate_remove))
                    }
                }
                Text(
                    stringResource(R.string.pet_form_age_display, calculatePetAge(context, birthDateMs)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            OutlinedTextField(
                value         = uiState.weightKg,
                onValueChange = viewModel::onWeightChange,
                label         = { Text(stringResource(R.string.pet_form_weight_label)) },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                suffix        = { Text("kg") }
            )

            Text(stringResource(R.string.pet_form_sex_label), style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = uiState.sex == "MALE",
                    onClick = { viewModel.onSexChange("MALE") }, label = { Text(stringResource(R.string.sex_male)) })
                FilterChip(selected = uiState.sex == "FEMALE",
                    onClick = { viewModel.onSexChange("FEMALE") }, label = { Text(stringResource(R.string.sex_female)) })
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = uiState.isNeutered, onCheckedChange = viewModel::onNeuteredChange)
                Text(stringResource(R.string.sex_neutered), style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 4.dp))
            }

            Spacer(Modifier.height(4.dp))
            SectionTitle(stringResource(R.string.pet_form_clinical_section))
            Text(stringResource(R.string.pet_form_clinical_select_all),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            clinicalStateLabels.forEach { (key, labelRes) ->
                FilterChip(
                    selected = key in uiState.clinicalStates,
                    onClick  = { viewModel.onClinicalStateToggle(key) },
                    label    = { Text(stringResource(labelRes)) },
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
                    Text(stringResource(if (petId == null) R.string.pet_form_save_btn else R.string.pet_form_update_btn))
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showPhotoPickerDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoPickerDialog = false },
            title            = { Text(stringResource(R.string.pet_form_photo_dialog_title)) },
            text             = { Text(stringResource(R.string.pet_form_photo_dialog_msg)) },
            confirmButton    = {
                Button(onClick = {
                    showPhotoPickerDialog = false
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }) {
                    Icon(Icons.Default.Photo, null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.pet_form_photo_gallery))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPhotoPickerDialog = false
                    val file = viewModel.createCameraFile()
                    cameraFile = file
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    cameraLauncher.launch(uri)
                }) {
                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.pet_form_photo_camera))
                }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

private fun calculatePetAge(context: Context, birthDateMs: Long): String {
    val birth = Calendar.getInstance().also { it.timeInMillis = birthDateMs }
    val today = Calendar.getInstance()
    var years  = today.get(Calendar.YEAR)  - birth.get(Calendar.YEAR)
    var months = today.get(Calendar.MONTH) - birth.get(Calendar.MONTH)
    if (months < 0) { years--; months += 12 }
    if (today.get(Calendar.DAY_OF_MONTH) < birth.get(Calendar.DAY_OF_MONTH)) {
        if (months == 0) { years--; months = 11 } else months--
    }
    return when {
        years > 0 && months > 0 -> "$years ${if (years == 1) context.getString(R.string.age_year) else context.getString(R.string.age_years)} e $months ${if (months == 1) context.getString(R.string.age_month) else context.getString(R.string.age_months)}"
        years > 0               -> "$years ${if (years == 1) context.getString(R.string.age_year) else context.getString(R.string.age_years)}"
        months > 0              -> "$months ${if (months == 1) context.getString(R.string.age_month) else context.getString(R.string.age_months)}"
        else                    -> context.getString(R.string.age_less_than_1_month)
    }
}

@Composable
private fun DisclaimerText() {
    Text(
        stringResource(R.string.pet_form_disclaimer),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
