package net.samyn.kapper.internal.converter

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import net.samyn.kapper.KapperParseException
import net.samyn.kapper.KapperUnsupportedOperationException
import net.samyn.kapper.internal.convertChar
import org.junit.jupiter.api.Test

class CharConverterTest {
    @Test
    fun `convert valid String to Char`() {
        val charString = "A"
        val char = convertChar(charString)
        char.shouldBe('A')
    }

    @Test
    fun `String too short for Char`() {
        val charString = ""
        shouldThrow<KapperParseException> {
            convertChar(charString)
        }
    }

    @Test
    fun `String too long for Char`() {
        val charString = "AB"
        shouldThrow<KapperParseException> {
            convertChar(charString)
        }
    }

    @Test
    fun `convert valid CharArray to Char`() {
        val charArray = charArrayOf('A')
        val char = convertChar(charArray)
        char.shouldBe('A')
    }

    @Test
    fun `CharArray too short for Char`() {
        val charArray = charArrayOf()
        shouldThrow<KapperParseException> {
            convertChar(charArray)
        }
    }

    @Test
    fun `CharArray too long for Char`() {
        val charArray = charArrayOf('A', 'B')
        shouldThrow<KapperParseException> {
            convertChar(charArray)
        }
    }

    @Test
    fun `throw exception when invalid type is converted to Char`() {
        shouldThrow<KapperUnsupportedOperationException> {
            convertChar(123) // Invalid input type
        }
    }
}
