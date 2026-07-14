package com.example.myapplication.presentation.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.domain.model.Mountain
import com.example.myapplication.domain.model.MountainDifficulty
import com.example.myapplication.presentation.viewmodel.MountainRecommendViewModel
import com.example.myapplication.presentation.viewmodel.RecommendUiState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

/**
 * 지도를 메인 화면으로 전면 활용하여 GPS 실시간 본인 위치 노출 및
 * 주변 30km 반경의 큰 산/작은 산들을 구글 맵스 마커 핀으로 렌더링하고,
 * 하단 카드로 상세 정보를 연동하여 포커싱 및 스위칭하는 메인 화면 컴포저블입니다. (Google Maps Compose 버전)
 */
@Composable
fun MapScreen(
    viewModel: MountainRecommendViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // 화면 시작 시 사용자 GPS 수집 초기 기동
    LaunchedEffect(Unit) {
        viewModel.fetchLocationAndRecommend(context)
    }

    // 어두운 그라데이션 우아무드 배경을 기조로 배치합니다.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
                )
            )
    ) {
        when (val state = uiState) {
            is RecommendUiState.Loading -> {
                // 멋진 유리 글래스 로더 로딩 서클
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF00FFCC))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "위치 수집 및 지도 로드 중...",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            is RecommendUiState.Success -> {
                MapContent(
                    mountains = state.mountains,
                    addressName = state.addressName,
                    onRefresh = { viewModel.fetchLocationAndRecommend(context) }
                )
            }

            is RecommendUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "💡 에러 발생",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.fetchLocationAndRecommend(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC))
                        ) {
                            Text("다시 시도", color = Color(0xFF0F2027))
                        }
                    }
                }
            }

            is RecommendUiState.PermissionRequired -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Transparent),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "위치 권한 필요 📍",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "주변 산에 마크를 띄우기 위해서는 GPS 현재 위치 정보 권한이 요청됩니다.",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { viewModel.fetchLocationAndRecommend(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC))
                            ) {
                                Text("위치 권한 허용", color = Color(0xFF0F2027))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 실시간 Google Maps 구성 및 Compose 클릭 마커 결합 콘텐츠입니다.
 */
@Composable
fun MapContent(
    mountains: List<Mountain>,
    addressName: String,
    onRefresh: () -> Unit
) {
    var selectedMountain by remember { mutableStateOf<Mountain?>(null) }
    val scope = rememberCoroutineScope()

    // GPS 중심 좌표 추출
    val centerLat = if (mountains.isNotEmpty()) mountains[0].latitude else 37.5665
    val centerLon = if (mountains.isNotEmpty()) mountains[0].longitude else 126.9780

    // Google Maps Compose 카메라 관리 상태
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(centerLat, centerLon), 13f)
    }

    // 위치 갱신/로딩 성공 시 해당 중심 위치로 부드럽게 오토포커스
    LaunchedEffect(centerLat, centerLon) {
        cameraPositionState.animate(
            update = CameraUpdateFactory.newLatLngZoom(LatLng(centerLat, centerLon), 13f),
            durationMs = 1000
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 구글 맵스 Compose 컴포넌트 렌더링
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false
            ),
            properties = MapProperties(
                isBuildingEnabled = true
            )
        ) {
            // 1) 내 중심 위치 표시 마커 (하늘색 특수 마커)
            Marker(
                state = MarkerState(position = LatLng(centerLat, centerLon)),
                title = "내 현재 위치",
                snippet = addressName,
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
            )

            // 2) 주변의 산 마커 표시
            mountains.forEach { mountain ->
                val isSelected = selectedMountain?.id == mountain.id
                Marker(
                    state = MarkerState(position = LatLng(mountain.latitude, mountain.longitude)),
                    title = mountain.name,
                    snippet = "${mountain.elevation}m | ${mountain.distanceKm}km",
                    icon = if (isSelected) {
                        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                    } else {
                        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    },
                    onClick = {
                        selectedMountain = mountain
                        scope.launch {
                            cameraPositionState.animate(
                                update = CameraUpdateFactory.newLatLngZoom(
                                    LatLng(mountain.latitude, mountain.longitude),
                                    14f
                                ),
                                durationMs = 800
                            )
                        }
                        true
                    }
                )
            }
        }

        // 가독성 유지를 위한 투명 그라데이션 커버 상하단 배치
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent)
                    )
                )
                .align(Alignment.TopCenter)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
                .align(Alignment.BottomCenter)
        )

        // 1) 상단 헤더: 깔끔 지명 노출 바
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2027).copy(alpha = 0.75f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(14.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "내 동네 주변 산 탐색 (Google Map) 🗺️",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "기준: $addressName",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF00FFCC)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onRefresh) {
                    Text("🔄", fontSize = 18.sp)
                }
            }
        }

        // 2) 하단 영역: 선택된 산의 상세 속성 카드 or 가로 스크롤 추천 산 카드 슬라이더
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .fillMaxWidth()
        ) {
            AnimatedVisibility(
                visible = selectedMountain != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                selectedMountain?.let { mountain ->
                    DetailOverlayCard(
                        mountain = mountain,
                        onClose = { selectedMountain = null }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 가로 슬라이더: 카드 목록을 쓸어넘겨서 볼 수 있음
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(mountains) { mountain ->
                    SliderMiniCard(
                        mountain = mountain,
                        isSelected = selectedMountain?.id == mountain.id,
                        onClick = {
                            selectedMountain = mountain
                            scope.launch {
                                cameraPositionState.animate(
                                    update = CameraUpdateFactory.newLatLngZoom(
                                        LatLng(mountain.latitude, mountain.longitude),
                                        14f
                                    ),
                                    durationMs = 800
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * 맵 하단에 노출되는 산 미니어처 가로 스크롤 카드 양식입니다.
 */
@Composable
fun SliderMiniCard(
    mountain: Mountain,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.15f)
    val cardBackground = if (isSelected) Color(0xFF203A43).copy(alpha = 0.9f) else Color(0xFF0F2027).copy(alpha = 0.75f)

    Card(
        modifier = Modifier
            .width(140.dp)
            .height(72.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = mountain.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${mountain.elevation}m",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = "${mountain.distanceKm}km",
                    fontSize = 11.sp,
                    color = Color(0xFF00FFCC),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 핀 마커 또는 가로 카드를 눌렀을 때 부상하며 나타나는 디테일 글래스 마커 카드입니다.
 */
@Composable
fun DetailOverlayCard(
    mountain: Mountain,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2027).copy(alpha = 0.85f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = mountain.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 난이도 태그
                    val diffColor = when (mountain.difficulty) {
                        MountainDifficulty.EASY -> Color(0xFF00FFCC)
                        MountainDifficulty.MEDIUM -> Color(0xFFFF9900)
                        MountainDifficulty.HARD -> Color(0xFFFF3366)
                    }
                    Text(
                        text = mountain.difficulty.name,
                        color = diffColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(diffColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text("❌", fontSize = 12.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = mountain.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("소요 시간", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                    Text("${mountain.estimatedTimeMinutes}분", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                }
                Column {
                    Text("해발 고도", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                    Text("${mountain.elevation}m", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                }
                Column {
                    Text("현위치 거리", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                    Text("${mountain.distanceKm}km", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF00FFCC))
                }
            }
        }
    }
}
