package io.github.halilozel1903.prayertimes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProvincesTest {

    @Test
    fun `there are exactly 81 provinces`() {
        assertEquals(81, TurkeyProvinces.all.size)
        assertEquals(81, TurkeyProvinces.alphabetical.size)
    }

    @Test
    fun `plate codes are unique and run from 1 to 81`() {
        val plates = TurkeyProvinces.all.map { it.plate }
        assertEquals(81, plates.toSet().size)
        assertEquals((1..81).toList(), plates.sorted())
        assertEquals((1..81).toList(), plates, "the list is ordered by plate code")
    }

    @Test
    fun `names are unique, also after folding`() {
        assertEquals(81, TurkeyProvinces.all.map { it.name }.toSet().size)
        assertEquals(81, TurkeyProvinces.all.map { it.searchKey }.toSet().size)
    }

    @Test
    fun `every center lies inside Turkey`() {
        for (province in TurkeyProvinces.all) {
            assertTrue(province.latitude in 35.8..42.2, "$province latitude ${province.latitude}")
            assertTrue(province.longitude in 25.6..44.9, "$province longitude ${province.longitude}")
        }
    }

    @Test
    fun `well known plate codes`() {
        assertEquals("Adana", TurkeyProvinces.byPlate(1).name)
        assertEquals("Ankara", TurkeyProvinces.byPlate(6).name)
        assertEquals("İstanbul", TurkeyProvinces.byPlate(34).name)
        assertEquals("İzmir", TurkeyProvinces.byPlate(35).name)
        assertEquals("Şanlıurfa", TurkeyProvinces.byPlate(63).name)
        assertEquals("Düzce", TurkeyProvinces.byPlate(81).name)
        assertEquals("06", TurkeyProvinces.byPlate(6).plateCode)
        assertNull(TurkeyProvinces.byPlateOrNull(82))
        assertFailsWith<IllegalArgumentException> { TurkeyProvinces.byPlate(0) }
    }

    @Test
    fun `alphabetical order follows the Turkish alphabet`() {
        val names = TurkeyProvinces.alphabetical.map { it.name }
        assertEquals("Adana", names.first())
        assertEquals("Zonguldak", names.last())
        // C < Ç < D
        assertTrue(names.indexOf("Bursa") < names.indexOf("Çanakkale"))
        assertTrue(names.indexOf("Çorum") < names.indexOf("Denizli"))
        // I (dotless) < İ, and ğ < s
        assertTrue(names.indexOf("Iğdır") < names.indexOf("Isparta"))
        assertTrue(names.indexOf("Isparta") < names.indexOf("İstanbul"))
        assertTrue(names.indexOf("İzmir") < names.indexOf("Kahramanmaraş"))
        // S < Ş < T
        assertTrue(names.indexOf("Sivas") < names.indexOf("Şanlıurfa"))
        assertTrue(names.indexOf("Şırnak") < names.indexOf("Tekirdağ"))
    }

    @Test
    fun `upper case uses Turkish rules`() {
        assertEquals("İZMİR", TurkeyProvinces.byPlate(35).upperName)
        assertEquals("IĞDIR", TurkeyProvinces.byPlate(76).upperName)
    }

    @Test
    fun `nearest finds the province of a point`() {
        assertEquals("İstanbul", TurkeyProvinces.nearest(41.04, 29.0).name)   // Beşiktaş
        assertEquals("Antalya", TurkeyProvinces.nearest(36.88, 30.70).name)
    }
}
