package com.cuidadopet.domain

import com.cuidadopet.data.db.entity.PetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetFilterTest {

    private fun pet(
        name: String,
        species: String = "DOG",
        breed: String? = null,
        customSpecies: String? = null
    ) = PetEntity(
        name = name,
        species = species,
        breed = breed,
        customSpecies = customSpecies,
        weightKg = 5.0,
        sex = "MALE"
    )

    @Test
    fun `busca por nome ignora acento e maiusculas`() {
        val luna = pet("Luná")
        assertTrue(PetFilter.matches(luna, "LUNA", null))
        assertTrue(PetFilter.matches(luna, "  lun ", null))
        assertFalse(PetFilter.matches(luna, "rex", null))
    }

    @Test
    fun `busca tambem encontra pela raca`() {
        val poodle = pet("Bolinha", breed = "Poodle")
        assertTrue(PetFilter.matches(poodle, "pood", null))
        assertFalse(PetFilter.matches(pet("Bolinha"), "pood", null))
    }

    @Test
    fun `filtro de especie separa os pets`() {
        val dog = pet("Rex", species = "DOG")
        val cat = pet("Mia", species = "CAT")

        assertTrue(PetFilter.matches(dog, "", "DOG"))
        assertFalse(PetFilter.matches(cat, "", "DOG"))
        assertTrue(PetFilter.matches(cat, "", null))
    }

    @Test
    fun `especie OTHER usa o nome personalizado como filtro`() {
        val furao = pet("Zé", species = "OTHER", customSpecies = "Furão")
        val chinchila = pet("Nina", species = "OTHER", customSpecies = "Chinchila")

        assertEquals("OTHER:furao", PetFilter.speciesKey(furao))
        assertFalse(PetFilter.matches(chinchila, "", PetFilter.speciesKey(furao)))
        assertTrue(PetFilter.matches(furao, "", PetFilter.speciesKey(furao)))
    }

    @Test
    fun `texto e especie juntos precisam bater os dois`() {
        val rex = pet("Rex", species = "DOG")
        assertTrue(PetFilter.matches(rex, "re", "DOG"))
        assertFalse(PetFilter.matches(rex, "re", "CAT"))
        assertFalse(PetFilter.matches(rex, "mia", "DOG"))
    }
}
