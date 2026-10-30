package com.nibras.nova.core.prayer

import org.junit.Assert.assertEquals
import org.junit.Test

class QiblaTest {
    @Test fun baku() { assertEquals(207.2, Qibla.bearing(40.4093, 49.8671), 1.0); assertEquals(2314.0, Qibla.distanceKm(40.4093, 49.8671), 15.0) }
    @Test fun istanbul() { assertEquals(152.0, Qibla.bearing(41.0082, 28.9784), 2.0) }
    @Test fun london() { assertEquals(119.0, Qibla.bearing(51.5072, -0.1276), 2.0) }
    @Test fun jakarta() { assertEquals(295.0, Qibla.bearing(-6.2088, 106.8456), 2.0) }
    @Test fun arrow() { assertEquals(13.0, Qibla.arrowRotation(213.0, 200.0), 1e-9); assertEquals(350.0, Qibla.arrowRotation(10.0, 20.0), 1e-9) }
}
