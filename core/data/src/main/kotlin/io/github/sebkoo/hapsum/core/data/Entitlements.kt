package io.github.sebkoo.hapsum.core.data

import io.github.sebkoo.hapsum.core.model.Entitlement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Read surface for gated capabilities (ADR-0007). */
interface Entitlements {
    /** Emits the current grant and every subsequent change; never errors. */
    fun isGranted(entitlement: Entitlement): Flow<Boolean>
}

/** MVP default: everyone is free tier; nothing is granted, ever. */
object FreeTierEntitlements : Entitlements {
    override fun isGranted(entitlement: Entitlement): Flow<Boolean> = flowOf(false)
}
