import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)

        freeCompilerArgs.add("-XXLanguage:+WhenGuards")
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)

    // 用于序列化 @Serializable 标注的对象，且其需要启用插件 org.jetbrains.kotlin.plugin.serialization
    // https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serialization-guide.md
    implementation(libs.kotlinx.serialization.json)

    // https://kotlinlang.org/api/core/kotlin-test/
    testImplementation(kotlin("test"))
    // https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/
    testImplementation(libs.kotlinx.coroutines.test)
}
