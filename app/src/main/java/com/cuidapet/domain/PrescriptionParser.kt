package com.cuidadopet.domain

import java.util.Locale

// Um medicamento encontrado no texto da receita, ainda não salvo.
// O tutor sempre confere esses dados antes de salvar.
data class ParsedMedication(
    val name: String,
    val dose: String,
    val doseUnit: String,
    // "INTERVAL" ou "FIXED_TIMES", igual ao MedicationEntity
    val frequencyType: String,
    // Usado quando frequencyType = "INTERVAL" (a cada X horas)
    val frequencyHours: Int? = null,
    // Usado quando frequencyType = "FIXED_TIMES" (formato HH:mm)
    val fixedTimes: List<String> = emptyList(),
    // Primeira dose, quando a receita informa um horário (formato HH:mm)
    val startTime: String? = null,
    // null quando a receita não informa a duração (ou o tratamento é contínuo): o tutor precisa preencher
    val durationDays: Int?,
    // true quando algum dado não foi encontrado, foi preenchido por padrão, ou o tratamento é contínuo
    val needsReview: Boolean,
    // false quando o nome não parece de medicamento (ex.: palavra de cabeçalho). Não sugerido para salvar.
    val nameRecognized: Boolean = true,
    // Linha original da receita, para o tutor conferir
    val sourceLine: String
)

// Interpreta o texto cru do OCR e tenta achar nome, dose, frequência, horário e duração.
// É heurística: o resultado sempre passa pela conferência do tutor.
object PrescriptionParser {

    private const val DEFAULT_INTERVAL_HOURS = 24

    // Unidades de dose. (?![\p{L}]) impede casar o começo de outra palavra ("dose", "comprimidos" etc.)
    private const val UNIT =
        """(comprimidos?|comp\.?|cpr?s?|gotas?|ml|ampolas?|sach[eê]s?|aplica[cç][aã]o(?:es)?|c[aá]psulas?)(?![\p{L}])"""

    private val mixedFractionDose = Regex("""(\d+)\s+(\d+)/(\d+)\s*$UNIT""", RegexOption.IGNORE_CASE)
    private val oneAndHalfDose = Regex("""(\d+)\s*½\s*$UNIT""", RegexOption.IGNORE_CASE)
    private val fractionDose = Regex("""(?<![\d/])(\d+)/(\d+)\s*$UNIT""", RegexOption.IGNORE_CASE)
    private val halfDose = Regex("""(½|meio|meia)\s*$UNIT""", RegexOption.IGNORE_CASE)
    private val numberDose = Regex("""(\d+(?:[.,]\d+)?)\s*$UNIT""", RegexOption.IGNORE_CASE)

    // Intervalo: "a cada 12 horas", "a cada 8h", "de 8 em 8 horas", "8/8h", "8h/8h", "q8h"
    private val intervalRegex = Regex("""a cada\s*(\d{1,2})\s*(?:h|hs|horas?)(?![\p{L}])""", RegexOption.IGNORE_CASE)
    private val deEmRegex = Regex("""(?:de\s*)?(\d{1,2})\s*em\s*(\d{1,2})\s*(?:h|hs|horas?)(?![\p{L}])""", RegexOption.IGNORE_CASE)
    private val slashRegex = Regex("""(\d{1,2})\s*h?\s*/\s*(\d{1,2})\s*(?:h|hs|horas?)(?![\p{L}])""", RegexOption.IGNORE_CASE)
    private val qRegex = Regex("""\bq(\d{1,2})\s*h(?![\p{L}])""", RegexOption.IGNORE_CASE)

    // Vezes ao dia: "2x ao dia", "2x/dia", "3 vezes por dia", "duas vezes ao dia", "uma vez ao dia"
    private val timesPerDayRegex = Regex(
        """(\d{1,2}|um|uma|dois|duas|tr[eê]s|quatro|cinco|seis)\s*(?:x|vezes?)\s*(?:ao|por|/|a)?\s*dia""",
        RegexOption.IGNORE_CASE
    )
    // Abreviações: BID (2x), TID (3x), QID (4x)
    private val bidTidQidRegex = Regex("""\b(bid|bd|tid|tds|qid|qds)(?![\p{L}])""", RegexOption.IGNORE_CASE)

    // Horários explícitos: "08:00", "8:30", "às 8h", "às 8h30", "as 20h"
    private val colonTimeRegex = Regex("""(?<![\d:])(\d{1,2}:\d{2})(?![\d:])""")
    private val asTimeRegex = Regex(
        """(?:^|\s)(?:às|as)\s+(\d{1,2}(?:\s*[:h.]\s*\d{2}|\s*h(?:s)?)?)""",
        RegexOption.IGNORE_CASE
    )

