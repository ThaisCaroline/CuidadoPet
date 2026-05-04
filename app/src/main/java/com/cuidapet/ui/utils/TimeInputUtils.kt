package com.cuidadopet.ui.utils

// Formata automaticamente uma string enquanto o tutor digita um horário HH:mm.
// Exemplos: "1" → "1", "13" → "13:", "130" → "13:0", "1300" → "13:00"
// Trata horas impossíveis: "250" → "2:50" (25h não existe)
fun autoFormatTime(raw: String): String {
    if (Regex("""^\d{1,2}:\d{0,2}$""").matches(raw)) return raw
    val digits = raw.filter { it.isDigit() }.take(4)
    if (digits.isEmpty()) return ""
    return when (digits.length) {
        1    -> digits
        2    -> {
            val twoDigit = digits[0].digitToInt() * 10 + digits[1].digitToInt()
            if (digits[0].digitToInt() >= 3 || twoDigit > 23) "${digits[0]}:${digits[1]}"
            else "${digits[0]}${digits[1]}:"
        }
        3    -> {
            val twoDigit = digits[0].digitToInt() * 10 + digits[1].digitToInt()
            if (digits[0].digitToInt() >= 3 || twoDigit > 23) "${digits[0]}:${digits[1]}${digits[2]}"
            else "${digits[0]}${digits[1]}:${digits[2]}"
        }
        else -> "${digits[0]}${digits[1]}:${digits[2]}${digits[3]}"
    }
}

// Completa um horário incompleto ao sair do campo (perda de foco).
// Garante que o resultado final sempre seja "HH:mm" válido.
// Exemplos: "8" → "08:00" | "13" → "13:00" | "8:3" → "08:30" | "13:5" → "13:50"
fun normalizeTime(raw: String): String {
    if (raw.isBlank()) return ""
    val colonIdx = raw.indexOf(':')
    val hourStr: String
    val minStr: String
    if (colonIdx >= 0) {
        hourStr = raw.take(colonIdx).filter { it.isDigit() }
        minStr  = raw.drop(colonIdx + 1).filter { it.isDigit() }
    } else {
        val digits   = raw.filter { it.isDigit() }
        val twoDigit = if (digits.length >= 2)
            digits[0].digitToInt() * 10 + digits[1].digitToInt() else -1
        if (digits.length >= 2 && digits[0].digitToInt() < 3 && twoDigit <= 23) {
            hourStr = digits.take(2)
            minStr  = digits.drop(2)
        } else {
            hourStr = digits.take(1)
            minStr  = digits.drop(1)
        }
    }
    val hour   = hourStr.toIntOrNull()?.coerceIn(0, 23) ?: 0
    val minute = when {
        minStr.length >= 2 -> minStr.take(2).toInt().coerceIn(0, 59)
        minStr.length == 1 -> (minStr[0].digitToInt() * 10).coerceIn(0, 59)
        else               -> 0
    }
    return "%02d:%02d".format(hour, minute)
}

// Calcula onde o cursor deve ficar no texto formatado com base em quantos dígitos
// existiam antes do cursor no texto bruto. Evita que o cursor sempre vá pro final,
// o que tornava impossível editar as horas (início do campo).
// Quando um ":" é auto-inserido, o cursor avança automaticamente para depois dele.
fun cursorAfterFormat(rawText: String, rawCursorPos: Int, formatted: String): Int {
    val digitsBeforeCursor = rawText.take(rawCursorPos).count { it.isDigit() }
    if (digitsBeforeCursor == 0) return 0
    var digitCount = 0
    for (i in formatted.indices) {
        if (formatted[i].isDigit()) {
            digitCount++
            if (digitCount == digitsBeforeCursor) {
                // Se o próximo char é ":", avança o cursor para depois dele
                val next = i + 1
                return if (next < formatted.length && formatted[next] == ':') next + 1 else next
            }
        }
    }
    return formatted.length
}
