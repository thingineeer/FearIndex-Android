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

    /** 앱 시작 시 알림 권한 상태에 따른 master toggle 초기화 액션. */
    sealed interface InitialAuthorizationAction {
        data object NoChange : InitialAuthorizationAction
        data class InitializeLocalOnly(val enabled: Boolean) : InitialAuthorizationAction
        data class InitializeAndSyncServer(val enabled: Boolean) : InitialAuthorizationAction
    }

    /**
     * iOS `NotificationAuthorizationSyncPolicy.initialAuthorizationAction` 1:1 포팅.
     *
     * - 시스템 프롬프트가 실제로 표시된 최초 결정에서 허용 → 저장값 무시하고 ON + 서버 동기화 (리텐션 핵심).
     * - 이미 결정된 기기(저장값 존재) → 불가침. 사용자가 직접 OFF한 설정은 자동 ON 금지.
     * - 저장값 없이 허용 상태(프롬프트 없이 부여) → 로컬만 ON.
     * - 저장값 없이 미허용 → OFF + 서버 동기화.
     */
    fun initialAuthorizationAction(
        systemAuthorized: Boolean,
        hasStoredPreference: Boolean,
        isFirstAuthorizationDecision: Boolean = false,
    ): InitialAuthorizationAction {
        if (isFirstAuthorizationDecision && systemAuthorized) {
            return InitialAuthorizationAction.InitializeAndSyncServer(enabled = true)
        }
        if (hasStoredPreference) {
            return InitialAuthorizationAction.NoChange
        }
        if (systemAuthorized) {
            return InitialAuthorizationAction.InitializeLocalOnly(enabled = true)
        }
        return InitialAuthorizationAction.InitializeAndSyncServer(enabled = false)
    }
}
