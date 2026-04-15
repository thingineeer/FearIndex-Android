package th1ngjin.fearindex.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// MARK: - Chart Skeleton View (Chart Screen)

/**
 * iOS `ChartSkeletonView` 대응 - 차트 화면 로딩 스켈레톤.
 *
 * 구조:
 * - 현재 지수 (40dp x 120dp)
 * - 차트 영역 (200dp, 너비 full)
 * - 기간 버튼 6개 Row (각 32dp x 48dp)
 */
@Composable
fun ChartSkeletonView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 1. 현재 지수 스켈레톤
        SkeletonBox(
            modifier = Modifier
                .width(120.dp)
                .height(40.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 차트 영역 스켈레톤
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 3. 기간 버튼 스켈레톤 (6개)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            repeat(6) {
                SkeletonBox(
                    modifier = Modifier
                        .width(48.dp)
                        .height(32.dp),
                )
            }
        }
    }
}
