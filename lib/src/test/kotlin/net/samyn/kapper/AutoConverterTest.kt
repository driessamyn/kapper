package net.samyn.kapper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import net.samyn.kapper.internal.AutoConverter
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

class AutoConverterTest {
    @Test
    fun `when string UUID convert`() {
        val uuid = "123e4567-e89b-12d3-a456-426614174000"
        val autoConvertedUuid = AutoConverter.convert(uuid, UUID::class)
        autoConvertedUuid.shouldBe(UUID.fromString(uuid))
    }

    @Test
    fun `when binary UUID convert`() {
        val uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
        val autoConvertedUuid = AutoConverter.convert(uuid.asBytes(), UUID::class)
        autoConvertedUuid.shouldBe(uuid)
    }

    @Test
    fun `when int cannot convert to UUID`() {
        shouldThrow<KapperUnsupportedOperationException> {
            AutoConverter.convert(123, UUID::class)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "123e4567-e89b-12d3-a456-426614ZZZZZZ"])
    fun `when invalid string cannot convert to UUID`(input: String) {
        shouldThrow<KapperParseException> {
            AutoConverter.convert(input, UUID::class)
        }
    }

    @Test
    fun `when invalid target type throw`() {
        shouldThrow<KapperUnsupportedOperationException> {
            AutoConverter.convert("123", String::class)
        }
    }

    @Test
    fun `convert valid Date to LocalDate`() {
        val date = Date.from(Instant.parse("2023-10-01T00:00:00Z"))
        val localDate = AutoConverter.convert(date, LocalDate::class) as LocalDate
        localDate.shouldBe(LocalDate.of(2023, 10, 1))
    }

    @Test
    fun `throw exception when invalid type is converted to LocalDate`() {
        shouldThrow<KapperUnsupportedOperationException> {
            AutoConverter.convert("2023-01-01", LocalDate::class) // Invalid input type
        }
    }

    @Test
    fun `convert valid LocalTime to Instant`() {
        val time = LocalTime.of(12, 30) // 12:30 PM
        val instant = AutoConverter.convert(time, Instant::class) as Instant
        val expectedInstant = LocalDate.now().atTime(12, 30).toInstant(java.time.ZoneOffset.UTC)
        instant.shouldBe(expectedInstant)
    }

    @Test
    fun `convert LocalTime at midnight to Instant`() {
        val midnight = LocalTime.MIDNIGHT
        val instant = AutoConverter.convert(midnight, Instant::class) as Instant
        val expectedInstant = LocalDate.now().atTime(0, 0).toInstant(java.time.ZoneOffset.UTC)
        instant.shouldBe(expectedInstant)
    }

    @Test
    fun `convert valid LocalDateTime to Instant`() {
        val now = LocalDateTime.now()
        val instant = AutoConverter.convert(now, Instant::class) as Instant
        val expectedInstant = now.toInstant(java.time.ZoneOffset.UTC)
        instant.shouldBe(expectedInstant)
    }

    @Test
    fun `throw exception when invalid type is converted to Instant`() {
        shouldThrow<KapperUnsupportedOperationException> {
            AutoConverter.convert(Date(), Instant::class) // Invalid input type
        }
    }

    private fun UUID.asBytes(): ByteArray {
        val b = ByteBuffer.wrap(ByteArray(16))
        b.putLong(mostSignificantBits)
        b.putLong(leastSignificantBits)
        return b.array()
    }
}
