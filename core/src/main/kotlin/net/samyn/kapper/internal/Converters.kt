@file:JvmSynthetic

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

internal fun convertInstant(value: Any): Instant =
    when (value) {
        is LocalTime -> {
            value.atDate(LocalDate.now()).toInstant(java.time.ZoneOffset.UTC)
        }

        is LocalDateTime -> {
            value.atZone(java.time.ZoneOffset.UTC).toInstant()
        }

        else -> {
            throw KapperUnsupportedOperationException(
                "Cannot auto-convert from ${value.javaClass} to Instant",
            )
        }
    }

internal fun convertLocalTime(value: Any): LocalTime =
    if (value is Instant) {
        LocalTime.ofInstant(value, java.time.ZoneOffset.UTC)
    } else if (value is String) {
        LocalTime.parse(value)
    } else {
        throw KapperUnsupportedOperationException(
            "Cannot auto-convert value '$value' from ${value.javaClass} to LocalTime",
        )
    }

internal fun convertLocalDateTime(value: Any): LocalDateTime =
    if (value is Instant) {
        LocalDateTime.ofInstant(value, java.time.ZoneOffset.UTC)
    } else if (value is String) {
        LocalDateTime.parse(value)
    } else {
        throw KapperUnsupportedOperationException(
            "Cannot auto-convert from ${value.javaClass} to LocalDateTime",
        )
    }

internal fun convertLocalDate(value: Any): LocalDate =
    if (value is Date) {
        val cal = Calendar.getInstance()
        cal.time = value
        LocalDate.of(cal[Calendar.YEAR], cal[Calendar.MONTH] + 1, cal[Calendar.DAY_OF_MONTH])
    } else if (value is String) {
        LocalDate.parse(value)
    } else {
        throw KapperUnsupportedOperationException(
            "Cannot auto-convert from ${value.javaClass} to LocalDate",
        )
    }

internal fun convertUUID(value: Any): UUID =
    when (value) {
        is String -> {
            try {
                UUID.fromString(value)
            } catch (e: Exception) {
                throw KapperParseException(
                    "Cannot parse $value to UUID",
                    e,
                )
            }
        }

        is CharArray -> {
            try {
                UUID.fromString(String(value))
            } catch (e: Exception) {
                throw KapperParseException(
                    "Cannot parse $value to UUID",
                    e,
                )
            }
        }

        is ByteArray -> {
            value.asUUID()
        }

        else -> {
            throw KapperUnsupportedOperationException(
                "Cannot auto-convert from ${value.javaClass} to UUID",
            )
        }
    }

internal fun convertChar(value: Any): Char =
    if (value is String) {
        if (value.length > 1) {
            throw KapperParseException(
                "Cannot parse $value to Char (length > 1)",
            )
        }
        value[0]
    } else if (value is CharArray) {
        if (value.size > 1) {
            throw KapperParseException(
                "Cannot parse $value to Char (size > 1)",
            )
        }
        value[0]
    } else {
        throw KapperUnsupportedOperationException(
            "Cannot auto-convert from ${value.javaClass} to Char",
        )
    }

fun ByteArray.asUUID(): UUID {
    val b = ByteBuffer.wrap(this)
    return UUID(b.getLong(), b.getLong())
}
