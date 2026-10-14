package com.cuidadopet.domain

import com.cuidadopet.data.db.entity.PetEntity
import java.text.Normalizer
import java.util.Locale

// Filtro da lista de pets na tela inicial: busca por nome ou raça e filtro por espécie.
// Pensado para quem cadastra muitos pets (ex.: petshop), para não ter que rolar a lista toda.
object PetFilter {

    // Expressões compiladas uma vez só (são usadas a cada item da lista)
    private val accentMarks = Regex("\\p{Mn}+")
    private val spaces = Regex("\\s+")

    // Texto para busca: sem acento, sem maiúsculas e com espaços simples
    fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(accentMarks, "")
            .trim()
            .replace(spaces, " ")

    // Chave da espécie para o filtro. "OTHER" usa o nome personalizado (ex.: "Furão"),
    // então dois pets "OTHER" com nomes diferentes viram filtros diferentes.
    fun speciesKey(pet: PetEntity): String =
        if (pet.species == "OTHER") "OTHER:" + normalize(pet.customSpecies.orEmpty()) else pet.species

    // Nome ou raça contém o texto digitado, e (se houver) a espécie selecionada bate
    fun matches(pet: PetEntity, query: String, selectedSpeciesKey: String?): Boolean {
        if (selectedSpeciesKey != null && speciesKey(pet) != selectedSpeciesKey) return false
        val q = normalize(query)
        if (q.isEmpty()) return true
        return normalize(pet.name).contains(q) || normalize(pet.breed.orEmpty()).contains(q)
    }
}
