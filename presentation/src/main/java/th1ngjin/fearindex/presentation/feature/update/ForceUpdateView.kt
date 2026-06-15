package th1ngjin.fearindex.presentation.feature.update

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import th1ngjin.fearindex.presentation.R
import th1ngjin.fearindex.presentation.theme.FearIndexTheme

/**
 * 강제 업데이트 전체 화면 — iOS `ForceUpdateView` 디자인 대칭.
 *
 * 레이아웃 (위→아래):
 * - (여백)
 * - 업데이트 아이콘
 * - 타이틀 + 메시지
 * - (여백)
 * - "업데이트" 버튼 (하단)
 *
 * 사용자가 뒤로 가기/dismiss 로 빠져나갈 수 없도록 [BackHandler] 로 차단한다.
 * AdMob 정책상 광고가 제한된 구버전 사용자를 최신 버전으로 유도하는 게이트.
 */
@Composable
fun ForceUpdateView(
    onUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 뒤로 가기로 화면을 닫지 못하게 차단 (no-op).
    BackHandler(enabled = true) {}

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Spacer(modifier = Modifier.size(1.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.SystemUpdate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(72.dp)
                            .padding(bottom = 20.dp),
                    )
                    Text(
                        text = stringResource(R.string.force_update_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.force_update_message),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Button(
                    onClick = onUpdate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 48.dp),
                ) {
                    Text(
                        text = stringResource(R.string.force_update_button),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ForceUpdateViewPreview() {
    FearIndexTheme {
        ForceUpdateView(onUpdate = {})
    }
}
