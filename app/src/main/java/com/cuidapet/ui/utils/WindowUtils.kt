package com.cuidadopet.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** True quando a tela tem largura ≥ 600 dp (tablet ou telefone em paisagem). */
@Composable
fun isTablet(): Boolean = LocalConfiguration.current.screenWidthDp >= 600

/**
 * Padding horizontal adaptativo para formulários.
 * Em telas largas, centraliza o conteúdo em ~560 dp de largura útil.
 * Em telas pequenas, retorna 16 dp.
 */
@Composable
fun adaptiveHorizontalPadding(): Dp {
    val w = LocalConfiguration.current.screenWidthDp
    return if (w >= 600) ((w - 560).coerceAtLeast(32) / 2).dp else 16.dp
}
