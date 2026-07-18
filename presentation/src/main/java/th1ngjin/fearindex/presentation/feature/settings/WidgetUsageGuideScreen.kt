package th1ngjin.fearindex.presentation.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import th1ngjin.fearindex.presentation.R

private data class WidgetGuidePage(val icon: ImageVector, val captionRes: Int)

/**
 * 위젯 사용법 가이드 — iOS `WidgetUsageGuideView` 미러(Android 절차용으로 재작성).
 * 5단계 페이지(일러스트 + 캡션)를 좌우 스와이프. 설정 "위젯 사용법" 행에서 진입.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetUsageGuideScreen(onBack: () -> Unit = {}) {
    val pages = listOf(
        WidgetGuidePage(Icons.Default.Widgets, R.string.widget_guide_step1),
        WidgetGuidePage(Icons.Default.Search, R.string.widget_guide_step2),
        WidgetGuidePage(Icons.Default.Tune, R.string.widget_guide_step3),
        WidgetGuidePage(Icons.Default.Add, R.string.widget_guide_step4),
        WidgetGuidePage(Icons.Default.TouchApp, R.string.widget_guide_step5),
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.widget_guide_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                WidgetGuidePageContent(pages[page], page + 1, pages.size)
            }
            PageIndicator(count = pages.size, selected = pagerState.currentPage)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WidgetGuidePageContent(page: WidgetGuidePage, step: Int, total: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp),
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(
            text = "$step / $total",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(page.captionRes),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun PageIndicator(count: Int, selected: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(count) { i ->
            val active = i == selected
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(if (active) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    ),
            )
        }
    }
}
