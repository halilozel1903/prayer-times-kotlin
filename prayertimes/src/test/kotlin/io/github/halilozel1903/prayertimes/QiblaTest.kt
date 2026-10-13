package io.github.halilozel1903.prayertimes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QiblaTest {

    @Test
    fun `Istanbul matches Aladhan's qibla endpoint`() {
        // Aladhan /v1/qibla/41.0082/28.9784 answers 151.6206517614259 (fixtures/qibla-istanbul.json).
        assertEquals(151.6206517614259, Qibla.bearing(41.0082, 28.9784), 0.01)
        assertEquals(151.62, TurkeyProvinces.byPlate(34).qiblaBearing, 0.05)
    }

    @Test
    fun `Ankara and other provinces`() {
        assertEquals(160.17, Qibla.bearing(TurkeyProvinces.byPlate(6)), 0.05)
        assertEquals(143.66, Qibla.bearing(TurkeyProvinces.byPlate(35)), 0.1)   // İzmir: more to the east
        assertEquals(191.0, Qibla.bearing(TurkeyProvinces.byPlate(65)), 0.5)    // Van: west of south
    }

    @Test
    fun `every province faces between south east and south west`() {
        for (province in TurkeyProvinces.all) {
            assertTrue(province.qiblaBearing in 135.0..200.0, "$province ${province.qiblaBearing}")
        }
    }

    @Test
    fun `cardinal directions`() {
        // Due north of the Kaaba the Qibla is due south, and due west of it roughly east.
        assertEquals(180.0, Qibla.bearing(40.0, Qibla.KAABA_LONGITUDE), 1e-9)
        assertEquals(0.0, Qibla.bearing(-10.0, Qibla.KAABA_LONGITUDE), 1e-9)
        val fromWest = Qibla.bearing(Qibla.KAABA_LATITUDE, 0.0)
        assertTrue(fromWest in 75.0..90.0, "$fromWest")
    }

    @Test
    fun `distance to the Kaaba`() {
        assertEquals(2405.0, Qibla.distanceKm(41.0082, 28.9784), 5.0)
        assertEquals(2162.0, Qibla.distanceKm(39.9334, 32.8597), 5.0)
        assertEquals(0.0, Qibla.distanceKm(Qibla.KAABA_LATITUDE, Qibla.KAABA_LONGITUDE), 1e-6)
    }

    @Test
    fun `relative bearing and compass points`() {
        assertEquals(61.62, Qibla.relativeBearing(151.62, 90.0), 1e-9)
        assertEquals(301.62, Qibla.relativeBearing(151.62, 210.0), 1e-9)
        assertEquals("SSE", Qibla.compassPoint(151.62))
        assertEquals("SSE", Qibla.compassPoint(160.17))
        assertEquals("N", Qibla.compassPoint(359.0))
        assertEquals("S", Qibla.compassPoint(180.0))
    }
}
