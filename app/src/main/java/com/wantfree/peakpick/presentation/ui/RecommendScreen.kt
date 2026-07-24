package com.wantfree.peakpick.presentation.ui

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wantfree.peakpick.domain.model.Mountain
import com.wantfree.peakpick.domain.model.MountainDifficulty
import com.wantfree.peakpick.presentation.viewmodel.MountainRecommendViewModel
import com.wantfree.peakpick.presentation.viewmodel.RecommendUiState
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

/**
 * GPS 정보를 기반으로 주변 산을 정렬하여 추천 리스트를 제공하는 메인 화면 Composable입니다.
 */
@Composable
fun RecommendScreen(
    viewModel: MountainRecommendViewModel = remember { MountainRecommendViewModel() }
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // 위치 권한 체크 및 획득을 위한 런처 구성
    val locationPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            viewModel.fetchLocationAndRecommend(context)
        } else {
            viewModel.onPermissionDenied()
            Toast.makeText(context, "위치 권한 권한이 거부되어 주변 산 추천이 제한됩니다.", Toast.LENGTH_LONG).show()
        }
    }

    // 권한 최초 체크 및 위치 데이터 획득 실행
    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        if (hasFine || hasCoarse) {
            viewModel.fetchLocationAndRecommend(context)
        } else {
            locationPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // 세련된 어두운 회색 다크 모드 배경
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF121212), Color(0xFF1E271E))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is RecommendUiState.Loading -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF4CAF50))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "현재 GPS 좌표를 파악하여 주변 산을 탐색 중입니다...",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }

            is RecommendUiState.Success -> {
                MountainListContent(
                    mountains = state.mountains,
                    addressName = state.addressName,
                    onRefresh = { viewModel.fetchLocationAndRecommend(context) }
                )
            }

            is RecommendUiState.Error -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "주의",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5252)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.fetchLocationAndRecommend(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("다시 시도", color = Color.White)
                    }
                }
            }

            is RecommendUiState.PermissionRequired -> {
                PermissionRequiredContent(
                    onRequestPermission = {
                        locationPermissionsLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )
            }
        }
    }
}

/**
 * 산 정보 목록 및 헤더 영역을 구성합니다.
 */
@Composable
fun MountainListContent(
    mountains: List<Mountain>,
    addressName: String,
    onRefresh: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "내 주변 등산 코스 추천 ⛰️",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "내 위치: $addressName",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.6f)
                    )
                )
            }
            IconButton(onClick = onRefresh) {
                Text("🔄", fontSize = 20.sp)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        
        if (mountains.isEmpty()) {
            Box(modifier = Modifier.fillMapSize(), contentAlignment = Alignment.Center) {
                Text("현재 50km 근방 추천 가능한 명산이 존재하지 않습니다.", color = Color.LightGray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(mountains) { mountain ->
                    MountainCard(mountain = mountain)
                }
            }
        }
    }
}

private fun Modifier.fillMapSize() = this.fillMaxSize()

/**
 * 개별 산 정보를 보여주는 그라데이션 카드 UI 컴포저블입니다.
 */
@Composable
fun MountainCard(mountain: Mountain) {
    var expanded by remember { mutableStateOf(false) }

    // 난이도 분류별 색상 설정
    val difficultyColor = when (mountain.difficulty) {
        MountainDifficulty.EASY -> Color(0xFF8BC34A) // 초록
        MountainDifficulty.MEDIUM -> Color(0xFFFFB300) // 노랑/오렌지
        MountainDifficulty.HARD -> Color(0xFFFF5252) // 빨강
    }
    
    val difficultyText = when (mountain.difficulty) {
        MountainDifficulty.EASY -> "쉬움"
        MountainDifficulty.MEDIUM -> "보통"
        MountainDifficulty.HARD -> "어려움"
    }

    // 흐리게 비쳐 보이는 글래스모피즘 효과 카드
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF222822)),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mountain.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = mountain.address,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
                
                // 내 거리 Km 표시
                Text(
                    text = "${mountain.distanceKm} km",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4CAF50)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 난이도 칩
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(difficultyColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = difficultyText,
                        color = difficultyColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // 고도 표시
                Text(
                    text = "해발 ${mountain.elevation}m",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
                
                Text(
                    text = "•",
                    color = Color.DarkGray
                )
                
                // 소요 시간
                Text(
                    text = "약 ${mountain.estimatedTimeMinutes}분 소요",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "설명 및 특징",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF8BC34A)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = mountain.description,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

/**
 * 위치 권한 요구 상태 화면 UI
 */
@Composable
fun PermissionRequiredContent(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "GPS 권한 접근 필요 🗺️",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "주변 산 추천 기능을 제공하기 위해서는 사용자의 실시간 GPS 위치 확인 정보가 필수적으로 요구됩니다.",
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Text("위치 서비스 동의 요청", color = Color.White, modifier = Modifier.padding(horizontal = 8.dp))
        }
    }
}
