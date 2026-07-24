package com.wantfree.peakpick.domain.repository

import com.wantfree.peakpick.domain.model.Mountain

/**
 * 산 데이터와 관련된 비즈니스 데이터 처리를 선언하는 Repository 인터페이스입니다.
 */
interface MountainRepository {
    /**
     * 사용자의 실시간 위경도를 파악하여 주변 산 목록 정보를 반환합니다.
     *
     * @param latitude 현재 위도
     * @param longitude 현재 경도
     * @return 주위 산 엔티티 목록
     */
    suspend fun getMountains(latitude: Double, longitude: Double): List<Mountain>
}
