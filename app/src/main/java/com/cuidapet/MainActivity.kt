package com.cuidadopet

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.cuidadopet.ui.navigation.AppNavigation
import com.cuidadopet.ui.theme.CuidadoPetTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // installSplashScreen() deve ser chamado ANTES de super.onCreate().
        // Ele intercepta a tela de inicialização do sistema e aplica nosso tema splash
        // (definido em themes.xml como Theme.CuidadoPet.Splash).
        // A patinha fica visível enquanto o Hilt e o Room inicializam em background.
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CuidadoPetTheme {
                // Android 13+ exige que a permissão de notificação seja pedida em runtime.
                // Sem isso, nenhuma notificação é exibida mesmo com o canal configurado.
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { /* o sistema exibe o diálogo — não precisamos tratar o resultado aqui */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                AppNavigation()
            }
        }
    }
}