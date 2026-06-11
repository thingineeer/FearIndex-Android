package th1ngjin.fearindex.domain.entity

object NotificationPermissionSyncPolicy {
    enum class Action {
        NO_OP,
        DISABLE_AND_SYNC_SERVER,
    }

    fun foregroundAction(
        systemAuthorized: Boolean,
        appNotificationEnabled: Boolean,
    ): Action =
        if (!systemAuthorized && appNotificationEnabled) {
            Action.DISABLE_AND_SYNC_SERVER
        } else {
            Action.NO_OP
        }
}
