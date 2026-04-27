package com.cuidadopet.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cuidadopet.data.db.entity.PetEntity
import kotlinx.coroutines.flow.Flow

// @Dao (Data Access Object) é uma interface que define as operações
// que podemos fazer na tabela "pets".
// O Room gera automaticamente o código de implementação dessas funções —
// você não precisa escrever nenhum SQL manualmente (exceto nas @Query).
@Dao
interface PetDao {

    // Flow<List<PetEntity>> é um "stream" de dados reativo.
    // Em vez de buscar os pets uma vez, o Flow fica "escutando" o banco.
    // Toda vez que um pet for adicionado, editado ou removido,
    // a tela que está observando esse Flow é atualizada automaticamente.
    // É como uma TV ao vivo em vez de uma foto tirada uma vez.
    @Query("SELECT * FROM pets ORDER BY name ASC")
    fun getAll(): Flow<List<PetEntity>>

    // Busca um pet específico pelo id.
    // Flow<PetEntity?> — o "?" significa que pode retornar null
    // caso o pet com esse id não exista.
    @Query("SELECT * FROM pets WHERE id = :petId")
    fun getById(petId: Long): Flow<PetEntity?>

    // suspend fun = função que pode ser pausada e retomada (coroutine).
    // Operações de banco de dados não devem rodar na thread principal
    // (a que desenha a tela) para não travar o app.
    // O "suspend" garante que ela rode em background automaticamente.
    //
    // OnConflictStrategy.REPLACE: se tentar inserir um pet com id já existente,
    // substitui o existente em vez de dar erro.
    // Retorna o id gerado para o novo pet.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pet: PetEntity): Long

    // Atualiza um pet já existente no banco.
    // O Room encontra o registro pelo id e atualiza os outros campos.
    @Update
    suspend fun update(pet: PetEntity)

    // Remove um pet do banco.
    // Por causa do CASCADE definido nas ForeignKeys das outras tabelas,
    // isso também apaga todos os medicamentos, logs, etc. deste pet.
    @Delete
    suspend fun delete(pet: PetEntity)
}
