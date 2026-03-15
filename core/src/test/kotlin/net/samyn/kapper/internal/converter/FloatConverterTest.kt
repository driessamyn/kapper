package net.samyn.kapper.internal.converter

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import net.samyn.kapper.KapperUnsupportedOperationException
import net.samyn.kapper.internal.convertFloat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Date

class FloatConverterTest {
    @Test
    fun `convert valid Float to Float`() {
        val result = convertFloat(123.45F)
        result.shouldBe(123.45F)
    }

    @Test
    fun `convert valid java Float to Float`() {
        val result = convertFloat(java.lang.Float.valueOf(123.45F))
        result.shouldBe(123.45F)
    }

    @Test
    fun `convert valid BigDecimal to Float`() {
        val result = convertFloat(BigDecimal("123.45"))
        result.shouldBe(123.45F)
    }

    @Test
    fun `throw exception when invalid type is converted to Float`() {
        shouldThrow<KapperUnsupportedOperationException> {
            convertFloat(Date())
        }
    }
}
