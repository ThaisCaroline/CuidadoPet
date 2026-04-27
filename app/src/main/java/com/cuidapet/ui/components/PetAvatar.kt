package com.cuidadopet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

// Exibe a foto do pet em um círculo.
// Se não houver foto (photoPath == null), mostra um ícone de pata como placeholder.
// Reutilizado no HomeScreen (lista de pets) e no PetDashboardScreen (cabeçalho).
@Composable
fun PetAvatar(
    photoPath: String?,
    petName: String,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!photoPath.isNullOrBlank()) {
            // AsyncImage: carrega a imagem de forma assíncrona sem bloquear a UI.
            // model = File porque o caminho está no armazenamento interno do app.
            // ContentScale.Crop preenche o círculo sem distorção.
            AsyncImage(
                model              = File(photoPath),
                contentDescription = "Foto de $petName",
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize()
            )
        } else {
            // Placeholder com ícone de pata quando não há foto cadastrada
            Icon(
                imageVector        = Icons.Default.Pets,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(size * 0.5f)
            )
        }
    }
}
