package com.example.myapplication.data.source.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Overpass API 서버의 JSON 응답을 역직렬화하기 위한 데이터 모델 클래스들입니다.
 */
data class OverpassResponse(
    @SerializedName("elements") val elements: List<OverpassElement>
)

data class OverpassElement(
    @SerializedName("id") val id: Long,
    @SerializedName("lat") val lat: Double?,
    @SerializedName("lon") val lon: Double?,
    @SerializedName("center") val center: OverpassCenter?,
    @SerializedName("tags") val tags: OverpassTags?
) {
    /**
     * 노드 지형일 경우 lat/lon이, 영역 지형(way)일 경우 center.lat/center.lon이 응답되므로
     * 이를 안전하게 위도 좌표로 단일화하여 제공합니다.
     */
    val actualLat: Double
        get() = lat ?: center?.lat ?: 0.0

    /**
     * 안전하게 경도 좌표로 단일화하여 제공합니다.
     */
    val actualLon: Double
        get() = lon ?: center?.lon ?: 0.0
}

/**
 * 영역형 지형의 중심점 좌표를 의미하는 모델입니다.
 */
data class OverpassCenter(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double
)

data class OverpassTags(
    @SerializedName("name") val name: String?,
    @SerializedName("ele") val ele: String?
)

/**
 * Retrofit2 통신을 위한 Overpass API 서비스 인터페이스입니다.
 */
interface OverpassService {
    /**
     * Overpass QL 쿼리를 해석기 엔드포인트로 전송하여 결과를 수신합니다.
     */
    @GET("api/interpreter")
    suspend fun getNearbyPeaks(
        @Query("data") query: String
    ): OverpassResponse
}

/**
 * 실시간 OpenStreetMap(OSM) 데이터를 Overpass API를 통해 비동기 조회하는 원격 데이터소스 클래스입니다.
 */
class OverpassRemoteDataSource {

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://overpass-api.de/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(OverpassService::class.java)

    /**
     * 사용자의 현 위경도 및 거리를 파라미터로 삼아 주변 산봉우리 및 동산(natural=peak, natural=hill) 데이터를 쿼리 수집합니다.
     *
     * @param latitude 기준점 위도
     * @param longitude 기준점 경도
     * @param radiusSearchKm 검색 반경 (단위: km, 기본값 30km)
     */
    suspend fun getNearbyMountains(
        latitude: Double,
        longitude: Double,
        radiusSearchKm: Double = 30.0
    ): OverpassResponse {
        val radiusInMeters = (radiusSearchKm * 1000).toInt()
        // Overpass QL(Query Language) 조건에 소형 동산(hill) 및 영역(way) 데이터 수집 중심점 처리를 가미합니다.
        val overpassQuery = """
            [out:json];
            (
              node(around:$radiusInMeters,$latitude,$longitude)[natural=peak];
              node(around:$radiusInMeters,$latitude,$longitude)[natural=hill];
              way(around:$radiusInMeters,$latitude,$longitude)[natural=peak];
              way(around:$radiusInMeters,$latitude,$longitude)[natural=hill];
            );
            out center;
        """.trimIndent()
        return service.getNearbyPeaks(overpassQuery)
    }
}
