package com.example.myapplication.domain.model

/**
 * 추천 대상인 산 정보를 담는 도메인 엔티티 클래스입니다.
 *
 * @property id 산 식별자 ID
 * @property name 산 이름
 * @property address 산의 대략적인 주소 (예: 서울시 강북구)
 * @property latitude 위도
 * @property longitude 경도
 * @property elevation 해발 고도 (m)
 * @property difficulty 난이도 (EASY: 쉬움, MEDIUM: 보통, HARD: 어려움)
 * @property estimatedTimeMinutes 예상 소요 시간 (분)
 * @property description 산에 대한 추천 설명 및 특징
 * @property distanceKm 현재 위치로부터의 거리 (km) - 동적으로 계산되어 주입됩니다.
 */
data class Mountain(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: Int,
    val difficulty: MountainDifficulty,
    val estimatedTimeMinutes: Int,
    val description: String,
    val distanceKm: Double = 0.0
)

enum class MountainDifficulty {
    EASY, MEDIUM, HARD
}
