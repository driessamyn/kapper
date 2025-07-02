package net.samyn.kapper.internal

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifySequence
import net.samyn.kapper.DbFlavour
import net.samyn.kapper.internal.automapper.setParameter
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
    private val mockSqlTemplate = "UPDATE foo set name = :name WHERE id = :id"
    private val mockSqlQuery = "MOCK UPDATE foo"
    private val mockQueryBuilder = mockk<(String) -> Query>(relaxed = true)
    private val mockQuery =
        mockk<Query>(relaxed = true) {
            every { sql } returns mockSqlQuery
            every { tokens } returns (
                mapOf(
                    "name" to listOf(1),
                    "id" to listOf(2),
                )
            )
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
                mapOf(
                    "id" to 3,
                    "name" to "Bob",
                ),
            )
        verify {
            mockStatement.setParameter(1, "Bob", DbFlavour.UNKNOWN)
            mockStatement.setParameter(2, 3, DbFlavour.UNKNOWN)
        }
    }

    @Test
    fun `executes statement`() {
        kapper
            .execute(
                mockConnection,
                mockSqlTemplate,
                mapOf(
                    "id" to 3,
                    "name" to "Bob",
                ),
            )
        verify { mockStatement.executeUpdate() }
    }

    data class User(val id: Int, val name: String)

    @Test
    fun `execute with object mapping sets parameters and executes`() {
        val user = User(1, "Alice")
        val args =
            mapOf(
                "id" to { u: User -> u.id },
                "name" to { u: User -> u.name },
            )
        every { mockStatement.executeUpdate() } returns 1
        val result = kapper.execute(User::class.java, mockConnection, mockSqlTemplate, user, args)
        verify {
            mockStatement.setParameter(1, "Alice", DbFlavour.UNKNOWN)
            mockStatement.setParameter(2, 1, DbFlavour.UNKNOWN)
            mockStatement.executeUpdate()
        }
        result shouldBe 1
    }

    @Test
    fun `executeAll with object mapping sets parameters for each object and executes batch`() {
        val users =
            listOf(
                User(1, "Alice"),
                User(2, "Bob"),
            )
        val args =
            mapOf(
                "id" to { u: User -> u.id },
                "name" to { u: User -> u.name },
            )
        every { mockStatement.executeBatch() } returns intArrayOf(1, 1)
        val result = kapper.executeAll(User::class.java, mockConnection, mockSqlTemplate, users, args)
        verifySequence {
            mockStatement.setParameter(2, 1, DbFlavour.UNKNOWN)
            mockStatement.setParameter(1, "Alice", DbFlavour.UNKNOWN)
            mockStatement.addBatch()
            mockStatement.setParameter(2, 2, DbFlavour.UNKNOWN)
            mockStatement.setParameter(1, "Bob", DbFlavour.UNKNOWN)
            mockStatement.addBatch()
            mockStatement.executeBatch()
            mockStatement.close()
        }
        result shouldBe intArrayOf(1, 1)
    }
}
