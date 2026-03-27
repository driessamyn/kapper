package net.samyn.kapper.internal

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import net.samyn.kapper.DbFlavour
import net.samyn.kapper.Field
import net.samyn.kapper.KapperQueryException
import net.samyn.kapper.internal.automapper.setParameter
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import java.sql.Connection
import java.sql.JDBCType
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

// Complements tests in KapperApiTest, targeting KapperImpl directly
@Isolated // avoid running in parallel as this test uses static mock for KapperInstance
class KapperImplExecuteReturningTest {
    private val mockSqlTemplate = "INSERT INTO foo (name) VALUES (:name) RETURNING *"
    private val mockSqlQuery = "MOCK INSERT INTO foo"
    private val mockQueryBuilder = mockk<(String) -> Query>(relaxed = true)
    private val mockQuery =
        mockk<Query>(relaxed = true) {
            every { sql } returns mockSqlQuery
            every { tokens } returns mapOf("name" to listOf(1))
        }
    private val mockResultSet = mockk<ResultSet>(relaxed = true)
    private val mockStatement =
        mockk<PreparedStatement>(relaxed = true) {
            every { executeQuery() } returns mockResultSet
        }
    private val mockConnection =
        mockk<Connection>(relaxed = true) {
            every { prepareStatement(any()) } returns mockStatement
        }
    private val mockMapper = mockk<(ResultSet, Map<String, Field>) -> TestEntity>(relaxed = true)
    private val testFieldMeta =
        mapOf(
            "id" to Field(1, JDBCType.INTEGER, "INTEGER", DbFlavour.UNKNOWN),
            "name" to Field(2, JDBCType.VARCHAR, "VARCHAR", DbFlavour.UNKNOWN),
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
        every { mockResultSet.extractFields(any()) } returns testFieldMeta
        every { mockMapper.invoke(any(), any()) } returns TestEntity(1, "test")
        every { mockQueryBuilder(mockSqlTemplate) } returns mockQuery
    }

    private val kapper = KapperImpl(mockQueryBuilder)

    data class TestEntity(val id: Int, val name: String)

    // --- map-args variant ---

    @Test
    fun `executeReturning executes query`() {
        every { mockResultSet.next() } returns true andThen false
        val args = mapOf("name" to "Superman")
        kapper.executeReturning(
            TestEntity::class.java,
            mockConnection,
            mockSqlTemplate,
            mockMapper,
            args,
        )
        verify { mockConnection.executeQuery(mockQuery, args) }
    }

    @Test
    fun `executeReturning closes ResultSet`() {
        every { mockResultSet.next() } returns false
        kapper.executeReturning(
            TestEntity::class.java,
            mockConnection,
            mockSqlTemplate,
            mockMapper,
            mapOf("name" to "Superman"),
        )
        verify { mockResultSet.close() }
    }

    @Test
    fun `executeReturning maps results`() {
        every { mockResultSet.next() } returns true andThen false
        kapper.executeReturning(
            TestEntity::class.java,
            mockConnection,
            mockSqlTemplate,
            mockMapper,
            mapOf("name" to "Superman"),
        )
        verify { mockMapper(mockResultSet, testFieldMeta) }
    }

    @Test
    fun `executeReturning returns all mapped rows`() {
        val entity1 = TestEntity(1, "Superman")
        val entity2 = TestEntity(2, "Batman")
        every { mockResultSet.next() } returns true andThen true andThen false
        every { mockMapper.invoke(any(), any()) } returnsMany listOf(entity1, entity2)
        val result =
            kapper.executeReturning(
                TestEntity::class.java,
                mockConnection,
                mockSqlTemplate,
                mockMapper,
                mapOf("name" to "Superman"),
            )
        result shouldBe listOf(entity1, entity2)
    }

    @Test
    fun `executeReturning returns empty list when no rows`() {
        every { mockResultSet.next() } returns false
        val result =
            kapper.executeReturning(
                TestEntity::class.java,
                mockConnection,
                mockSqlTemplate,
                mockMapper,
                mapOf("name" to "Superman"),
            )
        result shouldBe emptyList()
    }

