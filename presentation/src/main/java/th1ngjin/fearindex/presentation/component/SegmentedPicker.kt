package th1ngjin.fearindex.presentation.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview

/**
 * Material 3 TabRow 기반 상단 탭바.
 * iOS의 SegmentControl 대신 Android 플랫폼 표준인 Tab을 사용 —
 * 인디케이터(underline), ripple, 접근성(role=Tab)이 자동 적용된다.
 *
 * @param items 탭 라벨 목록
 * @param selectedIndex 현재 선택된 탭 index
 * @param onItemSelected 탭 클릭 시 호출, 선택된 index를 전달
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentedPicker(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    PrimaryTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        items.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            Tab(
                selected = isSelected,
                onClick = { onItemSelected(index) },
                text = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SegmentedPickerPreview() {
    SegmentedPicker(
        items = listOf("시장", "코스피", "암호화폐"),
        selectedIndex = 0,
        onItemSelected = {},
    )
}
