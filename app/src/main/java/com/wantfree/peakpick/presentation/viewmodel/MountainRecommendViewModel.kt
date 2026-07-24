package com.wantfree.peakpick.presentation.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.wantfree.peakpick.data.repository.MountainRepositoryImpl
import com.wantfree.peakpick.data.source.remote.OverpassRemoteDataSource
import com.wantfree.peakpick.domain.model.Mountain
import com.wantfree.peakpick.domain.usecase.GetRecommendedMountainsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * 주변 산 추천 화면의 UI 상태와 비즈니스 로직(위치 측정 제어)을 조율하는 ViewModel입니다.
 */
class MountainRecommendViewModel(
    private val getRecommendedMountainsUseCase: GetRecommendedMountainsUseCase
) : ViewModel() {

    // 수동 생성을 간편히 하기 위해 보조 생성자(Factory 대용)를 제공하여 편리한 인스턴스 생성을 지원합니다.
    constructor() : this(
        GetRecommendedMountainsUseCase(
            MountainRepositoryImpl(OverpassRemoteDataSource())
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
        // 권한이 활성되어 있는지 체크하여 결핍 시 위치 탐색을 차단하고 401/SecurityException 크래시를 우회합니다.
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            onPermissionDenied()
            return
        }

        _uiState.value = RecommendUiState.Loading
        
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    loadRecommendations(context, location.latitude, location.longitude)
                } else {
                    // 일시적으로 GPS 좌표 수집이 실패한 경우 서울 중심 좌표(서울시청 부근)를 기본값으로 사용합니다.
                    loadRecommendations(context, 37.5665, 126.9780)
                }
            }
            .addOnFailureListener { error ->
                _uiState.value = RecommendUiState.Error("위치 파악에 실패했습니다: ${error.localizedMessage}")
            }
    }

    /**
     * 도출된 위/경도 수치와 컨텍스트를 활용해 Geocoder 역지오코딩을 수행하고 UseCase를 호출합니다.
     */
    private fun loadRecommendations(context: Context, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                // 백그라운드 IO 스레드에서 안드로이드 Geocoder 역지오코딩을 수행해 한글 주소를 가져옵니다.
                val addressName = withContext(Dispatchers.IO) {
                    try {
                        val geocoder = Geocoder(context, Locale.KOREA)
                        val addressList = geocoder.getFromLocation(latitude, longitude, 1)
                        if (!addressList.isNullOrEmpty()) {
                            val address = addressList[0]
                            val admin = address.adminArea ?: "" // 특별/광역시/도 (예: 서울특별시)
                            val locality = address.locality ?: "" // 구/군 (예: 서대문구)
                            val subLocality = address.subLocality ?: "" // 읍/면/동 보정
                            val thoroughfare = address.thoroughfare ?: "" // 읍/면/동 (예: 신촌동)

                            buildString {
                                if (admin.isNotEmpty()) append(admin).append(" ")
                                if (locality.isNotEmpty()) append(locality).append(" ")
                                else if (subLocality.isNotEmpty()) append(subLocality).append(" ")
                                if (thoroughfare.isNotEmpty()) append(thoroughfare)
                            }.trim()
                        } else {
                            "위도 ${String.format("%.4f", latitude)}, 경도 ${String.format("%.4f", longitude)}"
                        }
                    } catch (e: Exception) {
                        "위도 ${String.format("%.4f", latitude)}, 경도 ${String.format("%.4f", longitude)}"
                    }
                }

                val recommendationList = getRecommendedMountainsUseCase(latitude, longitude)
                _uiState.value = RecommendUiState.Success(
                    mountains = recommendationList,
                    addressName = addressName
                )
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
    data class Success(
        val mountains: List<Mountain>,
        val addressName: String
    ) : RecommendUiState
    data class Error(val message: String) : RecommendUiState
    data object PermissionRequired : RecommendUiState
}
