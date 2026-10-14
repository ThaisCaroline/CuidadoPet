package com.cuidadopet.ui.screens.documents

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuidadopet.R
import com.cuidadopet.data.db.entity.PetDocumentEntity
import com.cuidadopet.data.repository.PetDocumentRepository
import com.cuidadopet.domain.PetDocumentRules
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

// Filtros da busca. Datas são início do dia no horário local; o fim do período é o fim do dia "Até".
data class PetDocumentsFilter(
    val query: String = "",
    val fromDate: Long? = null,
    val toDate: Long? = null
) {
    val isActive: Boolean get() = query.isNotBlank() || fromDate != null || toDate != null
}

// Documento da lista, com a indicação de que o arquivo PDF não existe mais no aparelho
data class DocumentItem(val document: PetDocumentEntity, val fileMissing: Boolean)

data class PetDocumentsUiState(
    val filter: PetDocumentsFilter = PetDocumentsFilter(),
    // Documentos que passam pelos filtros, do mais recente para o mais antigo
    val documents: List<DocumentItem> = emptyList(),
    // Total de documentos do pet, sem filtro (para diferenciar "vazio" de "nada encontrado")
    val totalCount: Int = 0,
    val isBusy: Boolean = false,
    // Mensagem já traduzida, mostrada uma vez e depois limpa
    val message: String? = null
)

@HiltViewModel
class PetDocumentsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PetDocumentRepository
) : ViewModel() {

    private val petId = MutableStateFlow(0L)
    // Filtros ficam salvos entre aberturas do app, para não precisar montar a busca de novo
    private val prefs = context.getSharedPreferences("documents_prefs", Context.MODE_PRIVATE)
    private val filter = MutableStateFlow(loadFilter())
    private val status = MutableStateFlow(Status())

    private fun loadFilter(): PetDocumentsFilter = PetDocumentsFilter(
        query = prefs.getString(KEY_QUERY, "") ?: "",
        fromDate = prefs.getLong(KEY_FROM, NO_DATE).takeIf { it != NO_DATE },
        toDate = prefs.getLong(KEY_TO, NO_DATE).takeIf { it != NO_DATE }
    )

    private fun saveFilter(f: PetDocumentsFilter) {
        prefs.edit()
            .putString(KEY_QUERY, f.query)
            .putLong(KEY_FROM, f.fromDate ?: NO_DATE)
            .putLong(KEY_TO, f.toDate ?: NO_DATE)
            .apply()
    }

    private fun updateFilter(transform: (PetDocumentsFilter) -> PetDocumentsFilter) {
        filter.update(transform)
        saveFilter(filter.value)
    }

    private data class Status(val isBusy: Boolean = false, val message: String? = null)

    // Verifica no armazenamento se cada PDF ainda existe (fora da thread principal)
    private val allDocuments: Flow<List<DocumentItem>> = petId
        .filter { it != 0L }
        .flatMapLatest { repository.observeForPet(it) }
        .map { docs -> docs.map { DocumentItem(it, fileMissing = !File(it.filePath).exists()) } }
        .flowOn(Dispatchers.IO)

    val uiState: StateFlow<PetDocumentsUiState> = combine(allDocuments, filter, status) { all, f, s ->
        val toEnd = f.toDate?.let { PetDocumentRules.localEndOfDay(it) }
        PetDocumentsUiState(
            filter = f,
            documents = all.filter {
                PetDocumentRules.matches(it.document.title, it.document.documentDate, f.query, f.fromDate, toEnd)
            },
            totalCount = all.size,
            isBusy = s.isBusy,
            message = s.message
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PetDocumentsUiState())

    fun setPetId(id: Long) {
        petId.value = id
    }

    fun onQueryChange(query: String) = updateFilter { it.copy(query = query) }
    fun onFromDateChange(date: Long?) = updateFilter { it.copy(fromDate = date) }
    fun onToDateChange(date: Long?) = updateFilter { it.copy(toDate = date) }
    fun clearFilters() = updateFilter { PetDocumentsFilter() }

    private companion object {
        const val KEY_QUERY = "query"
        const val KEY_FROM = "from_date"
        const val KEY_TO = "to_date"
        const val NO_DATE = -1L
    }

    fun clearMessage() = status.update { it.copy(message = null) }

    fun uriFor(document: PetDocumentEntity): Uri = repository.uriFor(document)

    // Sugere o título pelo nome do arquivo, sem a extensão .pdf
    suspend fun suggestTitle(uri: Uri): String = withContext(Dispatchers.IO) {
        val name = try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        } catch (_: Exception) {
            null
        }
        name?.replace(Regex("\\.pdf$", RegexOption.IGNORE_CASE), "")?.trim()?.takeIf { it.isNotEmpty() }
            ?: context.getString(R.string.docs_default_title)
    }

    fun addDocument(uri: Uri, title: String, documentDate: Long) {
        viewModelScope.launch {
            status.update { it.copy(isBusy = true, message = null) }
            val error = try {
                repository.addDocument(petId.value, uri, title, documentDate)
                null
            } catch (e: PetDocumentRepository.PetDocumentException) {
                when (e.reason) {
                    PetDocumentRepository.Reason.NOT_PDF -> context.getString(R.string.docs_error_not_pdf)
                    PetDocumentRepository.Reason.TOO_LARGE -> context.getString(R.string.docs_error_too_large)
                    PetDocumentRepository.Reason.DUPLICATE -> context.getString(R.string.docs_error_duplicate)
                    PetDocumentRepository.Reason.FAILED -> context.getString(R.string.docs_error_save)
                }
            }
            status.update { it.copy(isBusy = false, message = error) }
        }
    }

    fun updateDocument(document: PetDocumentEntity, title: String, documentDate: Long) {
        viewModelScope.launch {
            val error = try {
                repository.updateDocument(document, title, documentDate)
                null
            } catch (e: PetDocumentRepository.PetDocumentException) {
                if (e.reason == PetDocumentRepository.Reason.DUPLICATE) {
                    context.getString(R.string.docs_error_duplicate)
                } else {
                    context.getString(R.string.docs_error_save)
                }
            } catch (_: Exception) {
                context.getString(R.string.docs_error_save)
            }
            status.update { it.copy(message = error) }
        }
    }

    fun deleteDocument(document: PetDocumentEntity) {
        viewModelScope.launch {
            val error = try {
                repository.deleteDocument(document)
                null
            } catch (_: Exception) {
                context.getString(R.string.docs_error_save)
            }
            status.update { it.copy(message = error) }
        }
    }

    fun saveCopy(document: PetDocumentEntity, target: Uri) {
        viewModelScope.launch {
            status.update { it.copy(isBusy = true, message = null) }
            val message = try {
                if (repository.saveCopyTo(document, target)) {
                    context.getString(R.string.docs_copy_saved)
                } else {
                    context.getString(R.string.docs_error_copy)
                }
            } catch (_: IOException) {
                context.getString(R.string.docs_error_copy)
            }
            status.update { it.copy(isBusy = false, message = message) }
        }
    }

    // Mensagem para erros que a tela detecta (ex.: nenhum app abre PDF)
    fun notify(messageRes: Int) {
        status.update { it.copy(message = context.getString(messageRes)) }
    }
}
