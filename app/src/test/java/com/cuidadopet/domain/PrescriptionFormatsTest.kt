package com.cuidadopet.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Cobre as variações de escrita mais comuns em receitas (ver tabela no pedido de cobertura)
class PrescriptionFormatsTest {

    // ── Horários ─────────────────────────────────────────────────────

    @Test
    fun `normalizeTime aceita as formas comuns de horario`() {
        assertEquals("08:00", PrescriptionParser.normalizeTime("8h"))
        assertEquals("08:00", PrescriptionParser.normalizeTime("8hs"))
        assertEquals("08:00", PrescriptionParser.normalizeTime("8 h"))
        assertEquals("08:30", PrescriptionParser.normalizeTime("8h30"))
        assertEquals("08:30", PrescriptionParser.normalizeTime("8:30"))
        assertEquals("08:30", PrescriptionParser.normalizeTime("8.30"))
        assertEquals("08:00", PrescriptionParser.normalizeTime("08:00"))
        assertEquals("20:00", PrescriptionParser.normalizeTime("20h"))
        assertEquals("08:00", PrescriptionParser.normalizeTime("às 8h"))
    }

    @Test
    fun `normalizeTime recusa horario impossivel ou lixo`() {
        assertNull(PrescriptionParser.normalizeTime("24h"))
        assertNull(PrescriptionParser.normalizeTime("12:60"))
        assertNull(PrescriptionParser.normalizeTime("08:0"))
        assertNull(PrescriptionParser.normalizeTime("abc"))
        assertNull(PrescriptionParser.normalizeTime(""))
    }

    // ── Frequência ───────────────────────────────────────────────────

    @Test
    fun `q8h e de 8 em 8 horas viram intervalo de 8 horas`() {
        val q = PrescriptionParser.parse("Amoxicilina 250mg - 1 comprimido q8h por 7 dias").single()
        assertEquals("INTERVAL", q.frequencyType)
        assertEquals(8, q.frequencyHours)
        assertFalse(q.needsReview)

        val deEm = PrescriptionParser.parse("Amoxicilina 250mg - 1 comprimido de 8 em 8 horas por 7 dias").single()
        assertEquals(8, deEm.frequencyHours)
        assertFalse(deEm.needsReview)
    }

    @Test
    fun `barra 8 barra 8h com unidade cp`() {
        val med = PrescriptionParser.parse("Cefalexina 500mg - 1 cp 8/8h por 7 dias").single()
        assertEquals(8, med.frequencyHours)
        assertEquals("comprimido", med.doseUnit)
    }

    @Test
    fun `BID e TID viram horarios fixos espalhados`() {
        val bid = PrescriptionParser.parse("Dipirona - 1 cp BID por 3 dias").single()
        assertEquals("FIXED_TIMES", bid.frequencyType)
        assertEquals(listOf("08:00", "20:00"), bid.fixedTimes)

        val tid = PrescriptionParser.parse("Dipirona - 1 cp TID por 3 dias").single()
        assertEquals(listOf("08:00", "14:00", "20:00"), tid.fixedTimes)
    }

    @Test
    fun `duas vezes ao dia por extenso`() {
        val med = PrescriptionParser.parse("Corticoide 1 comprimido duas vezes ao dia por 5 dias").single()
        assertEquals(listOf("08:00", "20:00"), med.fixedTimes)
    }

    @Test
    fun `horario explicito tem prioridade sobre vezes ao dia`() {
        val med = PrescriptionParser.parse("Medicamento 1 comprimido uma vez ao dia às 7h30 por 5 dias").single()
        assertEquals(listOf("07:30"), med.fixedTimes)
    }

    @Test
    fun `dois horarios com as e virgula`() {
        val med = PrescriptionParser.parse("Prednisona 20mg - 1 comprimido às 8h e às 20h por 5 dias").single()
        assertEquals("FIXED_TIMES", med.frequencyType)
        assertEquals(listOf("08:00", "20:00"), med.fixedTimes)
        assertFalse(med.needsReview)
    }

    @Test
    fun `horario com dois pontos sem as`() {
        val med = PrescriptionParser.parse("Vitamina - 2 gotas 8:30 e 20:30 por 10d").single()
        assertEquals(listOf("08:30", "20:30"), med.fixedTimes)
        assertEquals(10, med.durationDays)
    }

    @Test
    fun `intervalo com horario de inicio`() {
        val med = PrescriptionParser.parse("Antialérgico 1 comprimido a cada 12h às 8h por 5 dias").single()
        assertEquals("INTERVAL", med.frequencyType)
        assertEquals(12, med.frequencyHours)
        assertEquals("08:00", med.startTime)
    }

    // ── Dose ─────────────────────────────────────────────────────────

    @Test
    fun `meio comprimido e fracao viram 0 5`() {
        val meio = PrescriptionParser.parse("Antibiótico meio comprimido de 6 em 6 horas por 3 dias").single()
        assertEquals("Antibiótico", meio.name)
        assertEquals("0.5", meio.dose)
        assertEquals("comprimido", meio.doseUnit)

        val fracao = PrescriptionParser.parse("Antibiótico 1/2 comprimido a cada 12 horas por 5 dias").single()
        assertEquals("0.5", fracao.dose)
    }

    @Test
    fun `um e meio comprimido vira 1 5`() {
        val med = PrescriptionParser.parse("Corticoide 1 1/2 comprimido 2x ao dia por 1 semana").single()
        assertEquals("1.5", med.dose)
        assertEquals(7, med.durationDays)
    }

    @Test
    fun `dose em ml`() {
        val med = PrescriptionParser.parse("Xarope 5 ml a cada 8h por 5 dias").single()
        assertEquals("5", med.dose)
        assertEquals("ml", med.doseUnit)
    }

    // ── Duração e uso contínuo ───────────────────────────────────────

    @Test
    fun `duracao abreviada em d`() {
        val med = PrescriptionParser.parse("Vitamina - 2 gotas 8h por 10d").single()
        assertEquals(10, med.durationDays)
    }

    @Test
    fun `uso continuo nao inventa fim e marca para revisao`() {
        val med = PrescriptionParser.parse("Remédio 1 comprimido uso contínuo a cada 24 horas").single()
        assertTrue(med.needsReview)
        assertNull(med.durationDays)
    }
}
