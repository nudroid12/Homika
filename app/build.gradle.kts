plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("androidx.room")
}

android {
    namespace = "com.homiq.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.homiq.app"
        minSdk = 26
        targetSdk = 36
        versionCode = System.getenv("HOMIKA_VERSION_CODE")?.toIntOrNull() ?: 10019
        versionName = System.getenv("HOMIKA_VERSION_NAME")?.takeIf { it.isNotBlank() } ?: "1.0.19"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("homiqDebug") {
            // Stable development certificate retained so installed Phase 10 debug builds
            // and the existing Google OAuth debug client keep working.
            storeFile = file("homiq-debug.keystore")
            storePassword = "homiq-debug"
            keyAlias = "homiqdebug"
            keyPassword = "homiq-debug"
        }

        val releaseStorePath = System.getenv("HOMIKA_KEYSTORE_PATH") ?: System.getenv("HOMIQU_KEYSTORE_PATH")
        val releaseStorePassword = System.getenv("HOMIKA_KEYSTORE_PASSWORD") ?: System.getenv("HOMIQU_KEYSTORE_PASSWORD")
        val releaseKeyAlias = (System.getenv("HOMIKA_KEY_ALIAS") ?: System.getenv("HOMIQU_KEY_ALIAS"))
            ?.takeIf { it.isNotBlank() }
            ?: "homika"
        val releaseKeyPassword = (System.getenv("HOMIKA_KEY_PASSWORD") ?: System.getenv("HOMIQU_KEY_PASSWORD"))
            ?.takeIf { it.isNotBlank() }
            ?: releaseStorePassword

        if (
            !releaseStorePath.isNullOrBlank() &&
            !releaseStorePassword.isNullOrBlank() &&
            !releaseKeyAlias.isNullOrBlank() &&
            !releaseKeyPassword.isNullOrBlank()
        ) {
            create("homikaRelease") {
                storeFile = file(releaseStorePath)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("homiqDebug")
        }

        getByName("release") {
            signingConfig = signingConfigs.findByName("homikaRelease")
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

ksp {
    arg("room.incremental", "true")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")
    val roomVersion = "2.8.4"

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("com.google.android.gms:play-services-auth:21.4.0")

    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    androidTestImplementation("androidx.room:room-testing:$roomVersion")
    androidTestImplementation("androidx.test:core-ktx:1.7.0")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("junit:junit:4.13.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
