@file:JvmSynthetic

package net.samyn.kapper.internal

import net.samyn.kapper.KapperUnsupportedOperationException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Date
import java.util.UUID

fun interface AutoConverter {
    fun convert(
        value: Any,
        target: Class<*>,
    ): Any
}

// register converters
//   we can/should extend this to allow users to register custom converters.
private val converters: Map<Class<*>, (Any) -> Any> =
    mapOf(
        UUID::class.java to ::convertUUID,
        LocalDate::class.java to ::convertLocalDate,
        LocalDateTime::class.java to ::convertLocalDateTime,
        LocalTime::class.java to ::convertLocalTime,
        Instant::class.java to ::convertInstant,
        Char::class.java to ::convertChar,
        Int::class.java to ::convertInt,
        Long::class.java to ::convertLong,
        Date::class.java to ::convertDate,
        Boolean::class.java to ::convertBoolean,
        String::class.java to ::convertString,
    )

val autoConverter =
    AutoConverter { value, target ->
        // Kover considers this not covered, which I think is a bug.
        //  https://github.com/Kotlin/kotlinx-kover/issues/729
        converters[target]?.invoke(value) ?: throw KapperUnsupportedOperationException(
            "Cannot auto-convert from ${value.javaClass} to ${target.canonicalName}",
        )
    }
