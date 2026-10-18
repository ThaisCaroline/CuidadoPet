package com.cuidadopet.ui.screens.medication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimilarMedicationNameTest {

    @Test
    fun `nomes iguais ignoram maiusculas e acentos`() {
        assertTrue(isSimilarMedicationName("Dipirona", "dipirona"))
        assertTrue(isSimilarMedicationName("Analgésico", "analgesico"))
    }

    @Test
    fun `nome que comeca com o outro e considerado o mesmo medicamento`() {
        assertTrue(isSimilarMedicationName("Dipirona", "Dipirona Sódica"))
        assertTrue(isSimilarMedicationName("Amoxicilina + Clavulanato", "Amoxicilina"))
    }

    @Test
    fun `nomes diferentes nao sao duplicados`() {
        assertFalse(isSimilarMedicationName("Amoxicilina", "Cefalexina"))
        assertFalse(isSimilarMedicationName("Dipirona", "Prednisona"))
    }

    @Test
    fun `nome curto so casa por igualdade`() {
        // "Ana" dentro de "Anafranil" não pode bloquear o cadastro
        assertFalse(isSimilarMedicationName("Ana", "Anafranil"))
        assertTrue(isSimilarMedicationName("Ana", "ana"))
    }

    @Test
    fun `normalizacao remove acento e espacos extras`() {
        assertEquals("analgesico", normalizeMedicationName("  Analgésico "))
        assertEquals("dipirona sodica", normalizeMedicationName("Dipirona   Sódica"))
    }

    @Test
    fun `nome vazio nunca e duplicado`() {
        assertFalse(isSimilarMedicationName("", "Dipirona"))
    }
}
