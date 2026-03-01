import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.google.devtools.ksp")

    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.crazydan.studio.ime.libtrime"
    compileSdk = 35

    defaultConfig {
        minSdk = 25

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            // 可重复构建不能在发布包中包含版本控制信息：
            // https://f-droid.org/en/docs/Reproducible_Builds/#vcs-info
            @Suppress("UnstableApiUsage")
            vcsInfo.include = false

            isMinifyEnabled = true

            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlin {
        compilerOptions {
            // https://youtrack.jetbrains.com/issue/KT-55947
            jvmTarget.set(JvmTarget.JVM_1_8)
            // https://youtrack.jetbrains.com/issue/KT-73255/Change-defaulting-rule-for-annotations
            freeCompilerArgs.add("-Xannotation-default-target=param-property")
        }
    }
}

dependencies {
    ksp(project(":libtrime:codegen"))

    implementation("androidx.core:core-ktx:1.17.0")

    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("com.louiscad.splitties:splitties-bitflags:3.0.0")

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}