@file:JvmSynthetic

package net.samyn.kapper.internal.automapper

import net.samyn.kapper.DbFlavour
import net.samyn.kapper.Field
import net.samyn.kapper.KapperUnsupportedOperationException
import java.nio.ByteBuffer
import java.sql.JDBCType
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Date
import java.util.UUID

fun interface SQLTypesConverter {
    fun convert(
        field: Field,
        resultSet: ResultSet,
    ): Any?
}

private val BINARY_TYPES = listOf(JDBCType.BINARY, JDBCType.BLOB, JDBCType.LONGVARBINARY, JDBCType.VARBINARY)
private val BOOLEAN_TYPES = listOf(JDBCType.BIT, JDBCType.BOOLEAN)
private val CHAR_TYPES = listOf(JDBCType.CHAR)
private val STRING_TYPES =
    listOf(
        JDBCType.CLOB, JDBCType.LONGNVARCHAR, JDBCType.LONGVARCHAR,
        JDBCType.NCHAR, JDBCType.NCLOB, JDBCType.NVARCHAR, JDBCType.ROWID, JDBCType.SQLXML, JDBCType.VARCHAR,
    )
private val DATE_TYPES = listOf(JDBCType.DATE)
private val FLOAT_TYPES = listOf(JDBCType.DECIMAL, JDBCType.FLOAT, JDBCType.NUMERIC, JDBCType.REAL)
private val INTEGER_TYPES = listOf(JDBCType.INTEGER, JDBCType.SMALLINT, JDBCType.TINYINT)
private val TIME_TYPES = listOf(JDBCType.TIME, JDBCType.TIME_WITH_TIMEZONE)
private val TIMESTAMP_TYPES = listOf(JDBCType.TIMESTAMP, JDBCType.TIMESTAMP_WITH_TIMEZONE)

val sqlTypesConverter =
    SQLTypesConverter { field, resultSet ->
        when (field.type) {
            JDBCType.ARRAY -> resultSet.getArray(field.columnIndex)
            JDBCType.BIGINT -> resultSet.getNullableLong(field.columnIndex)
            in BINARY_TYPES -> resultSet.getBytes(field.columnIndex)

            in BOOLEAN_TYPES -> resultSet.getNullableBoolean(field.columnIndex)
            in CHAR_TYPES -> resultSet.getString(field.columnIndex)?.toCharArray()

            in STRING_TYPES -> resultSet.getString(field.columnIndex)

            in DATE_TYPES -> convertDate(resultSet, field.columnIndex, field.dbFlavour)

            in FLOAT_TYPES -> resultSet.getNullableFloat(field.columnIndex)

            JDBCType.DOUBLE ->
                resultSet.getNullableDouble(field.columnIndex)

            in INTEGER_TYPES -> resultSet.getNullableInt(field.columnIndex)

            JDBCType.JAVA_OBJECT,
            -> resultSet.getObject(field.columnIndex)

            in TIME_TYPES -> resultSet.getTime(field.columnIndex)?.toLocalTime()

            in TIMESTAMP_TYPES -> convertTimestamp(resultSet, field.columnIndex, field.typeName)

            // includes: DATALINK, DISTINCT, OTHER, REF, REF_CURSOR, STRUCT, NULL
            else -> {
                // use name if type is
                when (field.typeName.lowercase()) {
                    "uuid" -> resultSet.getString(field.columnIndex)?.let { UUID.fromString(it) }
                    // oracle types
                    "binary_float" -> resultSet.getNullableFloat(field.columnIndex)
                    "binary_double" -> resultSet.getNullableDouble(field.columnIndex)
                    else ->
                        throw KapperUnsupportedOperationException(
                            "Conversion of field[${field.columnIndex}] from type ${field.typeName} is not supported",
                        )
                }
            }
        }
    }

/**
 * Returns the value of the specified column as an [Boolean], or `null` if the value is SQL `NULL`.
 *
 * NOTE: this does not align with general JDBC logic, which means NULL values are returned as 0.
 * However, for the auto-mapper this seems more intuitive.
 * This change will be documented in the user docs.
 */
fun ResultSet.getNullableBoolean(columnIndex: Int): Boolean? {
    val dbValue = getBoolean(columnIndex)
    if (wasNull()) {
        return null
    }
    return dbValue
}

/**
 * Returns the value of the specified column as an [Int], or `null` if the value is SQL `NULL`.
 *
 * NOTE: this does not align with general JDBC logic, which means NULL values are returned as 0.
 * However, for the auto-mapper this seems more intuitive.
 * This change will be documented in the user docs.
 */
fun ResultSet.getNullableInt(columnIndex: Int): Int? {
    val dbValue = getInt(columnIndex)
    if (wasNull()) {
        return null
    }
    return dbValue
}

/**
 * Returns the value of the specified column as an [Long], or `null` if the value is SQL `NULL`.
 *
 * NOTE: this does not align with general JDBC logic, which means NULL values are returned as 0.
 * However, for the auto-mapper this seems more intuitive.
 * This change will be documented in the user docs.
 */
fun ResultSet.getNullableLong(columnIndex: Int): Long? {
    val dbValue = getLong(columnIndex)
    if (wasNull()) {
        return null
    }
    return dbValue
}

/**
 * Returns the value of the specified column as an [Float], or `null` if the value is SQL `NULL`.
 *
 * NOTE: this does not align with general JDBC logic, which means NULL values are returned as 0.
 * However, for the auto-mapper this seems more intuitive.
 * This change will be documented in the user docs.
 */
fun ResultSet.getNullableFloat(columnIndex: Int): Float? {
    val dbValue = getFloat(columnIndex)
    if (wasNull()) {
        return null
    }
    return dbValue
}

