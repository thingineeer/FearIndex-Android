package th1ngjin.fearindex.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// MARK: - Fear Index Skeleton View (Home Screen)

/**
 * iOS `FearIndexSkeletonView` 대응 - 홈 화면 로딩 스켈레톤.
 *
 * 구조:
 * - 게이지 원형 (200dp)
 * - 비교카드 2줄 (각 80dp, 너비 full)
 * - 타임스탬프 (16dp x 150dp)
 */
@Composable
fun FearIndexSkeletonView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // 1. 게이지 원형 스켈레톤
        SkeletonCircle(size = 200.dp)

        Spacer(modifier = Modifier.height(20.dp))

        // 2. 비교카드 스켈레톤 (2줄)
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 타임스탬프 스켈레톤
        SkeletonBox(
            modifier = Modifier
                .width(150.dp)
                .height(16.dp),
        )
    }
}
