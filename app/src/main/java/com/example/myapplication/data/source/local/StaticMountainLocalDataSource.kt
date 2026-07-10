package com.example.myapplication.data.source.local

import com.example.myapplication.domain.model.Mountain
import com.example.myapplication.domain.model.MountainDifficulty

/**
 * 대한민국 주요 산 정보를 하드코딩된 형태로 제공하는 정적 데이터소스 클래스입니다.
 */
class StaticMountainLocalDataSource {

    /**
     * 추천 서비스를 위한 원천 산 정보 목록을 조회합니다.
     */
    fun getStaticMountains(): List<Mountain> {
        return listOf(
            Mountain(
                id = "1",
                name = "북한산",
                address = "서울시 강북구",
                latitude = 37.6611,
                longitude = 126.9936,
                elevation = 836,
                difficulty = MountainDifficulty.MEDIUM,
                estimatedTimeMinutes = 180,
                description = "서울의 대표적인 명산으로, 깎아지른 바위산의 웅장함을 감상하며 다채로운 코스로 등산의 묘미를 느낄 수 있는 수도권 국립공원입니다."
            ),
            Mountain(
                id = "2",
                name = "도봉산",
                address = "서울시 도봉구",
                latitude = 37.7011,
                longitude = 127.0298,
                elevation = 740,
                difficulty = MountainDifficulty.MEDIUM,
                estimatedTimeMinutes = 150,
                description = "북한산 국립공원의 일부로 만장봉, 자운봉 등 거대한 화강암 암벽이 장관을 이루며 매력적인 등반 루트를 제공합니다."
            ),
            Mountain(
                id = "3",
                name = "관악산",
                address = "서울시 관악구 / 경기 안양시",
                latitude = 37.4444,
                longitude = 126.9602,
                elevation = 629,
                difficulty = MountainDifficulty.MEDIUM,
                estimatedTimeMinutes = 120,
                description = "서울 남부를 대표하는 돌산으로 연주대 절벽 위의 사찰이 명소이며, 아기자기하고 거친 암반 지대를 등반하는 재미가 쏠쏠합니다."
            ),
            Mountain(
                id = "4",
                name = "수락산",
                address = "서울시 노원구 / 경기 남양주시",
                latitude = 37.6853,
                longitude = 127.0864,
                elevation = 638,
                difficulty = MountainDifficulty.MEDIUM,
                estimatedTimeMinutes = 120,
                description = "거대한 모래산과 화강암 계곡이 수려하게 어우러져 있고 철바위, 하강바위 등 가파른 바위 능선을 타는 짜릿함이 일품입니다."
            ),
            Mountain(
                id = "5",
                name = "남산",
                address = "서울시 중구",
                latitude = 37.5512,
                longitude = 126.9882,
                elevation = 262,
                difficulty = MountainDifficulty.EASY,
                estimatedTimeMinutes = 45,
                description = "도심 한가운데 위치해 산책로가 매우 잘 조성되어 있어 가벼운 옷차림으로 남산타워와 아름다운 서울 전망을 바라보며 걸을 수 있습니다."
            ),
            Mountain(
                id = "6",
                name = "아차산",
                address = "서울시 광진구 / 경기 구리시",
                latitude = 37.5519,
                longitude = 127.1026,
                elevation = 295,
                difficulty = MountainDifficulty.EASY,
                estimatedTimeMinutes = 60,
                description = "한강의 수려한 전망과 함께 완만한 경사로로 이어져 있어 초보 등산객도 아침 해돋이를 감상하며 부담 없이 즐겨 찾는 힐링 명소입니다."
            ),
            Mountain(
                id = "7",
                name = "인왕산",
                address = "서울시 종로구",
                latitude = 37.5818,
                longitude = 126.9610,
                elevation = 338,
                difficulty = MountainDifficulty.EASY,
                estimatedTimeMinutes = 50,
                description = "돌산이지만 성곽길이 고즈넉하게 정비되어 있어 서울의 고저스한 경치와 성벽 야경을 누구나 쉽게 거닐 수 있는 산입니다."
            ),
            Mountain(
                id = "8",
                name = "설악산",
                address = "강원도 속초시 / 인제군",
                latitude = 38.1189,
                longitude = 128.4658,
                elevation = 1708,
                difficulty = MountainDifficulty.HARD,
                estimatedTimeMinutes = 360,
                description = "사계절 내내 빼어난 기암괴석과 단풍, 설경을 자아내는 대한민국 최고봉 중 하나로, 수려하지만 체력적인 도전을 요구하는 대자연입니다."
            ),
            Mountain(
                id = "9",
                name = "한라산",
                address = "제주특별자치도 제주시",
                latitude = 33.3617,
                longitude = 126.5292,
                elevation = 1947,
                difficulty = MountainDifficulty.HARD,
                estimatedTimeMinutes = 420,
                description = "우리나라 최고 높이의 화산으로, 백록담의 풍치와 함께 아고산대 식물군 및 신비로운 구름 위 숲길을 만끽하는 국가대표 명산입니다."
            ),
            Mountain(
                id = "10",
                name = "지리산",
                address = "경상남도 산청군 / 함양군",
                latitude = 35.3371,
                longitude = 127.7307,
                elevation = 1915,
                difficulty = MountainDifficulty.HARD,
                estimatedTimeMinutes = 480,
                description = "끝없이 펼쳐지는 어머니의 품 같은 천왕봉 능선과 울창한 원시림 계곡이 공존하며 웅장한 크기와 신령스러운 멋을 자랑하는 명산입니다."
            )
        )
    }
}
