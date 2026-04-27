package com.cuidadopet.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────
// Tipografia do CuidadoPet
//
// Fonte ideal: Nunito (arredondada, acolhedora, gratuita no Google Fonts)
// Por enquanto usamos FontFamily.Default (fonte padrão do sistema Android)
// para manter o build limpo. A Nunito será adicionada como arquivo .ttf
// na pasta res/font/ em uma próxima etapa.
// ─────────────────────────────────────────────

// TODO: substituir por NunitoFontFamily quando os arquivos .ttf forem adicionados
// Passos para adicionar depois:
// 1. Baixar Nunito em fonts.google.com (pesos: 400, 500, 600, 700)
// 2. Colocar os .ttf em app/src/main/res/font/
// 3. Criar FontFamily referenciando os arquivos
// 4. Substituir FontFamily.Default abaixo por NunitoFontFamily
private val AppFontFamily = FontFamily.Default

// ─────────────────────────────────────────────
// Sistema tipográfico do Material3
//
// Material3 define uma hierarquia de estilos de texto com nomes semânticos.
// Os mais usados no CuidadoPet serão:
//   titleLarge   → títulos de tela ("Medicamentos do Rex")
//   titleMedium  → subtítulos de seção ("Hoje")
//   bodyLarge    → textos corridos principais
//   bodyMedium   → textos secundários, descrições
//   labelSmall   → legendas minúsculas, timestamps
// ─────────────────────────────────────────────
val Typography = Typography(

    // Título grande — nome do pet no topo da tela, por exemplo
    titleLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),

    // Título médio — títulos de seção dentro de uma tela
    titleMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),

    // Título pequeno — labels de cards
    titleSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // Texto principal — conteúdo corrido, descrições longas
    bodyLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),

    // Texto secundário — informações de apoio, horários, detalhes
    bodyMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),

    // Texto pequeno — observações, datas, metadados
    bodySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),

    // Label de botão — texto dentro de botões e chips
    labelLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // Label pequeno — badges, timestamps, rodapés de card
    labelSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
