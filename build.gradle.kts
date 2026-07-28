buildscript {
    dependencies {
        // AGP 9's built-in Kotlin embeds KGP 2.2.10; the project needs Kotlin 2.3.21
        // (Room 3 is KSP-only and KSP 2.3.x pairs with Kotlin 2.3.x — see ADR-0001).
        // Pinning a higher KGP on the build classpath is the documented override.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.kover) apply false
}

spotless {
    kotlin {
        target("**/src/**/*.kt")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target("*.gradle.kts", "**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
    }
}
