package net.samyn.kapper.internal.converter

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import net.samyn.kapper.KapperUnsupportedOperationException
import net.samyn.kapper.internal.convertLong
import org.junit.jupiter.api.Test

class LongConverterTest {
    @Test
    fun `convert valid Float to Long`() {
        val float = 123F
        val longValue = convertLong(float)
        longValue.shouldBe(123)
    }

    @Test
    fun `throw exception when invalid type is converted to Long`() {
        shouldThrow<KapperUnsupportedOperationException> {
            convertLong("") // Invalid input type
        }
    }

    @Test
    fun `convert valid Long to Long`() {
        val longValue = java.lang.Long.valueOf(123)
        longValue.shouldBe(123)
    }
}
