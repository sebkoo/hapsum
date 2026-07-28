package io.github.sebkoo.hapsum

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.github.sebkoo.hapsum.core.data.CategoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class HapsumApplication : Application() {
    @Inject
    lateinit var categoryRepository: CategoryRepository

    @Inject
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        // Idempotent by design — safe on every app start (ADR-0003).
        applicationScope.launch { categoryRepository.seedDefaults() }
    }
}
