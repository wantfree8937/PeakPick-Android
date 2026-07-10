package com.example.myapplication.presentation.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.example.myapplication.data.repository.MountainRepositoryImpl
import com.example.myapplication.data.source.local.StaticMountainLocalDataSource
import com.example.myapplication.domain.model.Mountain
import com.example.myapplication.domain.usecase.GetRecommendedMountainsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 주변 산 추천 화면의 UI 상태와 비즈니스 로직(위치 측정 제어)을 조율하는 ViewModel입니다.
 */
class MountainRecommendViewModel(
    private val getRecommendedMountainsUseCase: GetRecommendedMountainsUseCase
) : ViewModel() {

    // 수동 생성을 간편히 하기 위해 보조 생성자(Factory 대용)를 제공하여 편리한 인스턴스 생성을 지원합니다.
    constructor() : this(
        GetRecommendedMountainsUseCase(
            MountainRepositoryImpl(StaticMountainLocalDataSource())
        )
    )

    private val _uiState = MutableStateFlow<RecommendUiState>(RecommendUiState.Loading)
    val uiState: StateFlow<RecommendUiState> = _uiState.asStateFlow()

    /**
     * FusedLocationProviderClient를 연동하여 사용자의 실시간 GPS 위치를 가져옵니다.
     * 권한이 허용된 경우에만 직접 호출됩니다.
     *
     * @param context 위치 조회를 수행하기 위한 컨텍스트 객체
     */
    @SuppressLint("MissingPermission")
    fun fetchLocationAndRecommend(context: Context) {
        _uiState.value = RecommendUiState.Loading
        
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    loadRecommendations(location.latitude, location.longitude)
                } else {
                    // 일시적으로 GPS 좌표 수집이 실패한 경우 서울 중심 좌표(서울시청 부근)를 기본 기본값으로 사용합니다.
                    loadRecommendations(37.5665, 126.9780)
                }
            }
            .addOnFailureListener { error ->
                _uiState.value = RecommendUiState.Error("위치 파악에 실패했습니다: ${error.localizedMessage}")
            }
    }

    /**
     * 도출된 위/경도 수치를 UseCase에 투입하여 정밀 추천 목록 State를 반영합니다.
     */
    private fun loadRecommendations(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                val recommendationList = getRecommendedMountainsUseCase(latitude, longitude)
                _uiState.value = RecommendUiState.Success(recommendationList)
            } catch (e: Exception) {
                _uiState.value = RecommendUiState.Error("서버/데이터 조회 오류가 발생했습니다: ${e.localizedMessage}")
            }
        }
    }

    /**
     * 권한 거부 시 UI 상태를 갱신하기 위하여 호출합니다.
     */
    fun onPermissionDenied() {
        _uiState.value = RecommendUiState.PermissionRequired
    }
}

/**
 * 산 추천 화면의 각 처리 단계 상태를 정의하는 봉인 인터페이스(Sealed Interface)입니다.
 */
sealed interface RecommendUiState {
    data object Loading : RecommendUiState
    data class Success(val mountains: List<Mountain>) : RecommendUiState
    data class Error(val message: String) : RecommendUiState
    data object PermissionRequired : RecommendUiState
}
