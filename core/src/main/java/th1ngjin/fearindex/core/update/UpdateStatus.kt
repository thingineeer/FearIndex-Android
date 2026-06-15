package th1ngjin.fearindex.core.update

/**
 * 앱 업데이트 상태.
 *
 * iOS `UpdateStatus` 와 1:1 대응.
 */
enum class UpdateStatus {
    /** 최신 버전 */
    UP_TO_DATE,

    /** 업데이트 가능 (선택) */
    UPDATE_AVAILABLE,

    /** 강제 업데이트 필요 */
    FORCE_UPDATE_REQUIRED,
}
