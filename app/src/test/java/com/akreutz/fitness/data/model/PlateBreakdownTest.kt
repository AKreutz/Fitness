package com.akreutz.fitness.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PlateBreakdownTest {

    @Test
    fun `weight at or below bar weight needs no plates`() {
        assertEquals(emptyList<Double>(), PlateBreakdown.forWeight(20.0))
        assertEquals(emptyList<Double>(), PlateBreakdown.forWeight(10.0))
    }

    @Test
    fun `splits remaining weight evenly across both sides, largest plates first`() {
        // 42.5 - 20 bar = 22.5, per side 11.25 = 10 + 1.25
        assertEquals(listOf(10.0, 1.25), PlateBreakdown.forWeight(42.5))
    }

    @Test
    fun `uses multiple plates of the same size when needed`() {
        // 60 - 20 bar = 40, per side 20 = 20 (single plate)
        assertEquals(listOf(20.0), PlateBreakdown.forWeight(60.0))
        // 100 - 20 bar = 80, per side 40 = 20 + 20
        assertEquals(listOf(20.0, 20.0), PlateBreakdown.forWeight(100.0))
    }

    @Test
    fun `weight that cannot be made up exactly returns empty`() {
        // per side would be 1.1, not achievable from the available plate sizes
        assertEquals(emptyList<Double>(), PlateBreakdown.forWeight(22.2))
    }

    @Test
    fun `smallest plate increment is supported`() {
        // 22.5 - 20 bar = 2.5, per side 1.25
        assertEquals(listOf(1.25), PlateBreakdown.forWeight(22.5))
    }
}
