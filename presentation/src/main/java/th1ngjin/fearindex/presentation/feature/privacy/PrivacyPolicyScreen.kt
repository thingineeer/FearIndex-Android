package th1ngjin.fearindex.presentation.feature.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import th1ngjin.fearindex.presentation.R

/**
 * iOS `PrivacyPolicyView` (th1ngjin.FearIndex-iOS) 포팅.
 * 모든 문자열은 res/values-XX/strings.xml 의 privacy_* 키를 통해 locale별로 표시된다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.privacy_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.privacy_last_updated),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            PrivacySections.forEach { section ->
                SectionCard(section)
            }

            ContactCard()
        }
    }
}

@Composable
private fun SectionCard(section: PrivacySection) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(section.titleRes),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(section.contentRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
        )
        section.itemsRes?.let { items ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items.forEach { itemRes ->
                    BulletRow(text = stringResource(itemRes))
                }
            }
        }
        section.subsections?.forEach { sub ->
            SubsectionBlock(sub)
        }
    }
}

@Composable
private fun SubsectionBlock(sub: PrivacySubsection) {
    Column(
        modifier = Modifier.padding(start = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(sub.titleRes),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        sub.itemsRes.forEach { itemRes ->
            BulletRow(
                text = stringResource(itemRes),
                tertiary = true,
            )
        }
    }
}

@Composable
private fun BulletRow(text: String, tertiary: Boolean = false) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = "•",
            style = if (tertiary) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = if (tertiary) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ContactCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.privacy_contact),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.privacy_contact_email),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------------------
// Data model
// ---------------------------------------------------------------------------

private data class PrivacySubsection(
    val titleRes: Int,
    val itemsRes: List<Int>,
)

private data class PrivacySection(
    val titleRes: Int,
    val contentRes: Int,
    val itemsRes: List<Int>? = null,
    val subsections: List<PrivacySubsection>? = null,
)

private val PrivacySections: List<PrivacySection> = listOf(
    PrivacySection(
        titleRes = R.string.privacy_section_overview,
        contentRes = R.string.privacy_content_overview,
    ),
    PrivacySection(
        titleRes = R.string.privacy_section_collected,
        contentRes = R.string.privacy_content_collected,
        itemsRes = listOf(
            R.string.privacy_item_app_usage,
            R.string.privacy_item_device_info,
            R.string.privacy_item_error_info,
        ),
    ),
    PrivacySection(
        titleRes = R.string.privacy_section_not_collected,
        contentRes = R.string.privacy_content_not_collected,
        itemsRes = listOf(
            R.string.privacy_item_personal_info,
            R.string.privacy_item_location,
            R.string.privacy_item_device_data,
            R.string.privacy_item_financial,
            R.string.privacy_item_health,
        ),
    ),
    PrivacySection(
        titleRes = R.string.privacy_section_purpose,
        contentRes = R.string.privacy_content_purpose,
        itemsRes = listOf(
            R.string.privacy_item_improve,
            R.string.privacy_item_optimize,
            R.string.privacy_item_monitor,
            R.string.privacy_item_ads,
        ),
    ),
    PrivacySection(
        titleRes = R.string.privacy_section_third_party,
        contentRes = R.string.privacy_content_third_party,
        subsections = listOf(
            PrivacySubsection(
                titleRes = R.string.privacy_third_party_firebase,
                itemsRes = listOf(R.string.privacy_firebase_purpose, R.string.privacy_firebase_data),
            ),
            PrivacySubsection(
                titleRes = R.string.privacy_third_party_admob,
                itemsRes = listOf(R.string.privacy_admob_purpose, R.string.privacy_admob_data),
            ),
            PrivacySubsection(
                titleRes = R.string.privacy_third_party_remote_config,
                itemsRes = listOf(R.string.privacy_remote_config_purpose, R.string.privacy_remote_config_data),
            ),
        ),
    ),
    PrivacySection(
        titleRes = R.string.privacy_section_retention,
        contentRes = R.string.privacy_content_retention,
    ),
    PrivacySection(
        titleRes = R.string.privacy_section_rights,
        contentRes = R.string.privacy_content_rights,
        itemsRes = listOf(
            R.string.privacy_item_limit_tracking,
            R.string.privacy_item_opt_out,
            R.string.privacy_item_delete_data,
        ),
    ),
    PrivacySection(
        titleRes = R.string.privacy_section_children,
        contentRes = R.string.privacy_content_children,
    ),
    PrivacySection(
        titleRes = R.string.privacy_section_changes,
        contentRes = R.string.privacy_content_changes,
    ),
)
