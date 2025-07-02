package net.samyn.kapper.internal

import io.mockk.mockk
import io.mockk.verify
import net.samyn.kapper.DbFlavour
import net.samyn.kapper.KapperParseException
import net.samyn.kapper.internal.automapper.setParameter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.PreparedStatement

class ArgsMapperTest {
    data class User(val id: Int, val name: String)

    @Test
    fun `setParameters sets parameters using mapping functions`() {
        val tokens =
            mapOf(
                "id" to listOf(1),
                "name" to listOf(2),
            )
        val stmt = mockk<PreparedStatement>(relaxed = true)
        val user = User(42, "Bob")
        val args: Map<String, (User) -> Any?> =
            mapOf(
                "id" to { u: User -> u.id },
                "name" to { u: User -> u.name },
            )
        args.setParameters(tokens, stmt, user, DbFlavour.UNKNOWN)
        verify { stmt.setParameter(1, 42, DbFlavour.UNKNOWN) }
        verify { stmt.setParameter(2, "Bob", DbFlavour.UNKNOWN) }
    }

    @Test
    fun `setParameters throws if token missing`() {
        val tokens = mapOf("id" to listOf(1))
        val stmt = mockk<PreparedStatement>(relaxed = true)
        val user = User(1, "X")
        val args: Map<String, (User) -> Any?> =
            mapOf(
                "id" to { u: User -> u.id },
                "name" to { u: User -> u.name },
            )
        assertThrows<KapperParseException> {
            args.setParameters(tokens, stmt, user, DbFlavour.UNKNOWN)
        }
    }
}
