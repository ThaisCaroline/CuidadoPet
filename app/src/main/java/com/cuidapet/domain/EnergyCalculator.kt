package com.cuidadopet.domain

import kotlin.math.pow

// Calculadora de necessidade energética diária
// Fórmulas baseadas em WSAVA e Purina Institute
// Fonte: ARQUITETURA_TECNICA.md — seção 8.1
object EnergyCalculator {

    // RER = Resting Energy Requirement (energia em repouso)
    // Fórmula base para cães e gatos: 70 × (peso)^0.75 kcal/dia
    // Esta é a energia mínima para funções vitais — respirar, bombear sangue, etc.
    fun rer(weightKg: Double): Double = 70.0 * weightKg.pow(0.75)

    // Fator de condição clínica — multiplica o RER para obter a meta real
    // Pets doentes/restritos precisam de MENOS energia (não desperdiçar com atividade)
    // Pets saudáveis em manutenção precisam de MAIS (incluir atividade normal)
    fun conditionFactor(clinicalStates: Set<String>): Double = when {
        "POST_SURGICAL"    in clinicalStates -> 1.1  // pós-cirúrgico: levemente acima do RER
        "CHRONIC_DISEASE"  in clinicalStates -> 1.0  // doença crônica: só o básico
        "ACTIVE_TREATMENT" in clinicalStates -> 1.0  // tratamento ativo: conservador
        "RECOVERY"         in clinicalStates -> 1.2  // recuperação: um pouco mais
        else                                 -> 1.4  // manutenção preventiva: atividade normal
    }

    // Calcula a meta calórica diária em kcal
    // Exibe como SUGESTÃO — o tutor pode sobrescrever conforme orientação veterinária
    fun dailyKcalTarget(weightKg: Double, clinicalStates: Set<String>): Double {
        return rer(weightKg) * conditionFactor(clinicalStates)
    }

    // Avalia o consumo diário em relação à meta
    // Retorna um status para exibir no resumo do dia
    fun evaluateDailyIntake(eatenPercentageAvg: Int): FeedingStatus = when {
        eatenPercentageAvg == 0   -> FeedingStatus.DID_NOT_EAT
        eatenPercentageAvg < 50   -> FeedingStatus.BELOW_MINIMUM
        eatenPercentageAvg > 110  -> FeedingStatus.ABOVE_RECOMMENDED
        else                      -> FeedingStatus.SUFFICIENT
    }
}

// Status de alimentação do dia — usado na tela de resumo
enum class FeedingStatus {
    SUFFICIENT,          // ✅ Comeu o suficiente
    BELOW_MINIMUM,       // ⚠️ Comeu abaixo do mínimo
    ABOVE_RECOMMENDED,   // ⚠️ Comeu acima do recomendado
    DID_NOT_EAT          // ❌ Não comeu
}

// Retorna o texto e emoji de cada status para exibir na tela
fun FeedingStatus.toDisplayText(): String = when (this) {
    FeedingStatus.SUFFICIENT         -> "✅ Comeu o suficiente"
    FeedingStatus.BELOW_MINIMUM      -> "⚠️ Abaixo do mínimo recomendado"
    FeedingStatus.ABOVE_RECOMMENDED  -> "⚠️ Acima do recomendado"
    FeedingStatus.DID_NOT_EAT        -> "❌ Não comeu hoje"
}
