package th1ngjin.fearindex.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPermissionSyncPolicyTest {

    @Test
    fun `foregroundAction - 시스템 알림 OFF이고 앱 토글 ON이면 비활성화 동기화`() {
        assertEquals(
            NotificationPermissionSyncPolicy.Action.DISABLE_AND_SYNC_SERVER,
            NotificationPermissionSyncPolicy.foregroundAction(
                systemAuthorized = false,
                appNotificationEnabled = true,
            ),
        )
    }

    @Test
    fun `initialAuthorizationAction - 최초 결정에서 허용이면 저장값과 무관하게 ON + 서버 동기화`() {
        // 시스템 프롬프트가 실제로 표시된 최초 결정 = 리텐션 핵심 게이트 (iOS v1.8.8 parity).
        // 백업 복원 등으로 stale 저장값이 있어도 최초 허용이 우선.
        listOf(true, false).forEach { hasStored ->
            assertEquals(
                NotificationPermissionSyncPolicy.InitialAuthorizationAction
                    .InitializeAndSyncServer(enabled = true),
                NotificationPermissionSyncPolicy.initialAuthorizationAction(
                    systemAuthorized = true,
                    hasStoredPreference = hasStored,
                    isFirstAuthorizationDecision = true,
                ),
            )
        }
    }

    @Test
    fun `initialAuthorizationAction - 최초 결정에서 거부여도 저장값 있으면 불가침`() {
        assertEquals(
            NotificationPermissionSyncPolicy.InitialAuthorizationAction.NoChange,
            NotificationPermissionSyncPolicy.initialAuthorizationAction(
                systemAuthorized = false,
                hasStoredPreference = true,
                isFirstAuthorizationDecision = true,
            ),
        )
    }

    @Test
    fun `initialAuthorizationAction - 이미 결정된 기기는 저장값 불가침`() {
        // 매 실행 시 granted가 즉시 반환되는 경우 — 사용자가 직접 OFF한 설정 보존.
        listOf(true, false).forEach { authorized ->
            assertEquals(
                NotificationPermissionSyncPolicy.InitialAuthorizationAction.NoChange,
                NotificationPermissionSyncPolicy.initialAuthorizationAction(
                    systemAuthorized = authorized,
                    hasStoredPreference = true,
                    isFirstAuthorizationDecision = false,
                ),
            )
        }
    }

    @Test
    fun `initialAuthorizationAction - 저장값 없고 이미 허용 상태면 로컬만 ON`() {
        // 프롬프트 없이 허용된 기기(pre-33, 사전 부여) — 서버 스팸 없이 로컬 초기화만.
        assertEquals(
            NotificationPermissionSyncPolicy.InitialAuthorizationAction
                .InitializeLocalOnly(enabled = true),
            NotificationPermissionSyncPolicy.initialAuthorizationAction(
                systemAuthorized = true,
                hasStoredPreference = false,
                isFirstAuthorizationDecision = false,
            ),
        )
    }

    @Test
    fun `initialAuthorizationAction - 저장값 없고 미허용이면 OFF + 서버 동기화`() {
        listOf(true, false).forEach { firstDecision ->
            assertEquals(
                NotificationPermissionSyncPolicy.InitialAuthorizationAction
                    .InitializeAndSyncServer(enabled = false),
                NotificationPermissionSyncPolicy.initialAuthorizationAction(
                    systemAuthorized = false,
                    hasStoredPreference = false,
                    isFirstAuthorizationDecision = firstDecision,
                ),
            )
        }
    }

    @Test
    fun `foregroundAction - 시스템 알림과 앱 토글이 충돌하지 않으면 no-op`() {
        assertEquals(
            NotificationPermissionSyncPolicy.Action.NO_OP,
            NotificationPermissionSyncPolicy.foregroundAction(
                systemAuthorized = true,
                appNotificationEnabled = true,
            ),
        )
        assertEquals(
            NotificationPermissionSyncPolicy.Action.NO_OP,
            NotificationPermissionSyncPolicy.foregroundAction(
                systemAuthorized = false,
                appNotificationEnabled = false,
            ),
        )
    }
}
