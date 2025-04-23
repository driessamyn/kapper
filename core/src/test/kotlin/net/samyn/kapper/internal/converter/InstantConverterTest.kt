package net.samyn.kapper.internal.converter

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import net.samyn.kapper.KapperUnsupportedOperationException
import net.samyn.kapper.internal.convertInstant
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.Date

class InstantConverterTest {
    @Test
    fun `convert valid LocalTime to Instant`() {
        val time = LocalTime.of(12, 30) // 12:30 PM
        val instant = convertInstant(time) as Instant
        val expectedInstant = LocalDate.now().atTime(12, 30).toInstant(ZoneOffset.UTC)
        instant.shouldBe(expectedInstant)
    }

    @Test
    fun `convert LocalTime at midnight to Instant`() {
        val midnight = LocalTime.MIDNIGHT
        val instant = convertInstant(midnight) as Instant
        val expectedInstant = LocalDate.now().atTime(0, 0).toInstant(ZoneOffset.UTC)
        instant.shouldBe(expectedInstant)
    }

    @Test
    fun `convert valid LocalDateTime to Instant`() {
        val now = Instant.now()
        val instant = convertInstant(LocalDateTime.ofInstant(now, ZoneOffset.systemDefault())) as Instant
        instant.shouldBe(now)
    }

    @Test
    fun `throw exception when invalid type is converted to Instant`() {
        shouldThrow<KapperUnsupportedOperationException> {
            convertInstant(Date()) // Invalid input type
        }
    }
}
