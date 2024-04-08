import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    kotlin("plugin.serialization") version "1.9.22"

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
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")
                implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.0")
            }
        }
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                @OptIn(ExperimentalComposeLibrary::class)
                implementation(compose.components.resources)

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                implementation("org.jsoup:jsoup:1.17.2")

                implementation("ch.qos.logback:logback-classic:1.5.3")
                implementation("com.aallam.openai:openai-client:3.7.1")
                implementation("com.aallam.ktoken:ktoken:0.3.0")

                implementation("de.kherud:llama:2.2.1")

                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

                val ktorVersion = "2.3.10"
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-cio:$ktorVersion")
                implementation("io.ktor:ktor-client-serialization:$ktorVersion")
                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
                implementation("io.ktor:ktor-client-logging:$ktorVersion")

                implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.0")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.0")
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
