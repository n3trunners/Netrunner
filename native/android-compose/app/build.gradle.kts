plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val buildRustCore = tasks.register<Exec>("buildRustCoreArm64") {
    workingDir = rootProject.file("../rust-core")
    commandLine(
        "cargo", "ndk",
        "-t", "arm64-v8a",
        "--platform", "28",
        "-o", project.file("src/main/jniLibs").absolutePath,
        "build", "--release"
    )
    environment("RUSTFLAGS", "-C link-arg=-Wl,-z,max-page-size=16384")
}

fun resolveConfigValue(key: String, defaultValue: String = ""): String {
    val prop = project.findProperty(key)?.toString()?.trim()
    if (!prop.isNullOrEmpty()) return prop
    val env = System.getenv(key)?.trim()
    if (!env.isNullOrEmpty()) return env
    return defaultValue
}

fun escapeBuildConfig(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")

val githubClientId = resolveConfigValue("GITHUB_CLIENT_ID", "Ov23liXctgZr1SlUx5rh")
val githubClientSecret = resolveConfigValue("GITHUB_CLIENT_SECRET")
val oauthBaseUrl = resolveConfigValue(
    "OAUTH_BASE_URL",
    "https://ais-pre-27eedfzzz5vdywch54hxxg-668765759056.asia-northeast1.run.app"
)

val releaseStoreFilePath = resolveConfigValue("RELEASE_STORE_FILE")
val releaseStorePassword = resolveConfigValue("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = resolveConfigValue("RELEASE_KEY_ALIAS")
val releaseKeyPassword = resolveConfigValue("RELEASE_KEY_PASSWORD")
val hasReleaseSigning = releaseStoreFilePath.isNotEmpty() &&
    releaseStorePassword.isNotEmpty() &&
    releaseKeyAlias.isNotEmpty() &&
    releaseKeyPassword.isNotEmpty() &&
    file(releaseStoreFilePath).exists()

android {
    namespace = "com.netrunner.app"
    compileSdk = 36
    ndkVersion = "28.2.13676358"

    defaultConfig {
        applicationId = "com.netrunner.app"
        minSdk = 28
        targetSdk = 36
        versionCode = (System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1)
        versionName = "1.0.${System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 0}"

        buildConfigField("String", "GITHUB_CLIENT_ID", "\"${escapeBuildConfig(githubClientId)}\"")
        buildConfigField("String", "GITHUB_CLIENT_SECRET", "\"${escapeBuildConfig(githubClientSecret)}\"")
        buildConfigField("String", "OAUTH_BASE_URL", "\"${escapeBuildConfig(oauthBaseUrl)}\"")

        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFilePath)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
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

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.02.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.browser:browser:1.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}
