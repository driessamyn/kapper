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
            value.atZone(java.time.ZoneOffset.systemDefault()).toInstant()
        }

        else -> {
            throw KapperUnsupportedOperationException(
                "Cannot auto-convert from ${value.javaClass} to Instant",
            )
        }
    }

internal fun convertLocalTime(value: Any): LocalTime =
    when (value) {
        is Instant -> {
            LocalTime.ofInstant(value, java.time.ZoneOffset.systemDefault())
        }

        is String -> {
            LocalTime.parse(value)
        }

        is Long -> {
            LocalTime.ofInstant(Instant.ofEpochMilli(value), java.time.ZoneOffset.systemDefault())
        }

        is Int -> {
            LocalTime.ofInstant(Instant.ofEpochMilli(value.toLong()), java.time.ZoneOffset.systemDefault())
        }

        else -> {
            throw KapperUnsupportedOperationException(
                "Cannot auto-convert value '$value' from ${value.javaClass} to LocalTime",
            )
        }
    }

internal fun convertLocalDateTime(value: Any): LocalDateTime =
    when (value) {
        is Instant -> {
            LocalDateTime.ofInstant(value, java.time.ZoneOffset.systemDefault())
        }

        is String -> {
            LocalDateTime.parse(value)
        }

        else -> {
            throw KapperUnsupportedOperationException(
                "Cannot auto-convert from ${value.javaClass} to LocalDateTime",
            )
        }
    }

internal fun convertLocalDate(value: Any): LocalDate =
    when (value) {
        is Date -> {
            val cal = Calendar.getInstance()
            cal.time = value
            LocalDate.of(cal[Calendar.YEAR], cal[Calendar.MONTH] + 1, cal[Calendar.DAY_OF_MONTH])
        }
        is LocalDateTime -> {
            value.toLocalDate()
        }
        is String -> {
            LocalDate.parse(value)
        }
        is Instant -> {
            LocalDate.ofInstant(value, java.time.ZoneOffset.systemDefault())
        }
        else -> {
            throw KapperUnsupportedOperationException(
                "Cannot auto-convert from ${value.javaClass} to LocalDate",
            )
        }
    }

internal fun convertDate(value: Any): Date =
    when (value) {
        is Instant -> {
            Date.from(value)
        }

        is LocalDateTime -> {
            Date.from(value.atZone(java.time.ZoneOffset.systemDefault()).toInstant())
        }

        else -> {
            throw KapperUnsupportedOperationException(
                "Cannot auto-convert from ${value.javaClass} to Date",
            )
        }
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
        if (value.length != 1) {
            throw KapperParseException(
                "Cannot parse $value to Char (length != 1)",
            )
        }
        value[0]
    } else if (value is CharArray) {
        if (value.size != 1) {
            throw KapperParseException(
                "Cannot parse $value to Char (size != 1)",
            )
        }
        value[0]
    } else {
        throw KapperUnsupportedOperationException(
            "Cannot auto-convert from ${value.javaClass} to Char",
        )
    }

internal fun convertInt(value: Any): Int =
    when (value) {
        is Float -> {
            value.toInt()
        }

        else -> {
            throw KapperUnsupportedOperationException(
                "Cannot auto-convert from ${value.javaClass} to Int",
            )
        }
    }

internal fun convertLong(value: Any): Long =
    when (value) {
        is Float -> {
            value.toLong()
        }

        else -> {
            throw KapperUnsupportedOperationException(
                "Cannot auto-convert from ${value.javaClass} to Long",
            )
        }
    }

internal fun convertBoolean(value: Any): Boolean =
    when (value) {
        is String -> {
            value == "1" || value.toBoolean()
        }

        is Int -> {
            value != 0
        }

        is Byte -> {
            value != 0.toByte()
        }

        is Short -> {
            value != 0.toShort()
        }

        is Long -> {
            value != 0L
        }

        is Float -> {
            value != 0.0f
        }

        else -> {
            throw KapperUnsupportedOperationException(
                "Cannot auto-convert from ${value.javaClass} to Boolean",
            )
        }
    }

internal fun convertString(value: Any): String = value.toString()

fun ByteArray.asUUID(): UUID {
    val b = ByteBuffer.wrap(this)
    return UUID(b.getLong(), b.getLong())
}
