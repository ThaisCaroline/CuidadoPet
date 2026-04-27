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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuidadopet.ui.utils.adaptiveHorizontalPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Política de Privacidade") },
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

            PolicySection("CuidadoPet — Política de Privacidade",
                "Última atualização: abril de 2026")

            PolicySection("1. Sobre o aplicativo",
                "O CuidadoPet é um aplicativo de organização de cuidados com animais de " +
                "estimação domésticos. O app é local — todos os dados são armazenados " +
                "exclusivamente no dispositivo do usuário. Não há servidores, contas de " +
                "usuário ou sincronização na nuvem.")

            PolicySection("2. Dados coletados",
                "O CuidadoPet não coleta nem transmite nenhum dado pessoal ou de saúde " +
                "para servidores externos.\n\n" +
                "Os seguintes dados são criados e armazenados APENAS localmente no dispositivo:\n" +
                "• Nome, espécie, raça e peso dos pets cadastrados\n" +
                "• Registros de medicamentos e horários de administração\n" +
                "• Planos alimentares e registros de refeições\n" +
                "• Registros de hidratação\n" +
                "• Entradas do diário de saúde e histórico de peso\n" +
                "• Foto do pet (salva no armazenamento interno do app)")

            PolicySection("3. Permissões do aplicativo",
                "• Notificações: para enviar lembretes de medicamentos e refeições\n" +
                "• Alarmes exatos: para garantir que os lembretes sejam ponteiros\n" +
                "• Câmera: para tirar foto do pet (opcional)\n" +
                "• Acesso a imagens: para escolher foto da galeria (opcional)\n" +
                "• Inicialização do sistema: para reconfigurar alarmes após reinicialização")

            PolicySection("4. Compartilhamento de dados",
                "O app não compartilha dados com terceiros de forma automática.\n\n" +
                "O tutor pode optar por compartilhar relatórios manualmente (WhatsApp, " +
                "e-mail, etc.) usando as funções de exportação. Esse compartilhamento " +
                "é sempre iniciado pela ação explícita do usuário.")

            PolicySection("5. Aviso médico-veterinário",
                "O CuidadoPet é uma ferramenta de organização pessoal e NÃO substitui " +
                "avaliação, diagnóstico ou prescrição veterinária. As informações " +
                "registradas no app são observações do tutor e não devem ser usadas " +
                "como base para decisões médicas sem orientação profissional.")

            PolicySection("6. Retenção e exclusão de dados",
                "Todos os dados ficam no dispositivo. Para apagar os dados, o usuário " +
                "pode desinstalar o aplicativo, o que remove todos os arquivos e o " +
                "banco de dados local.")

            PolicySection("7. Menores de idade",
                "O aplicativo não coleta dados de menores de idade. O uso é destinado " +
                "a tutores adultos responsáveis pelos animais.")

            PolicySection("8. Contato",
                "Dúvidas sobre esta política podem ser enviadas para o e-mail de " +
                "suporte do desenvolvedor disponível na página do app na Google Play.")

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
