package net.samyn.kapper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.samyn.kapper.internal.DbConnectionUtils
import net.samyn.kapper.internal.KapperImpl
import net.samyn.kapper.internal.Query
import net.samyn.kapper.internal.SQLTypesConverter.setParameter
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.ResultSetMetaData

// Complements tests in KapperApiTest, targeting KapperImpl directly
class KapperImplTest {
    // some duplication with KapperApiTest, but maybe that's ok for readability?
    private val mockSqlTemplate = "SELECT * FROM test_entity WHERE id = :id"
    private val mockSqlQuery = "MOCK SELECT FROM foo"
    private val mockQueryBuilder = mockk<(String) -> Query>(relaxed = true)
    private val mockQuery =
        mockk<Query>(relaxed = true) {
            every { sql } returns mockSqlQuery
            every { tokens } returns (mapOf("id" to listOf(1)))
        }
    private val mockMetaData =
        mockk<ResultSetMetaData>(relaxed = true)
    private val mockResultSet =
        mockk<ResultSet>(relaxed = true) {
            every { metaData } returns mockMetaData
        }
    private val mockStatement =
        mockk<PreparedStatement>(relaxed = true) {
            every { executeQuery() } returns mockResultSet
        }
    private val mockConnection =
        mockk<Connection>(relaxed = true) {
            every { prepareStatement(any()) } returns mockStatement
            every { metaData } returns
                mockk(relaxed = true) {
                    every { databaseProductName } returns "driesDB"
                }
        }
    private val mockMapper = mockk<(ResultSet, Map<String, Field>) -> TestEntity>(relaxed = true)
    private val mockFieldExtractor = mockk<(ResultSet) -> Map<String, Field>>(relaxed = true)
    private val testFieldMeta =
        mapOf(
            "id" to Field(1, java.sql.JDBCType.INTEGER, "INTEGER"),
        )

    private val kapper = KapperImpl(mockQueryBuilder, mockFieldExtractor)

    init {
        every { mockMapper.invoke(any(), any()) } returns TestEntity(1, "test")
        every { mockQueryBuilder(mockSqlTemplate) } returns mockQuery
        every { mockFieldExtractor(mockResultSet) } returns testFieldMeta
    }

    data class TestEntity(val id: Int, val name: String)

    @Nested
    inner class QueryTest {
        init {
            every { mockResultSet.next() } returns true andThen true andThen false
        }

        @Test
        fun `processes SQL template`() {
            kapper
                .query(
                    TestEntity::class.java,
                    mockConnection,
                    "SELECT * FROM test_entity WHERE id = :id",
                    mockMapper,
                    mapOf("id" to 1),
                )
            verify { mockConnection.prepareStatement(mockSqlQuery) }
        }

        @Test
        fun `sets parameters`() {
            kapper
                .query(
                    TestEntity::class.java,
                    mockConnection,
                    "SELECT * FROM test_entity WHERE id = :id",
                    mockMapper,
                    mapOf("id" to 3),
                )
            verify { mockStatement.setParameter(1, 3, DbConnectionUtils.DbFlavour.UNKNOWN) }
        }

        @Test
        fun `executes statement`() {
            kapper
                .query(
                    TestEntity::class.java,
                    mockConnection,
                    "SELECT * FROM test_entity WHERE id = :id",
                    mockMapper,
                    mapOf("id" to 3),
                )
            verify { mockStatement.executeQuery() }
        }

        @Test
        fun `maps results`() {
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
    }

    @Nested
    inner class QuerySingleTest {
        init {
            every { mockResultSet.next() } returns true andThen false
        }

        @Test
        fun `processes SQL template`() {
            kapper
                .querySingle(
                    TestEntity::class.java,
                    mockConnection,
                    "SELECT * FROM test_entity WHERE id = :id",
                    mockMapper,
                    mapOf("id" to 1),
                )
            verify { mockConnection.prepareStatement(mockSqlQuery) }
        }

        @Test
        fun `sets parameters`() {
            kapper
                .querySingle(
                    TestEntity::class.java,
                    mockConnection,
                    "SELECT * FROM test_entity WHERE id = :id",
                    mockMapper,
                    mapOf("id" to 3),
                )
            verify { mockStatement.setParameter(1, 3, DbConnectionUtils.DbFlavour.UNKNOWN) }
        }

        @Test
        fun `executes statement`() {
            kapper
                .querySingle(
                    TestEntity::class.java,
                    mockConnection,
                    "SELECT * FROM test_entity WHERE id = :id",
                    mockMapper,
                    mapOf("id" to 3),
                )
            verify { mockStatement.executeQuery() }
        }

        @Test
        fun `maps results`() {
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
        fun `returns null when no results`() {
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
        fun `throw when multiple results`() {
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

    @Nested
    inner class ExecuteTest {
        @Test
        fun `processes SQL template`() {
            kapper
                .execute(
                    mockConnection,
                    "SELECT * FROM test_entity WHERE id = :id",
                    mapOf("id" to 3),
                )
            verify { mockConnection.prepareStatement(mockSqlQuery) }
        }

        @Test
        fun `sets parameters`() {
            kapper
                .execute(
                    mockConnection,
                    "SELECT * FROM test_entity WHERE id = :id",
                    mapOf("id" to 3),
                )
            verify { mockStatement.setParameter(1, 3, DbConnectionUtils.DbFlavour.UNKNOWN) }
        }

        @Test
        fun `executes statement`() {
            kapper
                .execute(
                    mockConnection,
                    "SELECT * FROM test_entity WHERE id = :id",
                    mapOf("id" to 3),
                )
            verify { mockStatement.executeUpdate() }
        }
    }
}
