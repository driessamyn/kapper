package net.samyn.kapper.internal.converter

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import net.samyn.kapper.KapperUnsupportedOperationException
import net.samyn.kapper.internal.convertDate
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime
import java.util.Date

class DateConverterTest {
    @Test
    fun `convert valid Instant to Long`() {
        val instant = Instant.now()
        val dateValue = convertDate(instant)
        dateValue.shouldBe(Date.from(instant))
    }

    @Test
    fun `convert valid LocalDateTime to Long`() {
        val instant = LocalDateTime.now()
        val dateValue = convertDate(instant)
        dateValue.shouldBe(Date.from(instant.atZone(java.time.ZoneOffset.systemDefault()).toInstant()))
    }

    @Test
    fun `throw exception when invalid type is converted to Date`() {
        shouldThrow<KapperUnsupportedOperationException> {
            convertDate("") // Invalid input type
        }
    }
}
