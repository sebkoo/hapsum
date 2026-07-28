package io.github.sebkoo.hapsum

import org.junit.Assert.assertEquals
import org.junit.Test

class ApplicationIdTest {
    @Test
    fun `applicationId — any build — matches the ADR-0001 namespace`() {
        assertEquals("io.github.sebkoo.hapsum", BuildConfig.APPLICATION_ID)
    }
}
