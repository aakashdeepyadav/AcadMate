plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.ksp)
    kotlin("plugin.serialization")

    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.acadmate"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.acadmate"
        minSdk = 24
        targetSdk = 35
        versionCode = 130
        versionName = "1.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    configurations.all {
        resolutionStrategy {
            force(libs.ktor.client.core)
            force(libs.ktor.client.okhttp)
            force(libs.ktor.client.content.negotiation)
            force(libs.ktor.client.logging)
            force(libs.ktor.client.serialization)
            force(libs.ktor.serialization.kotlinx.json)
            force(libs.com.google.dagger.hilt.android)
        }
    }
}

dependencies {
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(project(":core"))
    implementation(project(":auth"))
    implementation(project(":attendance"))
    implementation(project(":ai"))
    implementation(project(":dashboard"))
    implementation(project(":designsystem"))
    implementation(project(":admin"))
    implementation(project(":assignments"))
    implementation(libs.androidx.lottie.compose)
    implementation(libs.coil.compose)
    implementation(libs.com.google.play.services.auth)
    implementation(libs.play.services.basement)
    implementation(libs.com.google.dagger.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    ksp(libs.com.google.dagger.hilt.compiler)

    implementation(libs.androidx.work.runtime.ktx)

    // Import the Firebase BoM
    implementation(platform(libs.firebase.bom))

    // TODO: Add the dependencies for Firebase products you want to use
    // When using the BoM, don't specify versions in Firebase dependencies
    implementation("com.google.firebase:firebase-analytics")

    // Add the dependencies for any other desired Firebase products
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-functions")
    implementation("com.google.firebase:firebase-messaging")
    implementation(libs.firebase.appcheck)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.appcheck.debug)

    // Google Sign-In
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
