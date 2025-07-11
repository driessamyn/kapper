package net.samyn.kapper.internal.automapper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.samyn.kapper.DbFlavour
import net.samyn.kapper.Field
import net.samyn.kapper.KapperUnsupportedOperationException
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.sql.JDBCType
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Time
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID

class SQLTypesConverterTest {
    companion object {
        val statement = mockk<PreparedStatement>(relaxed = true)
        val resultSet = mockk<ResultSet>(relaxed = true)

        @JvmStatic
        fun parameterSetTests() =
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
                arguments(
                    named("LOCALDATE", LocalDate.now()),
                    { i: Int, v: LocalDate -> statement.setDate(i, java.sql.Date.valueOf(v)) },
                ),
                arguments(
                    named("LOCALDATETIME", LocalDateTime.now()),
                    {
                            i: Int,
                            v: LocalDateTime,
                        ->
                        statement.setTimestamp(i, Timestamp.from(v.atZone(ZoneOffset.systemDefault()).toInstant()))
                    },
                ),
                arguments(
                    named("LOCALTIME", LocalTime.now()),
                    { i: Int, v: LocalTime -> statement.setTime(i, Time.valueOf(v)) },
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
                arguments(named("UUID", uuid), statement::setBytes, uuid.toBytes(), DbFlavour.ORACLE),
            )

        @JvmStatic
        fun convertSQLTypeTests() =
            listOf(
                arguments(named("ARRAY", JDBCType.ARRAY), "ARRAY", { f: Int -> resultSet.getArray(f) }),
                arguments(named("BIGINT", JDBCType.BIGINT), "BIGINT", { f: Int -> resultSet.getLong(f) }),
                arguments(named("BINARY", JDBCType.BINARY), "BINARY", { f: Int -> resultSet.getBytes(f) }),
                arguments(named("BLOB", JDBCType.BLOB), "BLOB", { f: Int -> resultSet.getBytes(f) }),
                arguments(
                    named("LONGVARBINARY", JDBCType.LONGVARBINARY),
                    "LONGVARBINARY",
                    { f: Int -> resultSet.getBytes(f) },
                ),
                arguments(named("VARBINARY", JDBCType.VARBINARY), "VARBINARY", { f: Int -> resultSet.getBytes(f) }),
                arguments(named("BIT", JDBCType.BIT), "BIT", { f: Int -> resultSet.getBoolean(f) }),
                arguments(named("BOOLEAN", JDBCType.BOOLEAN), "BOOLEAN", { f: Int -> resultSet.getBoolean(f) }),
                arguments(named("CLOB", JDBCType.CLOB), "CLOB", { f: Int -> resultSet.getString(f) }),
                arguments(
                    named("LONGNVARCHAR", JDBCType.LONGNVARCHAR),
                    "LONGNVARCHAR",
                    { f: Int -> resultSet.getString(f) },
                ),
                arguments(
                    named("LONGVARCHAR", JDBCType.LONGVARCHAR),
                    "LONGVARCHAR",
                    { f: Int -> resultSet.getString(f) },
                ),
                arguments(named("NCHAR", JDBCType.NCHAR), "NCHAR", { f: Int -> resultSet.getString(f) }),
                arguments(named("NCLOB", JDBCType.NCLOB), "NCLOB", { f: Int -> resultSet.getString(f) }),
                arguments(named("NVARCHAR", JDBCType.NVARCHAR), "NVARCHAR", { f: Int -> resultSet.getString(f) }),
                arguments(named("ROWID", JDBCType.ROWID), "ROWID", { f: Int -> resultSet.getString(f) }),
                arguments(named("SQLXML", JDBCType.SQLXML), "SQLXML", { f: Int -> resultSet.getString(f) }),
                arguments(named("VARCHAR", JDBCType.VARCHAR), "VARCHAR", { f: Int -> resultSet.getString(f) }),
                arguments(named("DATE", JDBCType.DATE), "DATE", { f: Int -> resultSet.getDate(f) }),
                arguments(named("DECIMAL", JDBCType.DECIMAL), "DECIMAL", { f: Int -> resultSet.getFloat(f) }),
                arguments(named("FLOAT", JDBCType.FLOAT), "FLOAT", { f: Int -> resultSet.getFloat(f) }),
                arguments(named("NUMERIC", JDBCType.NUMERIC), "NUMERIC", { f: Int -> resultSet.getFloat(f) }),
                arguments(named("REAL", JDBCType.REAL), "REAL", { f: Int -> resultSet.getFloat(f) }),
                arguments(named("DOUBLE", JDBCType.DOUBLE), "DOUBLE", { f: Int -> resultSet.getDouble(f) }),
                arguments(named("INTEGER", JDBCType.INTEGER), "INTEGER", { f: Int -> resultSet.getInt(f) }),
                arguments(named("SMALLINT", JDBCType.SMALLINT), "SMALLINT", { f: Int -> resultSet.getInt(f) }),
                arguments(named("TINYINT", JDBCType.TINYINT), "TINYINT", { f: Int -> resultSet.getInt(f) }),
                arguments(
                    named("JAVA_OBJECT", JDBCType.JAVA_OBJECT),
                    "JAVA_OBJECT",
                    { f: Int -> resultSet.getObject(f) },
                ),
                arguments(named("TIME", JDBCType.TIME), "TIME", { f: Int -> resultSet.getTime(f) }),
                arguments(
                    named("TIME_WITH_TIMEZONE", JDBCType.TIME),
                    "TIME_WITH_TIMEZONE",
                    { f: Int -> resultSet.getTime(f) },
                ),
                arguments(named("TIMESTAMP", JDBCType.TIMESTAMP), "TIMESTAMP", { f: Int -> resultSet.getTimestamp(f) }),
                arguments(
                    named("TIMESTAMP_WITH_TIMEZONE", JDBCType.TIMESTAMP_WITH_TIMEZONE),
                    "TIMESTAMP_WITH_TIMEZONE",
                    { f: Int -> resultSet.getTimestamp(f) },
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
    @MethodSource("parameterSetTests")
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
    @MethodSource("parameterSetTests")
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
        expectedGetter: (Int) -> Any,
    ) {
        val field = Field(1, jdbcType, sqlTypeName, DbFlavour.UNKNOWN)
        sqlTypesConverter.convert(field, resultSet)
        verify { expectedGetter(1) }
    }

    @ParameterizedTest
    @MethodSource("convertSQLTypeUnsupportedTests")
    fun `unsupported SQL types throws`(jdbcType: JDBCType) {
        val field = Field(1, jdbcType, jdbcType.toString(), DbFlavour.UNKNOWN)
        assertThrows<KapperUnsupportedOperationException> {
            sqlTypesConverter.convert(field, resultSet)
        }
    }

    @Test
    fun `char needs converting`() {
        val field = Field(1, JDBCType.CHAR, "CHAR", DbFlavour.UNKNOWN)
        every { resultSet.getString(1) } returns "example"
        val result = sqlTypesConverter.convert(field, resultSet)

        result.shouldBe("example".toCharArray())
    }

    @Test
    fun `char can return null`() {
        val field = Field(1, JDBCType.CHAR, "CHAR", DbFlavour.UNKNOWN)
        every { resultSet.getString(1) } returns null
        val result = sqlTypesConverter.convert(field, resultSet)
        result.shouldBe(null)
    }

    @Test
    fun `uuid needs parsing`() {
        val field = Field(2, JDBCType.OTHER, "UUID", DbFlavour.UNKNOWN)
        val id = UUID.randomUUID()
        every { resultSet.getString(2) } returns id.toString()
        val result = sqlTypesConverter.convert(field, resultSet)
        result.shouldBe(id)
    }

    @Test
    fun `uuid supports null`() {
        val field = Field(2, JDBCType.OTHER, "UUID", DbFlavour.UNKNOWN)
        every { resultSet.getString(2) } returns null
        val result = sqlTypesConverter.convert(field, resultSet)
        result.shouldBe(null)
    }

    @Test
    fun `binary_float needs parsing`() {
        val field = Field(2, JDBCType.OTHER, "binary_float", DbFlavour.UNKNOWN)
        val f = 123.45F
        every { resultSet.getFloat(2) } returns f
        val result = sqlTypesConverter.convert(field, resultSet)
        result.shouldBe(f)
    }

    @Test
    fun `binary_double needs parsing`() {
        val field = Field(2, JDBCType.OTHER, "binary_double", DbFlavour.UNKNOWN)
        val d = 123.45
        every { resultSet.getDouble(2) } returns d
        val result = sqlTypesConverter.convert(field, resultSet)
        result.shouldBe(d)
    }

    @Test
    fun `time needs converting`() {
        val field = Field(3, JDBCType.TIME, "time", DbFlavour.UNKNOWN)
        val time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS)
        every { resultSet.getTime(3) } returns Time.valueOf(time)
        val result = sqlTypesConverter.convert(field, resultSet)
        result.shouldBe(time)
    }

    @Test
    fun `time supports null`() {
        val field = Field(3, JDBCType.TIME, "time", DbFlavour.UNKNOWN)
        every { resultSet.getTime(3) } returns null
        val result = sqlTypesConverter.convert(field, resultSet)
        result.shouldBe(null)
    }

    @Test
    fun `time with zone needs converting`() {
        val field = Field(4, JDBCType.TIME_WITH_TIMEZONE, "time", DbFlavour.UNKNOWN)
        val time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS)
        every { resultSet.getTime(4) } returns Time.valueOf(time)
        val result = sqlTypesConverter.convert(field, resultSet)
        result.shouldBe(time)
    }

