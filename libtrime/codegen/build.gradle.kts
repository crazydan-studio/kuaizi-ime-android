// SPDX-FileCopyrightText: 2015 - 2024 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    implementation("com.squareup:kotlinpoet-ksp:2.2.0")
    implementation("com.google.devtools.ksp:symbol-processing-api:2.3.2")
}
