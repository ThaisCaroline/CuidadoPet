package com.cuidadopet.ui.screens.medication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrescriptionScanTimesTest {

    @Test
    fun `aceita HH mm validos`() {
        assertTrue(isValidTime("00:00"))
        assertTrue(isValidTime("08:30"))
        assertTrue(isValidTime("23:59"))
    }

    @Test
    fun `aceita horarios sem zero a esquerda e em outros formatos`() {
        assertTrue(isValidTime("8:00"))
        assertTrue(isValidTime("8h"))
        assertTrue(isValidTime("8h30"))
        assertTrue(isValidTime("às 20h"))
    }

    @Test
    fun `recusa horarios invalidos`() {
        assertFalse(isValidTime("24:00"))
        assertFalse(isValidTime("12:60"))
        assertFalse(isValidTime("08:0"))
        assertFalse(isValidTime(""))
    }

    @Test
    fun `parseTimes separa por virgula e ignora espacos`() {
        assertEquals(listOf("08:00", "20:00"), parseTimes("08:00, 20:00"))
        assertEquals(listOf("08:00", "20:00"), parseTimes(" 08:00 ,20:00 ,"))
        assertEquals(listOf("08:00", "20:00"), parseTimes("8h, 20:00"))
    }

    @Test
    fun `parseTimes recusa lista vazia ou com algum horario invalido`() {
        assertNull(parseTimes(""))
        assertNull(parseTimes(" , "))
        assertNull(parseTimes("08:00, 25:00"))
    }

    @Test
    fun `parseTimes recusa mais de 10 horarios`() {
        val eleven = (1..11).joinToString(",") { "%02d:00".format(it) }
        assertNull(parseTimes(eleven))

        val ten = (1..10).joinToString(",") { "%02d:00".format(it) }
        assertEquals(10, parseTimes(ten)?.size)
    }
}
