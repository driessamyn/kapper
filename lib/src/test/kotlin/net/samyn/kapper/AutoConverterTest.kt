package net.samyn.kapper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import net.samyn.kapper.internal.AutoConverter
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.util.UUID

class AutoConverterTest {
    @Test
    fun `when string UUID convert`() {
        val uuid = "123e4567-e89b-12d3-a456-426614174000"
        val autoConvertedUuid = AutoConverter.convert(uuid, UUID::class)
        autoConvertedUuid.shouldBe(UUID.fromString(uuid))
    }

    @Test
    fun `when binary UUID convert`() {
        val uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
        val autoConvertedUuid = AutoConverter.convert(uuid.asBytes(), UUID::class)
        autoConvertedUuid.shouldBe(uuid)
    }

    @Test
    fun `when invalid source type throw`() {
        shouldThrow<KapperUnsupportedOperationException> {
            AutoConverter.convert(123, UUID::class)
        }
    }

    @Test
    fun `when invalid target type throw`() {
        shouldThrow<KapperUnsupportedOperationException> {
            AutoConverter.convert("123", String::class)
        }
    }

    private fun UUID.asBytes(): ByteArray {
        val b = ByteBuffer.wrap(ByteArray(16))
        b.putLong(mostSignificantBits)
        b.putLong(leastSignificantBits)
        return b.array()
    }
}
