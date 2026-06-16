package th1ngjin.fearindex.core.util

object ShareUrlBuilder {

    /** 공유 링크에 쓰는 production 패키지 ID. debug 빌드에서 공유해도 스토어엔 prod 만 존재. */
    private const val PRODUCTION_PACKAGE = "th1ngjin.fearindex"

    /**
     * Google Play 스토어 앱 페이지 링크.
     *
     * 수신자가 이 링크를 열면 Android 가 알아서 처리한다:
     * - 앱이 설치돼 있으면 Play Store 앱 내 앱 페이지(또는 우리 앱)로,
     * - 없으면 설치 페이지로 이동.
     */
    fun playStoreUrl(): String =
        "https://play.google.com/store/apps/details?id=$PRODUCTION_PACKAGE"
}
