package com.cuidadopet.data.repository

import com.cuidadopet.data.db.dao.PetDao
import com.cuidadopet.data.db.entity.PetEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

// @Singleton — uma única instância do repositório em todo o app
@Singleton
// @Inject constructor — o Hilt injeta o PetDao automaticamente aqui.
// Você não precisa criar o repositório na mão em lugar nenhum —
// o Hilt lê o construtor, vê que precisa de um PetDao, e entrega o que já criou.
class PetRepository @Inject constructor(
    private val petDao: PetDao
) {

    // Retorna um Flow com todos os pets cadastrados.
    // Flow é reativo: a tela que observar este Flow será atualizada automaticamente
    // sempre que um pet for adicionado, editado ou removido do banco.
    // O repositório apenas repassa o Flow do DAO — sem transformação necessária aqui.
    fun getAllPets(): Flow<List<PetEntity>> = petDao.getAll()

    // Retorna um Flow com um pet específico pelo id.
    // Útil para a tela de detalhe/edição do pet.
    fun getPetById(petId: Long): Flow<PetEntity?> = petDao.getById(petId)

    // Insere um novo pet e retorna o id gerado pelo banco.
    // suspend = deve ser chamado dentro de uma coroutine (o ViewModel faz isso).
    suspend fun insertPet(pet: PetEntity): Long = petDao.insert(pet)

    // Atualiza os dados de um pet existente.
    suspend fun updatePet(pet: PetEntity) = petDao.update(pet)

    // Remove um pet e todos os seus dados (CASCADE apaga medicamentos, logs, etc.)
    suspend fun deletePet(pet: PetEntity) = petDao.delete(pet)
}
