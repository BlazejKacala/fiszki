import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    android {
        namespace = "pl.bkacala.fiszkide.shared"
        compileSdk = 36
        minSdk = 29
        androidResources { enable = true }
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    // JVM target makes the shared data and session tests runnable without an emulator.
    jvm()

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "FiszkiShared"
            isStatic = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.compose.runtime:runtime:1.10.3")
            implementation("org.jetbrains.compose.foundation:foundation:1.10.3")
            implementation("org.jetbrains.compose.ui:ui:1.10.3")
            implementation("org.jetbrains.compose.material3:material3:1.9.0")
            implementation("org.jetbrains.compose.components:components-resources:1.10.3")
            implementation(libs.sqlite.bundled)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        }
    }
}

// The root CSV is the single source of vocabulary for both mobile applications.
val vocabularyDirectory = layout.buildDirectory.dir("generated/vocabulary")
val prepareVocabulary by tasks.registering(Sync::class) {
    from(rootProject.layout.projectDirectory.file("slowka.csv")) { into("files") }
    into(vocabularyDirectory)
}

compose.resources {
    packageOfResClass = "pl.bkacala.fiszkide.resources"
    customDirectory(
        sourceSetName = "commonMain",
        directoryProvider = prepareVocabulary.map { vocabularyDirectory.get() },
    )
}
