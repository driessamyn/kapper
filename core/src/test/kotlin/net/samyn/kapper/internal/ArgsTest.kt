package net.samyn.kapper.internal

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import io.mockk.verify
import net.samyn.kapper.DbFlavour
import net.samyn.kapper.KapperParseException
import net.samyn.kapper.internal.automapper.setParameter
import org.junit.jupiter.api.Test
import java.sql.PreparedStatement

class ArgsTest {
    private val preparedStatementMock = mockk<PreparedStatement>(relaxed = true)
    private val query = Query("SELECT  * FROM test_entity WHERE foo = :foo AND bar = :bar")

    @Test
    fun `set params`() {
        val args =
            mapOf(
                "foo" to 1,
                "bar" to "baz",
            )
        args.setParameters(query, preparedStatementMock, DbFlavour.UNKNOWN)
        verify { preparedStatementMock.setParameter(1, args["foo"], DbFlavour.UNKNOWN) }
        verify { preparedStatementMock.setParameter(2, args["bar"], DbFlavour.UNKNOWN) }
    }

    @Test
    fun `throws when token not found`() {
        val ex =
            shouldThrow<KapperParseException> {
                mapOf(
                    "id" to 1,
                ).setParameters(query, preparedStatementMock, DbFlavour.UNKNOWN)
            }
        ex.message shouldContain "'id'"
    }
}
