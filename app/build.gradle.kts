plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}
import java.util.Properties

val versionPropsFile = file("version.properties")
val versionProps = Properties().apply {
    if (versionPropsFile.exists()) load(versionPropsFile.inputStream())
}

val major = versionProps.getProperty("major", "1").toInt()
val minor = versionProps.getProperty("minor", "1").toInt()
val patch = versionProps.getProperty("patch", "0").toInt()

android {
    namespace = "com.potato.tapman104"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.potato.tapman104"
        minSdk = 24
        targetSdk = 35
        versionCode = major * 10000 + minor * 100 + patch
        versionName = "$major.$minor.$patch"
    }
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = false
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }
}

fun incrementPatch() {
    val newPatch = patch + 1
    versionProps["patch"] = newPatch.toString()
    versionPropsFile.writer().use { versionProps.store(it, null) }
    println("Version bumped to $major.$minor.$newPatch")
}

tasks.matching { it.name == "assembleDebug" }.configureEach {
    doLast { incrementPatch() }
}

tasks.matching { it.name == "assembleRelease" }.configureEach {
    doLast { incrementPatch() }
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
    ksp(libs.androidx.room.compiler)
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.media3:media3-session:1.3.1")
    testImplementation("junit:junit:4.13.2")
}
