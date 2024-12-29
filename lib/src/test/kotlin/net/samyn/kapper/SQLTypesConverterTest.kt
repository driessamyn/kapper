package net.samyn.kapper

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.samyn.kapper.internal.DbConnectionUtils.DbFlavour
import net.samyn.kapper.internal.SQLTypesConverter
import net.samyn.kapper.internal.SQLTypesConverter.setParameter
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.sql.JDBCType
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Time
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID

class SQLTypesConverterTest {
    companion object {
        val statement = mockk<PreparedStatement>(relaxed = true)
        val resultSet = mockk<ResultSet>(relaxed = true)

        @JvmStatic
        fun parameterTests() =
            listOf(
                arguments(named("BYTE", "test".toByteArray()[0]), statement::setByte),
                arguments(named("SHORT", (123).toShort()), statement::setShort),
                arguments(named("INT", 123), statement::setInt),
                arguments(named("LONG", 123L), statement::setLong),
                arguments(named("FLOAT", 123.45F), statement::setFloat),
                arguments(named("DOUBLE", 123.45), statement::setDouble),
                arguments(
                    named("CHAR", 'c'),
                    { i: Int, v: Char? -> statement.setString(i, v.toString()) },
                ),
                arguments(named("STRING", "test"), statement::setString),
                arguments(named("BYTE-ARRAY", "test".toByteArray()), statement::setBytes),
                arguments(named("BOOLEAN", true), statement::setBoolean),
                arguments(
                    named("INSTANT", Instant.now()),
                    { i: Int, v: Instant? -> statement.setTimestamp(i, Timestamp.from(v)) },
                ),
                arguments(
                    named("DATE", Date.from(Instant.now())),
                    { i: Int, v: Date -> statement.setDate(i, java.sql.Date(v.time)) },
                ),
            )

        private val uuid = UUID.randomUUID()

        @JvmStatic
        fun parameterWithConvertTests() =
            listOf(
                arguments(
                    named("UUID", uuid),
                    { i: Int, v: Any? -> statement.setObject(i, v) },
                    uuid,
                    DbFlavour.POSTGRESQL,
                ),
                arguments(named("UUID", uuid), statement::setString, uuid.toString(), DbFlavour.MYSQL),
            )

        @JvmStatic
        fun convertSQLTypeTests() =
            listOf(
                arguments(named("ARRAY", JDBCType.ARRAY), "ARRAY", { f: String -> resultSet.getArray(f) }),
                arguments(named("BIGINT", JDBCType.BIGINT), "BIGINT", { f: String -> resultSet.getLong(f) }),
                arguments(named("BINARY", JDBCType.BINARY), "BINARY", { f: String -> resultSet.getBytes(f) }),
                arguments(named("BLOB", JDBCType.BLOB), "BLOB", { f: String -> resultSet.getBytes(f) }),
                arguments(
                    named("LONGVARBINARY", JDBCType.LONGVARBINARY),
                    "LONGVARBINARY",
                    { f: String -> resultSet.getBytes(f) },
                ),
                arguments(named("VARBINARY", JDBCType.VARBINARY), "VARBINARY", { f: String -> resultSet.getBytes(f) }),
                arguments(named("BIT", JDBCType.BIT), "BIT", { f: String -> resultSet.getBoolean(f) }),
                arguments(named("BOOLEAN", JDBCType.BOOLEAN), "BOOLEAN", { f: String -> resultSet.getBoolean(f) }),
                arguments(named("CLOB", JDBCType.CLOB), "CLOB", { f: String -> resultSet.getString(f) }),
                arguments(
                    named("LONGNVARCHAR", JDBCType.LONGNVARCHAR),
                    "LONGNVARCHAR",
                    { f: String -> resultSet.getString(f) },
                ),
                arguments(
                    named("LONGVARCHAR", JDBCType.LONGVARCHAR),
                    "LONGVARCHAR",
                    { f: String -> resultSet.getString(f) },
                ),
                arguments(named("NCHAR", JDBCType.NCHAR), "NCHAR", { f: String -> resultSet.getString(f) }),
                arguments(named("NCLOB", JDBCType.NCLOB), "NCLOB", { f: String -> resultSet.getString(f) }),
                arguments(named("NVARCHAR", JDBCType.NVARCHAR), "NVARCHAR", { f: String -> resultSet.getString(f) }),
                arguments(named("ROWID", JDBCType.ROWID), "ROWID", { f: String -> resultSet.getString(f) }),
                arguments(named("SQLXML", JDBCType.SQLXML), "SQLXML", { f: String -> resultSet.getString(f) }),
                arguments(named("VARCHAR", JDBCType.VARCHAR), "VARCHAR", { f: String -> resultSet.getString(f) }),
                arguments(named("DATE", JDBCType.DATE), "DATE", { f: String -> resultSet.getDate(f) }),
                arguments(named("DECIMAL", JDBCType.DECIMAL), "DECIMAL", { f: String -> resultSet.getFloat(f) }),
                arguments(named("FLOAT", JDBCType.FLOAT), "FLOAT", { f: String -> resultSet.getFloat(f) }),
                arguments(named("NUMERIC", JDBCType.NUMERIC), "NUMERIC", { f: String -> resultSet.getFloat(f) }),
                arguments(named("REAL", JDBCType.REAL), "REAL", { f: String -> resultSet.getFloat(f) }),
                arguments(named("DOUBLE", JDBCType.DOUBLE), "DOUBLE", { f: String -> resultSet.getDouble(f) }),
                arguments(named("INTEGER", JDBCType.INTEGER), "INTEGER", { f: String -> resultSet.getInt(f) }),
                arguments(named("SMALLINT", JDBCType.SMALLINT), "SMALLINT", { f: String -> resultSet.getInt(f) }),
                arguments(named("TINYINT", JDBCType.TINYINT), "TINYINT", { f: String -> resultSet.getInt(f) }),
                arguments(
                    named("JAVA_OBJECT", JDBCType.JAVA_OBJECT),
                    "JAVA_OBJECT",
                    { f: String -> resultSet.getObject(f) },
                ),
                arguments(named("TIME", JDBCType.TIME), "TIME", { f: String -> resultSet.getTime(f) }),
                arguments(
                    named("TIME_WITH_TIMEZONE", JDBCType.TIME),
                    "TIME_WITH_TIMEZONE",
                    { f: String -> resultSet.getTime(f) },
                ),
                arguments(named("TIMESTAMP", JDBCType.TIMESTAMP), "TIMESTAMP", { f: String -> resultSet.getTimestamp(f) }),
                arguments(
                    named("TIMESTAMP_WITH_TIMEZONE", JDBCType.TIMESTAMP_WITH_TIMEZONE),
                    "TIMESTAMP_WITH_TIMEZONE",
                    { f: String -> resultSet.getTimestamp(f) },
                ),
            )

        @JvmStatic
        fun convertSQLTypeUnsupportedTests() =
            listOf(
                arguments(named("OTHER", JDBCType.OTHER)),
                arguments(named("DATALINK", JDBCType.DATALINK)),
                arguments(named("DISTINCT", JDBCType.DISTINCT)),
                arguments(named("REF", JDBCType.REF)),
                arguments(named("REF_CURSOR", JDBCType.REF_CURSOR)),
                arguments(named("STRUCT", JDBCType.STRUCT)),
                arguments(named("NULL", JDBCType.NULL)),
            )
    }

