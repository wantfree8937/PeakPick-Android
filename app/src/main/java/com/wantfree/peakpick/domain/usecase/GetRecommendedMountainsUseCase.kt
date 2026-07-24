package com.wantfree.peakpick.domain.usecase

import com.wantfree.peakpick.domain.model.Mountain
import com.wantfree.peakpick.domain.repository.MountainRepository

/**
 * 사용자의 현재 GPS 위치를 기준으로 주변 산 목록 중
 * 가까운 순서대로 가공하여 추천 리스트를 가져오는 유즈케이스 클래스입니다.
 */
class GetRecommendedMountainsUseCase(
    private val repository: MountainRepository
) {
    /**
     * 위도와 경도를 받아서 주변의 추천 산 리스트를 계산 및 가깝게 정렬하여 반환합니다.
     *
     * @param latitude 현재 위도
     * @param longitude 현재 경도
     * @return 정렬된 산 추천 목록
     */
    suspend operator fun invoke(latitude: Double, longitude: Double): List<Mountain> {
        val mountains = repository.getMountains(latitude, longitude)
        
        // 거리가 가까운 가까운 좌표부터 우선 정렬합니다.
        return mountains.sortedBy { it.distanceKm }
    }
}