    // Duração: "por 7 dias", "10d", "durante 10 dias", "por 2 semanas", "1 semana"
    private val durationDaysRegex = Regex("""(?:por|durante)?\s*(\d{1,3})\s*(?:dias?|d)(?![\p{L}])""", RegexOption.IGNORE_CASE)
    private val durationWeeksRegex = Regex("""(\d{1,2})\s*semanas?(?![\p{L}])""", RegexOption.IGNORE_CASE)
    private val continuousRegex = Regex(
        """uso\s+cont[ií]nuo|cont[ií]nuo|at[eé]\s+acabar|sem\s+prazo""",
        RegexOption.IGNORE_CASE
    )

    // Linhas que começam com verbo de instrução não são nome de medicamento
    private val instructionStart = Regex(
        """^(dar|tomar|usar|aplicar|administrar|ingerir|uso|dose|via|posologia)(?![\p{L}])""",
        RegexOption.IGNORE_CASE
    )

    private val timeRegex = Regex("""(\d{1,2})\s*(?:[:h.]\s*(\d{2})?\s*s?)?""")

    // Palavras que começam a parte de dose/instrução dentro da linha, e não fazem parte do nome
    private val nameStopWords = Regex(
        """\s(?:meio|meia|uso|tomar|dar|por|durante|às|as)(?=\s|$)""",
        RegexOption.IGNORE_CASE
    )

    private data class Frequency(
        val type: String,
        val hours: Int?,
        val times: List<String>,
        val startTime: String?
    )

