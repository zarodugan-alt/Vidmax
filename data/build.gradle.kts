plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.comet.data"
    compileSdk = 34

    defaultConfig {
        minSdk = 28
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
    // Exposed upward: :ui, :download and :app consume Room/Flow types defined here.
    api(libs.room.runtime)
    api(libs.room.ktx)
    api(libs.datastore.preferences)
    api(libs.kotlinx.coroutines.android)

    ksp(libs.room.compiler)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.hilt.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
