package io.github.sebkoo.hapsum

import android.content.Context
import io.github.sebkoo.hapsum.core.data.ExpenseRepository
import io.github.sebkoo.hapsum.core.data.HapsumDataContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manual composition root — holds until a DI ladder row exists. Everything here is
 * constructor-injectable, so a future Hilt commit replaces this file, not its clients.
 */
class AppContainer(
    context: Context,
) {
    private val data = HapsumDataContainer(context)

    val expenseRepository: ExpenseRepository get() = data.expenseRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // Idempotent by design — safe on every app start (ADR-0003).
        applicationScope.launch { data.categoryRepository.seedDefaults() }
    }
}
