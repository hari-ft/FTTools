import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val keysDir = rootProject.file("keys")
val releaseKeyProps = Properties().apply {
    val file = keysDir.resolve("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.pdns.switcher"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pdns.switcher"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = keysDir.resolve("debug.keystore")
        }
        create("release") {
            if (!releaseKeyProps.isEmpty) {
                storeFile = keysDir.resolve(releaseKeyProps.getProperty("storeFile"))
                storePassword = releaseKeyProps.getProperty("storePassword")
                keyAlias = releaseKeyProps.getProperty("keyAlias")
                keyPassword = releaseKeyProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
}

listOf("debug", "release").forEach { buildType ->
    val variant = buildType.replaceFirstChar { it.uppercase() }
    val copyApk = tasks.register<Copy>("copy${variant}Apk") {
        from(layout.buildDirectory.dir("outputs/apk/$buildType"))
        include("*.apk")
        rename { "PDNSapp-${android.defaultConfig.versionName}-$buildType.apk" }
        into(rootProject.layout.buildDirectory.dir(buildType))
    }
    tasks.matching { it.name == "assemble$variant" }.configureEach { finalizedBy(copyApk) }
}
