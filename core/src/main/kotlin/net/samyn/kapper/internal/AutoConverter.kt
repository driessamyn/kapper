@file:JvmSynthetic

package net.samyn.kapper.internal

import net.samyn.kapper.Field
import net.samyn.kapper.KapperParseException
import net.samyn.kapper.KapperUnsupportedOperationException
import java.math.BigDecimal
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

/**
 * Identifies the column a value was read from, so conversion failures can point at it.
 *
 * @param label the column label as returned by the driver.
 * @param field the JDBC metadata for the column, when available.
 */
internal data class ColumnContext(val label: String, val field: Field?)

/**
 * Converts [value] to [target], enriching any failure with the [context] of the column it came from.
 *
 * This is an overload of [AutoConverter.convert] rather than a change to the interface itself, so that
 * existing callers (including Java callers, which cannot use default arguments) keep compiling.
 */
internal fun AutoConverter.convert(
    value: Any,
    target: Class<*>,
    context: ColumnContext?,
): Any =
    try {
        convert(value, target)
    } catch (e: KapperUnsupportedOperationException) {
        throw KapperUnsupportedOperationException(e.message.withColumnContext(context), e)
    } catch (e: KapperParseException) {
        throw KapperParseException(e.message.withColumnContext(context), e)
    }

private fun String?.withColumnContext(context: ColumnContext?): String {
    if (context == null) return this ?: "Conversion failed"
    val field = context.field
    val suffix =
        buildString {
            append(" (column '${context.label}'")
            if (field != null) {
                append(", index ${field.columnIndex}, driver type '${field.typeName}', flavour ${field.dbFlavour}")
            }
            append("). Register a custom converter or alias the column.")
        }
    return (this ?: "Conversion failed") + suffix
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
        Character::class.java to ::convertChar,
        Int::class.java to ::convertInt,
        Integer::class.java to ::convertInt,
        Long::class.java to ::convertLong,
        java.lang.Long::class.java to ::convertLong,
        Float::class.java to ::convertFloat,
        java.lang.Float::class.java to ::convertFloat,
        Double::class.java to ::convertDouble,
        java.lang.Double::class.java to ::convertDouble,
        BigDecimal::class.java to ::convertBigDecimal,
        Date::class.java to ::convertDate,
        Boolean::class.java to ::convertBoolean,
        java.lang.Boolean::class.java to ::convertBoolean,
        String::class.java to ::convertString,
    )

@JvmOverloads
fun convertToPrimitive(
    wrapper: Any,
    target: Class<*> = wrapper.javaClass,
): Any {
    fun unsupported(): Nothing =
        throw KapperUnsupportedOperationException(
            "Cannot auto-convert from ${wrapper.javaClass} to ${target.canonicalName}",
        )
    val number = wrapper as? Number
    return when (target) {
        Int::class.java, Integer::class.java -> number?.toInt() ?: unsupported()
        Long::class.java, java.lang.Long::class.java -> number?.toLong() ?: unsupported()
        Double::class.java, java.lang.Double::class.java -> number?.toDouble() ?: unsupported()
        Float::class.java, java.lang.Float::class.java -> number?.toFloat() ?: unsupported()
        Byte::class.java, java.lang.Byte::class.java -> number?.toByteChecked() ?: unsupported()
        Short::class.java, java.lang.Short::class.java -> number?.toShortChecked() ?: unsupported()
        Boolean::class.java, java.lang.Boolean::class.java -> wrapper as? Boolean ?: unsupported()
        Character::class.java, Char::class.java -> wrapper as? Char ?: unsupported()
        else -> unsupported()
    }
}

/**
 * Narrows [this] to a [Byte], rejecting values that would silently wrap around, mirroring the
 * range check used by [convertInt] for Long sources.
 */
private fun Number.toByteChecked(): Byte {
    val converted = toByte()
    if (converted.toLong() != toLong()) {
        throw KapperParseException("Cannot parse $this to Byte (out of range)")
    }
    return converted
}

/**
 * Narrows [this] to a [Short], rejecting values that would silently wrap around, mirroring the
 * range check used by [convertInt] for Long sources.
 */
private fun Number.toShortChecked(): Short {
    val converted = toShort()
    if (converted.toLong() != toLong()) {
        throw KapperParseException("Cannot parse $this to Short (out of range)")
    }
    return converted
}

val autoConverter =
    AutoConverter { value, target ->
        converters[target]?.let { converter ->
            try {
                return@AutoConverter converter.invoke(value)
            } catch (_: KapperUnsupportedOperationException) {
                // The registered converter does not handle this source type; fall back to the
                // target-aware conversion below (e.g. a Long element into a Double target).
            }
        }
        convertToPrimitive(value, target)
    }
