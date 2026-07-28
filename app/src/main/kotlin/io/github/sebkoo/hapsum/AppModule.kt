package io.github.sebkoo.hapsum

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.sebkoo.hapsum.core.mvi.DefaultDispatcherProvider
import io.github.sebkoo.hapsum.core.mvi.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/** App-lifecycle Hilt bindings — the bindings `AppContainer` used to wire by hand (ADR-0005). */
@Module
@InstallIn(SingletonComponent::class)
internal object AppModule {
    @Provides
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
