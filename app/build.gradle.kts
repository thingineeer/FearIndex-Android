plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google-services)
    alias(libs.plugins.firebase-crashlytics)
    alias(libs.plugins.kotlin-serialization)
}

android {
    namespace = "com.thingineer.fearindex"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.thingineer.fearindex"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // AdMob App ID — 테스트 ID (실제 ID로 교체 필요)
        manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
    }

    signingConfigs {
        create("release") {
            val keystoreFile = file("${rootDir}/keystore/release.keystore")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("KEY_ALIAS") ?: "fearindex"
                keyPassword = System.getenv("KEY_PASSWORD") ?: ""
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
            // Release: 실제 광고 ID (AdMob 콘솔에서 생성 후 교체 필요)
            // 현재는 임시로 iOS와 같은 publisher 사용 - 실제 Android 단위 ID로 교체 필수
            buildConfigField("String", "ADMOB_BANNER_HOME", "\"ca-app-pub-3940256099942544/9214589741\"")
            buildConfigField("String", "ADMOB_BANNER_CHART", "\"ca-app-pub-3940256099942544/9214589741\"")
            buildConfigField("String", "ADMOB_BANNER_COMMUNITY", "\"ca-app-pub-3940256099942544/9214589741\"")
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
