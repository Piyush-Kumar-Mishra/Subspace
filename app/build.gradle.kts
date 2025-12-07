//plugins {
//    alias(libs.plugins.android.application)
//    alias(libs.plugins.kotlin.android)
//    alias(libs.plugins.kotlin.compose)
//    kotlin("kapt")
//    alias(libs.plugins.dagger.hilt)
//}
//
//android {
//    namespace = "com.example.linkit"
//    compileSdk = 35
//
//    defaultConfig {
//        applicationId = "com.example.linkit"
//        minSdk = 26
//        targetSdk = 35
//        versionCode = 1
//        versionName = "1.0"
//
//        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//    }
//
//    buildTypes {
//        release {
//            isMinifyEnabled = false
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
//            )
//        }
//    }
//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_11
//        targetCompatibility = JavaVersion.VERSION_11
//    }
//    kotlinOptions {
//        jvmTarget = "11"
//    }
//    buildFeatures {
//        compose = true
//    }
//}
//
//hilt {
//    enableAggregatingTask = false
//}
//
//dependencies {
//    implementation(libs.androidx.core.ktx)
//    implementation(libs.androidx.lifecycle.runtime.ktx)
//    implementation(libs.androidx.activity.compose)
//    implementation(platform(libs.androidx.compose.bom))
//    implementation(libs.androidx.ui)
//    implementation(libs.androidx.ui.graphics)
//    implementation(libs.androidx.ui.tooling.preview)
//    implementation(libs.androidx.material3)
//    testImplementation(libs.junit)
//    androidTestImplementation(libs.androidx.junit)
//    androidTestImplementation(libs.androidx.espresso.core)
//    androidTestImplementation(platform(libs.androidx.compose.bom))
//    androidTestImplementation(libs.androidx.ui.test.junit4)
//    debugImplementation(libs.androidx.ui.tooling)
//    debugImplementation(libs.androidx.ui.test.manifest)
//    // Compose & Navigation
////    implementation("androidx.compose.ui:ui:1.5.0")
////    implementation("androidx.compose.material:material:1.5.0")
//    implementation("androidx.navigation:navigation-compose:2.7.0")
//
//    // Lifecycle & ViewModel
//    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
//
//    // Retrofit & OkHttp
//    implementation("com.squareup.retrofit2:retrofit:2.9.0")
//    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
//    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.11")
//
//    // Hilt
//    implementation("com.google.dagger:hilt-android:2.57")
//    kapt("com.google.dagger:hilt-compiler:2.57")
//    implementation("androidx.hilt:hilt-navigation-compose:1.0.0")
//
//    // DataStore
//    implementation("androidx.datastore:datastore-preferences:1.1.0")
//
//    // Coroutines & Logging
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
//    implementation("com.jakewharton.timber:timber:5.0.1")
//
//    // Image handling
//    implementation(libs.coil.compose)
//    // Removed older activity-compose alias; using libs.androidx.activity.compose
//
//    // File picking
//    implementation(libs.androidx.activity.ktx)
//
//    // Room database
//    implementation(libs.androidx.room.runtime)
//    implementation(libs.androidx.room.ktx)
//    kapt("androidx.room:room-compiler:2.7.2")
//
//    // JSON parsing for JWT
//    implementation(libs.kotlinx.serialization.json)
//
//    // Network connectivity
//    implementation(libs.androidx.lifecycle.process)
//
//    implementation(libs.retrofit2.kotlinx.serialization.converter)
//
//    implementation(libs.vico.compose)
//    implementation(libs.vico.compose.m3)
//
//
//}
//kapt{ correctErrorTypes = true }
//
//
//


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("kapt")
    alias(libs.plugins.dagger.hilt)
}

android {
    namespace = "com.example.linkit"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.linkit"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

hilt {
    enableAggregatingTask = false
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.0")

    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")

    // Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.11")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.57")
    kapt("com.google.dagger:hilt-compiler:2.57")
    implementation("androidx.hilt:hilt-navigation-compose:1.0.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.0")

    // Coroutines & Logging
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Image handling
    implementation(libs.coil.compose)

    // File picking
    implementation(libs.androidx.activity.ktx)

    // Room database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt("androidx.room:room-compiler:2.7.2")

    // JSON parsing for JWT
    implementation(libs.kotlinx.serialization.json)

    // Network connectivity
    implementation(libs.androidx.lifecycle.process)

    implementation(libs.retrofit2.kotlinx.serialization.converter)

//    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

//    implementation(libs.androidchart)
//    implementation(libs.vico.views)

    implementation(libs.mpAndroidChart)
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
    implementation(libs.vico.core)

    implementation("androidx.compose.ui:ui:1.5.0")
    implementation("androidx.compose.material:material:1.5.0")
}

kapt { correctErrorTypes = true }