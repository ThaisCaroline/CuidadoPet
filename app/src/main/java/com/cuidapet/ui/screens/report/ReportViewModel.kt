package com.cuidadopet.ui.screens.report

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuidadopet.data.repository.ReportRepository
import com.cuidadopet.domain.PetReport
import com.cuidadopet.domain.ReportGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

// Estado da tela de relatório
data class ReportUiState(
    val isLoading: Boolean       = true,
    val report: PetReport?       = null,
    val pdfFile: File?           = null,   // preenchido após geração do PDF
    val isPdfGenerating: Boolean = false,
    val error: String?           = null
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    // @ApplicationContext injeta o Context da aplicação — seguro para usar em ViewModel
    // (evita memory leak que aconteceria com Activity Context)
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ReportUiState())
    val state: StateFlow<ReportUiState> = _state.asStateFlow()

    // Período padrão — pode ser alterado pelo tutor na tela
    private var selectedDays = 7

    // Carrega os dados do pet e monta o PetReport.
    // Chamado com LaunchedEffect(petId) na tela para iniciar assim que ela abre.
    fun load(petId: Long, days: Int = selectedDays) {
        selectedDays = days
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, pdfFile = null) }
            try {
                val report = reportRepository.buildReport(petId, days)
                _state.update { it.copy(isLoading = false, report = report) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Erro ao carregar dados: ${e.message}") }
            }
        }
    }

    // Gera o texto do relatório para compartilhamento (WhatsApp, e-mail etc.)
    // Retorna null se o relatório ainda não foi carregado.
    fun getShareText(): String? =
        _state.value.report?.let { ReportGenerator.generateText(it) }

    // Gera o PDF em background e atualiza pdfFile no estado quando pronto.
    // A tela observa pdfFile e dispara o share dialog quando não for null.
    fun generatePdf() {
        val report = _state.value.report ?: return
        viewModelScope.launch {
            _state.update { it.copy(isPdfGenerating = true) }
            try {
                // withContext(Dispatchers.IO) move a execução para uma thread de I/O.
                // Isso libera a thread principal (Main) durante a escrita do arquivo,
                // evitando que a UI trave enquanto o PDF é gerado.
                val file = withContext(Dispatchers.IO) {
                    ReportGenerator.generatePdf(context, report)
                }
                _state.update { it.copy(isPdfGenerating = false, pdfFile = file) }
            } catch (e: Exception) {
                _state.update { it.copy(isPdfGenerating = false, error = "Erro ao gerar PDF: ${e.message}") }
            }
        }
    }

    // Limpa o pdfFile após o share dialog ter sido aberto
    // (evita reabrir o dialog ao girar a tela)
    fun clearPdfFile() {
        _state.update { it.copy(pdfFile = null) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
