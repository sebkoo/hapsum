package io.github.sebkoo.hapsum.core.ai

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The ordered engine ladder (ADR-0006), colocated with the layer that owns the [AiEngine] type
 * — the ADR-0005 `DataModule` precedent. Gemini Nano first, the deterministic
 * [RuleBasedAiEngine] floor last; [AiEngineChain] owns all fallback policy.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object AiModule {
    @Provides
    @Singleton
    fun provideAiEngine(): AiEngine = AiEngineChain(listOf(GeminiNanoEngine(), RuleBasedAiEngine()))
}
