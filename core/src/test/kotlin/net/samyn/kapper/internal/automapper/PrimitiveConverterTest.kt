package net.samyn.kapper.internal.automapper

import io.kotest.matchers.types.shouldBeTypeOf
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
}
