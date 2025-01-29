package net.samyn.kapper.internal

import net.samyn.kapper.KapperUnsupportedOperationException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import kotlin.reflect.KClass

internal class AutoConverter(
    // register converters
    //   we can/should extend this to allow users to register custom converters.
    private val converters: Map<KClass<*>, (Any) -> Any> =
        mapOf(
            UUID::class to ::convertUUID,
            LocalDate::class to ::convertLocalDate,
            LocalDateTime::class to ::convertLocalDateTime,
            LocalTime::class to ::convertLocalTime,
            Instant::class to ::convertInstant,
        ),
) {
    fun convert(
        value: Any,
        target: KClass<*>,
    ): Any =
        // Kover considers this not covered, which I think is a bug. Will reproduce in separate project and raise issue.
        converters[target]?.invoke(value) ?: throw KapperUnsupportedOperationException(
            "Cannot auto-convert from ${value.javaClass} to ${target.qualifiedName}",
        )
}
