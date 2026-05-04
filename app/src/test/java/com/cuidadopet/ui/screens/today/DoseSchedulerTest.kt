package com.cuidadopet.ui.screens.today

import com.cuidadopet.data.db.entity.MedicationEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DoseSchedulerTest {

    private val H   = 3_600_000L    // 1 hora em ms
    private val DAY = 86_400_000L   // 1 dia em ms

    // Janela de teste: dia 2 (valores fixos, independentes de fuso horário)
    private val dayStart = 2 * DAY
    private val dayEnd   = 3 * DAY - 1L

    private fun med(
        frequencyType: String  = "INTERVAL",
        frequencyHours: Int?   = 24,
        fixedTimes: String?    = null,
        startDate: Long        = 0L
    ) = MedicationEntity(
        petId         = 1L,
        name          = "Teste",
        form          = "TABLET",
        dose          = "1",
        doseUnit      = "comprimido",
        frequencyType = frequencyType,
        frequencyHours = frequencyHours,
        fixedTimes    = fixedTimes,
        startDate     = startDate
    )

    // ── FIXED_TIMES ───────────────────────────────────────────────────────────

    @Test
    fun `fixedTimes retorna timestamp correto para horario unico`() {
        val m = med(frequencyType = "FIXED_TIMES", fixedTimes = "[\"08:00\"]")
        val doses = DoseScheduler.calculateDosesForDay(m, dayStart, dayEnd)
        assertEquals(listOf(dayStart + 8 * H), doses)
    }

    @Test
    fun `fixedTimes retorna dois horarios no mesmo dia`() {
        val m = med(frequencyType = "FIXED_TIMES", fixedTimes = "[\"08:00\",\"20:00\"]")
        val doses = DoseScheduler.calculateDosesForDay(m, dayStart, dayEnd)
        assertEquals(listOf(dayStart + 8 * H, dayStart + 20 * H), doses)
    }

    @Test
    fun `fixedTimes nulo retorna lista vazia`() {
        val m = med(frequencyType = "FIXED_TIMES", fixedTimes = null)
        assertTrue(DoseScheduler.calculateDosesForDay(m, dayStart, dayEnd).isEmpty())
    }

    @Test
    fun `fixedTimes com entrada corrompida ignora o valor invalido`() {
        val m = med(frequencyType = "FIXED_TIMES", fixedTimes = "[\"08:00\",\"INVALIDO\",\"20:00\"]")
        val doses = DoseScheduler.calculateDosesForDay(m, dayStart, dayEnd)
        assertEquals(listOf(dayStart + 8 * H, dayStart + 20 * H), doses)
    }

    // ── INTERVAL 24h ──────────────────────────────────────────────────────────

    @Test
    fun `intervalo 24h aparece todo dia`() {
        val m = med(frequencyHours = 24, startDate = 0L)
        val doses = DoseScheduler.calculateDosesForDay(m, dayStart, dayEnd)
        assertEquals(1, doses.size)
    }

    @Test
    fun `intervalo 24h dose no horario correto`() {
        val m = med(frequencyHours = 24, startDate = 10 * H)  // iniciou às 10h do dia 0
        val doses = DoseScheduler.calculateDosesForDay(m, dayStart, dayEnd)
        assertEquals(listOf(dayStart + 10 * H), doses)
    }

    // ── INTERVAL 48h ──────────────────────────────────────────────────────────

    @Test
    fun `intervalo 48h aparece no dia da dose`() {
        // Começou no dia 0 meia-noite — doses nos dias 0, 2, 4...
        val m = med(frequencyHours = 48, startDate = 0L)
        val doses = DoseScheduler.calculateDosesForDay(m, dayStart, dayEnd)
        assertEquals(1, doses.size)
    }

    @Test
    fun `intervalo 48h nao aparece no dia intermediario`() {
        val day1Start = DAY
        val day1End   = 2 * DAY - 1L
        val m = med(frequencyHours = 48, startDate = 0L)
        assertTrue(DoseScheduler.calculateDosesForDay(m, day1Start, day1End).isEmpty())
    }

    @Test
    fun `intervalo 48h com startDate no meio do dia`() {
        val m = med(frequencyHours = 48, startDate = 10 * H)  // dia 0 às 10h
        val doses = DoseScheduler.calculateDosesForDay(m, dayStart, dayEnd)
        assertEquals(listOf(dayStart + 10 * H), doses)
    }

    // ── INTERVAL 72h ──────────────────────────────────────────────────────────

    @Test
    fun `intervalo 72h aparece a cada 3 dias`() {
        val m = med(frequencyHours = 72, startDate = 0L)
        assertTrue(DoseScheduler.calculateDosesForDay(m, 0L,       DAY - 1L    ).isNotEmpty()) // dia 0
        assertTrue(DoseScheduler.calculateDosesForDay(m, DAY,      2*DAY - 1L  ).isEmpty())   // dia 1
        assertTrue(DoseScheduler.calculateDosesForDay(m, dayStart, dayEnd       ).isEmpty())   // dia 2
        assertTrue(DoseScheduler.calculateDosesForDay(m, 3*DAY,    4*DAY - 1L  ).isNotEmpty()) // dia 3
    }

    // ── Fronteiras ────────────────────────────────────────────────────────────

    @Test
    fun `dose exatamente na meia-noite do dia e incluida`() {
        val m = med(frequencyHours = 48, startDate = 0L)
        val doses = DoseScheduler.calculateDosesForDay(m, dayStart, dayEnd)
        assertTrue(doses.contains(dayStart))
    }

    @Test
    fun `medicamento que ainda nao comecou nao aparece`() {
        val m = med(frequencyHours = 24, startDate = 5 * DAY)
        assertTrue(DoseScheduler.calculateDosesForDay(m, dayStart, dayEnd).isEmpty())
    }

    @Test
    fun `frequencyType desconhecido retorna lista vazia`() {
        val m = med(frequencyType = "WEEKLY")
        assertTrue(DoseScheduler.calculateDosesForDay(m, dayStart, dayEnd).isEmpty())
    }
}
