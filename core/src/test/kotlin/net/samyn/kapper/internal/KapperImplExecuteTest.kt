package net.samyn.kapper.internal

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import net.samyn.kapper.internal.SQLTypesConverter.setParameter
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

// Complements tests in KapperApiTest, targeting KapperImpl directly
@Isolated // avoid running in parallel as this test uses static mock for KapperInstance
class KapperImplExecuteTest {
    // some duplication with KapperApiTest, but maybe that's ok for readability?
    private val mockSqlTemplate = "UPDATE foo set name = 'bar' WHERE id = :id"
    private val mockSqlQuery = "MOCK UPDATE foo"
    private val mockQueryBuilder = mockk<(String) -> Query>(relaxed = true)
    private val mockQuery =
        mockk<Query>(relaxed = true) {
            every { sql } returns mockSqlQuery
            every { tokens } returns (mapOf("id" to listOf(1)))
        }
    private val mockStatement =
        mockk<PreparedStatement>(relaxed = true)
    private val mockConnection =
        mockk<Connection>(relaxed = true) {
            every { prepareStatement(any()) } returns mockStatement
        }

    companion object {
        @AfterAll
        @JvmStatic
        fun tearDown() {
            unmockkAll()
        }
    }

    init {
        mockkStatic(Connection::executeQuery)
        mockkStatic(Connection::getDbFlavour)
        mockkStatic(ResultSet::extractFields)
        every { mockConnection.getDbFlavour() } returns DbFlavour.UNKNOWN
        every { mockQueryBuilder(mockSqlTemplate) } returns mockQuery
    }

    private val kapper = KapperImpl(mockQueryBuilder)

    @Test
    fun `processes SQL template`() {
        kapper
            .execute(
                mockConnection,
                mockSqlTemplate,
                mapOf("id" to 3),
            )
        verify { mockConnection.prepareStatement(mockSqlQuery) }
    }

    @Test
    fun `sets parameters`() {
        kapper
            .execute(
                mockConnection,
                mockSqlTemplate,
                mapOf("id" to 3),
            )
        verify { mockStatement.setParameter(1, 3, DbFlavour.UNKNOWN) }
    }

    @Test
    fun `executes statement`() {
        kapper
            .execute(
                mockConnection,
                mockSqlTemplate,
                mapOf("id" to 3),
            )
        verify { mockStatement.executeUpdate() }
    }
}
