plugins {
    id("com.android.library")
    alias(libs.plugins.ksp)
}

android {
    // The genai-prompt dependency is an Android AAR (ADR-0006) — the row where the module
    // first needs the Android framework at all.
    namespace = "io.github.sebkoo.hapsum.core.ai"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.coroutines.android)
    implementation(libs.mlkit.genai.prompt)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
