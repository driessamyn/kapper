package net.samyn.kapper.internal

import java.sql.JDBCType
import java.sql.ResultSet
import java.sql.Types
import java.util.UUID

// TODO: this could be more sophisticated by allowing type conversion hints.
// TODO: check what hibernate does for these conversions.
object SQLTypesConverter {
    fun convert(
        sqlType: Int,
        sqlTypeName: String,
        resultSet: ResultSet,
        field: String,
    ): Any {
        val result =
            when (sqlType) {
                Types.ARRAY -> resultSet.getArray(field)
                Types.BIGINT -> resultSet.getLong(field)
                in listOf(Types.BINARY, Types.BLOB, Types.LONGVARBINARY, Types.VARBINARY),
                -> resultSet.getBytes(field)
                in listOf(Types.BIT, Types.BOOLEAN) -> resultSet.getBoolean(field)
                in
                listOf(
                    Types.CHAR, Types.CLOB, Types.LONGNVARCHAR, Types.LONGVARCHAR,
                    Types.NCHAR, Types.NCLOB, Types.NVARCHAR, Types.ROWID, Types.SQLXML, Types.VARCHAR,
                ),
                -> resultSet.getString(field)
                in listOf(Types.DATE),
                -> resultSet.getDate(field)
                in listOf(Types.DECIMAL, Types.FLOAT, Types.NUMERIC, Types.REAL),
                -> resultSet.getFloat(field)
                Types.DOUBLE -> resultSet.getDouble(field)
                in listOf(Types.INTEGER, Types.SMALLINT, Types.TINYINT),
                -> resultSet.getInt(field)
                Types.JAVA_OBJECT,
                -> resultSet.getObject(field)
                // TODO: validate these with regards to timezones etc. This may be DB specific.
                in
                listOf(
                    Types.TIME,
                    Types.TIME_WITH_TIMEZONE,
                ),
                -> resultSet.getTime(field).toInstant()
                in
                listOf(
                    Types.TIMESTAMP,
                    Types.TIMESTAMP_WITH_TIMEZONE,
                ),
                -> resultSet.getTimestamp(field).toInstant()

                // includes: DATALINK, DISTINCT, OTHER, REF, REF_CURSOR, STRUCT, NULL
                else -> {
                    // use name if type is
                    when (sqlTypeName.lowercase()) {
                        "uuid" -> UUID.fromString(resultSet.getString(field))
                        else ->
                            throw UnsupportedOperationException("Conversion from type ${JDBCType.valueOf(sqlType)} is not supported")
                    }
                }
            }
        return result
    }
}
