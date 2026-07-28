plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    // Cross-compiles to bytecode 17 on whatever JDK Gradle runs on — matches :app's
    // compileOptions without forcing a JDK 17 toolchain to be installed (ADR-0001).
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    testImplementation(libs.junit)
}
