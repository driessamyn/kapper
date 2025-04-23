@file:JvmSynthetic

package net.samyn.kapper.internal

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

// TODO: this could be more sophisticated by allowing type conversion hints.
internal object SQLTypesConverter {
    fun convertSQLType(
        sqlType: JDBCType,
        sqlTypeName: String,
        resultSet: ResultSet,
        fieldIndex: Int,
        dbFlavour: DbFlavour,
    ): Any? {
        val result =
            when (sqlType) {
                JDBCType.ARRAY -> resultSet.getArray(fieldIndex)
                JDBCType.BIGINT -> resultSet.getLong(fieldIndex)
                in listOf(JDBCType.BINARY, JDBCType.BLOB, JDBCType.LONGVARBINARY, JDBCType.VARBINARY),
                -> resultSet.getBytes(fieldIndex)
                in listOf(JDBCType.BIT, JDBCType.BOOLEAN) -> resultSet.getBoolean(fieldIndex)
                in
                listOf(JDBCType.CHAR),
                -> resultSet.getString(fieldIndex)?.toCharArray()
                in
                listOf(
                    JDBCType.CLOB, JDBCType.LONGNVARCHAR, JDBCType.LONGVARCHAR,
                    JDBCType.NCHAR, JDBCType.NCLOB, JDBCType.NVARCHAR, JDBCType.ROWID, JDBCType.SQLXML, JDBCType.VARCHAR,
                ),
                -> resultSet.getString(fieldIndex)
                in listOf(JDBCType.DATE),
                -> convertDate(resultSet, fieldIndex, dbFlavour)
                in listOf(JDBCType.DECIMAL, JDBCType.FLOAT, JDBCType.NUMERIC, JDBCType.REAL),
                -> resultSet.getFloat(fieldIndex)
                JDBCType.DOUBLE ->
                    resultSet.getDouble(fieldIndex)
                in listOf(JDBCType.INTEGER, JDBCType.SMALLINT, JDBCType.TINYINT),
                -> resultSet.getInt(fieldIndex)
                JDBCType.JAVA_OBJECT,
                -> resultSet.getObject(fieldIndex)
                in
                listOf(
                    JDBCType.TIME,
                    JDBCType.TIME_WITH_TIMEZONE,
                ),
                -> resultSet.getTime(fieldIndex)?.toLocalTime()
                in
                listOf(
                    JDBCType.TIMESTAMP,
                    JDBCType.TIMESTAMP_WITH_TIMEZONE,
                ),
                -> convertTimestamp(resultSet, fieldIndex, sqlTypeName)

                // includes: DATALINK, DISTINCT, OTHER, REF, REF_CURSOR, STRUCT, NULL
                else -> {
                    // use name if type is
                    when (sqlTypeName.lowercase()) {
                        "uuid" -> resultSet.getString(fieldIndex)?.let { UUID.fromString(it) }
                        // oracle types
                        "binary_float" -> resultSet.getFloat(fieldIndex)
                        "binary_double" -> resultSet.getDouble(fieldIndex)
                        else ->
                            throw KapperUnsupportedOperationException("Conversion from type $sqlType is not supported")
                    }
                }
            }
        return result
    }

    private fun convertTimestamp(
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
}

fun UUID.toBytes(): ByteArray {
    val buffer = ByteBuffer.wrap(ByteArray(16))
    buffer.putLong(this.mostSignificantBits)
    buffer.putLong(this.leastSignificantBits)
    return buffer.array()
}

val formatters =
    mapOf(
        "yyyy-MM-dd" to SimpleDateFormat("yyyy-MM-dd"),
        "yyyy-MM-dd HH:mm" to SimpleDateFormat("yyyy-MM-dd HH:mm"),
        "yyyy-MM-dd HH:mm:ss" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
        "yyyy-MM-dd HH:mm:ss.SSS" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"),
        "yyyy-MM-dd'T'HH:mm" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm"),
        "yyyy-MM-dd'T'HH:mm:ss" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"),
        "HH:mm" to SimpleDateFormat("HH:mm"),
        "HH:mm:ss" to SimpleDateFormat("HH:mm:ss"),
        "HH:mm:ss.SSS" to SimpleDateFormat("HH:mm:ss.SSS"),
        "HH:mm'Z'" to SimpleDateFormat("HH:mm'Z'"),
        "HH:mm:ss'Z'" to SimpleDateFormat("HH:mm:ss'Z'"),
        "HH:mm:ss.SSS'Z'" to SimpleDateFormat("HH:mm:ss.SSS'Z'"),
        "yyyy-MM-dd HH:mm'Z'" to SimpleDateFormat("yyyy-MM-dd HH:mm'Z'"),
        "yyyy-MM-dd HH:mm:ss'Z'" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'"),
        "yyyy-MM-dd HH:mm:ss.SSS'Z'" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'Z'"),
        "yyyy-MM-dd'T'HH:mm'Z'" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"),
        "yyyy-MM-dd'T'HH:mm:ss'Z'" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"),
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"),
    )

fun convertDate(
    resultSet: ResultSet,
    fieldIndex: Int,
    dbFlavour: DbFlavour,
): Date? {
    when (dbFlavour) {
        DbFlavour.SQLITE -> {
            val date = resultSet.getObject(fieldIndex) ?: return null
            if (date is Long) {
                return Date(date)
            }
            if (date is String) {
                // https://sqlite.org/lang_datefunc.html#tmval
                if (date[2] == ':') {
                    // time formats
                    when (date.length) {
                        5 -> return formatters["HH:mm"]!!.parse(date)
                        6 -> return formatters["HH:mm'Z'"]!!.parse(date)
                        8 -> return formatters["HH:mm:ss"]!!.parse(date)
                        9 -> return formatters["HH:mm:ss'Z'"]!!.parse(date)
                        12 -> return formatters["HH:mm:ss.SSS"]!!.parse(date)
                        13 -> return formatters["HH:mm:ss.SSS'Z'"]!!.parse(date)
                    }
                } else if (date.length > 10 && date[10] == 'T') {
                    when (date.length) {
                        16 -> return formatters["yyyy-MM-dd'T'HH:mm"]!!.parse(date)
                        17 -> return formatters["yyyy-MM-dd'T'HH:mm'Z'"]!!.parse(date)
                        19 -> return formatters["yyyy-MM-dd'T'HH:mm:ss"]!!.parse(date)
                        20 -> return formatters["yyyy-MM-dd'T'HH:mm:ss'Z'"]!!.parse(date)
                        23 -> return formatters["yyyy-MM-dd'T'HH:mm:ss.SSS"]!!.parse(date)
                        24 -> return formatters["yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"]!!.parse(date)
                    }
                }
                when (date.length) {
                    10 -> return formatters["yyyy-MM-dd"]!!.parse(date)
                    16 -> return formatters["yyyy-MM-dd HH:mm"]!!.parse(date)
                    17 -> return formatters["yyyy-MM-dd HH:mm'Z'"]!!.parse(date)
                    19 -> return formatters["yyyy-MM-dd HH:mm:ss"]!!.parse(date)
                    20 -> return formatters["yyyy-MM-dd HH:mm:ss'Z'"]!!.parse(date)
                    23 -> return formatters["yyyy-MM-dd HH:mm:ss.SSS"]!!.parse(date)
                    24 -> return formatters["yyyy-MM-dd HH:mm:ss.SSS'Z'"]!!.parse(date)
                }
            }
            throw KapperUnsupportedOperationException("Conversion from type ${date.javaClass} to Date is not supported")
        }
        else -> return resultSet.getDate(fieldIndex)?.let { Date(it.time) }
    }
}
