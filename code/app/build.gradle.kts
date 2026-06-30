import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "org.crazydan.studio.app.ime.kuaizi"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.crazydan.studio.app.ime.kuaizi"
        minSdk = 25
        versionCode = 400
        versionName = "4.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        // 显式启用在 buildTypes 中对 buildConfigField 的使用支持
        // https://stackoverflow.com/questions/74634321/fixing-the-build-type-contains-custom-buildconfig-fields-but-the-feature-is-di#answer-74634322
        buildConfig = true
    }
    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore/release.properties")
            val keystoreProperties = Properties().apply {
                load(FileInputStream(keystorePropertiesFile))
            }

            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }

    buildTypes {
        debug {
            // 应用 id 添加调试信息后缀：需避免与 release 包同名，否则，在同一机器上二者不能共存
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"

            buildConfigField("String", "BUILD_TYPE", "\"debug\"")
        }

        release {
            signingConfig = signingConfigs["release"]

            // 可重复构建不能在发布包中包含版本控制信息：
            // https://f-droid.org/en/docs/Reproducible_Builds/#vcs-info
            vcsInfo.include = false

            isShrinkResources = true
            isMinifyEnabled = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            buildConfigField("String", "BUILD_TYPE", "\"release\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    dependenciesInfo {
        // <<<< 可重复构建不能在发布包中包含依赖信息：
        // https://f-droid.org/en/docs/Reproducible_Builds/
        includeInApk = false
        includeInBundle = false
        // >>>>>
    }

    applicationVariants.all {
        val variant = this
        val projectName = rootProject.name.replace(Regex("\\s+"), "_")
        val buildType = variant.buildType.name
        val versionName = variant.versionName

        // 统一设置 apk 的打包名称
        if (buildType != "debug") {
            variant.outputs.all {
                val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
                output.outputFileName = "${projectName}-${versionName}.apk"
            }
        }
    }
}

kotlin {
    sourceSets.configureEach {
        kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/$name/kotlin"))
    }
}

dependencies {
    implementation(project(":engine"))
    implementation(project(":ui"))
    ksp(project(":app-codegen"))

    implementation(libs.appcompat)
    implementation(libs.preference)
    implementation(libs.material)
    implementation(libs.datastore.preferences)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // https://kotlinlang.org/api/core/kotlin-test/
    testImplementation(kotlin("test"))
    // https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
