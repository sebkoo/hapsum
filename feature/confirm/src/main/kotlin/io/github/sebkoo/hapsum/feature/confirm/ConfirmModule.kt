package io.github.sebkoo.hapsum.feature.confirm

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.sebkoo.hapsum.core.ai.RuleBasedEngine

/**
 * `:core:ai` stays a plain-JVM module with no Hilt dependency (row 18) — the binding lives here
 * instead, the same shape `:app`'s `AppModule` already uses for `:core:mvi`'s `DispatcherProvider`.
 * `RuleBasedEngine` is deterministic and stateless, so a single app-wide instance is correct.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object ConfirmModule {
    @Provides
    fun provideRuleBasedEngine(): RuleBasedEngine = RuleBasedEngine()
}
