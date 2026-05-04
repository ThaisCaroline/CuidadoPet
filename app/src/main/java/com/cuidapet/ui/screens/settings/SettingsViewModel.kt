package com.cuidadopet.ui.screens.settings

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuidadopet.data.backup.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val backupManager: BackupManager
) : ViewModel() {

    var uiState by mutableStateOf(SettingsUiState())
        private set

    fun exportBackup() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, message = null)
            try {
                val file = backupManager.exportBackup()
                uiState = uiState.copy(isLoading = false, fileToShare = file)
            } catch (e: Exception) {
                uiState = uiState.copy(isLoading = false, message = "Erro ao exportar: ${e.message}")
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, message = null)
            try {
                backupManager.importBackup(uri)
                uiState = uiState.copy(isLoading = false, message = "Backup restaurado com sucesso! Reinicie o app.")
            } catch (e: Exception) {
                uiState = uiState.copy(isLoading = false, message = "Erro ao importar: ${e.message}")
            }
        }
    }

    fun onShareHandled() {
        uiState = uiState.copy(fileToShare = null)
    }

    fun clearMessage() {
        uiState = uiState.copy(message = null)
    }
}
