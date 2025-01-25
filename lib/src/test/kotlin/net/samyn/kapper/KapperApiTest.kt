package net.samyn.kapper

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.samyn.kapper.internal.DbConnectionUtils
import net.samyn.kapper.internal.KapperImpl
import net.samyn.kapper.internal.SQLTypesConverter.setParameter
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.ResultSetMetaData

// Purpose of these tests is to exercise the public API of Kapper in Kotlin.
//  additional tests of the Kapper implementation are in the KapperImplTest
class KapperApiTest {
    private val kapper = Kapper.getInstance()
    private val mockMetaData =
        mockk<ResultSetMetaData>(relaxed = true) {
            every { columnCount } returns 2
            every { getColumnLabel(1) } returns "id"
            every { getColumnLabel(2) } returns "name"
            every { getColumnType(1) } returns java.sql.Types.INTEGER
            every { getColumnType(2) } returns java.sql.Types.VARCHAR
            every { getColumnTypeName(1) } returns "INTEGER"
            every { getColumnTypeName(2) } returns "VARCHAR"
        }
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

    data class TestEntity(val id: Int, val name: String)

    @Test
    fun `getInstance should return a Kapper instance`() {
        Kapper.getInstance().shouldBeInstanceOf<KapperImpl>()
    }

    @Nested
    inner class QueryTests {
        init {
            every { mockResultSet.next() } returns true andThen true andThen false
            every { mockResultSet.getInt("id") } returns 1 andThen 2
            every { mockResultSet.getInt(1) } returns 1 andThen 2
            every { mockResultSet.getString("name") } returns "Test1" andThen "Test2"
            every { mockResultSet.getString(2) } returns "Test1" andThen "Test2"
        }

        @Test
        fun `query with kotlin extension function`() {
            val result =
                mockConnection.query<TestEntity>(
                    "SELECT * FROM test_table where id = :id",
                    "id" to 1,
                )
            assertQuery(result)
        }

        @Test
        fun `query with kotlin extension function and clazz`() {
            val result =
                mockConnection.query(
                    TestEntity::class,
                    "SELECT * FROM test_table where id = :id",
                    "id" to 1,
                )
            assertQuery(result)
        }

        @Test
        fun `query with kotlin extension function and custom mapper`() {
            val result =
                mockConnection.query<TestEntity>(
                    "SELECT * FROM test_table where id = :id",
                    { rs, _ ->
                        TestEntity(
                            rs.getInt("id"),
                            rs.getString("name"),
                        )
                    },
                    "id" to 1,
                )
            assertQuery(result)
        }

        @Test
        fun `query with kotlin extension function and custom mapper and clazz`() {
            val result =
                mockConnection.query(
                    TestEntity::class,
                    "SELECT * FROM test_table where id = :id",
                    { rs, _ ->
                        TestEntity(
                            rs.getInt("id"),
                            rs.getString("name"),
                        )
                    },
                    mapOf("id" to 1),
                )
            assertQuery(result)
        }

        @Test
        fun `query with kotlin map`() {
            val args = mapOf("id" to 1)

            val result =
                kapper.query(
                    TestEntity::class.java,
                    mockConnection,
                    "SELECT * FROM test_table where id = :id",
                    args,
                )
            assertQuery(result)
        }

        @Test
        fun `query with kotlin map and custom mapper`() {
            val args = mapOf("id" to 1)

            val result =
                kapper.query(
                    TestEntity::class.java,
                    mockConnection,
                    "SELECT * FROM test_table where id = :id",
                    { rs, _ ->
                        TestEntity(
                            rs.getInt("id"),
                            rs.getString("name"),
                        )
                    },
                    args,
                )
            assertQuery(result)
        }

        @Test
        fun `query with hashmap`() {
            val args =
                HashMap<String, Any?>().also {
                    it["id"] = 1
                }

            val result =
                kapper.query(
                    TestEntity::class.java,
                    mockConnection,
                    "SELECT * FROM test_table where id = :id",
                    args,
                )
            assertQuery(result)
        }

        @Test
        fun `query with hashmap and custom mapper`() {
            val args =
                HashMap<String, Any?>().also {
                    it["id"] = 1
                }

            val result =
                kapper.query(
                    TestEntity::class.java,
                    mockConnection,
                    "SELECT * FROM test_table where id = :id",
                    { rs, _ ->
                        TestEntity(
                            rs.getInt("id"),
                            rs.getString("name"),
                        )
                    },
                    args,
                )
            assertQuery(result)
        }

        private fun assertQuery(result: List<TestEntity>) {
            assertSoftly(result) {
                size.shouldBe(2)
                it[0] shouldBe (TestEntity(1, "Test1"))
                it[1] shouldBe (TestEntity(2, "Test2"))
            }
            verify { mockStatement.setParameter(1, 1, DbConnectionUtils.DbFlavour.UNKNOWN) }
            verify { mockStatement.close() }
            verify { mockResultSet.close() }
        }
    }

