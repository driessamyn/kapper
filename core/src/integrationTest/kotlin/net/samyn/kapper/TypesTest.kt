package net.samyn.kapper

import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.sql.Connection
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID
import kotlin.random.Random

class TypesTest : AbstractDbTests() {
    @ParameterizedTest()
    @MethodSource("databaseContainers")
    fun `can insert and retreive the same types`(connection: Connection) {
        val testData = createTestObject()
        val result =
            connection.execute(
                """
                INSERT INTO types_test (
                    t_uuid,
                    t_char,
                    t_varchar,
                    t_clob,
                    t_binary,
                    t_varbinary,
                    t_large_binary,
                    t_numeric,
                    t_decimal,
                    t_smallint,
                    t_int,
                    t_bigint,
                    t_float,
                    t_real,
                    t_double,
                    t_date,
                    t_local_date,
                    t_local_time,
                    t_timestamp,
                    t_boolean
                ) VALUES (
                    :uuid,
                    :char,
                    :varchar,
                    :clob,
                    :binary,
                    :varbinary,
                    :large_binary,
                    :numeric,  
                    :decimal,
                    :smallint,
                    :int,
                    :bigint,
                    :float,
                    :real,
                    :double,
                    :date,
                    :local_date,
                    :local_time,
                    :timestamp,
                    :boolean
                  );
                """.trimIndent(),
                "uuid" to testData.t_uuid,
                "char" to testData.t_char,
                "varchar" to testData.t_varchar,
                "clob" to testData.t_clob,
                "binary" to testData.t_binary,
                "varbinary" to testData.t_varbinary,
                "large_binary" to testData.t_large_binary,
                "numeric" to testData.t_numeric,
                "decimal" to testData.t_decimal,
                "smallint" to testData.t_smallint,
                "int" to testData.t_int,
                "bigint" to testData.t_bigint,
                "float" to testData.t_float,
                "real" to testData.t_real,
                "double" to testData.t_double,
                "date" to testData.t_date,
                "local_date" to testData.t_local_date,
                "local_time" to testData.t_local_time,
                "timestamp" to testData.t_timestamp,
                "boolean" to testData.t_boolean,
            )

        result.shouldBe(1)

        val selectResult =
            connection.querySingle<TypeTest>(
                "SELECT * FROM types_test where t_uuid = :uuid",
                "uuid" to testData.t_uuid,
            )
        selectResult.shouldBe(testData)
    }

    data class TypeTest(
        val t_uuid: UUID,
        val t_char: Char,
        val t_varchar: String,
        val t_clob: String,
        val t_binary: ByteArray,
        val t_varbinary: ByteArray,
        val t_large_binary: ByteArray,
        val t_numeric: Float,
        val t_decimal: Float,
        val t_smallint: Int,
        val t_int: Int,
        val t_bigint: Long,
        val t_float: Float,
        val t_real: Float,
        val t_double: Double,
        val t_date: Date,
        val t_local_date: LocalDate,
        val t_local_time: LocalTime,
        val t_timestamp: Instant,
        val t_boolean: Boolean,
    ) {
        // override to handle byte array correctly
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as TypeTest

            if (t_uuid != other.t_uuid) return false
            if (t_char != other.t_char) return false
            if (t_varchar != other.t_varchar) return false
            if (t_clob != other.t_clob) return false
            if (!t_binary.contentEquals(other.t_binary)) return false
            if (!t_varbinary.contentEquals(other.t_varbinary)) return false
            if (!t_large_binary.contentEquals(other.t_large_binary)) return false
            if (t_numeric != other.t_numeric) return false
            if (t_decimal != other.t_decimal) return false
            if (t_smallint != other.t_smallint) return false
            if (t_int != other.t_int) return false
            if (t_bigint != other.t_bigint) return false
            if (t_float != other.t_float) return false
            if (t_real != other.t_real) return false
            if (t_double != other.t_double) return false
            if (t_date != other.t_date) return false
            if (t_local_date != other.t_local_date) return false
            if (t_local_time != other.t_local_time) return false
            if (t_timestamp != other.t_timestamp) return false
            if (t_boolean != other.t_boolean) return false

            return true
        }

        override fun hashCode(): Int {
            var result = t_uuid.hashCode()
            result = 31 * result + t_char.hashCode()
            result = 31 * result + t_varchar.hashCode()
            result = 31 * result + t_clob.hashCode()
            result = 31 * result + t_binary.contentHashCode()
            result = 31 * result + t_varbinary.contentHashCode()
            result = 31 * result + t_large_binary.contentHashCode()
            result = 31 * result + t_numeric.hashCode()
            result = 31 * result + t_decimal.hashCode()
            result = 31 * result + t_smallint
            result = 31 * result + t_int
            result = 31 * result + t_bigint.hashCode()
            result = 31 * result + t_float.hashCode()
            result = 31 * result + t_real.hashCode()
            result = 31 * result + t_double.hashCode()
            result = 31 * result + t_date.hashCode()
            result = 31 * result + t_local_date.hashCode()
            result = 31 * result + t_local_time.hashCode()
            result = 31 * result + t_timestamp.hashCode()
            result = 31 * result + t_boolean.hashCode()
            return result
        }
    }

    private fun createTestObject() =
        TypeTest(
            t_uuid = UUID.randomUUID(),
            t_char = 'c',
            t_varchar = "var",
            t_clob = "clob",
            t_binary = Random.nextBytes(16),
            t_varbinary = Random.nextBytes(16),
            t_large_binary = Random.nextBytes(16),
            t_numeric = 123.456F,
            t_decimal = 789.012F,
            t_smallint = 1,
            t_int = 123,
            t_bigint = 123456L,
            t_float = 123.45f,
            t_real = 123.45f,
            t_double = 123.45,
            t_date = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()),
            t_local_date = LocalDate.now(),
            // TODO - more precise time tests.
            t_local_time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS),
            t_timestamp = randomInstantAroundNow().truncatedTo(ChronoUnit.SECONDS),
            t_boolean = true,
        )

    private fun randomInstantAroundNow(): Instant {
        val now = Instant.now()
        val randomDays = Random.nextLong(-365, 365) // ±1 year
        return now.plusSeconds(randomDays * 24 * 60 * 60)
    }
}