    // Normaliza um horário digitado de vários jeitos para "HH:mm".
    // Aceita: 8h, 8hs, 8h30, 8:30, 8.30, 08:00, 8 h, às 8h. Retorna null se não for um horário válido.
    fun normalizeTime(raw: String): String? {
        val text = raw.trim().lowercase(Locale.ROOT)
            .removePrefix("às").removePrefix("as").trim()
        val match = timeRegex.matchEntire(text) ?: return null
        val hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].ifEmpty { "0" }.toInt()
        if (hour !in 0..23 || minute !in 0..59) return null
        return "%02d:%02d".format(hour, minute)
    }

    fun parse(text: String): List<ParsedMedication> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val found = mutableListOf<ParsedMedication>()
        var lastName: String? = null

        for (line in lines) {
            val nameOnLine = extractName(line)
            val dose = findDose(line)
            val frequencyFound = frequencyOf(line)

            // Linha sem dose nem frequência: provavelmente é só o nome do medicamento.
            // Guardamos para usar na próxima linha de instrução ("Dar 1 comprimido...").
            if (dose == null && frequencyFound == null) {
                if (nameOnLine != null) lastName = nameOnLine
                continue
            }

            val name = nameOnLine ?: lastName ?: continue
            lastName = null

            val durationFound = durationOf(line)
            val continuous = continuousRegex.containsMatchIn(line)
            val frequency = frequencyFound ?: Frequency("INTERVAL", DEFAULT_INTERVAL_HOURS, emptyList(), null)

            val parsed = ParsedMedication(
                name = name,
                dose = dose?.first ?: "1",
                doseUnit = dose?.second ?: "comprimido",
                frequencyType = frequency.type,
                frequencyHours = frequency.hours,
                fixedTimes = frequency.times,
                startTime = frequency.startTime,
                durationDays = durationFound?.coerceIn(1, 3650),
                nameRecognized = MedicationNames.isLikelyMedication(name),
                needsReview = nameOnLine == null || dose == null || frequencyFound == null ||
                    durationFound == null || continuous,
                sourceLine = line
            )

            // Mesmo medicamento aparecendo duas vezes, mantém só o primeiro
            if (found.none { it.name.equals(name, ignoreCase = true) }) found += parsed
        }

        return found
    }

    // Nome = texto antes do primeiro número ou palavra de dose/instrução.
    // Ex: "Amoxicilina 250mg" → "Amoxicilina"; "Antibiótico meio comprimido" → "Antibiótico"
    private fun extractName(line: String): String? {
        if (instructionStart.containsMatchIn(line)) return null
        val beforeDigit = line.takeWhile { !it.isDigit() }
        val stop = nameStopWords.find(beforeDigit)?.range?.first
        val raw = if (stop != null) beforeDigit.substring(0, stop) else beforeDigit
        val name = raw.trim(' ', '-', ':', ',', '.', '*')
        // Cabeçalho (tutor, CRMV, clínica, endereço) e frases longas não são nome de medicamento
        if (headerRegex.containsMatchIn(name) || name.split(" ").size > 4) return null
        return name.takeIf { n -> n.count { it.isLetter() } >= 3 && n.length <= 60 }
    }

    private val headerRegex = Regex(
        """:|\b(crmv?|tutor|paciente|data|receita|rua|av|avenida|cpf|cnpj|cep|tel|telefone|fone|endere[cç]o|cl[ií]nica|veterin[aá]ri[oa]|dr|dra)\b""",
        RegexOption.IGNORE_CASE
    )

    // Retorna (valor, unidade) da dose, na ordem de especificidade: misto, fração, meio, número
    private fun findDose(line: String): Pair<String, String>? {
        mixedFractionDose.find(line)?.let { m ->
            val (whole, num, den) = m.destructured
            return formatAmount(whole.toDouble() + num.toDouble() / den.toDouble()) to normalizeUnit(m.groupValues[4])
        }
        oneAndHalfDose.find(line)?.let { m ->
            return formatAmount(m.groupValues[1].toDouble() + 0.5) to normalizeUnit(m.groupValues[2])
        }
        fractionDose.find(line)?.let { m ->
            val (num, den) = m.destructured
            if (den.toDouble() != 0.0) {
                return formatAmount(num.toDouble() / den.toDouble()) to normalizeUnit(m.groupValues[3])
            }
        }
        halfDose.find(line)?.let { m ->
            return formatAmount(0.5) to normalizeUnit(m.groupValues[2])
        }
        numberDose.find(line)?.let { m ->
            return m.groupValues[1].replace(',', '.') to normalizeUnit(m.groupValues[2])
        }
        return null
    }

    // 1.0 → "1", 0.5 → "0.5", 0.25 → "0.25"
    private fun formatAmount(value: Double): String =
        "%.2f".format(Locale.US, value).trimEnd('0').trimEnd('.')

    private fun frequencyOf(line: String): Frequency? {
        val explicitTimes = explicitTimes(line)

        intervalHours(line)?.let { hours ->
            return Frequency("INTERVAL", hours, emptyList(), explicitTimes.firstOrNull())
        }
        if (explicitTimes.isNotEmpty()) {
            return Frequency("FIXED_TIMES", null, explicitTimes, null)
        }
        timesPerDay(line)?.let { count ->
            return Frequency("FIXED_TIMES", null, spreadTimes(count), null)
        }
        return null
    }

    private fun intervalHours(line: String): Int? {
        val raw = listOf(intervalRegex, deEmRegex, slashRegex, qRegex).firstNotNullOfOrNull { regex ->
            regex.find(line)?.let { match -> match.groupValues.last().toIntOrNull() }
        }
        return raw?.takeIf { it in 1..720 }
    }

    private fun timesPerDay(line: String): Int? {
        timesPerDayRegex.find(line)?.let { match ->
            return wordToNumber(match.groupValues[1])?.takeIf { it in 1..6 }
        }
        bidTidQidRegex.find(line)?.let { match ->
            return when (match.groupValues[1].lowercase(Locale.ROOT)) {
                "bid", "bd" -> 2
                "tid", "tds" -> 3
                else -> 4
            }
        }
        return null
    }

    private fun wordToNumber(word: String): Int? = when (word.lowercase(Locale.ROOT)) {
        "um", "uma" -> 1
        "dois", "duas" -> 2
        "tres", "três" -> 3
        "quatro" -> 4
        "cinco" -> 5
        "seis" -> 6
        else -> word.toIntOrNull()
    }

    // Horários escritos na linha, normalizados para HH:mm, sem repetição
    private fun explicitTimes(line: String): List<String> {
        val found = mutableListOf<String>()
        colonTimeRegex.findAll(line).forEach { match ->
            normalizeTime(match.groupValues[1])?.let { found += it }
        }
        asTimeRegex.findAll(line).forEach { match ->
            normalizeTime(match.groupValues[1])?.let { found += it }
        }
        return found.distinct()
    }

    // Espalha as doses entre 08:00 e 20:00. Ex: 2x → 08:00, 20:00 | 3x → 08:00, 14:00, 20:00
    private fun spreadTimes(count: Int): List<String> {
        if (count == 1) return listOf("08:00")
        val step = 12 * 60 / (count - 1)
        return (0 until count).map { i ->
            val total = 8 * 60 + i * step
            "%02d:%02d".format(total / 60, total % 60)
        }
    }

    private fun durationOf(line: String): Int? {
        durationDaysRegex.find(line)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
        durationWeeksRegex.find(line)?.groupValues?.get(1)?.toIntOrNull()?.let { return it * 7 }
        return null
    }

    private fun normalizeUnit(raw: String): String {
        val u = raw.lowercase(Locale.ROOT)
        return when {
            u.startsWith("comp") || u.startsWith("cp") -> "comprimido"
            u.startsWith("gota") -> "gota"
            u == "ml" -> "ml"
            u.startsWith("ampola") -> "ampola"
            u.startsWith("sach") -> "sachê"
            u.startsWith("aplica") -> "aplicação"
            u.contains("psula") -> "cápsula"
            else -> "comprimido"
        }
    }
}
