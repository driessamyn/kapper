package net.samyn.kapper.internal

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import net.samyn.kapper.KapperParseException
import net.samyn.kapper.KapperUnsupportedOperationException
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.ByteBuffer
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Date
import java.util.UUID

class ConverterTest {
    @Nested
    inner class UUIDConverter {
        @Test
        fun `when string UUID convert`() {
            val uuid = "123e4567-e89b-12d3-a456-426614174000"
            val autoConvertedUuid = convertUUID(uuid)
            autoConvertedUuid.shouldBe(UUID.fromString(uuid))
        }

        @Test
        fun `when binary UUID convert`() {
            val uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
            val autoConvertedUuid = convertUUID(uuid.asBytes())
            autoConvertedUuid.shouldBe(uuid)
        }

        @Test
        fun `when int cannot convert to UUID`() {
            shouldThrow<KapperUnsupportedOperationException> {
                convertUUID(123)
            }
        }

        @ParameterizedTest
        @ValueSource(strings = ["", "123e4567-e89b-12d3-a456-426614ZZZZZZ"])
        fun `when invalid string cannot convert to UUID`(input: String) {
            shouldThrow<KapperParseException> {
                convertUUID(input)
            }
        }
    }

    @Nested
    inner class LocalDateConverter {
        @Test
        fun `convert valid Date to LocalDate`() {
            val date = Date.from(Instant.parse("2023-10-01T00:00:00Z"))
            val localDate = convertLocalDate(date)
            localDate.shouldBe(LocalDate.of(2023, 10, 1))
        }

        @Test
        fun `convert valid string to LocalDate`() {
            val date = "2023-10-01"
            val localDate = convertLocalDate(date)
            localDate.shouldBe(LocalDate.of(2023, 10, 1))
        }

        @Test
        fun `throw exception when invalid type is converted to LocalDate`() {
            shouldThrow<KapperUnsupportedOperationException> {
                convertLocalDate(2023) // Invalid input type
            }
        }
    }

    @Nested
    inner class InstantConverter {
        @Test
        fun `convert valid LocalTime to Instant`() {
            val time = LocalTime.of(12, 30) // 12:30 PM
            val instant = convertInstant(time) as Instant
            val expectedInstant = LocalDate.now().atTime(12, 30).toInstant(java.time.ZoneOffset.UTC)
            instant.shouldBe(expectedInstant)
        }

        @Test
        fun `convert LocalTime at midnight to Instant`() {
            val midnight = LocalTime.MIDNIGHT
            val instant = convertInstant(midnight) as Instant
            val expectedInstant = LocalDate.now().atTime(0, 0).toInstant(java.time.ZoneOffset.UTC)
            instant.shouldBe(expectedInstant)
        }

        @Test
        fun `convert valid LocalDateTime to Instant`() {
            val now = LocalDateTime.now()
            val instant = convertInstant(now) as Instant
            val expectedInstant = now.toInstant(java.time.ZoneOffset.UTC)
            instant.shouldBe(expectedInstant)
        }

        @Test
        fun `throw exception when invalid type is converted to Instant`() {
            shouldThrow<KapperUnsupportedOperationException> {
                convertInstant(Date()) // Invalid input type
            }
        }
    }

    @Nested
    inner class LocalDateTimeConverter {
        @Test
        fun `convert valid Instant to LocalDateTime`() {
            val now = Instant.now()
            val localDateTime = convertLocalDateTime(now) as LocalDateTime
            val expectedDt = LocalDateTime.ofInstant(now, java.time.ZoneOffset.UTC)
            localDateTime.shouldBe(expectedDt)
        }

        @Test
        fun `convert valid String to LocalDateTime`() {
            val dateTimeString = "2023-10-01T12:30:00"
            val localDateTime = convertLocalDateTime(dateTimeString) as LocalDateTime
            val expectedDt = LocalDateTime.parse(dateTimeString)
            localDateTime.shouldBe(expectedDt)
        }

        @Test
        fun `throw exception when invalid type is converted to LocalDateTime`() {
            shouldThrow<KapperUnsupportedOperationException> {
                convertLocalDateTime(Date()) // Invalid input type
            }
        }
    }

    @Nested
    inner class LocalTimeConverter {
        @Test
        fun `convert valid Instant to LocalTime`() {
            val now = Instant.now()
            val locaTime = convertLocalTime(now) as LocalTime
            val expectedTime = LocalTime.ofInstant(now, java.time.ZoneOffset.UTC)
            locaTime.shouldBe(expectedTime)
        }

        @Test
        fun `convert valid String to LocalTime`() {
            val timeString = "12:30:00"
            val locaTime = convertLocalTime(timeString)
            val expectedTime = LocalTime.parse(timeString)
            locaTime.shouldBe(expectedTime)
        }

        @Test
        fun `throw exception when invalid type is converted to LocalTime`() {
            shouldThrow<KapperUnsupportedOperationException> {
                convertLocalTime(Date()) // Invalid input type
            }
        }
    }

    @Nested
    inner class CharConverter {
        @Test
        fun `convert valid String to Char`() {
            val charString = "A"
            val char = convertChar(charString)
            char.shouldBe('A')
        }

        @Test
        fun `String too long for Char`() {
            val charString = "AB"
            shouldThrow<KapperParseException> {
                convertChar(charString)
            }
        }

        @Test
        fun `convert valid CharArray to Char`() {
            val charArray = charArrayOf('A')
            val char = convertChar(charArray)
            char.shouldBe('A')
        }

        @Test
        fun `CharArray too long for Char`() {
            val charArray = charArrayOf('A', 'B')
            shouldThrow<KapperParseException> {
                convertChar(charArray)
            }
        }

        @Test
        fun `throw exception when invalid type is converted to Char`() {
            shouldThrow<KapperUnsupportedOperationException> {
                convertChar(123) // Invalid input type
            }
        }
    }

    private fun UUID.asBytes(): ByteArray {
        val b = ByteBuffer.wrap(ByteArray(16))
        b.putLong(mostSignificantBits)
        b.putLong(leastSignificantBits)
        return b.array()
    }
}
