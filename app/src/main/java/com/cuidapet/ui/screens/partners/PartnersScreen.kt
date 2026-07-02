package com.cuidadopet.ui.screens.partners

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cuidadopet.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class Partner(
    val name: String = "",
    val type: String = "",
    val city: String = "",
    val photoUrl: String? = null,
    val instagram: String? = null,
    val whatsapp: String? = null,
    val phone: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnersScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current

    val partners = remember {
        try {
            val json = context.assets.open("partners.json").bufferedReader().readText()
            val type = object : TypeToken<List<Partner>>() {}.type
            Gson().fromJson<List<Partner>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.partners_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor      = MaterialTheme.colorScheme.primary,
                    titleContentColor   = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        if (partners.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.Store,
                        contentDescription = null,
                        modifier           = Modifier.size(72.dp),
                        tint               = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text      = stringResource(R.string.partners_empty),
                        style     = MaterialTheme.typography.bodyLarge,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding      = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(partners) { partner ->
                    PartnerCard(partner = partner)
                }
            }
        }
    }
}

@Composable
private fun PartnerCard(partner: Partner) {
    val context = LocalContext.current

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            if (!partner.photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model              = partner.photoUrl,
                    contentDescription = partner.name,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text       = partner.name,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                if (partner.type.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text  = partner.type,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (partner.city.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text  = partner.city,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val hasActions = !partner.instagram.isNullOrBlank() ||
                        !partner.whatsapp.isNullOrBlank() ||
                        !partner.phone.isNullOrBlank()

                if (hasActions) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!partner.instagram.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(partner.instagram))
                                    )
                                }
                            ) {
                                Icon(
                                    Icons.Default.Language,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Instagram")
                            }
                        }
                        if (!partner.whatsapp.isNullOrBlank()) {
                            Button(
                                onClick = {
                                    val uri = Uri.parse("https://wa.me/${partner.whatsapp}")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text("WhatsApp")
                            }
                        }
                        if (!partner.phone.isNullOrBlank()) {
                            IconButton(
                                onClick = {
                                    val number = partner.phone!!.replace(Regex("[^0-9+]"), "")
                                    context.startActivity(
                                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                                    )
                                }
                            ) {
                                Icon(
                                    Icons.Default.Call,
                                    contentDescription = partner.phone,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
