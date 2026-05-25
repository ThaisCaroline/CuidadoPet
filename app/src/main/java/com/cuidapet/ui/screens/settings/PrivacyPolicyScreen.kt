package com.cuidadopet.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuidadopet.R
import com.cuidadopet.ui.utils.adaptiveHorizontalPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_title)) },
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = adaptiveHorizontalPadding())
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            PolicySection(
                "${stringResource(R.string.app_name)} — ${stringResource(R.string.privacy_title)}",
                stringResource(R.string.privacy_last_update)
            )
            PolicySection(stringResource(R.string.privacy_s1_title), stringResource(R.string.privacy_s1_body))
            PolicySection(stringResource(R.string.privacy_s2_title), stringResource(R.string.privacy_s2_body))
            PolicySection(stringResource(R.string.privacy_s3_title), stringResource(R.string.privacy_s3_body))
            PolicySection(stringResource(R.string.privacy_s4_title), stringResource(R.string.privacy_s4_body))
            PolicySection(stringResource(R.string.privacy_s5_title), stringResource(R.string.privacy_s5_body))
            PolicySection(stringResource(R.string.privacy_s6_title), stringResource(R.string.privacy_s6_body))
            PolicySection(stringResource(R.string.privacy_s7_title), stringResource(R.string.privacy_s7_body))
            PolicySection(stringResource(R.string.privacy_s8_title), stringResource(R.string.privacy_s8_body))

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(body, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface)
    }
}
