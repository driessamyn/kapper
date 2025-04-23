package net.samyn.kapper.internal.converter

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import net.samyn.kapper.KapperUnsupportedOperationException
import net.samyn.kapper.internal.convertLocalDate
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date

class LocalDateConverterTest {
    @Test
    fun `convert valid Date to LocalDate`() {
        val date = Date.from(Instant.parse("2023-10-01T00:00:00Z"))
        val localDate = convertLocalDate(date)
        localDate.shouldBe(LocalDate.of(2023, 10, 1))
    }

    @Test
    fun `convert valid LocalDateTime to LocalDate`() {
        val date = LocalDateTime.of(2023, 10, 1, 0, 0)
        val localDate = convertLocalDate(date)
        localDate.shouldBe(LocalDate.of(2023, 10, 1))
    }

    @Test
    fun `convert valid Instant to LocalDate`() {
        val date = Instant.parse("2023-10-01T00:00:00Z")
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
