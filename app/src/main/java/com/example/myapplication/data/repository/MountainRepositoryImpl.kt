package com.example.myapplication.data.repository

import com.example.myapplication.data.source.local.StaticMountainLocalDataSource
import com.example.myapplication.domain.model.Mountain
import com.example.myapplication.domain.repository.MountainRepository
import kotlin.math.*

/**
 * MountainRepository의 실제 데이터 처리를 구현하는 클래스입니다.
 */
class MountainRepositoryImpl(
    private val localDataSource: StaticMountainLocalDataSource
) : MountainRepository {

    /**
     * 현재 위치를 받아 정적 데이터소스에서 산 데이터를 조회한 후
     * Haversine 공식을 통해 사용자와의 거리를 계산 및 채워 목록을 반환합니다.
     */
    override suspend fun getMountains(latitude: Double, longitude: Double): List<Mountain> {
        val staticMountains = localDataSource.getStaticMountains()
        
        return staticMountains.map { mountain ->
            val dist = calculateDistance(
                latitude, longitude,
                mountain.latitude, mountain.longitude
            )
            // 소수점 첫째 자리까지만 나타내어 거리 km 값을 복사 저장합니다.
            val roundedDist = (dist * 10).roundToInt() / 10.0
            mountain.copy(distanceKm = roundedDist)
        }
    }

    /**
     * 두 위경도 좌표 간의 거리를 Haversine(하버사인) 공식을 통해 km 단위로 계산합니다.
     */
    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadius = 6371.0 // 지구 반지름 (단위: km)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
}
