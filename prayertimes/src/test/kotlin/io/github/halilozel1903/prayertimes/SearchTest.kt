package io.github.halilozel1903.prayertimes

import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchTest {
    private val defaultLocale = Locale.getDefault()

    @AfterTest
    fun restoreLocale() = Locale.setDefault(defaultLocale)

    private fun names(query: String) = TurkeyProvinces.search(query).map { it.name }

    @Test
    fun `fold ignores case and Turkish letters`() {
        assertEquals("istanbul", TurkishText.fold("İSTANBUL"))
        assertEquals("istanbul", TurkishText.fold("Istanbul"))
        assertEquals("istanbul", TurkishText.fold("istanbul"))
        assertEquals("igdir", TurkishText.fold("IĞDIR"))
        assertEquals("igdir", TurkishText.fold("Iğdır"))
        assertEquals("sanliurfa", TurkishText.fold("Şanlıurfa"))
        assertEquals("gumushane", TurkishText.fold("GÜMÜŞHANE"))
        assertEquals("corum", TurkishText.fold("ÇORUM"))
        assertEquals("kutahya", TurkishText.fold("Kütahya"))
        assertEquals("hala", TurkishText.fold(" hâlâ "))
    }

    @Test
    fun `fold gives the same result with a Turkish default locale`() {
        Locale.setDefault(Locale.forLanguageTag("tr-TR"))
        // With tr as default, "I".lowercase() would be "ı"; fold must not depend on that.
        assertEquals("izmir", TurkishText.fold("IZMIR"))
        assertEquals("istanbul", TurkishText.fold("İstanbul"))
        assertEquals(listOf("İzmir"), names("IZMIR"))
    }

    @Test
    fun `search matches without Turkish letters`() {
        assertEquals(listOf("İzmir"), names("izmir"))
        assertEquals(listOf("İzmir"), names("İZMİR"))
        assertEquals(listOf("Şanlıurfa"), names("sanliurfa"))
        assertEquals(listOf("Çanakkale"), names("canak"))
        assertEquals(listOf("Gümüşhane"), names("gumus"))
        assertEquals(listOf("Muğla"), names("MUGLA"))
        assertEquals(listOf("Iğdır"), names("igdir"))
    }

    @Test
    fun `search matches with Turkish letters typed on a Turkish keyboard`() {
        assertEquals(listOf("Eskişehir"), names("eskişehir"))
        assertEquals(listOf("Diyarbakır"), names("dıyarbakır"))   // dotless ı where i belongs still matches
    }

    @Test
    fun `prefix matches come before matches inside the name`() {
        val result = names("kır")
        assertEquals(listOf("Kırıkkale", "Kırklareli", "Kırşehir", "Çankırı", "Diyarbakır", "Tekirdağ"), result)
        assertEquals(result, names("kir"))
        assertTrue("Tekirdağ" in result, "contains match: te-kir-dag")
        assertTrue(result.indexOf("Tekirdağ") > 2)
    }

    @Test
    fun `search by plate code`() {
        assertEquals(listOf("Ankara"), names("06"))
        assertEquals(listOf("Ankara"), names("6"))
        assertEquals(listOf("İstanbul"), names("34"))
        assertEquals(emptyList(), names("82"))
    }

    @Test
    fun `blank query lists all 81 alphabetically`() {
        assertEquals(TurkeyProvinces.alphabetical, TurkeyProvinces.search(""))
        assertEquals(81, TurkeyProvinces.search("   ").size)
        assertEquals(emptyList(), names("xyz"))
    }

    @Test
    fun `byName ignores case and Turkish letters`() {
        assertEquals(76, TurkeyProvinces.byName("IGDIR")?.plate)
        assertEquals(34, TurkeyProvinces.byName("istanbul")?.plate)
        assertEquals(46, TurkeyProvinces.byName("kahramanmaras")?.plate)
        assertEquals(null, TurkeyProvinces.byName("İst"))
    }
}
