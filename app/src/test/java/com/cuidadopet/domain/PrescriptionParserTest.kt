package com.cuidadopet.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrescriptionParserTest {

    @Test
    fun `nome na linha anterior e instrucao na seguinte`() {
        val result = PrescriptionParser.parse(
            """
            Amoxicilina 250mg
            Dar 1 comprimido a cada 12 horas por 7 dias
            """.trimIndent()
        )

        assertEquals(1, result.size)
        val med = result[0]
        assertEquals("Amoxicilina", med.name)
        assertEquals("1", med.dose)
        assertEquals("comprimido", med.doseUnit)
        assertEquals("INTERVAL", med.frequencyType)
        assertEquals(12, med.frequencyHours)
        assertEquals(7, med.durationDays)
        // Nome veio da linha anterior, então precisa de conferência
        assertTrue(med.needsReview)
    }

    @Test
    fun `linha completa com horarios fixos`() {
        val result = PrescriptionParser.parse("Dipirona 500mg - 2 gotas 3x ao dia por 3 dias")

        assertEquals(1, result.size)
        val med = result[0]
        assertEquals("Dipirona", med.name)
        assertEquals("2", med.dose)
        assertEquals("gota", med.doseUnit)
        assertEquals("FIXED_TIMES", med.frequencyType)
        assertEquals(listOf("08:00", "14:00", "20:00"), med.fixedTimes)
        assertEquals(3, med.durationDays)
        assertFalse(med.needsReview)
    }

    @Test
    fun `notacao barra 8 por 8h vira intervalo de 8 horas`() {
        val result = PrescriptionParser.parse("Meloxicam 2 ml - 8/8h por 5 dias")

        assertEquals(1, result.size)
        assertEquals("INTERVAL", result[0].frequencyType)
        assertEquals(8, result[0].frequencyHours)
        assertEquals("ml", result[0].doseUnit)
        assertFalse(result[0].needsReview)
    }

    @Test
    fun `dose com virgula vira ponto e semanas viram dias`() {
        val result = PrescriptionParser.parse("Prednisolona 5 mg - 0,5 ml a cada 24 horas por 2 semanas")

        assertEquals(1, result.size)
        assertEquals("0.5", result[0].dose)
        assertEquals("ml", result[0].doseUnit)
        assertEquals(24, result[0].frequencyHours)
        assertEquals(14, result[0].durationDays)
        assertFalse(result[0].needsReview)
    }

    @Test
    fun `sem frequencia nem duracao usa padrao e marca para revisao`() {
        val result = PrescriptionParser.parse("Vermifugo 2 comprimidos")

        assertEquals(1, result.size)
        val med = result[0]
        assertEquals("Vermifugo", med.name)
        assertEquals("2", med.dose)
        assertEquals("INTERVAL", med.frequencyType)
        assertEquals(24, med.frequencyHours)
        // Sem duração na receita: não assume tratamento, o tutor precisa preencher
        assertNull(med.durationDays)
        assertTrue(med.needsReview)
    }

    @Test
    fun `uma vez ao dia vira um unico horario fixo`() {
        val result = PrescriptionParser.parse("Cetoconazol 1x ao dia por 5 dias")

        assertEquals(1, result.size)
        assertEquals("FIXED_TIMES", result[0].frequencyType)
        assertEquals(listOf("08:00"), result[0].fixedTimes)
    }

    @Test
    fun `cabecalho sem medicamento nao gera nada`() {
        val result = PrescriptionParser.parse(
            """
            Clinica Veterinaria Bem Estar
            Tutor: Maria
            """.trimIndent()
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `instrucao sem nome anterior nao vira medicamento`() {
        val result = PrescriptionParser.parse("Dar 1 comprimido a cada 8 horas por 3 dias")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `mesmo medicamento duas vezes fica so com a primeira ocorrencia`() {
        val result = PrescriptionParser.parse(
            """
            Dipirona 500mg - 2 gotas 3x ao dia por 3 dias
            Dipirona 500mg - 1 gota 2x ao dia por 2 dias
            """.trimIndent()
        )

        assertEquals(1, result.size)
        assertEquals("2", result[0].dose)
    }

    @Test
    fun `sem frequencia na receita a marca para o tutor informar`() {
        val med = PrescriptionParser.parse("Dipirona 500mg 1 comprimido por 3 dias").single()

        assertFalse(med.hasFrequency)
        assertTrue(med.needsReview)
    }

    @Test
    fun `com frequencia na receita a marca como encontrada`() {
        val med = PrescriptionParser.parse("Amoxicilina 250mg - 1 comprimido q8h por 7 dias").single()

        assertTrue(med.hasFrequency)
    }

    @Test
    fun `sem quantidade na receita a dose fica vazia e nao e assumida`() {
        val med = PrescriptionParser.parse("Amoxicilina 250mg de 12/12h por 7 dias").single()

        assertEquals("", med.dose)
        assertEquals(12, med.frequencyHours)
        assertTrue(med.needsReview)
    }

    @Test
    fun `texto vazio nao gera nada`() {
        assertTrue(PrescriptionParser.parse("").isEmpty())
        assertTrue(PrescriptionParser.parse("   \n  \n").isEmpty())
    }

    @Test
    fun `intervalo fora da faixa valida e ignorado`() {
        // 0 horas não é um intervalo válido, então cai no padrão de 24 horas
        val result = PrescriptionParser.parse("Antibiotico 1 comprimido a cada 0 horas por 4 dias")

        assertEquals(1, result.size)
        assertEquals(24, result[0].frequencyHours)
        assertTrue(result[0].needsReview)
    }
}
