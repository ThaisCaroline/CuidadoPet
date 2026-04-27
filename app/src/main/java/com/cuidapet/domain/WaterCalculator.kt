package com.cuidadopet.domain

// Calculadora de necessidade hídrica diária
// Referência: WSAVA — 40 a 60 ml por kg de peso corporal por dia
// Para pets doentes o veterinário pode prescrever valores diferentes —
// o app exibe a sugestão mas sempre permite sobrescrever
object WaterCalculator {

    // Referência mínima: 40 ml/kg/dia (pet saudável em repouso)
    const val MIN_ML_PER_KG = 40.0

    // Referência máxima: 60 ml/kg/dia (pet ativo ou em ambiente quente)
    const val MAX_ML_PER_KG = 60.0

    // Sugestão central usada como padrão inicial (média entre min e max)
    const val DEFAULT_ML_PER_KG = 50.0

    // Calcula a meta sugerida de água em ml para o dia
    // Ex: pet de 5 kg → 250 ml/dia
    fun suggestedDailyMl(weightKg: Double): Double = weightKg * DEFAULT_ML_PER_KG

    // Calcula o percentual de hidratação do dia em relação à meta
    // Retorna 0–100+ (pode ultrapassar 100 se o pet bebeu mais que a meta)
    fun hydrationPercentage(consumedMl: Double, targetMl: Double): Int {
        if (targetMl <= 0) return 0
        return ((consumedMl / targetMl) * 100).toInt()
    }

    // Avalia o status de hidratação do dia
    fun evaluateHydration(consumedMl: Double, targetMl: Double): HydrationStatus {
        val pct = hydrationPercentage(consumedMl, targetMl)
        return when {
            consumedMl == 0.0 -> HydrationStatus.NO_RECORD
            pct < 50          -> HydrationStatus.BELOW_MINIMUM
            pct < 80          -> HydrationStatus.PARTIAL
            pct > 120         -> HydrationStatus.ABOVE_RECOMMENDED
            else              -> HydrationStatus.SUFFICIENT
        }
    }
}

// Status de hidratação do dia — usado no card de resumo
enum class HydrationStatus {
    SUFFICIENT,          // Hidratação adequada
    PARTIAL,             // Bebeu, mas ainda abaixo da meta
    BELOW_MINIMUM,       // Muito abaixo — alerta
    ABOVE_RECOMMENDED,   // Acima do recomendado (pode indicar polidipsia)
    NO_RECORD            // Nenhum registro no dia
}

// Texto de exibição para cada status
fun HydrationStatus.toDisplayText(): String = when (this) {
    HydrationStatus.SUFFICIENT        -> "Hidratação adequada"
    HydrationStatus.PARTIAL           -> "Abaixo da meta — continue oferecendo água"
    HydrationStatus.BELOW_MINIMUM     -> "Muito pouca água hoje — monitore com atenção"
    HydrationStatus.ABOVE_RECOMMENDED -> "Consumo elevado — consulte o veterinário se persistir"
    HydrationStatus.NO_RECORD         -> "Nenhum registro hoje"
}
