package com.cuidadopet.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class PetDocumentRulesTest {

    private fun localDay(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply {
            set(year, month, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test
    fun `aceita so arquivo que comeca com a assinatura de PDF`() {
        assertTrue(PetDocumentRules.isPdfHeader("%PDF-1.7".toByteArray()))
        assertFalse(PetDocumentRules.isPdfHeader(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())))
        assertFalse(PetDocumentRules.isPdfHeader("%PD".toByteArray()))
    }

    @Test
    fun `busca por titulo ignora acento e maiusculas`() {
        val exam = localDay(2026, Calendar.MARCH, 10)
        assertTrue(PetDocumentRules.matches("Exame de Sangue", exam, "sangue", null, null))
        assertTrue(PetDocumentRules.matches("Hemograma Completo", exam, "HEMOGRAMÁ", null, null))
        assertTrue(PetDocumentRules.matches("Exame de Sangue", exam, "  ", null, null))
        assertFalse(PetDocumentRules.matches("Exame de Sangue", exam, "ultrassom", null, null))
    }

    @Test
    fun `filtro de periodo inclui os dois extremos`() {
        val from = localDay(2026, Calendar.MARCH, 1)
        val to = localDay(2026, Calendar.MARCH, 31)
        val toEnd = PetDocumentRules.localEndOfDay(to)

        assertTrue(PetDocumentRules.matches("Exame", from, "", from, toEnd))
        assertTrue(PetDocumentRules.matches("Exame", to, "", from, toEnd))
        assertFalse(PetDocumentRules.matches("Exame", localDay(2026, Calendar.FEBRUARY, 28), "", from, toEnd))
        assertFalse(PetDocumentRules.matches("Exame", localDay(2026, Calendar.APRIL, 1), "", from, toEnd))
    }

    @Test
    fun `fim do dia termina antes da meia-noite seguinte`() {
        val day = localDay(2026, Calendar.MARCH, 10)
        val end = PetDocumentRules.localEndOfDay(day)
        val cal = Calendar.getInstance().apply { timeInMillis = end }

        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, cal.get(Calendar.MINUTE))
        assertEquals(59, cal.get(Calendar.SECOND))
        assertEquals(999, cal.get(Calendar.MILLISECOND))
    }

    @Test
    fun `data do seletor em UTC vira o mesmo dia local`() {
        // Meia-noite UTC de 10/03/2026 pode cair no dia 9 no horário local; o dia escolhido tem que ser 10
        val utcMidnight = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.MARCH, 10, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val local = Calendar.getInstance().apply {
            timeInMillis = PetDocumentRules.utcMillisToLocalStartOfDay(utcMidnight)
        }
        assertEquals(2026, local.get(Calendar.YEAR))
        assertEquals(Calendar.MARCH, local.get(Calendar.MONTH))
        assertEquals(10, local.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, local.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun `ida e volta do seletor preserva o dia`() {
        val day = localDay(2026, Calendar.JULY, 4)
        val roundTrip = PetDocumentRules.utcMillisToLocalStartOfDay(
            PetDocumentRules.localStartOfDayToUtcMillis(day)
        )
        assertEquals(day, roundTrip)
    }

    @Test
    fun `mesmo titulo e mesmo tamanho e o mesmo documento`() {
        assertTrue(PetDocumentRules.isSameDocument("Hemograma", 1000L, "HEMOGRAMA ", 1000L))
        assertTrue(PetDocumentRules.isSameDocument("Exame Ação", 500L, "exame acao", 500L))
    }

    @Test
    fun `titulo igual com tamanho diferente nao e duplicado`() {
        assertFalse(PetDocumentRules.isSameDocument("Hemograma", 1000L, "Hemograma", 1001L))
    }

    @Test
    fun `tamanho igual com titulo diferente nao e duplicado`() {
        assertFalse(PetDocumentRules.isSameDocument("Hemograma", 1000L, "Ultrassom", 1000L))
    }

    @Test
    fun `limites de tamanho batem com o combinado`() {
        assertEquals(20L * 1024 * 1024, PetDocumentRules.MAX_DOCUMENT_BYTES)
        assertEquals(500L * 1024 * 1024, PetDocumentRules.BACKUP_LIMIT_BYTES)
        assertEquals(400L * 1024 * 1024, PetDocumentRules.BACKUP_WARNING_BYTES)
    }
}
