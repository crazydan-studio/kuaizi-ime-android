import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.google.devtools.ksp")

    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.crazydan.studio.ime.libtrime"
    compileSdk = 36

    defaultConfig {
        minSdk = 25

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        // https://developer.android.com/reference/tools/gradle-api/7.4/com/android/build/api/dsl/ExternalNativeBuildOptions
        @Suppress("UnstableApiUsage")
        externalNativeBuild {
            cmake {
                //arguments += listOf("-DCMAKE_ANDROID_STL_TYPE=c++_static", "-DCMAKE_POSITION_INDEPENDENT_CODE=ON")
                abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            }
        }
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

    // https://developer.android.com/reference/tools/gradle-api/7.4/com/android/build/api/dsl/ExternalNativeBuild
    externalNativeBuild {
        cmake {
            path = file("src/main/jni/CMakeLists.txt")
        }
    }
}

dependencies {
    ksp(project(":libtrime:codegen"))

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")

    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("com.louiscad.splitties:splitties-bitflags:3.0.0")

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}