    @Nested
    inner class QuerySingleTests {
        init {
            every { mockResultSet.next() } returns true andThen false
            every { mockResultSet.getInt("id") } returns 1
            every { mockResultSet.getInt(1) } returns 1
            every { mockResultSet.getString("name") } returns "Test1"
            every { mockResultSet.getString(2) } returns "Test1"
        }

        @Test
        fun `query with kotlin extension function`() {
            val result =
                mockConnection.querySingle<TestEntity>(
                    "SELECT * FROM test_table where id = :id",
                    "id" to 1,
                )
            assertQuery(result)
        }

        @Test
        fun `query with kotlin extension function and clazz`() {
            val result =
                mockConnection.querySingle(
                    TestEntity::class,
                    "SELECT * FROM test_table where id = :id",
                    "id" to 1,
                )
            assertQuery(result)
        }

        @Test
        fun `query with kotlin extension function and custom mapper`() {
            val result =
                mockConnection.querySingle<TestEntity>(
                    "SELECT * FROM test_table where id = :id",
                    { rs, _ ->
                        TestEntity(
                            rs.getInt("id"),
                            rs.getString("name"),
                        )
                    },
                    "id" to 1,
                )
            assertQuery(result)
        }

        @Test
        fun `query with kotlin extension function and custom mapper and clazz`() {
            val result =
                mockConnection.querySingle(
                    TestEntity::class,
                    "SELECT * FROM test_table where id = :id",
                    { rs, _ ->
                        TestEntity(
                            rs.getInt("id"),
                            rs.getString("name"),
                        )
                    },
                    "id" to 1,
                )
            assertQuery(result)
        }

        @Test
        fun `query with kotlin map`() {
            val args = mapOf("id" to 1)

            val result =
                kapper.querySingle(
                    TestEntity::class.java,
                    mockConnection,
                    "SELECT * FROM test_table where id = :id",
                    args,
                )
            assertQuery(result)
        }

        @Test
        fun `query with kotlin map and custom mapper`() {
            val args = mapOf("id" to 1)

            val result =
                kapper.querySingle(
                    TestEntity::class.java,
                    mockConnection,
                    "SELECT * FROM test_table where id = :id",
                    { rs, _ ->
                        TestEntity(
                            rs.getInt("id"),
                            rs.getString("name"),
                        )
                    },
                    args,
                )
            assertQuery(result)
        }

        @Test
        fun `query with hashmap`() {
            val args =
                HashMap<String, Any?>().also {
                    it["id"] = 1
                }

            val result =
                kapper.querySingle(
                    TestEntity::class.java,
                    mockConnection,
                    "SELECT * FROM test_table where id = :id",
                    args,
                )
            assertQuery(result)
        }

        @Test
        fun `query with hashmap and custom mapper`() {
            val args =
                HashMap<String, Any?>().also {
                    it["id"] = 1
                }

            val result =
                kapper.querySingle(
                    TestEntity::class.java,
                    mockConnection,
                    "SELECT * FROM test_table where id = :id",
                    { rs, _ ->
                        TestEntity(
                            rs.getInt("id"),
                            rs.getString("name"),
                        )
                    },
                    args,
                )
            assertQuery(result)
        }

        private fun assertQuery(result: TestEntity?) {
            result shouldBe (TestEntity(1, "Test1"))
            verify { mockStatement.setParameter(1, 1, DbConnectionUtils.DbFlavour.UNKNOWN) }
            verify { mockStatement.close() }
            verify { mockResultSet.close() }
        }
    }

    @Nested
    inner class ExecuteTests {
        init {
            every { mockStatement.executeUpdate() } returns 1
        }

        @Test
        fun `execute with kotlin extension function`() {
            val result =
                mockConnection.execute(
                    "INSERT INTO test_table(id, name) VALUES(:id, :name)",
                    "id" to 1,
                    "name" to "Test1",
                )
            assertResult(result)
        }

        @Test
        fun `execute with kotlin map`() {
            val args = mapOf("id" to 1, "name" to "Test1")

            val result =
                kapper.execute(
                    mockConnection,
                    "INSERT INTO test_table(id, name) VALUES(:id, :name)",
                    args,
                )
            assertResult(result)
        }

        @Test
        fun `execute with hashmap`() {
            val args =
                HashMap<String, Any?>().also {
                    it["id"] = 1
                    it["name"] = "Test1"
                }

            val result =
                kapper.execute(
                    mockConnection,
                    "INSERT INTO test_table(id, name) VALUES(:id, :name)",
                    args,
                )
            assertResult(result)
        }

        private fun assertResult(result: Int) {
            result shouldBe (1)
            verify { mockStatement.setParameter(1, 1, DbConnectionUtils.DbFlavour.UNKNOWN) }
            verify { mockStatement.setParameter(2, "Test1", DbConnectionUtils.DbFlavour.UNKNOWN) }
            verify { mockStatement.close() }
        }
    }
}
