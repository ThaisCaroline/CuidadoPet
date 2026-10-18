package com.cuidadopet.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuidadopet.R
import com.cuidadopet.data.backup.BackupException
import com.cuidadopet.data.backup.BackupManager
import com.cuidadopet.data.repository.PurchaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val fileToShare: File? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupManager: BackupManager,
    private val purchaseRepository: PurchaseRepository
) : ViewModel() {

    val isPremium: StateFlow<Boolean> = purchaseRepository.isPremium

    var uiState by mutableStateOf(SettingsUiState())
        private set

    // Tamanho do conteúdo atual do backup (fotos e PDFs), para a tela mostrar o quanto falta para o limite
    var backupSizeBytes by mutableStateOf<Long?>(null)
        private set

    init {
        refreshBackupSize()
    }

    fun refreshBackupSize() {
        viewModelScope.launch {
            backupSizeBytes = try {
                backupManager.estimateBackupBytes()
            } catch (_: Exception) {
                null
            }
        }
    }

    fun exportBackup() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, message = null)
            try {
                val file = backupManager.exportBackup()
                uiState = uiState.copy(isLoading = false, fileToShare = file)
            } catch (e: Exception) {
                uiState = uiState.copy(isLoading = false, message = backupErrorMessage(e))
            }
            refreshBackupSize()
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, message = null)
            try {
                val report = backupManager.importBackup(uri)
                val message = if (report.skippedDocuments > 0) {
                    context.resources.getQuantityString(
                        R.plurals.settings_restore_success_skipped,
                        report.skippedDocuments,
                        report.skippedDocuments
                    )
                } else {
                    context.getString(R.string.settings_restore_success)
                }
                uiState = uiState.copy(isLoading = false, message = message)
            } catch (e: Exception) {
                uiState = uiState.copy(isLoading = false, message = backupErrorMessage(e))
            }
            refreshBackupSize()
        }
    }

    // Traduz o motivo do erro para o idioma do app
    private fun backupErrorMessage(e: Exception): String {
        if (e !is BackupException) return context.getString(R.string.backup_error_unknown)
        return when (e.reason) {
            BackupException.Reason.ENTRY_TOO_LARGE -> context.getString(R.string.backup_error_file_too_large, e.detail)
            BackupException.Reason.BACKUP_TOO_LARGE -> context.getString(R.string.backup_error_too_large)
            BackupException.Reason.NO_SPACE -> context.getString(R.string.backup_error_no_space, e.detail)
            BackupException.Reason.INVALID -> context.getString(R.string.backup_error_invalid)
        }
    }

    fun onShareHandled() {
        uiState = uiState.copy(fileToShare = null)
    }

    fun clearMessage() {
        uiState = uiState.copy(message = null)
    }
}
