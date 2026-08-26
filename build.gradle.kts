plugins {
    // Bumped from 8.7.0: navigation-compose 2.10.0's transitive deps
    // (androidx.activity 1.13.0, androidx.core 1.18.0, etc.) require AGP
    // 9.1.0+. Paired with Gradle 9.7.1 in gradle-wrapper.properties.
    // AGP 9 also provides Kotlin support built in, so the separate
    // org.jetbrains.kotlin.android plugin is deliberately NOT applied here
    // — applying both throws "Cannot add extension with name 'kotlin', as
    // there is an extension already registered with that name" (confirmed
    // by a real build, 2026-08-26). The Kotlin version is still pinned via
    // kotlin.plugin.compose/serialization below.
    id("com.android.application") version "9.3.2" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0" apply false
    // Must track the Kotlin version above — KSP publishes as
    // <kotlin-version>-<ksp-version>, e.g. this is the KSP release paired
    // with Kotlin 2.2.0.
    id("com.google.devtools.ksp") version "2.2.0-2.0.2" apply false
    // First-party Kotlin compiler plugin — version matches the Kotlin
    // version directly (no separate versioning scheme like KSP's).
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0" apply false
}