    @Test
    fun `executeReturning throws when query fails`() {
        val ex = SQLException("test")
        every { mockResultSet.next() } throws ex
        shouldThrow<KapperQueryException> {
            kapper.executeReturning(
                TestEntity::class.java,
                mockConnection,
                mockSqlTemplate,
                mockMapper,
                mapOf("name" to "Superman"),
            )
        }.cause shouldBe ex
    }

    @Test
    fun `executeReturning throws when sql is blank`() {
        shouldThrow<IllegalArgumentException> {
            kapper.executeReturning(
                TestEntity::class.java,
                mockConnection,
                "",
                mockMapper,
                mapOf("name" to "Superman"),
            )
        }
    }

    // --- object-args variant ---

    @Test
    fun `executeReturning with object processes SQL template`() {
        every { mockResultSet.next() } returns false
        val obj = TestEntity(1, "Superman")
        val args = mapOf("name" to { e: TestEntity -> e.name })
        kapper.executeReturning(
            TestEntity::class.java,
            mockConnection,
            mockSqlTemplate,
            mockMapper,
            obj,
            args,
        )
        verify { mockConnection.prepareStatement(mockSqlQuery) }
    }

    @Test
    fun `executeReturning with object sets parameters`() {
        every { mockResultSet.next() } returns false
        val obj = TestEntity(1, "Superman")
        val args = mapOf("name" to { e: TestEntity -> e.name })
        kapper.executeReturning(
            TestEntity::class.java,
            mockConnection,
            mockSqlTemplate,
            mockMapper,
            obj,
            args,
        )
        verify { mockStatement.setParameter(1, "Superman", DbFlavour.UNKNOWN) }
    }

    @Test
    fun `executeReturning with object uses executeQuery`() {
        every { mockResultSet.next() } returns false
        val obj = TestEntity(1, "Superman")
        val args = mapOf("name" to { e: TestEntity -> e.name })
        kapper.executeReturning(
            TestEntity::class.java,
            mockConnection,
            mockSqlTemplate,
            mockMapper,
            obj,
            args,
        )
        verify { mockStatement.executeQuery() }
    }

    @Test
    fun `executeReturning with object maps results`() {
        every { mockResultSet.next() } returns true andThen false
        val obj = TestEntity(1, "Superman")
        val args = mapOf("name" to { e: TestEntity -> e.name })
        kapper.executeReturning(
            TestEntity::class.java,
            mockConnection,
            mockSqlTemplate,
            mockMapper,
            obj,
            args,
        )
        verify { mockMapper(mockResultSet, testFieldMeta) }
    }

    @Test
    fun `executeReturning with object returns mapped rows`() {
        val entity = TestEntity(1, "Superman")
        every { mockResultSet.next() } returns true andThen false
        every { mockMapper.invoke(any(), any()) } returns entity
        val result =
            kapper.executeReturning(
                TestEntity::class.java,
                mockConnection,
                mockSqlTemplate,
                mockMapper,
                TestEntity(1, "Superman"),
                mapOf("name" to { e: TestEntity -> e.name }),
            )
        result shouldBe listOf(entity)
    }

    @Test
    fun `executeReturning with object throws when sql is blank`() {
        shouldThrow<IllegalArgumentException> {
            kapper.executeReturning(
                TestEntity::class.java,
                mockConnection,
                "",
                mockMapper,
                TestEntity(1, "Superman"),
                mapOf("name" to { e: TestEntity -> e.name }),
            )
        }
    }

    @Test
    fun `executeReturning with object throws when statement fails`() {
        val ex = SQLException("test")
        every { mockResultSet.next() } throws ex
        shouldThrow<KapperQueryException> {
            kapper.executeReturning(
                TestEntity::class.java,
                mockConnection,
                mockSqlTemplate,
                mockMapper,
                TestEntity(1, "Superman"),
                mapOf("name" to { e: TestEntity -> e.name }),
            )
        }.cause shouldBe ex
    }
}
