package net.samyn.kapper.internal

import net.samyn.kapper.KapperParseException
import net.samyn.kapper.KapperUnsupportedOperationException
import java.nio.ByteBuffer
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Calendar
import java.util.Date
import java.util.UUID
import kotlin.reflect.KClass

internal object AutoConverter {
    fun convert(
        value: Any,
        target: KClass<*>,
    ): Any {
        val converted =
            when (target) {
                UUID::class -> {
                    if (value is String) {
                        try {
                            UUID.fromString(value)
                        } catch (e: Exception) {
                            throw KapperParseException(
                                "Cannot parse $value to UUID",
                                e,
                            )
                        }
                    } else if (value is ByteArray) {
                        value.asUUID()
                    } else {
                        throw KapperUnsupportedOperationException(
                            "Cannot auto-convert from ${value.javaClass} to ${target.qualifiedName}",
                        )
                    }
                }
                LocalDate::class ->
                    if (value is Date) {
                        val cal = Calendar.getInstance()
                        cal.time = value
                        LocalDate.of(cal[Calendar.YEAR], cal[Calendar.MONTH] + 1, cal[Calendar.DAY_OF_MONTH])
                    } else {
                        throw KapperUnsupportedOperationException(
                            "Cannot auto-convert from ${value.javaClass} to ${target.qualifiedName}",
                        )
                    }
                LocalDateTime::class ->
                    if (value is Instant) {
                        LocalDateTime.ofInstant(value, java.time.ZoneOffset.UTC)
                    } else {
                        throw KapperUnsupportedOperationException(
                            "Cannot auto-convert from ${value.javaClass} to ${target.qualifiedName}",
                        )
                    }
                LocalTime::class ->
                    if (value is Instant) {
                        LocalTime.ofInstant(value, java.time.ZoneOffset.UTC)
                    } else {
                        throw KapperUnsupportedOperationException(
                            "Cannot auto-convert from ${value.javaClass} to ${target.qualifiedName}",
                        )
                    }
                Instant::class ->
                    if (value is LocalTime) {
                        value.atDate(LocalDate.now()).toInstant(java.time.ZoneOffset.UTC)
                    } else if (value is LocalDateTime) {
                        value.atZone(java.time.ZoneOffset.UTC).toInstant()
                    } else {
                        throw KapperUnsupportedOperationException(
                            "Cannot auto-convert from ${value.javaClass} to ${target.qualifiedName}",
                        )
                    }
                else ->
                    throw KapperUnsupportedOperationException(
                        "Cannot auto-convert from ${value.javaClass} to ${target.qualifiedName}",
                    )
            }
        return converted
    }

    private fun ByteArray.asUUID(): UUID {
        val b = ByteBuffer.wrap(this)
        return UUID(b.getLong(), b.getLong())
    }
}
