plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("org.jetbrains.kotlin.kapt")
}
import java.text.SimpleDateFormat
import java.util.Date

// Automated build versioning
val baseVersionName = "0.0.1"
val buildTimestamp = SimpleDateFormat("yyyyMMdd.HHmm").format(Date())
val autoVersionCode = ((System.currentTimeMillis() / 1000L) - 1700000000L).toInt().coerceAtLeast(1)
val gitCommitCount = try {
    val process = ProcessBuilder("git", "rev-list", "--count", "HEAD")
        .redirectErrorStream(true)
        .start()
    process.inputStream.bufferedReader().readText().trim().toIntOrNull() ?: 1
} catch (e: Exception) { 1 }
val gitSha = try {
    val process = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
        .redirectErrorStream(true)
        .start()
    process.inputStream.bufferedReader().readText().trim()
} catch (e: Exception) { "" }

val autoVersionName = if (gitSha.isNotEmpty()) {
    "$baseVersionName.$gitCommitCount-$gitSha ($buildTimestamp)"
} else {
    "$baseVersionName ($buildTimestamp)"
}
println("-> Building Potato Player version: $autoVersionName (versionCode: $autoVersionCode)")

android {
    namespace = "com.potato.tapman104"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.potato.tapman104"
        minSdk = 24
        targetSdk = 35
        versionCode = autoVersionCode
        versionName = autoVersionName
    }
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = false
        }
    }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }
}
dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)
    implementation(libs.androidx.preference.ktx)
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation("com.google.android.material:material:1.12.0")
    kapt(libs.androidx.room.compiler)
    testImplementation("junit:junit:4.13.2")
}
