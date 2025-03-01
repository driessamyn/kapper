package net.samyn.kapper

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import java.sql.Connection
import java.sql.ResultSet

// Purpose of these tests is to exercise the public API of Kapper in Kotlin.
//  additional tests of the Kapper implementation are in the KapperImplTest
@Isolated // avoid running in parallel as this test uses static mock for KapperInstance
class KapperExtensionFunctionApiTest {
    private val connection = mockk<Connection>(relaxed = true)
    private val kapperMock = mockk<Kapper>(relaxed = true)

    private val queryTemplate = "FOO"
    val mapper: (ResultSet, Map<String, Field>) -> TestEntity = { _, _ -> TestEntity(1, "foo") }

    companion object {
        @AfterAll
        @JvmStatic
        fun tearDown() {
            unmockkObject(Kapper.Companion)
        }
    }

    init {
        mockkObject(Kapper.Companion)
        every { Kapper.instance } returns kapperMock
    }

    data class TestEntity(val id: Int, val name: String)

    @Nested
    inner class Query {
        @Test
        fun query() {
            connection.query<TestEntity>(
                queryTemplate,
                "id" to 1,
            )
            verify { kapperMock.query(TestEntity::class.java, connection, queryTemplate, mapOf("id" to 1)) }
        }

        @Test
        fun `query with clazz`() {
            connection.query(
                TestEntity::class,
                queryTemplate,
                "id" to 1,
            )
            verify { kapperMock.query(TestEntity::class.java, connection, queryTemplate, mapOf("id" to 1)) }
        }

        @Test
        fun `query with custom mapper`() {
            connection.query<TestEntity>(
                queryTemplate,
                mapper,
                "id" to 1,
            )
            verify { kapperMock.query(TestEntity::class.java, connection, queryTemplate, mapper, mapOf("id" to 1)) }
        }

        @Test
        fun `query with custom mapper and clazz`() {
            connection.query(
                TestEntity::class,
                queryTemplate,
                mapper,
                mapOf("id" to 1),
            )
            verify { kapperMock.query(TestEntity::class.java, connection, queryTemplate, mapper, mapOf("id" to 1)) }
        }
    }

    @Nested
    inner class QuerySingleTests {
        @Test
        fun query() {
            connection.querySingle<TestEntity>(
                queryTemplate,
                "id" to 1,
            )
            verify { kapperMock.querySingle(TestEntity::class.java, connection, queryTemplate, mapOf("id" to 1)) }
        }

        @Test
        fun `query with clazz`() {
            connection.querySingle(
                TestEntity::class,
                queryTemplate,
                "id" to 1,
            )
            verify { kapperMock.querySingle(TestEntity::class.java, connection, queryTemplate, mapOf("id" to 1)) }
        }

        @Test
        fun `query with kotlin extension function and custom mapper`() {
            connection.querySingle<TestEntity>(
                queryTemplate,
                mapper,
                "id" to 1,
            )
            verify { kapperMock.querySingle(TestEntity::class.java, connection, queryTemplate, mapper, mapOf("id" to 1)) }
        }

        @Test
        fun `query with kotlin extension function and custom mapper and clazz`() {
            connection.querySingle(
                TestEntity::class,
                queryTemplate,
                mapper,
                "id" to 1,
            )
            verify { kapperMock.querySingle(TestEntity::class.java, connection, queryTemplate, mapper, mapOf("id" to 1)) }
        }
    }

    @Nested
    inner class ExecuteTests {
        @Test
        fun execute() {
            val result =
                connection.execute(
                    queryTemplate,
                    "id" to 1,
                    "name" to "Test1",
                )
            verify { kapperMock.execute(connection, queryTemplate, mapOf("id" to 1, "name" to "Test1")) }
        }
    }
}
