package net.samyn.kapper.internal.converter

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import net.samyn.kapper.KapperParseException
import net.samyn.kapper.KapperUnsupportedOperationException
import net.samyn.kapper.internal.convertUUID
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.ByteBuffer
import java.util.UUID

class UUIDConverterTest {
    @Test
    fun `when string UUID convert`() {
        val uuid = "123e4567-e89b-12d3-a456-426614174000"
        val autoConvertedUuid = convertUUID(uuid)
        autoConvertedUuid.shouldBe(UUID.fromString(uuid))
    }

    @Test
    fun `when char array UUID convert`() {
        val uuid = "123e4567-e89b-12d3-a456-426614174000"
        val autoConvertedUuid = convertUUID(uuid.toCharArray())
        autoConvertedUuid.shouldBe(UUID.fromString(uuid))
    }

    @Test
    fun `when binary UUID convert`() {
        val uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
        val autoConvertedUuid = convertUUID(uuid.asBytes())
        autoConvertedUuid.shouldBe(uuid)
    }

    @Test
    fun `when int cannot convert to UUID`() {
        shouldThrow<KapperUnsupportedOperationException> {
            convertUUID(123)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "123e4567-e89b-12d3-a456-426614ZZZZZZ"])
    fun `when invalid string cannot convert to UUID`(input: String) {
        shouldThrow<KapperParseException> {
            convertUUID(input)
        }
    }

    private fun UUID.asBytes(): ByteArray {
        val b = ByteBuffer.wrap(ByteArray(16))
        b.putLong(mostSignificantBits)
        b.putLong(leastSignificantBits)
        return b.array()
    }
}
