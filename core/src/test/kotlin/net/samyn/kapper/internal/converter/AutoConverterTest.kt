package net.samyn.kapper.internal.converter

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import net.samyn.kapper.KapperUnsupportedOperationException
import net.samyn.kapper.internal.autoConverter
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
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
                arrayOf(java.lang.Integer::class.java, 123F, 123),
                arrayOf(Long::class.java, 123F, 123L),
                arrayOf(
                    Date::class.java,
                    Instant.parse("2023-10-01T00:00:00Z"),
                    Date.from(Instant.parse("2023-10-01T00:00:00Z")),
                ),
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
        autoConverter.convert(java.lang.Integer.valueOf(1), Int::class.java) shouldBe 1
    }
}
