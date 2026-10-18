package com.cuidadopet.domain

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

// Regras dos documentos em PDF do pet: limites, validação, busca e datas.
// Ficam fora da tela para poderem ser testadas sem Android.
object PetDocumentRules {

    // Tamanho máximo de um PDF
    const val MAX_DOCUMENT_BYTES = 20L * 1024 * 1024

    // Teto do backup inteiro (fotos + documentos). Acima disso o backup não é gerado,
    // porque a restauração também recusa esse tamanho.
    const val BACKUP_LIMIT_BYTES = 500L * 1024 * 1024

    // A partir daqui a tela de backup avisa que está perto do limite
    const val BACKUP_WARNING_BYTES = 400L * 1024 * 1024

    private val PDF_SIGNATURE = "%PDF".toByteArray(Charsets.US_ASCII)

    // Todo PDF começa com "%PDF"; isso evita aceitar qualquer arquivo renomeado
    fun isPdfHeader(head: ByteArray): Boolean =
        head.size >= PDF_SIGNATURE.size &&
            head.copyOfRange(0, PDF_SIGNATURE.size).contentEquals(PDF_SIGNATURE)

    // Texto para busca: sem acento, sem maiúsculas e com espaços simples
    fun normalizeForSearch(text: String): String = PetFilter.normalize(text)

    // Mesmo documento = mesmo título (sem acento e sem maiúsculas) e mesmo tamanho em bytes
    fun isSameDocument(titleA: String, sizeA: Long, titleB: String, sizeB: Long): Boolean =
        sizeA == sizeB && normalizeForSearch(titleA) == normalizeForSearch(titleB)

    // Busca por título (parte do texto, sem acento) e filtro de período opcional.
    // fromMillis é início do dia e toMillis é fim do dia, ambos no horário local.
    fun matches(
        title: String,
        documentDate: Long,
        query: String,
        fromMillis: Long?,
        toMillis: Long?
    ): Boolean {
        val q = normalizeForSearch(query)
        if (q.isNotEmpty() && !normalizeForSearch(title).contains(q)) return false
        if (fromMillis != null && documentDate < fromMillis) return false
        if (toMillis != null && documentDate > toMillis) return false
        return true
    }

    // O seletor de data do Material devolve meia-noite em UTC. O app guarda o início do dia local.
    fun utcMillisToLocalStartOfDay(utcMillis: Long): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
        return Calendar.getInstance().apply {
            set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // Início do dia local de um instante (usado para "hoje" no formulário)
    fun localStartOfDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return Calendar.getInstance().apply {
            set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // Caminho inverso: início do dia local vira meia-noite UTC do mesmo dia, para preencher o seletor
    fun localStartOfDayToUtcMillis(localStartOfDay: Long): Long {
        val local = Calendar.getInstance().apply { timeInMillis = localStartOfDay }
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // Último instante do dia local (23:59:59.999), para o fim do período de busca
    fun localEndOfDay(localStartOfDay: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = localStartOfDay
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

    // Ex.: 12.4 MB
    fun formatMegabytes(bytes: Long): String =
        String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
}
