package com.cuidadopet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// ─────────────────────────────────────────────
// Tema do CuidadoPet
//
// O Material3 (sistema de design do Google) organiza as cores
// em "papéis" semânticos — cada papel tem um propósito claro na interface.
// Aqui mapeamos as cores do CuidadoPet para esses papéis.
//
// Nota: não implementamos tema escuro na v1.0 para manter o escopo gerenciável.
// ─────────────────────────────────────────────

// Esquema de cores claro do CuidadoPet
// lightColorScheme() recebe os papéis do Material3 e retorna um objeto
// que o MaterialTheme distribui automaticamente para todos os componentes
private val CuidadoPetColorScheme = lightColorScheme(

    // primary: cor mais importante do app — usada em botões, AppBar, FAB
    primary = Teal,

    // onPrimary: cor do texto/ícone em cima do primary
    // "on" sempre significa "o que vai em cima de"
    onPrimary = WarmWhite,

    // primaryContainer: versão suave do primary — fundo de cards de destaque
    primaryContainer = TealLight,

    // onPrimaryContainer: texto em cima do primaryContainer
    onPrimaryContainer = TealDark,

    // secondary: cor de destaque secundária — badges, chips, ações alternativas
    secondary = Amber,

    // onSecondary: texto em cima do secondary
    onSecondary = CharcoalGray,

    // secondaryContainer: versão suave do secondary
    secondaryContainer = AmberLight,

    // onSecondaryContainer: texto em cima do secondaryContainer
    onSecondaryContainer = AmberDark,

    // tertiary: terceira cor — usada para sucesso e indicadores positivos
    tertiary = GreenSage,

    // onTertiary: texto em cima do tertiary
    onTertiary = WarmWhite,

    // tertiaryContainer: versão suave do tertiary
    tertiaryContainer = GreenSageLight,

    // onTertiaryContainer: texto em cima do tertiaryContainer
    onTertiaryContainer = GreenSage,

    // error: cor de erro e alerta — "não tomou o remédio", avisos críticos
    error = CoralSoft,

    // onError: texto em cima do error
    onError = WarmWhite,

    // errorContainer: fundo suave de mensagens de erro
    errorContainer = CoralSoftLight,

    // onErrorContainer: texto em cima do errorContainer
    onErrorContainer = CoralSoft,

    // background: cor de fundo de todas as telas
    background = WarmWhite,

    // onBackground: cor do texto principal sobre o fundo
    onBackground = CharcoalGray,

    // surface: fundo de cards, dialogs, bottom sheets
    surface = WarmWhite,

    // onSurface: texto principal sobre surface
    onSurface = CharcoalGray,

    // surfaceVariant: fundo de elementos de input (campos de texto, etc.)
    surfaceVariant = LightGray,

    // onSurfaceVariant: texto secundário — legendas, dicas, placeholders
    onSurfaceVariant = MediumGray,

    // outline: bordas de campos, divisores entre seções
    outline = LightGray
)

// ─────────────────────────────────────────────
// CuidadoPetTheme — função que envolve todo o app
//
// Todo app Compose precisa de um "tema raiz" que envolve todos os composables.
// Funciona como uma configuração global: qualquer componente dentro dele
// automaticamente herda as cores, fontes e formas definidas aqui.
//
// Uso:
//   CuidadoPetTheme {
//       // todo o conteúdo do app aqui dentro
//   }
// ─────────────────────────────────────────────
@Composable
fun CuidadoPetTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CuidadoPetColorScheme, // nossa paleta de cores
        typography = Typography,            // nossa tipografia (Nunito)
        content = content                   // o conteúdo real do app
    )
}
