package net.samyn.kapper.internal

import net.samyn.kapper.KapperUnsupportedOperationException
import net.samyn.kapper.internal.DbConnectionUtils.DbFlavour
import java.sql.JDBCType
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.Date
import java.util.UUID

// TODO: this could be more sophisticated by allowing type conversion hints.
// TODO: check what hibernate does for these conversions.
internal object SQLTypesConverter {
    fun convertSQLType(
        sqlType: JDBCType,
        sqlTypeName: String,
        resultSet: ResultSet,
        field: String,
    ): Any {
        val result =
            when (sqlType) {
                JDBCType.ARRAY -> resultSet.getArray(field)
                JDBCType.BIGINT -> resultSet.getLong(field)
                in listOf(JDBCType.BINARY, JDBCType.BLOB, JDBCType.LONGVARBINARY, JDBCType.VARBINARY),
                -> resultSet.getBytes(field)
                in listOf(JDBCType.BIT, JDBCType.BOOLEAN) -> resultSet.getBoolean(field)
                in
                listOf(JDBCType.CHAR),
                -> resultSet.getString(field).toCharArray()[0]
                in
                listOf(
                    JDBCType.CLOB, JDBCType.LONGNVARCHAR, JDBCType.LONGVARCHAR,
                    JDBCType.NCHAR, JDBCType.NCLOB, JDBCType.NVARCHAR, JDBCType.ROWID, JDBCType.SQLXML, JDBCType.VARCHAR,
                ),
                -> resultSet.getString(field)
                in listOf(JDBCType.DATE),
                -> Date(resultSet.getDate(field).time)
                in listOf(JDBCType.DECIMAL, JDBCType.FLOAT, JDBCType.NUMERIC, JDBCType.REAL),
                -> resultSet.getFloat(field)
                JDBCType.DOUBLE ->
                    resultSet.getDouble(field)
                in listOf(JDBCType.INTEGER, JDBCType.SMALLINT, JDBCType.TINYINT),
                -> resultSet.getInt(field)
                JDBCType.JAVA_OBJECT,
                -> resultSet.getObject(field)
                // TODO: validate these with regards to timezones etc. This may be DB specific.
                in
                listOf(
                    JDBCType.TIME,
                    JDBCType.TIME_WITH_TIMEZONE,
                ),
                -> resultSet.getTime(field).toLocalTime()
                in
                listOf(
                    JDBCType.TIMESTAMP,
                    JDBCType.TIMESTAMP_WITH_TIMEZONE,
                ),
                -> resultSet.getTimestamp(field).toInstant()

                // includes: DATALINK, DISTINCT, OTHER, REF, REF_CURSOR, STRUCT, NULL
                else -> {
                    // use name if type is
                    when (sqlTypeName.lowercase()) {
                        "uuid" -> UUID.fromString(resultSet.getString(field))
                        else ->
                            throw KapperUnsupportedOperationException("Conversion from type $sqlType is not supported")
                    }
                }
            }
        return result
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
            is UUID -> {
                if (DbFlavour.MYSQL == dbFlavour) {
                    setString(index, value.toString())
                } else {
                    setObject(index, value)
                }
            }
            is Instant -> setTimestamp(index, Timestamp.from(value))
            is Date -> setDate(index, java.sql.Date(value.time))
            else -> setObject(index, value)
        }
    }
}
