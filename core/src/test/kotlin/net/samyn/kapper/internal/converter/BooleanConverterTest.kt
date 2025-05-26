package net.samyn.kapper.internal.converter

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import net.samyn.kapper.KapperUnsupportedOperationException
import net.samyn.kapper.internal.convertBoolean
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.Date
import java.util.stream.Stream

class BooleanConverterTest {
    companion object {
        @JvmStatic
        fun booleanTrueValues(): Stream<Arguments> =
            Stream.of(
                Arguments.of(1),
                Arguments.of(1F),
                Arguments.of(1L),
                Arguments.of(1.toByte()),
                Arguments.of(1.toShort()),
                Arguments.of("true"),
                Arguments.of("1"),
            )

        @JvmStatic
        fun booleanFalseValues(): Stream<Arguments> =
            Stream.of(
                Arguments.of(0),
                Arguments.of(0F),
                Arguments.of(0L),
                Arguments.of(0.toByte()),
                Arguments.of(0.toShort()),
                Arguments.of("false"),
                Arguments.of("0"),
            )
    }

    @ParameterizedTest
    @MethodSource("booleanTrueValues")
    fun `convert valid true to Bool`(value: Any) {
        val bool = convertBoolean(value)
        bool.shouldBe(true)
    }

    @ParameterizedTest
    @MethodSource("booleanFalseValues")
    fun `convert valid false to Bool`(value: Any) {
        val bool = convertBoolean(value)
        bool.shouldBe(false)
    }

    @Test
    fun `throw exception when invalid type is converted to Bool`() {
        shouldThrow<KapperUnsupportedOperationException> {
            convertBoolean(Date()) // Invalid input type
        }
    }

    @Test
    fun `convert valid Boolean to Bool`() {
        val bool = convertBoolean(java.lang.Boolean.TRUE)
        bool.shouldBe(true)
    }
}
