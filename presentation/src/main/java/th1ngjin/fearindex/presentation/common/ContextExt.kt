package th1ngjin.fearindex.presentation.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Compose `LocalContext` 에서 호스트 Activity 를 찾는다 (결제 시트/공유 등 Activity 가 필요한 호출용). */
internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
