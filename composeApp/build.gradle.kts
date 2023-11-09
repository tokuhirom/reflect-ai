import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    kotlin("plugin.serialization") version "1.5.31"
    
    alias(libs.plugins.jetbrainsCompose)
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    targetHierarchy.default()

    jvm("desktop")
    
    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.2")
                implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.3")
                implementation("com.halilibo.compose-richtext:richtext-commonmark:0.17.0")
                implementation("com.halilibo.compose-richtext:richtext-commonmark-jvm:0.17.0")
            }
        }
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                @OptIn(ExperimentalComposeLibrary::class)
                implementation(compose.components.resources)

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2")

                implementation("ch.qos.logback:logback-classic:1.4.11")
                implementation("com.aallam.openai:openai-client:3.5.1")
                implementation("com.aallam.ktoken:ktoken:0.3.0")

                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

//                implementation("com.github.jeziellago:compose-markdown:0.3.5")
                implementation("com.halilibo.compose-richtext:richtext-commonmark:0.17.0")
                implementation("com.halilibo.compose-richtext:richtext-commonmark-jvm:0.17.0")

                val ktorVersion = "2.3.6"
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-cio:$ktorVersion")
                implementation("io.ktor:ktor-client-serialization:$ktorVersion")
                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

                implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.3")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.3")
            }
        }
    }
}


compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ai.reflect"
            packageVersion = "1.0.0"
        }
    }
}
