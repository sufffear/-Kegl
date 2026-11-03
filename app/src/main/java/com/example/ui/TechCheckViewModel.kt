package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppBuildInfoRepository
import com.example.domain.AppBuildInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface TechCheckUiState {
    data object Loading : TechCheckUiState
    data class Ready(val buildInfo: AppBuildInfo) : TechCheckUiState
}

/**
 * ViewModel экрана технической проверки (этап 0A).
 * Однонаправленный поток данных через StateFlow.
 */
class TechCheckViewModel(
    private val repository: AppBuildInfoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TechCheckUiState>(TechCheckUiState.Loading)
    val uiState: StateFlow<TechCheckUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = TechCheckUiState.Ready(repository.loadBuildInfo())
    }

    class Factory(private val repository: AppBuildInfoRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TechCheckViewModel(repository) as T
        }
    }
}
