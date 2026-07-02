package com.cuidadopet.ui.screens.home

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cuidadopet.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit
import com.cuidadopet.data.db.entity.PetEntity
import com.cuidadopet.ui.components.PetAvatar
import com.cuidadopet.ui.utils.adaptiveHorizontalPadding
import com.cuidadopet.ui.utils.isTablet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddPetClick: () -> Unit,
    onPetClick: (Long) -> Unit,
    onPrivacyPolicy: () -> Unit = {},
    onSettings: () -> Unit = {},
    onOpenPaywall: () -> Unit = {},
    onPartnersClick: () -> Unit = {}
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val pets by viewModel.pets.collectAsStateWithLifecycle()
    val birthdayPets by viewModel.birthdayPets.collectAsStateWithLifecycle()
    // Filtra pets cujo banner já foi mostrado hoje (persiste entre aberturas do app)
    // remember(birthdayPets) garante que o filtro só recalcula quando o banco muda,
    // não quando SharedPreferences é escrito — evita o banner sumir antes de aparecer
    val visibleBirthdayPets = remember(birthdayPets) {
        birthdayPets.filter { !viewModel.wasBirthdayBannerShownToday(it.id) }
    }
    var dismissedBirthdays by remember { mutableStateOf(emptySet<Long>()) }
    var pendingDelete by remember { mutableStateOf<PetEntity?>(null) }

    // Marca banner como visto hoje na primeira vez que aparece
    LaunchedEffect(visibleBirthdayPets) {
        visibleBirthdayPets.forEach { viewModel.markBirthdayBannerShown(it.id) }
    }

    var showConfetti by remember { mutableStateOf(false) }
    var confettiShown by remember { mutableStateOf(false) }
    LaunchedEffect(visibleBirthdayPets.isNotEmpty()) {
        if (visibleBirthdayPets.isNotEmpty() && !confettiShown) {
            showConfetti = true
            confettiShown = true
            delay(3500)
            showConfetti = false
        }
    }

    val confettiParties = remember {
        listOf(
            Party(
                speed = 0f, maxSpeed = 55f, damping = 0.9f,
                angle = 270, spread = 60,
                colors = listOf(0xFFFFA0C4.toInt(), 0xFFFFD966.toInt(), 0xFFB39DDB.toInt(), 0xFF80CBC4.toInt(), 0xFFFFAB40.toInt()),
                emitter = Emitter(duration = 250L, TimeUnit.MILLISECONDS).max(120),
                position = Position.Relative(0.2, 0.0)
            ),
            Party(
                speed = 0f, maxSpeed = 55f, damping = 0.9f,
                angle = 270, spread = 60,
                colors = listOf(0xFFFFA0C4.toInt(), 0xFFFFD966.toInt(), 0xFFB39DDB.toInt(), 0xFF80CBC4.toInt(), 0xFFFFAB40.toInt()),
                emitter = Emitter(duration = 250L, TimeUnit.MILLISECONDS).max(120),
                position = Position.Relative(0.8, 0.0)
            )
        )
    }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title   = { Text(stringResource(R.string.dialog_delete_pet_title)) },
            text    = {
                Text(stringResource(R.string.dialog_delete_pet_msg, pendingDelete!!.name))
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePet(pendingDelete!!)
                    pendingDelete = null
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter            = painterResource(R.drawable.home_background),
            contentDescription = null,
            modifier           = Modifier.fillMaxSize(),
            contentScale       = ContentScale.Crop,
            alignment          = Alignment.BottomCenter,
            alpha              = 0.75f
        )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(R.mipmap.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                        Text(
                            text = stringResource(R.string.home_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onPartnersClick) {
                        Icon(
                            Icons.Default.Store,
                            contentDescription = stringResource(R.string.partners_title),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, "Menu",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    DropdownMenu(
                        expanded         = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        // TODO: descomentar quando o premium estiver ativo
                        // DropdownMenuItem(
                        //     text         = { Text(stringResource(R.string.home_menu_be_premium), color = MaterialTheme.colorScheme.primary) },
                        //     leadingIcon  = { Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        //     onClick      = { menuExpanded = false; onOpenPaywall() }
                        // )
                        DropdownMenuItem(
                            text    = { Text(stringResource(R.string.home_menu_settings)) },
                            onClick = { menuExpanded = false; onSettings() }
                        )
                        DropdownMenuItem(
                            text    = { Text(stringResource(R.string.home_menu_privacy)) },
                            onClick = { menuExpanded = false; onPrivacyPolicy() }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPetClick,
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.home_add_pet_cd),
                    tint = MaterialTheme.colorScheme.onSecondary
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            visibleBirthdayPets.forEach { pet ->
                AnimatedVisibility(
                    visible = !dismissedBirthdays.contains(pet.id),
                    enter = slideInVertically() + fadeIn(),
                    exit  = slideOutVertically() + fadeOut()
                ) {
                    BirthdayBanner(
                        petName   = pet.name,
                        onDismiss = { dismissedBirthdays = dismissedBirthdays + pet.id }
                    )
                }
            }
            if (pets.isEmpty()) {
                EmptyPetsContent(modifier = Modifier.weight(1f))
            } else {
                PetList(
                    pets       = pets,
                    onPetClick = onPetClick,
                    onDeletePet = { pendingDelete = it },
                    modifier   = Modifier.weight(1f)
                )
            }
        }
    }
    if (showConfetti) {
        KonfettiView(
            modifier = Modifier.fillMaxSize(),
            parties  = confettiParties
        )
    }
    } // Box
}

@Composable
private fun BirthdayBanner(
    petName: String,
    onDismiss: () -> Unit
) {
    val gradientColors = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(gradientColors))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector        = Icons.Default.Cake,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier           = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = stringResource(R.string.birthday_banner_title, petName),
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text  = stringResource(R.string.birthday_banner_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector        = Icons.Default.Close,
                contentDescription = stringResource(R.string.action_dismiss),
                tint               = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier           = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun EmptyPetsContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.home_no_pets_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_no_pets_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PetList(
    pets: List<PetEntity>,
    onPetClick: (Long) -> Unit,
    onDeletePet: (PetEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val tablet = isTablet()
    if (tablet) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = adaptiveHorizontalPadding(), vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = pets, key = { it.id }) { pet ->
                PetCard(pet = pet, onClick = { onPetClick(pet.id) }, onDelete = { onDeletePet(pet) })
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = pets, key = { it.id }) { pet ->
                PetCard(pet = pet, onClick = { onPetClick(pet.id) }, onDelete = { onDeletePet(pet) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PetCard(
    pet: PetEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier             = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PetAvatar(photoPath = pet.photoPath, petName = pet.name, size = 56.dp)

            val context = LocalContext.current
            Column(modifier = Modifier.weight(1f)) {
                Text(pet.name, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                val speciesLabel = petSpeciesLabel(context, pet.species, pet.customSpecies)
                val subtitle = if (pet.breed != null) "$speciesLabel • ${pet.breed}" else speciesLabel
                Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                Text("${pet.weightKg} kg", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (pet.photoPath == null || pet.birthDate == null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text  = stringResource(R.string.home_profile_incomplete),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                    )
                }
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.home_pet_options_cd),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                        onClick = { menuExpanded = false; onDelete() }
                    )
                }
            }
        }
    }
}

private fun petSpeciesLabel(context: Context, species: String, customSpecies: String?): String = when (species) {
    "DOG"     -> context.getString(com.cuidadopet.R.string.species_dog)
    "CAT"     -> context.getString(com.cuidadopet.R.string.species_cat)
    "RABBIT"  -> context.getString(com.cuidadopet.R.string.species_rabbit)
    "BIRD"    -> context.getString(com.cuidadopet.R.string.species_bird)
    "HAMSTER" -> context.getString(com.cuidadopet.R.string.species_hamster)
    "TURTLE"  -> context.getString(com.cuidadopet.R.string.species_turtle)
    "FISH"    -> context.getString(com.cuidadopet.R.string.species_fish)
    "OTHER"   -> customSpecies?.takeIf { it.isNotBlank() } ?: context.getString(com.cuidadopet.R.string.species_other)
    else      -> species
}
