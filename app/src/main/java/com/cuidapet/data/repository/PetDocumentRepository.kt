package com.cuidadopet.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.cuidadopet.data.db.dao.PetDocumentDao
import com.cuidadopet.data.db.entity.PetDocumentEntity
import com.cuidadopet.domain.PetDocumentRules
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PetDocumentRepository @Inject constructor(
    private val dao: PetDocumentDao,
    @ApplicationContext private val context: Context
) {

    // Motivo de falha ao adicionar, para a tela mostrar a mensagem certa
    enum class Reason { NOT_PDF, TOO_LARGE, DUPLICATE, FAILED }

    class PetDocumentException(val reason: Reason, cause: Throwable? = null) : Exception(cause)

    fun observeForPet(petId: Long): Flow<List<PetDocumentEntity>> = dao.observeForPet(petId)

    // Copia o PDF escolhido para o armazenamento do app. Se não for PDF ou passar do limite, nada é salvo.
    suspend fun addDocument(petId: Long, uri: Uri, title: String, documentDate: Long): PetDocumentEntity =
        withContext(Dispatchers.IO) {
            val dest = File(petDir(petId).also { it.mkdirs() }, "${UUID.randomUUID()}.pdf")
            try {
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw PetDocumentException(Reason.FAILED)
                val size = input.use { src -> dest.outputStream().use { out -> copyLimited(src, out) } }

                val head = ByteArray(4)
                val read = dest.inputStream().use { it.read(head) }
                if (read < 0 || !PetDocumentRules.isPdfHeader(head.copyOf(read))) {
                    throw PetDocumentException(Reason.NOT_PDF)
                }

                // Mesmo título e mesmo tamanho = mesmo documento: não duplica
                val duplicate = dao.getForPetOnce(petId).any {
                    PetDocumentRules.isSameDocument(it.title, it.fileSizeBytes, title, size)
                }
                if (duplicate) throw PetDocumentException(Reason.DUPLICATE)

                val entity = PetDocumentEntity(
                    petId = petId,
                    title = title.trim(),
                    documentDate = documentDate,
                    filePath = dest.absolutePath,
                    fileSizeBytes = size
                )
                val id = dao.insert(entity)
                entity.copy(id = id)
            } catch (e: Exception) {
                // Não deixa arquivo parcial para trás quando a cópia falha
                dest.delete()
                if (e is PetDocumentException) throw e
                throw PetDocumentException(Reason.FAILED, e)
            }
        }

    suspend fun updateDocument(document: PetDocumentEntity, title: String, documentDate: Long) {
        withContext(Dispatchers.IO) {
            // Editar para o título de outro documento de mesmo tamanho também é duplicado
            val duplicate = dao.getForPetOnce(document.petId).any {
                it.id != document.id &&
                    PetDocumentRules.isSameDocument(it.title, it.fileSizeBytes, title, document.fileSizeBytes)
            }
            if (duplicate) throw PetDocumentException(Reason.DUPLICATE)
            dao.update(document.copy(title = title.trim(), documentDate = documentDate))
        }
    }

    // Apaga o arquivo copiado e o registro
    suspend fun deleteDocument(document: PetDocumentEntity) {
        withContext(Dispatchers.IO) {
            File(document.filePath).delete()
            dao.delete(document)
        }
    }

    // Endereço do arquivo copiado para abrir ou compartilhar com outros apps (acesso temporário)
    fun uriFor(document: PetDocumentEntity): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(document.filePath))

    // Salva uma cópia do PDF no local escolhido pelo tutor (Salvar como)
    suspend fun saveCopyTo(document: PetDocumentEntity, target: Uri): Boolean =
        withContext(Dispatchers.IO) {
            val out = context.contentResolver.openOutputStream(target) ?: return@withContext false
            out.use { output -> File(document.filePath).inputStream().use { it.copyTo(output) } }
            true
        }

    private fun petDir(petId: Long): File = File(context.filesDir, "documents/pet_$petId")

    private fun copyLimited(src: InputStream, out: OutputStream): Long {
        val buf = ByteArray(8192)
        var total = 0L
        var read: Int
        while (src.read(buf).also { read = it } != -1) {
            total += read
            if (total > PetDocumentRules.MAX_DOCUMENT_BYTES) throw PetDocumentException(Reason.TOO_LARGE)
            out.write(buf, 0, read)
        }
        return total
    }
}