    @Test
    fun `time with zone supports null`() {
        val field = Field(4, JDBCType.TIME_WITH_TIMEZONE, "time", DbFlavour.UNKNOWN)
        every { resultSet.getTime(4) } returns null
        val result = sqlTypesConverter.convert(field, resultSet)
        result.shouldBe(null)
    }

    @Test
    fun `timestamp as DATE needs converting`() {
        val field = Field(6, JDBCType.TIMESTAMP, "DATE", DbFlavour.UNKNOWN)
        val timestamp = Instant.now()
        every { resultSet.getTimestamp(6) } returns Timestamp.from(timestamp)
        val result = sqlTypesConverter.convert(field, resultSet)
        result.shouldBe(LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault()))
    }

    @Test
    fun `timestamp as DATE supports null`() {
        val field = Field(6, JDBCType.TIMESTAMP, "DATE", DbFlavour.UNKNOWN)
        every { resultSet.getTimestamp(6) } returns null
        val result = sqlTypesConverter.convert(field, resultSet)
        result.shouldBe(null)
    }

    @Test
    fun `timestamp as other supports null`() {
        val field = Field(6, JDBCType.TIMESTAMP, "DATETIME", DbFlavour.UNKNOWN)
        every { resultSet.getTimestamp(6) } returns null
        val result = sqlTypesConverter.convert(field, resultSet)
        result.shouldBe(null)
    }

    @Test
    fun `timestamp with timezone as DATE needs converting`() {
        val field = Field(6, JDBCType.TIMESTAMP_WITH_TIMEZONE, "DATE", DbFlavour.UNKNOWN)
        val timestamp = Instant.now()
        every { resultSet.getTimestamp(6) } returns Timestamp.from(timestamp)
        val result = sqlTypesConverter.convert(field, resultSet)
        result.shouldBe(LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault()))
    }

    @Test
    fun `timestamp with timezone as DATE supports null`() {
        val field = Field(6, JDBCType.TIMESTAMP_WITH_TIMEZONE, "DATE", DbFlavour.UNKNOWN)
        every { resultSet.getTimestamp(6) } returns null
        val result = sqlTypesConverter.convert(field, resultSet)
        result.shouldBe(null)
    }

    @Test
    fun `sqlite long date needs converting`() {
        val field = Field(7, JDBCType.DATE, "DATE", DbFlavour.SQLITE)
        val date = Instant.now().toEpochMilli()
        every { resultSet.getObject(7) } returns date
        val result = sqlTypesConverter.convert(field, resultSet)
        result.shouldBe(Date(date))
    }

    @Test
    fun `sqlite long date supports null`() {
        val field = Field(7, JDBCType.DATE, "DATE", DbFlavour.SQLITE)
        every { resultSet.getObject(7) } returns null
        val result = sqlTypesConverter.convert(field, resultSet)
        result.shouldBe(null)
    }

    @ParameterizedTest
    // https://sqlite.org/lang_datefunc.html#tmval
    @ValueSource(
        strings = [
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
            "yyyy-MM-dd HH:mm'Z'",
            "yyyy-MM-dd HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "HH:mm'Z'",
            "HH:mm:ss'Z'",
            "HH:mm:ss.SSS'Z'",
        ],
    )
    fun `sqlite string date needs converting`(format: String) {
        val field = Field(7, JDBCType.DATE, "DATE", DbFlavour.SQLITE)
        val df = SimpleDateFormat(format)
        val date = df.format(Date.from(Instant.now()))
        every { resultSet.getObject(7) } returns date
        val result = sqlTypesConverter.convert(field, resultSet)
        df.format(result).shouldBe(date)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "FF:PP",
            "Thu Aug 08 17:26:32 GMT+08:00 2013",
            "17:26:32+08:00zz",
            "2001-01-31T17:26:32+08:00zz",
            "2001-01-31 17:26:32+08:00zz",
        ],
    )
    fun `sqlite invalid date throws`(input: String) {
        val field = Field(7, JDBCType.DATE, "DATE", DbFlavour.SQLITE)
        every { resultSet.getObject(7) } returns input
        shouldThrow<KapperUnsupportedOperationException> {
            sqlTypesConverter.convert(field, resultSet)
        }
    }

    @Test
    fun `sqlite invalid date type throws`() {
        val field = Field(7, JDBCType.DATE, "DATE", DbFlavour.SQLITE)
        every { resultSet.getObject(7) } returns 123
        shouldThrow<KapperUnsupportedOperationException> {
            sqlTypesConverter.convert(field, resultSet)
        }
    }

    @Test
    fun `when convertDate null, return null`() {
        every { resultSet.getDate(1) } returns null
        val result = convertDate(resultSet, 1, DbFlavour.UNKNOWN)
        result.shouldBe(null)
    }
}
