package th1ngjin.fearindex.update

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import timber.log.Timber

/**
 * Play In-App Update IMMEDIATE 플로우 래퍼.
 *
 * 강제 업데이트 게이트에서 사용한다. Remote Config 가 강제 업데이트로 판정한 뒤
 * Play Store 에도 새 버전이 올라가 있으면 IMMEDIATE 풀스크린 업데이트를 띄운다.
 *
 * Play Store 에 아직 새 버전이 없거나(검토 중 등) In-App Update 를 못 띄우는 경우엔
 * [onUnavailable] 콜백으로 폴백하여 ForceUpdateView 가 스토어 링크를 직접 열도록 한다.
 */
class InAppUpdateManager(
    private val appUpdateManager: AppUpdateManager,
) {

    /**
     * IMMEDIATE 업데이트 플로우를 시작한다.
     *
     * @param activity 결과를 받을 Activity.
     * @param launcher `registerForActivityResult(StartIntentSenderForResult)` 런처.
     * @param onUnavailable 업데이트를 띄울 수 없을 때 호출 (스토어 링크 폴백용).
     */
    fun startImmediateUpdate(
        activity: Activity,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        onUnavailable: () -> Unit,
    ) {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                val updatable =
                    info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE ||
                        info.updateAvailability() ==
                        UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                if (updatable && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    runCatching {
                        appUpdateManager.startUpdateFlowForResult(
                            info,
                            launcher,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                        )
                    }.onFailure {
                        Timber.w(it, "In-App Update 시작 실패 → 스토어 폴백")
                        onUnavailable()
                    }
                } else {
                    Timber.i("In-App Update 불가 (availability=${info.updateAvailability()}) → 스토어 폴백")
                    onUnavailable()
                }
            }
            .addOnFailureListener {
                Timber.w(it, "appUpdateInfo 조회 실패 → 스토어 폴백")
                onUnavailable()
            }
    }

    /**
     * 강제 업데이트 중 사용자가 IMMEDIATE 플로우를 떠났다가 돌아왔을 때,
     * 다운로드가 멈춘 상태라면 다시 플로우를 재개한다.
     */
    fun resumeIfInProgress(
        activity: Activity,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
    ) {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() ==
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
            ) {
                runCatching {
                    appUpdateManager.startUpdateFlowForResult(
                        info,
                        launcher,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                    )
                }.onFailure { Timber.w(it, "In-App Update 재개 실패") }
            }
        }
    }
}
