package th1ngjin.fearindex.presentation.feature.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import th1ngjin.fearindex.presentation.R

/**
 * Splash view — iOS SplashScreen 디자인 대칭.
 *
 * 레이아웃 (위→아래):
 * - (여백)
 * - 런처 아이콘 (약 112dp)
 * - "Fear & Greed Index" 타이틀
 * - "공포-탐욕 지수" 서브타이틀
 * - (여백)
 * - info icon + disclaimer (하단)
 *
 * Manifest 의 OS SplashScreen 은 투명 아이콘 + 흰 배경으로 깜빡만 뜨고,
 * 이어서 이 SplashView 가 fade-in 되어 사용자는 단일 splash 로 인식.
 */
@Composable
fun SplashView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // 상단 여백
            Spacer(modifier = Modifier.size(1.dp))

            // 중앙: 아이콘 + 타이틀
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Google Play Store 에 표시되는 실제 런처 아이콘과 동일하게 adaptive launcher icon 사용.
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher),
                    contentDescription = null,
                    modifier = Modifier.size(112.dp),
                )
                Text(
                    text = stringResource(R.string.splash_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                )
                Text(
                    text = stringResource(R.string.splash_subtitle),
                    fontSize = 16.sp,
                    color = Color(0xFF888888),
                )
            }

            // 하단: info + disclaimer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 48.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = Color(0xFFB0B0B0),
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = stringResource(R.string.splash_disclaimer),
                    fontSize = 14.sp,
                    color = Color(0xFF888888),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
