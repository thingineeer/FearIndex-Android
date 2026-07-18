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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
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
                // `R.mipmap.ic_launcher`는 API 26+에서 adaptive-icon XML로 해석돼
                // `painterResource`가 IllegalArgumentException을 던진다. 같은 아이콘의
                // 래스터 PNG 버전(`ic_splash_icon`)을 사용해 모든 디바이스에서 안전하게 로드.
                // 앱 업데이트 직후 구버전 프로세스가 교체된 리소스 테이블을 참조하면
                // Resources.NotFoundException 크래시가 난다(Crashlytics 1.0.1~1.2.0 이력).
                // painterResource는 try/catch로 감쌀 수 없어 Drawable을 직접 로드하고,
                // 실패 시 아이콘만 생략해 스플래시가 죽지 않게 방어한다.
                val context = LocalContext.current
                val splashIcon = remember {
                    runCatching {
                        ContextCompat.getDrawable(context, R.drawable.ic_splash_icon)
                            ?.toBitmap()?.asImageBitmap()
                    }.getOrNull()
                }
                if (splashIcon != null) {
                    Image(
                        bitmap = splashIcon,
                        contentDescription = null,
                        modifier = Modifier.size(112.dp),
                    )
                }
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
