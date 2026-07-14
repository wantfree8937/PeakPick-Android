package com.example.myapplication.data.repository

import com.example.myapplication.data.source.remote.OverpassRemoteDataSource
import com.example.myapplication.domain.model.Mountain
import com.example.myapplication.domain.model.MountainDifficulty
import com.example.myapplication.domain.repository.MountainRepository
import kotlin.math.*

/**
 * OpenStreetMap의 Overpass API를 통해 가져온 실시간 산봉우리 정보의
 * 비즈니스 로직 및 거리를 가공 처리하는 Repository 구현 클래스입니다.
 */
class MountainRepositoryImpl(
    private val remoteDataSource: OverpassRemoteDataSource = OverpassRemoteDataSource()
) : MountainRepository {

    /**
     * 현재 위치를 받아 원격 데이터소스(Overpass API)를 통해 주변 산봉우리 데이터를 수집한 뒤
     * 거리를 계산하고 산의 스펙(난이도, 소요시간 등)을 동적으로 변환 주입하여 반환합니다.
     */
    override suspend fun getMountains(latitude: Double, longitude: Double): List<Mountain> {
        val resultList = try {
            val response = remoteDataSource.getNearbyMountains(
                latitude = latitude,
                longitude = longitude,
                radiusSearchKm = 30.0 // 기본 30km 반경 검색
            )

            // 산 이름(name)을 가졌으며 유효한 노드들만 가공하여 도메인 모델로 마이그레이션합니다.
            response.elements
                .filter { it.tags?.name != null }
                .map { element ->
                    val name = element.tags?.name.orEmpty()
                    val eleString = element.tags?.ele
                    val ele = eleString?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: 150 // 기본 해발 150m로 처리

                    // 하버사인 공식으로 거리 측정
                    val dist = calculateDistance(latitude, longitude, element.actualLat, element.actualLon)
                    val roundedDist = (dist * 10).roundToInt() / 10.0

                    // 고도 높이에 비례한 산 난이도 스펙 동적 설정
                    val difficulty = when {
                        ele <= 150 -> MountainDifficulty.EASY
                        ele <= 500 -> MountainDifficulty.MEDIUM
                        else -> MountainDifficulty.HARD
                    }

                    // 고도 높이(m) 기준 도보 소요 시간 동적 계산 (최소 30분 보장)
                    val time = (ele / 2).coerceAtLeast(30)

                    Mountain(
                        id = element.id.toString(),
                        name = name,
                        address = "실시간 위치 인근 등산 코스(좌표: ${String.format("%.4f", element.actualLat)}, ${String.format("%.4f", element.actualLon)})",
                        latitude = element.actualLat,
                        longitude = element.actualLon,
                        elevation = ele,
                        difficulty = difficulty,
                        estimatedTimeMinutes = time,
                        description = "${name}은(는) 고도 ${ele}m의 등산지입니다. GPS 연동에 의해 실시간으로 안전하게 탐색된 명코스입니다.",
                        distanceKm = roundedDist
                    )
                }
        } catch (e: Exception) {
            emptyList() // 통신 실패 시 빈 목록 폴백 준비
        }

        // 만약 실시간 API 응답이 없거나 통신 에러가 난 경우 예비용 로컬 웰클래스 동산 데이터로 복구 반환합니다.
        return if (resultList.isEmpty()) {
            getFallbackLocalMountains(latitude, longitude)
        } else {
            resultList
        }
    }

    /**
     * API 통신이 지연되거나 제한적인 상황일 때 활용할 근거리 생활지 중심의 15개 백업 코스 데이터셋입니다.
     */
    private fun getFallbackLocalMountains(latitude: Double, longitude: Double): List<Mountain> {
        val fallbacks = listOf(
            Triple("북한산", 37.6618, 126.9934) to (836 to MountainDifficulty.HARD),
            Triple("남산", 37.5512, 126.9882) to (262 to MountainDifficulty.EASY),
            Triple("관악산", 37.4442, 126.9639) to (629 to MountainDifficulty.HARD),
            Triple("도봉산", 37.7001, 127.0298) to (740 to MountainDifficulty.HARD),
            Triple("아차산", 37.5516, 127.1009) to (295 to MountainDifficulty.EASY),
            Triple("서대문 안산", 37.5772, 126.9538) to (295 to MountainDifficulty.EASY),
            Triple("우면산", 37.4716, 127.0125) to (293 to MountainDifficulty.EASY),
            Triple("대모산", 37.4811, 127.0658) to (293 to MountainDifficulty.EASY),
            Triple("용마산", 37.5736, 127.0872) to (348 to MountainDifficulty.EASY),
            Triple("백련산", 37.5925, 126.9278) to (215 to MountainDifficulty.EASY),
            Triple("봉제산", 37.5398, 126.8465) to (117 to MountainDifficulty.EASY),
            Triple("서달산", 37.4996, 126.9612) to (179 to MountainDifficulty.EASY),
            Triple("배봉산", 37.5768, 127.0601) to (108 to MountainDifficulty.EASY),
            Triple("개운산", 37.5947, 127.0270) to (134 to MountainDifficulty.EASY),
            Triple("우장산", 37.5539, 126.8407) to (98 to MountainDifficulty.EASY)
        )

        return fallbacks.map { (info, spec) ->
            val (name, lat, lon) = info
            val (ele, diff) = spec
            val dist = calculateDistance(latitude, longitude, lat, lon)
            val roundedDist = (dist * 10).roundToInt() / 10.0
            val time = (ele / 2).coerceAtLeast(30)

            Mountain(
                id = "local_${name.hashCode()}",
                name = name,
                address = "백업 로컬 위치 정보 (좌표: ${String.format("%.4f", lat)}, ${String.format("%.4f", lon)})",
                latitude = lat,
                longitude = lon,
                elevation = ele,
                difficulty = diff,
                estimatedTimeMinutes = time,
                description = "${name}은(는) 가벼운 산책과 경량을 지원하는 도심지 백업 등산 코스입니다.",
                distanceKm = roundedDist
            )
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
