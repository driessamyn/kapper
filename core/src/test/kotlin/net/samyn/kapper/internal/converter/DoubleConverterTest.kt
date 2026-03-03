package net.samyn.kapper.internal.converter

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import net.samyn.kapper.KapperUnsupportedOperationException
import net.samyn.kapper.internal.convertDouble
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Date

class DoubleConverterTest {
    @Test
    fun `convert valid Double to Double`() {
        val result = convertDouble(123.45)
        result.shouldBe(123.45)
    }

    @Test
    fun `convert valid java Double to Double`() {
        val result = convertDouble(java.lang.Double.valueOf(123.45))
        result.shouldBe(123.45)
    }

    @Test
    fun `convert valid BigDecimal to Double`() {
        val result = convertDouble(BigDecimal("123.45"))
        result.shouldBe(123.45)
    }

    @Test
    fun `throw exception when invalid type is converted to Double`() {
        shouldThrow<KapperUnsupportedOperationException> {
            convertDouble(Date())
        }
    }
}
