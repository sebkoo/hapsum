package io.github.sebkoo.hapsum.core.data

import app.cash.turbine.test
import io.github.sebkoo.hapsum.core.model.Entitlement
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Test

class FreeTierEntitlementsTest {
    @Test
    fun `isGranted — CLOUD_AI on the free tier — emits false then completes`() =
        runTest {
            FreeTierEntitlements.isGranted(Entitlement.CLOUD_AI).test {
                assertFalse(awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `isGranted — every Entitlement entry — emits false`() =
        runTest {
            Entitlement.entries.forEach { entitlement ->
                FreeTierEntitlements.isGranted(entitlement).test {
                    assertFalse(awaitItem())
                    awaitComplete()
                }
            }
        }
}
