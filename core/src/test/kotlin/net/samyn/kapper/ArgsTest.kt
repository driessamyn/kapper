package net.samyn.kapper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.samyn.kapper.internal.DbConnectionUtils
import net.samyn.kapper.internal.Query
import net.samyn.kapper.internal.SQLTypesConverter.setParameter
import net.samyn.kapper.internal.setParameters
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.PreparedStatement

class ArgsTest {
    private val preparedStatementMock = mockk<PreparedStatement>(relaxed = true)
    private val connectionMock =
        mockk<Connection> {
            every { metaData } returns
                mockk<java.sql.DatabaseMetaData> {
                    every { databaseProductName } returns "driesDB"
                }
        }
    private val query = Query("SELECT  * FROM test_entity WHERE foo = :foo AND bar = :bar")

    @Test
    fun `set params`() {
        mapOf(
            "foo" to 1,
            "bar" to "baz",
        ).setParameters(query, preparedStatementMock, connectionMock)
        verify { preparedStatementMock.setParameter(1, 1, DbConnectionUtils.DbFlavour.UNKNOWN) }
        verify { preparedStatementMock.setParameter(2, "baz", DbConnectionUtils.DbFlavour.UNKNOWN) }
    }

    @Test
    fun `throws when token not found`() {
        val ex =
            shouldThrow<KapperParseException> {
                mapOf(
                    "id" to 1,
                ).setParameters(query, preparedStatementMock, connectionMock)
            }
        ex.message shouldContain "'id'"
    }
}
