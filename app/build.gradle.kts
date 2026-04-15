plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.kotlin.serialization)
}

android {
    // namespace는 R 클래스와 BuildConfig 생성 위치 — Kotlin 식별자 규칙을 따라야 함.
    // th1ngjin은 숫자로 시작하지 않으므로 유효한 식별자.
    namespace = "th1ngjin.fearindex"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        // iOS/macOS 컨벤션 `th1ngjin.FearIndex-iOS`와 대칭.
        // Android package는 소문자 관습을 따름 → th1ngjin.fearindex
        applicationId = "th1ngjin.fearindex"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // AdMob App ID — 실제 프로덕션 ID (2026-04-14 발급)
        // debug 빌드도 동일 App ID를 사용하되, 실제 광고 단위는 debug buildType에서 테스트 ID 사용
        manifestPlaceholders["admobAppId"] = "ca-app-pub-5283496525222246~1308884877"
    }

    signingConfigs {
        create("release") {
            // ~/.gradle/gradle.properties의 FEARINDEX_* 속성 사용 (CI/CD에서는 환경변수 fallback)
            val storePath = (project.findProperty("FEARINDEX_STORE_FILE") as String?)
                ?: System.getenv("FEARINDEX_STORE_FILE")
            if (!storePath.isNullOrBlank() && file(storePath).exists()) {
                storeFile = file(storePath)
                storePassword = (project.findProperty("FEARINDEX_STORE_PASSWORD") as String?)
                    ?: System.getenv("FEARINDEX_STORE_PASSWORD") ?: ""
                keyAlias = (project.findProperty("FEARINDEX_KEY_ALIAS") as String?)
                    ?: System.getenv("FEARINDEX_KEY_ALIAS") ?: "fearindex"
                keyPassword = (project.findProperty("FEARINDEX_KEY_PASSWORD") as String?)
                    ?: System.getenv("FEARINDEX_KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // Debug: 테스트 광고 (자동 세이프)
            buildConfigField("String", "ADMOB_BANNER_HOME", "\"ca-app-pub-3940256099942544/9214589741\"")
            buildConfigField("String", "ADMOB_BANNER_CHART", "\"ca-app-pub-3940256099942544/9214589741\"")
            buildConfigField("String", "ADMOB_BANNER_COMMUNITY", "\"ca-app-pub-3940256099942544/9214589741\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Release: 실제 AdMob 배너 ID (2026-04-14 발급, HomeBanner 단위)
            // TODO: 차트/커뮤니티용 별도 배너 단위는 추후 세분화
            buildConfigField("String", "ADMOB_BANNER_HOME", "\"ca-app-pub-5283496525222246/3189551565\"")
            buildConfigField("String", "ADMOB_BANNER_CHART", "\"ca-app-pub-5283496525222246/3189551565\"")
            buildConfigField("String", "ADMOB_BANNER_COMMUNITY", "\"ca-app-pub-5283496525222246/3189551565\"")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":presentation"))

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.work.runtime.ktx)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.config)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)

    // AdMob
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)

    // Logging
    implementation(libs.timber)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
