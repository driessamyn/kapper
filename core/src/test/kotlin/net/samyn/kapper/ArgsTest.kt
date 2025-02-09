package net.samyn.kapper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import io.mockk.verify
import net.samyn.kapper.internal.DbConnectionUtils
import net.samyn.kapper.internal.Query
import net.samyn.kapper.internal.SQLTypesConverter.setParameter
import net.samyn.kapper.internal.setParameters
import org.junit.jupiter.api.Test
import java.sql.PreparedStatement

class ArgsTest {
    private val preparedStatementMock = mockk<PreparedStatement>(relaxed = true)
    private val query = Query("SELECT  * FROM test_entity WHERE foo = :foo AND bar = :bar")

    @Test
    fun `set params`() {
        mapOf(
            "foo" to 1,
            "bar" to "baz",
        ).setParameters(query, preparedStatementMock, DbConnectionUtils.DbFlavour.UNKNOWN)
        verify { preparedStatementMock.setParameter(1, 1, DbConnectionUtils.DbFlavour.UNKNOWN) }
        verify { preparedStatementMock.setParameter(2, "baz", DbConnectionUtils.DbFlavour.UNKNOWN) }
    }

    @Test
    fun `throws when token not found`() {
        val ex =
            shouldThrow<KapperParseException> {
                mapOf(
                    "id" to 1,
                ).setParameters(query, preparedStatementMock, DbConnectionUtils.DbFlavour.UNKNOWN)
            }
        ex.message shouldContain "'id'"
    }
}
