package net.samyn.kapper

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.samyn.kapper.internal.AutoConverter
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Date
import java.util.UUID
import kotlin.reflect.KClass

class AutoConverterTest {
    private val mockConverter = mockk<(Any) -> Any>()
    private val mockConverters =
        mapOf<KClass<*>, (Any) -> Any>(
            String::class to mockConverter,
        )

    @Test
    fun `when converters not available throw`() {
        val autoConverter = AutoConverter(mockConverters)
        shouldThrow<KapperUnsupportedOperationException> {
            autoConverter.convert("123", Int::class)
        }
    }

    @Test
    fun `when converters available call`() {
        every { mockConverter.invoke("123") } returns "123"
        val autoConverter = AutoConverter(mockConverters)
        val converted = autoConverter.convert("123", String::class)
        verify { mockConverter.invoke("123") }
        converted shouldBe "123"
    }

    @Nested
    inner class DefaultConverters {
        private val autoConverter = AutoConverter()

        @Test
        fun `when invalid target type throw`() {
            shouldThrow<KapperUnsupportedOperationException> {
                autoConverter.convert("123", Int::class)
            }
        }

        @Test
        fun `supports UUID`() {
            shouldNotThrow<KapperUnsupportedOperationException> {
                autoConverter.convert("123e4567-e89b-12d3-a456-426614174000", UUID::class)
            }
        }

        @Test
        fun `supports LocalDate`() {
            shouldNotThrow<KapperUnsupportedOperationException> {
                autoConverter.convert(Date.from(Instant.parse("2023-10-01T00:00:00Z")), LocalDate::class)
            }
        }

        @Test
        fun `supports LocalDateTime`() {
            shouldNotThrow<KapperUnsupportedOperationException> {
                autoConverter.convert(Instant.now(), LocalDateTime::class)
            }
        }

        @Test
        fun `supports LocalTime`() {
            shouldNotThrow<KapperUnsupportedOperationException> {
                autoConverter.convert(Instant.now(), LocalTime::class)
            }
        }

        @Test
        fun `supports Instant`() {
            shouldNotThrow<KapperUnsupportedOperationException> {
                autoConverter.convert(LocalTime.of(12, 30), Instant::class)
            }
        }
    }
}
