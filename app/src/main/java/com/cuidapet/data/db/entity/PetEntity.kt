package com.cuidadopet.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// @Entity diz ao Room que esta classe representa uma tabela no banco de dados.
// O parâmetro tableName define o nome da tabela no SQLite.
@Entity(tableName = "pets")
data class PetEntity(

    // @PrimaryKey marca o campo como chave primária da tabela (identificador único).
    // autoGenerate = true faz o banco gerar um número único automaticamente
    // para cada pet cadastrado (1, 2, 3...). Você nunca precisa definir o id manualmente.
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Nome do pet — ex: "Rex", "Luna"
    val name: String,

    // Espécie — "DOG", "CAT", "RABBIT", "BIRD", "HAMSTER", "TURTLE", "FISH" ou "OTHER"
    val species: String,

    // Nome personalizado quando espécie = "OTHER" — ex: "Chinchila", "Furão"
    val customSpecies: String? = null,

    // Raça — opcional, por isso é String? (nullable — pode ser nulo)
    val breed: String? = null,

    // Data de nascimento armazenada como Long (timestamp em milissegundos)
    // O Android representa datas assim internamente — é mais simples de salvar no banco
    // Ex: 1672531200000 = 01/01/2023 00:00:00 UTC
    val birthDate: Long? = null,

    // Peso atual em quilogramas — usado nos cálculos de ração e água
    val weightKg: Double,

    // Sexo — "MALE" ou "FEMALE"
    val sex: String,

    // Se o pet é castrado — Boolean = true ou false
    val isNeutered: Boolean = false,

    // Estados clínicos ativos salvos como JSON em texto
    // Ex: ["ACTIVE_TREATMENT","CHRONIC_DISEASE"]
    // Usamos String porque o Room não salva listas diretamente —
    // precisamos converter para texto e converter de volta ao ler
    val clinicalStates: String = "[]",

    // Caminho da foto no armazenamento interno do celular — opcional
    // Ex: "/data/user/0/com.cuidadopet/files/pet_1.jpg"
    val photoPath: String? = null,

    // Data de cadastro — preenchida automaticamente com o momento atual
    // System.currentTimeMillis() retorna o timestamp atual em milissegundos
    val createdAt: Long = System.currentTimeMillis()
)
