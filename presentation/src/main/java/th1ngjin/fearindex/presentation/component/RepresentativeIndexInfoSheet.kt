package th1ngjin.fearindex.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import th1ngjin.fearindex.presentation.R

/**
 * 대표 기준(Representative Benchmarks) 설명 시트.
 *
 * iOS `RepresentativeIndexInfoSheet` 와 1:1 대응:
 * - 4개 항목: 글로벌 시장(S&P 500) / 한국(KOSPI) / 암호화폐(BTC) / KOSPI 업데이트 정책
 * - 헤더("자산군별 기준") + 푸터(보조 지표 설명) + 제목("대표 기준")
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepresentativeIndexInfoSheet(
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            // 제목
            Text(
                text = stringResource(R.string.representative_index_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 섹션 헤더
            Text(
                text = stringResource(R.string.representative_index_summary_header),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 4개 항목 (아이콘 + 텍스트)
            InfoRow(Icons.Default.Public, stringResource(R.string.representative_index_market))
            InfoRow(Icons.AutoMirrored.Filled.TrendingUp, stringResource(R.string.representative_index_kospi))
            InfoRow(Icons.Default.CurrencyBitcoin, stringResource(R.string.representative_index_crypto))
            InfoRow(Icons.Default.Update, stringResource(R.string.representative_index_update_policy))

            Spacer(modifier = Modifier.height(12.dp))

            // 섹션 푸터 (보조 지표 설명)
            Text(
                text = stringResource(R.string.representative_index_summary_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
