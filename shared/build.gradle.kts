import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.mavenPublish)
}

group = "org.pollyanna"
version = "0.1.1"

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.utils)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.security.crypto)
        }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/pollyannaanalytics/PollySoftNfc-SDK")
            credentials {
                username = providers.gradleProperty("githubActor").orNull
                    ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("githubToken").orNull
                    ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

android {
    namespace = "org.pollyanna.nfckmp.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    buildFeatures {
        buildConfig = true
    }
}
