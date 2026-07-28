plugins {
    id("com.android.library")
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
}

android {
    namespace = "io.github.sebkoo.hapsum.core.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        // MigrationTestHelper reads exported schema JSON from test assets at runtime.
        getByName("test") {
            assets.srcDirs("$projectDir/schemas")
        }
    }
}

room3 {
    // Schema JSON is checked in for migration testing — this schema shape (categories,
    // expenses, the FK between them) is hard to reverse once real data exists (ADR-0003).
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.room3.runtime)
    ksp(libs.room3.compiler)
    implementation(libs.coroutines.android)

    testImplementation(project(":core:testing"))
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.room3.testing)
}
