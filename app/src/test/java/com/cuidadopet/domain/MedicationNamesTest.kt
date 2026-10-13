package com.cuidadopet.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicationNamesTest {

    @Test
    fun `nomes conhecidos sao reconhecidos`() {
        assertTrue(MedicationNames.isLikelyMedication("Amoxicilina"))
        assertTrue(MedicationNames.isLikelyMedication("Dipirona Sódica"))
        assertTrue(MedicationNames.isLikelyMedication("Ivermectina"))
        assertTrue(MedicationNames.isLikelyMedication("Amoxicilina + Clavulanato"))
    }

    @Test
    fun `sufixo tipico de classe e reconhecido`() {
        assertTrue(MedicationNames.isLikelyMedication("Cetrazol"))      // -azol
        assertTrue(MedicationNames.isLikelyMedication("Fulanoprazol"))  // -prazol
        // Sem sufixo típico e fora da lista: não reconhecido
        assertFalse(MedicationNames.isLikelyMedication("Sicrolimus"))
    }

    @Test
    fun `palavras de cabecalho nao sao reconhecidas`() {
        assertFalse(MedicationNames.isLikelyMedication("Tutor"))
        assertFalse(MedicationNames.isLikelyMedication("Rua das Flores"))
        assertFalse(MedicationNames.isLikelyMedication("Clínica Veterinária"))
        assertFalse(MedicationNames.isLikelyMedication("Maria"))
    }

    @Test
    fun `nome vazio nao e reconhecido`() {
        assertFalse(MedicationNames.isLikelyMedication(""))
    }
}
