package net.samyn.kapper.internal.converter

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import net.samyn.kapper.KapperUnsupportedOperationException
import net.samyn.kapper.internal.convertLocalDateTime
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime
import java.util.Date

class LocalDateTimeConverterTest {
    @Test
    fun `convert valid Instant to LocalDateTime`() {
        val now = Instant.now()
        val localDateTime = convertLocalDateTime(now) as LocalDateTime
        val expectedDt = LocalDateTime.ofInstant(now, java.time.ZoneOffset.systemDefault())
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
