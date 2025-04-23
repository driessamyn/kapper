package net.samyn.kapper.internal.converter

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import net.samyn.kapper.KapperUnsupportedOperationException
import net.samyn.kapper.internal.convertLocalTime
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.Date

class LocalTimeConverterTest {
    @Test
    fun `convert valid Instant to LocalTime`() {
        val now = Instant.now()
        val localTime = convertLocalTime(now)
        val expectedTime = LocalTime.ofInstant(now, ZoneOffset.systemDefault())
        localTime.shouldBe(expectedTime)
    }

    @Test
    fun `convert valid String to LocalTime`() {
        val timeString = "12:30:00"
        val localTime = convertLocalTime(timeString)
        val expectedTime = LocalTime.parse(timeString)
        localTime.shouldBe(expectedTime)
    }

    @Test
    fun `convert valid Long to LocalTime`() {
        val now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS)
        val localTime = convertLocalTime(now.toEpochMilli())
        val expectedTime = LocalTime.ofInstant(now, ZoneOffset.systemDefault())
        localTime.shouldBe(expectedTime)
    }

    @Test
    fun `convert valid Int to LocalTime`() {
        val twoOclockPmUTC = 14 * 60 * 60 * 1000 // 14:00:00 - SQLite stores time as ms from 1970
        val localTime = convertLocalTime(twoOclockPmUTC)
        localTime.shouldBe(Instant.ofEpochMilli(twoOclockPmUTC.toLong()).atZone(ZoneOffset.systemDefault()).toLocalTime())
    }

    @Test
    fun `throw exception when invalid type is converted to LocalTime`() {
        shouldThrow<KapperUnsupportedOperationException> {
            convertLocalTime(Date()) // Invalid input type
        }
    }
}
