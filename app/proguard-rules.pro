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
