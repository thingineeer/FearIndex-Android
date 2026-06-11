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
