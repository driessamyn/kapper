package net.samyn.kapper.internal.automapper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.types.shouldBeTypeOf
import net.samyn.kapper.KapperParseException
import net.samyn.kapper.KapperUnsupportedOperationException
import net.samyn.kapper.internal.convertToPrimitive
import org.junit.jupiter.api.Test

class PrimitiveConverterTest {
    @Test
    fun `should convert Int`() {
        val value = 42
        val converted = convertToPrimitive(value)
        converted.shouldBeTypeOf<Int>()
    }

    @Test
    fun `should convert Long`() {
        val value = 42L
        val converted = convertToPrimitive(value)
        converted.shouldBeTypeOf<Long>()
    }

    @Test
    fun `should convert Double`() {
        val value = 3.14
        val converted = convertToPrimitive(value)
        converted.shouldBeTypeOf<Double>()
    }

    @Test
    fun `should convert Float`() {
        val value = 2.71f
        val converted = convertToPrimitive(value)
        converted.shouldBeTypeOf<Float>()
    }

    @Test
    fun `should convert Boolean`() {
        val value = true
        val converted = convertToPrimitive(value)
        converted.shouldBeTypeOf<Boolean>()
    }

    @Test
    fun `should convert Byte`() {
        val value: Byte = 0x1A
        val converted = convertToPrimitive(value)
        converted.shouldBeTypeOf<Byte>()
    }

    @Test
    fun `should convert Short`() {
        val value: Short = 123
        val converted = convertToPrimitive(value)
        converted.shouldBeTypeOf<Short>()
    }

    @Test
    fun `should convert Char`() {
        val value = 'A'
        val converted = convertToPrimitive(value)
        converted.shouldBeTypeOf<Char>()
    }

    @Test
    fun `should convert Long to Short when requested`() {
        val value = 42L
        val converted = convertToPrimitive(value, Short::class.java)
        converted.shouldBeTypeOf<Short>()
    }

    @Test
    fun `should convert Long to Byte when requested`() {
        val value = 42L
        val converted = convertToPrimitive(value, Byte::class.java)
        converted.shouldBeTypeOf<Byte>()
    }

    @Test
    fun `should convert Long to Int when requested`() {
        val value = 42L
        val converted = convertToPrimitive(value, Int::class.java)
        converted.shouldBeTypeOf<Int>()
    }

    @Test
    fun `should convert to primitive target classes`() {
        convertToPrimitive(42L, Long::class.java).shouldBeTypeOf<Long>()
        convertToPrimitive(42L, Double::class.java).shouldBeTypeOf<Double>()
        convertToPrimitive(42L, Float::class.java).shouldBeTypeOf<Float>()
        convertToPrimitive(true, Boolean::class.java).shouldBeTypeOf<Boolean>()
        convertToPrimitive('A', Char::class.java).shouldBeTypeOf<Char>()
    }

    @Test
    fun `should throw when wrapper is not a Number for numeric targets`() {
        listOf(
            Int::class.java,
            Long::class.java,
            Double::class.java,
            Float::class.java,
            Byte::class.java,
            Short::class.java,
            Integer::class.java,
            java.lang.Long::class.java,
        ).forEach { target ->
            shouldThrow<KapperUnsupportedOperationException> { convertToPrimitive("not a number", target) }
        }
    }

    @Test
    fun `should throw when wrapper is not Boolean or Char for boolean and char targets`() {
        shouldThrow<KapperUnsupportedOperationException> { convertToPrimitive(42, Boolean::class.java) }
        shouldThrow<KapperUnsupportedOperationException> { convertToPrimitive(42, java.lang.Boolean::class.java) }
        shouldThrow<KapperUnsupportedOperationException> { convertToPrimitive(42, Char::class.java) }
        shouldThrow<KapperUnsupportedOperationException> { convertToPrimitive(42, Character::class.java) }
    }

    @Test
    fun `should throw for unsupported target`() {
        shouldThrow<KapperUnsupportedOperationException> { convertToPrimitive(42, String::class.java) }
    }

    @Test
    fun `should throw when value is out of range for Byte target`() {
        shouldThrow<KapperParseException> { convertToPrimitive(128L, Byte::class.java) }
        shouldThrow<KapperParseException> { convertToPrimitive(-129L, Byte::class.java) }
        shouldThrow<KapperParseException> { convertToPrimitive(300, Byte::class.java) }
        convertToPrimitive(127L, Byte::class.java).shouldBeTypeOf<Byte>()
        convertToPrimitive(-128L, Byte::class.java).shouldBeTypeOf<Byte>()
    }

    @Test
    fun `should throw when value is out of range for Short target`() {
        shouldThrow<KapperParseException> { convertToPrimitive(32768L, Short::class.java) }
        shouldThrow<KapperParseException> { convertToPrimitive(-32769L, Short::class.java) }
        convertToPrimitive(32767L, Short::class.java).shouldBeTypeOf<Short>()
        convertToPrimitive(-32768L, Short::class.java).shouldBeTypeOf<Short>()
    }
}
