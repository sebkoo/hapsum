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
    alias(libs.plugins.kover)
}

dependencies {
    kover(project(":app"))
    kover(project(":core:ai"))
    kover(project(":core:data"))
    kover(project(":core:designsystem"))
    kover(project(":core:model"))
    kover(project(":core:mvi"))
    kover(project(":feature:ledger"))
    kover(project(":feature:capture"))
    kover(project(":feature:confirm"))
    kover(project(":feature:insights"))
}

kover {
    currentProject {
        // The merged root "gate" task doesn't exist without an explicit variant here,
        // even though this project contributes no sources of its own.
        createVariant("gate") { }
    }
    reports {
        filters {
            includes {
                classes(
                    "io.github.sebkoo.hapsum.core.model.*",
                    "io.github.sebkoo.hapsum.core.ai.*",
                    "io.github.sebkoo.hapsum.core.data.*Repository*",
                    "io.github.sebkoo.hapsum.core.data.ExpenseMappingKt",
                    "io.github.sebkoo.hapsum.core.data.FreeTierEntitlements*",
                    "io.github.sebkoo.hapsum.core.designsystem.MoneyFormatterKt",
                    "io.github.sebkoo.hapsum.core.mvi.*",
                    "io.github.sebkoo.hapsum.feature.capture.ReceiptParserKt",
                    "io.github.sebkoo.hapsum.feature.ledger.LedgerViewModel*",
                    "io.github.sebkoo.hapsum.feature.capture.CaptureViewModel*",
                    "io.github.sebkoo.hapsum.feature.confirm.ConfirmViewModel*",
                    "io.github.sebkoo.hapsum.feature.insights.InsightsViewModel*",
                    "io.github.sebkoo.hapsum.feature.insights.AggregateMonthlySummariesUseCase*",
                )
            }
            excludes {
                classes(
                    // Zero-coverage-by-design — ADR-0006.
                    "io.github.sebkoo.hapsum.core.ai.GeminiNanoEngine*",
                    // Production Dispatchers wiring; tests inject a fake instead.
                    "io.github.sebkoo.hapsum.core.mvi.DefaultDispatcherProvider*",
                    "io.github.sebkoo.hapsum.core.ai.AiModule*",
                    "*HiltWrapper_*",
                    "*_Factory",
                    "*_Factory$*",
                    "*_Provide*Factory",
                    "*_HiltModules*",
                    "*_MembersInjector",
                    "*_Impl",
                    "*_Impl$*",
                    "*ComposableSingletons*",
                )
            }
        }
        variant("gate") {
            verify {
                rule("gate scope line coverage") {
                    minBound(80)
                }
            }
        }
    }
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
