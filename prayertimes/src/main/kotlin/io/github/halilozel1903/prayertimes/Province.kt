package io.github.halilozel1903.prayertimes

/**
 * One of Turkey's 81 provinces (il).
 *
 * @property plate the vehicle registration (plaka) code, 1 to 81, which is also the official province number.
 * @property name the Turkish name, for example `"İstanbul"`.
 * @property latitude latitude of the provincial center (merkez), in degrees.
 * @property longitude longitude of the provincial center, in degrees.
 */
public data class Province(
    val plate: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
) {
    /** The plate code with two digits, for example `"06"`. */
    public val plateCode: String get() = plate.toString().padStart(2, '0')

    /** [name] folded for search, see [TurkishText.fold]. */
    public val searchKey: String = TurkishText.fold(name)

    /** [name] in Turkish upper case, for example `"İZMİR"`. */
    public val upperName: String get() = TurkishText.uppercase(name)

    /** The Qibla bearing from the provincial center, in degrees from true north. */
    public val qiblaBearing: Double get() = Qibla.bearing(latitude, longitude)

    override fun toString(): String = "$plateCode $name"
}
