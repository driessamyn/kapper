package net.samyn.kapper.internal.converter

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import net.samyn.kapper.KapperUnsupportedOperationException
import net.samyn.kapper.internal.convertInt
import org.junit.jupiter.api.Test
import java.util.Date

class IntConverterTest {
    @Test
    fun `convert valid Float to Int`() {
        val float = 123F
        val intValue = convertInt(float)
        intValue.shouldBe(123)
    }

    @Test
    fun `convert valid java Float to Int`() {
        val float = java.lang.Float.valueOf(123F)
        val intValue = convertInt(float)
        intValue.shouldBe(123)
    }

    @Test
    fun `throw exception when invalid type is converted to Int`() {
        shouldThrow<KapperUnsupportedOperationException> {
            convertInt(Date()) // Invalid input type
        }
    }

    @Test
    fun `convert valid Integer to Int`() {
        val intValue = java.lang.Integer.valueOf(123)
        intValue.shouldBe(123)
    }
}