/**
 * Returns the value of the specified column as an [Double], or `null` if the value is SQL `NULL`.
 *
 * NOTE: this does not align with general JDBC logic, which means NULL values are returned as 0.
 * However, for the auto-mapper this seems more intuitive.
 * This change will be documented in the user docs.
 */
fun ResultSet.getNullableDouble(columnIndex: Int): Double? {
    val dbValue = getDouble(columnIndex)
    if (wasNull()) {
        return null
    }
    return dbValue
}

fun convertTimestamp(
    resultSet: ResultSet,
    fieldIndex: Int,
    sqlTypeName: String,
): Any? =
    when (sqlTypeName.uppercase()) {
        "DATE" -> {
            resultSet.getTimestamp(fieldIndex)?.toLocalDateTime()
        }
        else -> resultSet.getTimestamp(fieldIndex)?.toInstant()
    }

fun PreparedStatement.setParameter(
    index: Int,
    value: Any?,
    dbFlavour: DbFlavour,
) {
    when (value) {
        is Byte -> setByte(index, value)
        is Short -> setShort(index, value)
        is Int -> setInt(index, value)
        is Long -> setLong(index, value)
        is Float -> setFloat(index, value)
        is Double -> setDouble(index, value)
        is Char -> setString(index, value.toString())
        is String -> setString(index, value)
        is ByteArray -> setBytes(index, value)
        is Boolean -> setBoolean(index, value)
        is UUID ->
            when (dbFlavour) {
                DbFlavour.MYSQL -> setString(index, value.toString())
                DbFlavour.ORACLE -> setBytes(index, value.toBytes())
                else -> setObject(index, value)
            }
        is Instant -> setTimestamp(index, Timestamp.from(value))
        is Date -> setDate(index, java.sql.Date(value.time))
        is LocalDate -> setDate(index, java.sql.Date.valueOf(value))
        is LocalDateTime -> setTimestamp(index, Timestamp.from(value.atZone(java.time.ZoneOffset.systemDefault()).toInstant()))
        is LocalTime -> setTime(index, java.sql.Time.valueOf(value))
        else -> setObject(index, value)
    }
}

fun UUID.toBytes(): ByteArray {
    val buffer = ByteBuffer.wrap(ByteArray(16))
    buffer.putLong(this.mostSignificantBits)
    buffer.putLong(this.leastSignificantBits)
    return buffer.array()
}

val formatters =
    listOf(
        "yyyy-MM-dd",
        "yyyy-MM-dd HH:mm",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "HH:mm",
        "HH:mm:ss",
        "HH:mm:ss.SSS",
        "HH:mm'Z'",
        "HH:mm:ss'Z'",
        "HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd HH:mm'Z'",
        "yyyy-MM-dd HH:mm:ss'Z'",
        "yyyy-MM-dd HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
    ).associateWith {
        SimpleDateFormat(it)
    }

fun convertDate(
    resultSet: ResultSet,
    fieldIndex: Int,
    dbFlavour: DbFlavour,
): Date? =
    when (dbFlavour) {
        DbFlavour.SQLITE -> {
            when (val date = resultSet.getObject(fieldIndex)) {
                null -> null
                is Long -> Date(date)
                is String -> convertSQliteDate(date)
                else -> throw KapperUnsupportedOperationException("Conversion from type ${date.javaClass} to Date is not supported")
            }
        }
        else -> resultSet.getDate(fieldIndex)?.let { Date(it.time) }
    }

fun convertSQliteDate(date: String): Date =
    try {
        when {
            date.length > 2 && date[2] == ':' ->
                when (date.length) {
                    5 -> formatters["HH:mm"]!!.parse(date)
                    6 -> formatters["HH:mm'Z'"]!!.parse(date)
                    8 -> formatters["HH:mm:ss"]!!.parse(date)
                    9 -> formatters["HH:mm:ss'Z'"]!!.parse(date)
                    12 -> formatters["HH:mm:ss.SSS"]!!.parse(date)
                    13 -> formatters["HH:mm:ss.SSS'Z'"]!!.parse(date)
                    else -> null
                }

            date.length > 10 && date[10] == 'T' ->
                when (date.length) {
                    16 -> formatters["yyyy-MM-dd'T'HH:mm"]!!.parse(date)
                    17 -> formatters["yyyy-MM-dd'T'HH:mm'Z'"]!!.parse(date)
                    19 -> formatters["yyyy-MM-dd'T'HH:mm:ss"]!!.parse(date)
                    20 -> formatters["yyyy-MM-dd'T'HH:mm:ss'Z'"]!!.parse(date)
                    23 -> formatters["yyyy-MM-dd'T'HH:mm:ss.SSS"]!!.parse(date)
                    24 -> formatters["yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"]!!.parse(date)
                    else -> null
                }

            else ->
                when (date.length) {
                    10 -> formatters["yyyy-MM-dd"]!!.parse(date)
                    16 -> formatters["yyyy-MM-dd HH:mm"]!!.parse(date)
                    17 -> formatters["yyyy-MM-dd HH:mm'Z'"]!!.parse(date)
                    19 -> formatters["yyyy-MM-dd HH:mm:ss"]!!.parse(date)
                    20 -> formatters["yyyy-MM-dd HH:mm:ss'Z'"]!!.parse(date)
                    23 -> formatters["yyyy-MM-dd HH:mm:ss.SSS"]!!.parse(date)
                    24 -> formatters["yyyy-MM-dd HH:mm:ss.SSS'Z'"]!!.parse(date)
                    else -> null
                }
        }
    } catch (e: Exception) {
        null
    } ?: throw KapperUnsupportedOperationException("Cannot convert $date to Date.")
