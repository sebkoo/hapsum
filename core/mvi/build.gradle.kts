plugins {
    id("com.android.library")
}

android {
    namespace = "io.github.sebkoo.hapsum.core.mvi"
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
    // Generic infrastructure only: lifecycle + coroutines, never :core:model or any
    // domain type — the runtime is parameterized over <State, Intent, Effect> (ADR-0004).
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
}
