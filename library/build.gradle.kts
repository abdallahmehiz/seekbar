import com.vanniktech.maven.publish.portal.SonatypeCentralPortal
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

group = "io.github.abdallahmehiz"
version = "1.1.1"

kotlin {
    jvm()
    androidTarget {
        publishLibraryVariants("release")
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(compose.uiTooling)
            }
        }
        val commonMain by getting {
            dependencies {
                implementation(compose.ui)
                implementation(compose.material3)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

dependencies {
    debugImplementation(compose.preview)
}

android {
    namespace = "org.jetbrains.kotlinx.multiplatform.library.template"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

afterEvaluate {
    mavenPublishing {
        publishToMavenCentral()
        signAllPublications()
        coordinates(
            group.toString(),
            "seekbar",
            version.toString()
        )
        pom {
            name = "SeekBar"
            description = "A Simple Seekbar library for media players."
            inceptionYear = "2024"
            url = "https://github.com/abdallahmehiz/seekbar"
            licenses {
                license {
                    name = "Apache License, Version 2.0"
                    url = "https://opensource.org/license/apache-2-0"
                    distribution = "repo"
                }
            }
            developers {
                developer {
                    id = "abdallahmehiz"
                    name = "Abdallah Mehiz"
                    url = "https://github.com/abdallahmehiz/"
                }
            }
            scm {
                url = "https://github.com/abdallahmehiz/seekbar"
                connection = "scm:git:git://github.com/abdallahmehiz/seekbar.git"
                developerConnection = "scm:git:ssh://git@github.com/abdallahmehiz/seekbar.git"
            }
        }
    }
}
