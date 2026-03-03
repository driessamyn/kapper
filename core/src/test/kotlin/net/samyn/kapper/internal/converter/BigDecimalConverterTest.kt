package net.samyn.kapper.internal.converter

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import net.samyn.kapper.KapperUnsupportedOperationException
import net.samyn.kapper.internal.convertBigDecimal
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Date

class BigDecimalConverterTest {
    @Test
    fun `convert BigDecimal to BigDecimal`() {
        val bd = BigDecimal("123.456")
        convertBigDecimal(bd).shouldBe(bd)
    }

    @Test
    fun `convert Double to BigDecimal`() {
        val result = convertBigDecimal(123.456)
        result.shouldBe(BigDecimal.valueOf(123.456))
    }

    @Test
    fun `convert Float to BigDecimal`() {
        val result = convertBigDecimal(123.5F)
        result.shouldBe(BigDecimal.valueOf(123.5))
    }

    @Test
    fun `convert Long to BigDecimal`() {
        val result = convertBigDecimal(123L)
        result.shouldBe(BigDecimal.valueOf(123L))
    }

    @Test
    fun `convert Int to BigDecimal`() {
        val result = convertBigDecimal(123)
        result.shouldBe(BigDecimal.valueOf(123L))
    }

    @Test
    fun `convert String to BigDecimal`() {
        val result = convertBigDecimal("123.456789")
        result.shouldBe(BigDecimal("123.456789"))
    }

    @Test
    fun `throw exception when invalid type is converted to BigDecimal`() {
        shouldThrow<KapperUnsupportedOperationException> {
            convertBigDecimal(Date())
        }
    }
}
