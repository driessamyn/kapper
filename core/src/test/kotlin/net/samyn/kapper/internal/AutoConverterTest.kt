package net.samyn.kapper.internal

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import net.samyn.kapper.KapperUnsupportedOperationException
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Date
import java.util.UUID

class AutoConverterTest {
    class NewType

    @Test
    fun `when converters not available throw`() {
        shouldThrow<KapperUnsupportedOperationException> {
            autoConverter.convert("123", NewType::class.java)
        }
    }

    @Test
    fun `when converters available call`() {
        val converted = autoConverter.convert("123", String::class.java)
        converted shouldBe "123"
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
}
