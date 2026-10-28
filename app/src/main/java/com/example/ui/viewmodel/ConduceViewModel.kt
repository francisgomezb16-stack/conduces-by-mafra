package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CompanyProfile
import com.example.data.model.Conduce
import com.example.data.repository.ConduceRepository
import com.example.util.PdfGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed interface ConducesUiState {
    data object Loading : ConducesUiState
    data class Success(val conduces: List<Conduce>) : ConducesUiState
    data class Error(val message: String) : ConducesUiState
}

class ConduceViewModel(
    private val repository: ConduceRepository
) : ViewModel() {

    val conducesState: StateFlow<ConducesUiState> = repository.observeConduces()
        .map<List<Conduce>, ConducesUiState> { ConducesUiState.Success(it) }
        .catch { error ->
            emit(ConducesUiState.Error(error.localizedMessage ?: "Error al sincronizar con la nube"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = ConducesUiState.Loading
        )

    val companyProfileState: StateFlow<CompanyProfile?> = repository.observeCompanyProfile()
        .catch { emit(null) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = CompanyProfile()
        )

    val projectsState: StateFlow<List<com.example.data.model.Project>> = repository.observeProjects()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _currentConduce = MutableStateFlow<Conduce?>(null)
    val currentConduce: StateFlow<Conduce?> = _currentConduce.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun selectConduce(conduce: Conduce?) {
        _currentConduce.value = conduce
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun saveConduce(
        conduce: Conduce,
        onSuccess: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.saveConduce(conduce)
            _isSaving.value = false
            result.onSuccess { docId ->
                _userMessage.value = "Conduce ${conduce.conduceNumber} guardado y sincronizado."
                onSuccess(docId)
            }.onFailure { e ->
                _userMessage.value = "Error al guardar conduce: ${e.localizedMessage}"
            }
        }
    }

    fun updateStatus(conduceId: String, newStatus: String) {
        viewModelScope.launch {
            val result = repository.updateStatus(conduceId, newStatus)
            result.onSuccess {
                _userMessage.value = "Estado actualizado a $newStatus."
                _currentConduce.value = _currentConduce.value?.copy(status = newStatus)
            }.onFailure { e ->
                _userMessage.value = "Error al actualizar estado: ${e.localizedMessage}"
            }
        }
    }

    fun deleteConduce(conduceId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.deleteConduce(conduceId)
            result.onSuccess {
                _userMessage.value = "Conduce eliminado."
                onDone()
            }.onFailure { e ->
                _userMessage.value = "Error al eliminar: ${e.localizedMessage}"
            }
        }
    }

    fun saveCompanyProfile(profile: CompanyProfile) {
        viewModelScope.launch {
            val result = repository.saveCompanyProfile(profile)
            result.onSuccess {
                _userMessage.value = "Datos de empresa actualizados."
            }.onFailure { e ->
                _userMessage.value = "Error al guardar empresa: ${e.localizedMessage}"
            }
        }
    }

    fun saveProject(project: com.example.data.model.Project, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.saveProject(project)
            result.onSuccess {
                _userMessage.value = "Obra / Proyecto '${project.name}' guardado correctamente."
                onDone()
            }.onFailure { e ->
                _userMessage.value = "Error al guardar proyecto: ${e.localizedMessage}"
            }
        }
    }

    fun deleteProject(projectId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.deleteProject(projectId)
            result.onSuccess {
                _userMessage.value = "Obra / Proyecto eliminada."
                onDone()
            }.onFailure { e ->
                _userMessage.value = "Error al eliminar proyecto: ${e.localizedMessage}"
            }
        }
    }

    fun generateAndSharePdf(
        context: Context,
        conduce: Conduce,
        evidenceBitmaps: List<Bitmap> = emptyList()
    ): File? {
        val company = companyProfileState.value ?: CompanyProfile()
        return try {
            val pdfFile = PdfGenerator.generateConducePdf(context, conduce, company, evidenceBitmaps)
            PdfGenerator.sharePdf(context, pdfFile)
            pdfFile
        } catch (e: Exception) {
            _userMessage.value = "Error generando PDF: ${e.localizedMessage}"
            null
        }
    }

    fun generateAndViewPdf(
        context: Context,
        conduce: Conduce,
        evidenceBitmaps: List<Bitmap> = emptyList()
    ): File? {
        val company = companyProfileState.value ?: CompanyProfile()
        return try {
            val pdfFile = PdfGenerator.generateConducePdf(context, conduce, company, evidenceBitmaps)
            PdfGenerator.viewPdf(context, pdfFile)
            pdfFile
        } catch (e: Exception) {
            _userMessage.value = "Error abriendo PDF: ${e.localizedMessage}"
            null
        }
    }
}
