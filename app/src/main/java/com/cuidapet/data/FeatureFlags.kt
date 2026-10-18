package com.cuidadopet.data

// Liga/desliga recursos inteiros do app sem apagar o código.
object FeatureFlags {
    // Leitura de receita por foto. Desligada: o código fica, mas o botão não aparece.
    // Reative só depois de testar com receitas reais.
    const val PRESCRIPTION_SCAN_ENABLED = false
}
