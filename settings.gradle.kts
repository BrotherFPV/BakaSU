@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        // 离线可用的本地仓库（由 Gradle 缓存重组而成）
        maven { url = uri("file:///D:/app_backup/wear/_m2") }
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
    }
    resolutionStrategy {
        eachPlugin {
            // 本地缓存只有 AGP 本体（com.android.tools.build:gradle），缺插件 marker，
            // 这里直接映射到模块，避免联网解析 marker 失败。
            when (requested.id.id) {
                "com.android.application",
                "com.android.library",
                "com.android.test",
                -> useModule("com.android.tools.build:gradle:${requested.version}")
                "org.jetbrains.kotlin.plugin.serialization" ->
                    useModule("org.jetbrains.kotlin:kotlin-serialization:${requested.version}")
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        maven { url = uri("file:///D:/app_backup/wear/_m2") }
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "BakaSU"
include(":app")
include(":baselineprofile")
include(":lint-rules")