    @ParameterizedTest
    @MethodSource("parameterTests")
    fun `should map values correctly`(
        value: Any?,
        expectedSetter: (Int, Any?) -> Unit,
    ) {
        statement.setParameter(1, value, DbFlavour.UNKNOWN)
        verify { expectedSetter(1, value) }
    }

    @ParameterizedTest
    @MethodSource("parameterWithConvertTests")
    internal fun `should map and convert values correctly`(
        value: Any?,
        expectedSetter: (Int, Any?) -> Unit,
        expectedValue: Any?,
        dbFlavour: DbFlavour,
    ) {
        statement.setParameter(1, value, dbFlavour)
        verify { expectedSetter(1, expectedValue) }
    }

    @ParameterizedTest
    @MethodSource("parameterTests")
    fun `should handle null values correctly`(
        value: Any?,
        expectedSetter: (Int, Any?) -> Unit,
    ) {
        statement.setParameter(1, null, DbFlavour.UNKNOWN)
        verify { statement.setObject(1, null) }
    }

    @ParameterizedTest
    @MethodSource("convertSQLTypeTests")
    fun `should convert SQL types correctly`(
        jdbcType: JDBCType,
        sqlTypeName: String,
        expectedGetter: (String) -> Any,
    ) {
        SQLTypesConverter.convertSQLType(jdbcType, sqlTypeName, resultSet, "foo")
        verify { expectedGetter("foo") }
    }

    @ParameterizedTest
    @MethodSource("convertSQLTypeUnsupportedTests")
    fun `unsupported SQL types throws`(jdbcType: JDBCType) {
        assertThrows<KapperUnsupportedOperationException> {
            SQLTypesConverter.convertSQLType(jdbcType, jdbcType.toString(), resultSet, "foo")
        }
    }

    @Test
    fun `char needs truncating`() {
        every { resultSet.getString("char") } returns "example"
        val result = SQLTypesConverter.convertSQLType(JDBCType.CHAR, "CHAR", resultSet, "char")

        result.shouldBe('e')
    }

    @Test
    fun `uuid needs parsing`() {
        val id = UUID.randomUUID()
        every { resultSet.getString("uuid") } returns id.toString()
        val result = SQLTypesConverter.convertSQLType(JDBCType.OTHER, "UUID", resultSet, "uuid")

        result.shouldBe(id)
    }

    @Test
    fun `time needs converting`() {
        val time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS)
        every { resultSet.getTime("time") } returns Time.valueOf(time)
        val result = SQLTypesConverter.convertSQLType(JDBCType.TIME, "time", resultSet, "time")

        result.shouldBe(time)
    }

    @Test
    fun `time with zone needs converting`() {
        val time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS)
        every { resultSet.getTime("time-zone") } returns Time.valueOf(time)
        val result = SQLTypesConverter.convertSQLType(JDBCType.TIME_WITH_TIMEZONE, "time", resultSet, "time-zone")

        result.shouldBe(time)
    }

    @Test
    fun `timestamp needs converting`() {
        val timestamp = Instant.now()
        every { resultSet.getTimestamp("timestamp") } returns Timestamp.from(timestamp)
        val result = SQLTypesConverter.convertSQLType(JDBCType.TIMESTAMP, "timestamp", resultSet, "timestamp")

        result.shouldBe(timestamp)
    }

    @Test
    fun `timestamp with zone needs converting`() {
        val timestamp = Instant.now()
        every { resultSet.getTimestamp("timestamp-zone") } returns Timestamp.from(timestamp)
        val result = SQLTypesConverter.convertSQLType(JDBCType.TIMESTAMP_WITH_TIMEZONE, "timestamp", resultSet, "timestamp-zone")

        result.shouldBe(timestamp)
    }
}
