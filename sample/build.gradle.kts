plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.halilozel1903.prayertimes.sample"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.halilozel1903.prayertimes.sample"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/INDEX.LIST",
                "/META-INF/DEPENDENCIES",
                "/META-INF/versions/9/OSGI-INF/MANIFEST.MF",
                "/META-INF/versions/9/module-info.class",
                "/META-INF/versions/9/previous-compilation-data.bin",
            )
        }
    }
}

dependencies {
    implementation(project(":prayertimes-android"))
    // OkHttp is the natural engine on Android; without this line the client uses Ktor's CIO engine.
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.core.ktx)
    // Per-app language (AppCompatDelegate.setApplicationLocales), also below Android 13.
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
