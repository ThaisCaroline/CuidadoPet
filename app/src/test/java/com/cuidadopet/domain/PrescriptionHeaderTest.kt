package com.cuidadopet.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrescriptionHeaderTest {

    @Test
    fun `linha de tutor nao vira nome de medicamento`() {
        val result = PrescriptionParser.parse("Tutor: Maria\nDar 1 comprimido a cada 8 horas por 3 dias")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `cabecalho com medico e CRMV e ignorado`() {
        val result = PrescriptionParser.parse(
            """
            Dr. João Silva CRMV 1234
            Amoxicilina 250mg
            Dar 1 comprimido a cada 12 horas por 7 dias
            """.trimIndent()
        )

        assertEquals(1, result.size)
        assertEquals("Amoxicilina", result[0].name)
    }

    @Test
    fun `endereco e clinica nao viram nome`() {
        val result = PrescriptionParser.parse(
            """
            Clínica Veterinária Bem Estar
            Rua das Flores 123
            Dipirona - 1 cp BID por 3 dias
            """.trimIndent()
        )

        assertEquals(1, result.size)
        assertEquals("Dipirona", result[0].name)
    }
}
