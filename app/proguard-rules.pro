# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Retrofit / OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# Moshi / Kotlin Serialization
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * { @kotlinx.serialization.Serializable *; }

# Compose
-dontwarn androidx.compose.**

# Hilt
-keep class dagger.hilt.** { *; }

# Play In-App Update (강제/선택 업데이트) — release minify에서 조용히 실패 방지
-keep class com.google.android.play.core.** { *; }
-dontwarn com.google.android.play.core.**
