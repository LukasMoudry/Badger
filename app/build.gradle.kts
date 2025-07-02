plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.example.badger"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.badger"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // runner for instrumentation tests
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("com.google.code.gson:gson:2.9.0")

    // for local unit tests
    testImplementation("junit:junit:4.13.2")

    // for instrumentation (androidTest) tests
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
