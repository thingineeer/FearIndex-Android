package th1ngjin.fearindex.presentation.feature.history

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
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoveToInbox
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
 * 알림 내역 저장 정책 안내 시트 (ⓘ 버튼) — iOS `NotificationHistoryInfoSheet` 대응.
 * 기기 저장 / 무료 30일·프리미엄 무제한 / 삭제 시 유실 / 기록 조건.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationHistoryInfoSheet(onDismiss: () -> Unit) {
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.notification_history_info_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))
            HistoryInfoRow(Icons.Default.PhoneAndroid, stringResource(R.string.notification_history_info_device))
            HistoryInfoRow(Icons.Default.CalendarMonth, stringResource(R.string.notification_history_info_retention_free))
            HistoryInfoRow(Icons.Default.AllInclusive, stringResource(R.string.notification_history_info_retention_premium))
            HistoryInfoRow(Icons.Default.Delete, stringResource(R.string.notification_history_info_loss))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            HistoryInfoRow(Icons.Default.MoveToInbox, stringResource(R.string.notification_history_info_capture))
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.common_close))
            }
        }
    }
}

@Composable
private fun HistoryInfoRow(icon: ImageVector, text: String) {
    Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
