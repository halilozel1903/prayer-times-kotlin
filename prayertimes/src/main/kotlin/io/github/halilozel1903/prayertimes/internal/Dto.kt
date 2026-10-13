package io.github.halilozel1903.prayertimes.internal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Aladhan's envelope: `data` is an object, an array, or an error message string. */
@Serializable
internal data class EnvelopeDto(
    val code: Int,
    val status: String = "",
    val data: JsonElement? = null,
)

@Serializable
internal data class DayDto(
    val timings: Map<String, String>,
    val date: DateDto,
    val meta: MetaDto,
)

@Serializable
internal data class DateDto(
    val readable: String = "",
    val timestamp: String = "",
    val gregorian: GregorianDto,
    val hijri: HijriDto,
)

@Serializable
internal data class GregorianDto(
    /** `DD-MM-YYYY`. */
    val date: String,
)

@Serializable
internal data class HijriDto(
    val date: String = "",
    val day: String,
    val month: HijriMonthDto,
    val year: String,
    val weekday: WeekdayDto? = null,
    val holidays: List<String> = emptyList(),
)

@Serializable
internal data class HijriMonthDto(
    val number: Int,
    val en: String = "",
    val ar: String = "",
)

@Serializable
internal data class WeekdayDto(
    val en: String = "",
    val ar: String = "",
)

@Serializable
internal data class MetaDto(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timezone: String,
    val method: MethodDto? = null,
    val school: String = "",
    val offset: Map<String, Int> = emptyMap(),
)

@Serializable
internal data class MethodDto(
    val id: Int,
    val name: String = "",
)

@Serializable
internal data class QiblaDto(
    val latitude: Double,
    val longitude: Double,
    @SerialName("direction") val direction: Double,
)
