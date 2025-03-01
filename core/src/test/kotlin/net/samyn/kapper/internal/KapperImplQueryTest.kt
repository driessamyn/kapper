package net.samyn.kapper.internal

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import net.samyn.kapper.Field
import net.samyn.kapper.KapperQueryException
import net.samyn.kapper.KapperResultException
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import java.sql.Connection
import java.sql.JDBCType
import java.sql.ResultSet
import java.sql.SQLException

// Complements tests in KapperApiTest, targeting KapperImpl directly
@Isolated // avoid running in parallel as this test uses static mock for KapperInstance
class KapperImplQueryTest {
    // some duplication with KapperApiTest, but maybe that's ok for readability?
    private val mockSqlTemplate = "SELECT * FROM test_entity WHERE id = :id"
    private val mockSqlQuery = "MOCK SELECT FROM foo"
    private val mockQueryBuilder = mockk<(String) -> Query>(relaxed = true)
    private val mockQuery =
        mockk<Query>(relaxed = true) {
            every { sql } returns mockSqlQuery
            every { tokens } returns (mapOf("id" to listOf(1)))
        }
    private val mockResultSet = mockk<ResultSet>(relaxed = true)
    private val mockConnection = mockk<Connection>(relaxed = true)
    private val mockMapper = mockk<(ResultSet, Map<String, Field>) -> TestEntity>(relaxed = true)
    private val testFieldMeta =
        mapOf(
            "id" to Field(1, JDBCType.INTEGER, "INTEGER"),
        )

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
        every { mockConnection.executeQuery(any(), any()) } returns mockResultSet
        every { mockConnection.getDbFlavour() } returns DbFlavour.UNKNOWN
        every { mockResultSet.extractFields() } returns testFieldMeta
        every { mockMapper.invoke(any(), any()) } returns TestEntity(1, "test")
        every { mockQueryBuilder(mockSqlTemplate) } returns mockQuery
    }

    private val kapper = KapperImpl(mockQueryBuilder)

    data class TestEntity(val id: Int, val name: String)

    @Test
    fun `query executes query`() {
        every { mockResultSet.next() } returns true andThen true andThen false
        val args = mapOf("id" to 1)
        kapper
            .query(
                TestEntity::class.java,
                mockConnection,
                mockSqlTemplate,
                mockMapper,
                args,
            )
        verify { mockConnection.executeQuery(mockQuery, args) }
    }

    @Test
    fun `query closes ResultSet`() {
        every { mockResultSet.next() } returns true andThen true andThen false
        kapper
            .query(
                TestEntity::class.java,
                mockConnection,
                "SELECT * FROM test_entity WHERE id = :id",
                mockMapper,
                mapOf("id" to 3),
            )
        verify { mockResultSet.close() }
    }

    @Test
    fun `query maps results`() {
        every { mockResultSet.next() } returns true andThen true andThen false
        kapper
            .query(
                TestEntity::class.java,
                mockConnection,
                "SELECT * FROM test_entity WHERE id = :id",
                mockMapper,
                mapOf("id" to 3),
            )
        verify { mockMapper(mockResultSet, testFieldMeta) }
    }

    @Test
    fun `when query fails throw`() {
        val ex = SQLException("test")
        every { mockResultSet.next() } throws ex
        shouldThrow<KapperQueryException> {
            kapper
                .query(
                    TestEntity::class.java,
                    mockConnection,
                    "SELECT * FROM test_entity WHERE id = :id",
                    mockMapper,
                    mapOf("id" to 3),
                )
        }.cause shouldBe ex
    }

    @Test
    fun `querySingle execute query`() {
        every { mockResultSet.next() } returns true andThen false
        val args = mapOf("id" to 1)
        kapper
            .querySingle(
                TestEntity::class.java,
                mockConnection,
                mockSqlTemplate,
                mockMapper,
                args,
            )
        verify { mockConnection.executeQuery(mockQuery, args) }
    }

    @Test
    fun `querySingle close ResultSet`() {
        every { mockResultSet.next() } returns true andThen false
        kapper
            .querySingle(
                TestEntity::class.java,
                mockConnection,
                "SELECT * FROM test_entity WHERE id = :id",
                mockMapper,
                mapOf("id" to 3),
            )
        verify { mockResultSet.close() }
    }

    @Test
    fun `querySingle maps results`() {
        every { mockResultSet.next() } returns true andThen false
        kapper
            .querySingle(
                TestEntity::class.java,
                mockConnection,
                "SELECT * FROM test_entity WHERE id = :id",
                mockMapper,
                mapOf("id" to 3),
            )
        verify { mockMapper(mockResultSet, testFieldMeta) }
    }

    @Test
    fun `querySingle returns null when no results`() {
        every { mockResultSet.next() } returns false
        val result =
            kapper
                .querySingle(
                    TestEntity::class.java,
                    mockConnection,
                    "SELECT * FROM test_entity WHERE id = :id",
                    mockMapper,
                    mapOf("id" to 3),
                )
        result.shouldBe(null)
    }

    @Test
    fun `querySingle throws when multiple results`() {
        every { mockResultSet.next() } returns true andThen true andThen false
        shouldThrow<KapperResultException> {
            kapper
                .querySingle(
                    TestEntity::class.java,
                    mockConnection,
                    "SELECT * FROM test_entity WHERE id = :id",
                    mockMapper,
                    mapOf("id" to 3),
                )
        }
    }
}
