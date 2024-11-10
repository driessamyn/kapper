package net.samyn.kapper.internal

import net.samyn.kapper.KapperUnsupportedOperationException
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.reflect.KClass

object AutoConverter {
    fun convert(
        value: Any,
        target: KClass<*>,
    ): Any {
        val converted =
            when (target) {
                UUID::class -> {
                    return if (value is String) {
                        UUID.fromString(value)
                    } else if (value is ByteArray) {
                        value.asUUID()
                    } else {
                        throw KapperUnsupportedOperationException(
                            "Cannot auto-convert from ${value.javaClass} to ${target::class.java}",
                        )
                    }
                }
                else ->
                    throw KapperUnsupportedOperationException(
                        "Cannot auto-convert from ${value.javaClass} to ${target::class.java}",
                    )
            }
        return converted
    }

    private fun ByteArray.asUUID(): UUID {
        val b = ByteBuffer.wrap(this)
        return UUID(b.getLong(), b.getLong())
    }
}
