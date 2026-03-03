package com.vigipro.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vigipro.core.data.repository.CameraRepository
import com.vigipro.core.data.repository.CloudRepository
import com.vigipro.core.model.Camera
import com.vigipro.core.model.LOCAL_SITE_ID
import com.vigipro.core.network.cloud.CategoryDto
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import timber.log.Timber
import javax.inject.Inject

data class DiscoverState(
    val cameras: List<Camera> = emptyList(),
    val localCameras: List<Camera> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val selectedCategory: String? = null,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
)

sealed interface DiscoverSideEffect {
    data class NavigateToPlayer(val cameraId: String) : DiscoverSideEffect
    data object NavigateToLogin : DiscoverSideEffect
    data object NavigateToAddCamera : DiscoverSideEffect
}

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val cloudRepository: CloudRepository,
    private val cameraRepository: CameraRepository,
) : ViewModel(), ContainerHost<DiscoverState, DiscoverSideEffect> {

    override val container = viewModelScope.container<DiscoverState, DiscoverSideEffect>(DiscoverState()) {
        observeLocalCameras()
        loadCategories()
        loadCameras()
    }

    private fun observeLocalCameras() = intent {
        cameraRepository.getCamerasBySite(LOCAL_SITE_ID).collect { cameras ->
            reduce { state.copy(localCameras = cameras) }
        }
    }

    private fun loadCategories() = intent {
        cloudRepository.fetchPublicCategories()
            .onSuccess { response ->
                reduce { state.copy(categories = response.categories) }
            }
            .onFailure { e ->
                Timber.d(e, "Failed to load categories")
            }
    }

    private fun loadCameras(page: Int = 1) = intent {
        cloudRepository.fetchPublicCameras(
            category = state.selectedCategory,
            page = page,
        ).onSuccess { (cameras, meta) ->
            reduce {
                state.copy(
                    cameras = if (page == 1) cameras else state.cameras + cameras,
                    isLoading = false,
                    isLoadingMore = false,
                    error = null,
                    currentPage = meta.page,
                    totalPages = meta.totalPages,
                )
            }
        }.onFailure { e ->
            Timber.e(e, "Failed to load public cameras")
            reduce { state.copy(isLoading = false, isLoadingMore = false, error = "Falha ao carregar câmeras") }
        }
    }

    fun onCategorySelected(category: String?) = intent {
        reduce { state.copy(selectedCategory = category, isLoading = true, cameras = emptyList(), currentPage = 1) }
        loadCameras(page = 1)
    }

    fun onLoadMore() = intent {
        if (state.isLoadingMore || state.currentPage >= state.totalPages) return@intent
        reduce { state.copy(isLoadingMore = true) }
        loadCameras(page = state.currentPage + 1)
    }

    fun onCameraClick(cameraId: String) = intent {
        postSideEffect(DiscoverSideEffect.NavigateToPlayer(cameraId))
    }

    fun onLoginClick() = intent {
        postSideEffect(DiscoverSideEffect.NavigateToLogin)
    }

    fun onAddCameraClick() = intent {
        postSideEffect(DiscoverSideEffect.NavigateToAddCamera)
    }

    fun onRetry() = intent {
        reduce { state.copy(isLoading = true, error = null) }
        loadCameras()
    }
}
