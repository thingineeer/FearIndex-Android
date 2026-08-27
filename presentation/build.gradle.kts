plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "th1ngjin.fearindex.presentation"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    buildTypes {
        debug {
            // AdMob Adaptive Banner 테스트 ID (정책 위반 방지). app 모듈과 동일.
            buildConfigField("String", "ADMOB_BANNER_HOME", "\"ca-app-pub-3940256099942544/9214589741\"")
            buildConfigField("String", "ADMOB_BANNER_INSIGHT", "\"ca-app-pub-3940256099942544/9214589741\"")
            buildConfigField("String", "ADMOB_BANNER_CHART", "\"ca-app-pub-3940256099942544/9214589741\"")
            buildConfigField("String", "ADMOB_BANNER_VOTE", "\"ca-app-pub-3940256099942544/9214589741\"")
            buildConfigField("String", "ADMOB_BANNER_SETTINGS", "\"ca-app-pub-3940256099942544/9214589741\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL", "\"ca-app-pub-3940256099942544/1033173712\"")
            // AdMob App Open 테스트 ID (Google 공식).
            buildConfigField("String", "ADMOB_APP_OPEN", "\"ca-app-pub-3940256099942544/9257395921\"")
        }
        release {
            // 화면별 AdMob 단위 (Adaptive Banner)
            buildConfigField("String", "ADMOB_BANNER_HOME", "\"ca-app-pub-5283496525222246/3189551565\"")
            buildConfigField("String", "ADMOB_BANNER_INSIGHT", "\"ca-app-pub-5283496525222246/1779867597\"")
            buildConfigField("String", "ADMOB_BANNER_CHART", "\"ca-app-pub-5283496525222246/1616216062\"")
            buildConfigField("String", "ADMOB_BANNER_VOTE", "\"ca-app-pub-5283496525222246/2417949811\"")
            buildConfigField("String", "ADMOB_BANNER_SETTINGS", "\"ca-app-pub-5283496525222246/4627498578\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL", "\"ca-app-pub-5283496525222246/1522532479\"")
            // ⚠️ App Open 프로덕션 단위 ID — AdMob Console에서 신규 발급 후 교체 필요.
            // 빈 값이면 AppOpenAdManager 게이트에서 로드/노출이 자동 차단된다.
            buildConfigField("String", "ADMOB_APP_OPEN", "\"ca-app-pub-5283496525222246/6583206280\"")
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
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // AdMob (Compose 광고 컴포넌트용)
    implementation(libs.gma.next.gen)
    implementation(libs.user.messaging.platform)

    // Accompanist
    implementation(libs.accompanist.permissions)

    // Play In-App Review (설정 "앱 평가하기")
    implementation(libs.play.review.ktx)

    // Images
    implementation(libs.coil.compose)

    implementation(libs.timber)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
