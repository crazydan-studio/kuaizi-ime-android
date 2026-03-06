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

    packaging {
        resources {
            excludes +=
                setOf(
                    // https://github.com/amzxyz/rime_wanxiang/tree/v15.0.3?tab=readme-ov-file#8--%E8%87%AA%E5%AE%9A%E4%B9%89%E6%89%A9%E5%B1%95-%E7%BA%BF%E4%B8%8A%E6%95%B0%E6%8D%AE%E8%8E%B7%E5%8F%96
                    "/assets/rime_wanxiang/.git*",
                    "/assets/rime_wanxiang/version.txt",
                    "/assets/rime_wanxiang/weasel.yaml",
                    "/assets/rime_wanxiang/wanxiang_t9.*",
                    "/assets/rime_wanxiang/wanxiang_mixedcode.*",
                    "/assets/rime_wanxiang/lua/data/chinese_english.txt",
                    "/assets/rime_wanxiang/lua/data/english_chinese.txt",
                    "/assets/rime_wanxiang/lua/data/t9_abbrev.txt",
                    "/assets/rime_wanxiang/lua/data/tips_*",
                    "/assets/rime_wanxiang/custom/*.txt",
                    "/assets/rime_wanxiang/custom/wanxiang_pro.*",
                    "/assets/rime_wanxiang/custom/wanxiang_chaifen*",
                    "/assets/rime_wanxiang/custom/wanxiang_mixedcode.*",
                    "/assets/rime_wanxiang/dicts/renming.dict.yaml",
                    "/assets/rime_wanxiang/dicts/wuzhong.dict.yaml",
                    "/assets/rime_wanxiang/**/*.json",
                    "/assets/rime_wanxiang/**/*.md",
                    "/assets/rime_wanxiang/**/*.png",
                    "/assets/rime_wanxiang/**/*.jpg",
                    "/assets/rime_wanxiang/**/*.zip",
                )
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