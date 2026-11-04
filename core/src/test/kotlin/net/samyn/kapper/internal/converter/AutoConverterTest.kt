package net.samyn.kapper.internal.converter

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import net.samyn.kapper.DbFlavour
import net.samyn.kapper.Field
import net.samyn.kapper.KapperParseException
import net.samyn.kapper.KapperUnsupportedOperationException
import net.samyn.kapper.internal.ColumnContext
import net.samyn.kapper.internal.autoConverter
import net.samyn.kapper.internal.convert
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.sql.JDBCType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import java.util.UUID

class AutoConverterTest {
    companion object {
        @JvmStatic
        fun classProvider(): List<Array<Any>> =
            listOf(
                arrayOf(UUID::class.java, "123e4567-e89b-12d3-a456-426614174000", UUID.fromString("123e4567-e89b-12d3-a456-426614174000")),
                arrayOf(
                    LocalDate::class.java,
                    LocalDateTime.of(2023, 10, 1, 0, 0, 0),
                    LocalDate.of(2023, 10, 1),
                ),
                arrayOf(
                    LocalDateTime::class.java,
                    LocalDateTime.of(2023, 10, 1, 12, 34, 56).toString(),
                    LocalDateTime.of(2023, 10, 1, 12, 34, 56),
                ),
                arrayOf(
                    LocalTime::class.java,
                    LocalTime.of(12, 34, 56).toString(),
                    LocalTime.of(12, 34, 56),
                ),
                arrayOf(
                    Instant::class.java,
                    LocalDateTime.ofInstant(Instant.parse("2023-10-01T12:34:56Z"), ZoneId.systemDefault()),
                    Instant.parse("2023-10-01T12:34:56Z"),
                ),
                arrayOf(Char::class.java, "A", 'A'),
                arrayOf(Int::class.java, 123F, 123),
                arrayOf(Integer::class.java, 123F, 123),
                arrayOf(Long::class.java, 123F, 123L),
                arrayOf(
                    Date::class.java,
                    Instant.parse("2023-10-01T00:00:00Z"),
                    Date.from(Instant.parse("2023-10-01T00:00:00Z")),
                ),
                arrayOf(BigDecimal::class.java, 123.456, BigDecimal.valueOf(123.456)),
                arrayOf(Boolean::class.java, "true", true),
                arrayOf(String::class.java, 123, "123"),
            )
    }

    class NewType

    @Test
    fun `when converters not available throw`() {
        shouldThrow<KapperUnsupportedOperationException> {
            autoConverter.convert("123", NewType::class.java)
        }
    }

    @ParameterizedTest
    @MethodSource("classProvider")
    fun `when converters available call`(
        clazz: Class<*>,
        input: Any,
        expected: Any,
    ) {
        autoConverter.convert(input, clazz) shouldBe expected
    }

    @Nested
    inner class DefaultConverters {
        @Test
        fun `when invalid target type throw`() {
            shouldThrow<KapperUnsupportedOperationException> {
                autoConverter.convert("123", Int::class.java)
            }
        }

        @Test
        fun `supports UUID`() {
            shouldNotThrow<KapperUnsupportedOperationException> {
                autoConverter.convert("123e4567-e89b-12d3-a456-426614174000", UUID::class.java)
            }
        }

        @Test
        fun `supports LocalDate`() {
            shouldNotThrow<KapperUnsupportedOperationException> {
                autoConverter.convert(Date.from(Instant.parse("2023-10-01T00:00:00Z")), LocalDate::class.java)
            }
        }

        @Test
        fun `supports LocalDateTime`() {
            shouldNotThrow<KapperUnsupportedOperationException> {
                autoConverter.convert(Instant.now(), LocalDateTime::class.java)
            }
        }

        @Test
        fun `supports LocalTime`() {
            shouldNotThrow<KapperUnsupportedOperationException> {
                autoConverter.convert(Instant.now(), LocalTime::class.java)
            }
        }

        @Test
        fun `supports Instant`() {
            shouldNotThrow<KapperUnsupportedOperationException> {
                autoConverter.convert(LocalTime.of(12, 30), Instant::class.java)
            }
        }
    }

    @Test
    fun `when primitive type convert`() {
        autoConverter.convert(1.toByte(), Byte::class.java) shouldBe 1.toByte()
    }

    @Test
    fun `when primitive type use custom converter`() {
        autoConverter.convert(Integer.valueOf(1), Int::class.java) shouldBe 1
    }

    @Test
    fun `when registered converter rejects source type fall back to target conversion`() {
        // convertDouble/convertFloat do not handle Long; the target-aware conversion should.
        autoConverter.convert(42L, Double::class.java) shouldBe 42.0
        autoConverter.convert(42L, java.lang.Double::class.java) shouldBe 42.0
        autoConverter.convert(42L, Float::class.java) shouldBe 42.0f
        // convertInt does not handle Double; the target-aware conversion should.
        autoConverter.convert(42.0, Int::class.java) shouldBe 42
    }

    @Test
    fun `when registered converter handles source type keep registered behavior`() {
        autoConverter.convert(42L, Int::class.java) shouldBe 42
        autoConverter.convert(123F, Long::class.java) shouldBe 123L
    }

    @Test
    fun `when range check fails the exception is not swallowed`() {
        // convertInt rejects out-of-range Longs with KapperParseException; the fallback must
        // not turn that into a silent wrap-around via convertToPrimitive.
        shouldThrow<KapperParseException> { autoConverter.convert(Int.MAX_VALUE.toLong() + 1, Int::class.java) }
        shouldThrow<KapperParseException> { autoConverter.convert(300L, Byte::class.java) }
        shouldThrow<KapperParseException> { autoConverter.convert(32768L, Short::class.java) }
    }

    @Nested
    inner class ColumnContextMessages {
        private val field = Field(2, JDBCType.INTEGER, "INT4", DbFlavour.POSTGRESQL)

        @Test
        fun `conversion failure names the column, index, driver type and flavour`() {
            val ex =
                shouldThrow<KapperUnsupportedOperationException> {
                    autoConverter.convert("not-a-number", NewType::class.java, ColumnContext("age", field))
                }
            ex.message.shouldContain("java.lang.String")
            ex.message.shouldContain("NewType")
            ex.message.shouldContain("age")
            ex.message.shouldContain("index 2")
            ex.message.shouldContain("INT4")
            ex.message.shouldContain("POSTGRESQL")
            ex.message.shouldContain("custom converter")
        }

        @Test
        fun `parse failure names the column`() {
            val ex =
                shouldThrow<KapperParseException> {
                    autoConverter.convert("not-a-uuid", UUID::class.java, ColumnContext("id", field))
                }
            ex.message.shouldContain("id")
            ex.message.shouldContain("custom converter")
        }

        @Test
        fun `without context the original message is preserved`() {
            val ex =
                shouldThrow<KapperUnsupportedOperationException> {
                    autoConverter.convert("123", NewType::class.java, null)
                }
            ex.message shouldBe
                "Cannot auto-convert from class java.lang.String to ${NewType::class.java.canonicalName}"
        }
    }
}
