import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    kotlin("plugin.serialization") version "2.0.0"

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
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")
                implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")
            }
        }
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                @OptIn(ExperimentalComposeLibrary::class)
                implementation(compose.components.resources)

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
                implementation("org.jsoup:jsoup:1.18.1")

                implementation("ch.qos.logback:logback-classic:1.5.6")
                implementation("com.aallam.openai:openai-client:3.7.2")
                implementation("com.aallam.ktoken:ktoken:0.4.0")

                implementation("de.kherud:llama:2.2.1")

                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")

                val ktorVersion = "2.3.12"
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-cio:$ktorVersion")
                implementation("io.ktor:ktor-client-serialization:$ktorVersion")
                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
                implementation("io.ktor:ktor-client-logging:$ktorVersion")

                implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
            }
        }
    }
}


compose.desktop {
    application {
        mainClass = "reflectai.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "reflectai"
            packageVersion = "1.0.0"

            macOS {
                iconFile.set(project.file("icon.icns"))
            }
        }
    }
}
