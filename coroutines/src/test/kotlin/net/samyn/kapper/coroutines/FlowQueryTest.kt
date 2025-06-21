package net.samyn.kapper.coroutines

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import net.samyn.kapper.DbFlavour
import net.samyn.kapper.Field
import net.samyn.kapper.Kapper
import net.samyn.kapper.KapperQueryException
import net.samyn.kapper.Mapper
import net.samyn.kapper.internal.Query
import net.samyn.kapper.internal.executeQuery
import net.samyn.kapper.internal.extractFields
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException

class FlowQueryTest {
    private val fields =
        mapOf(
            "id" to Field(1, java.sql.JDBCType.INTEGER, "id", DbFlavour.UNKNOWN),
            "name" to Field(1, java.sql.JDBCType.VARCHAR, "name", DbFlavour.UNKNOWN),
        )
    private val result = Hero(1, "Superman")
    private val resultSet =
        mockk<ResultSet>(relaxed = true) {
            every { next() } returns true andThen false
        }
    private val queryTemplate = "SELECT * FROM super_heroes where id = :id"
    private val query = Query(queryTemplate)
    private val connection = mockk<Connection>(relaxed = true)
    private val mapper: (ResultSet, Map<String, Field>) -> Hero =
        mockk<(ResultSet, Map<String, Field>) -> Hero>()

    init {
        mockkStatic(Connection::executeQuery)
        mockkStatic(ResultSet::extractFields)
        every { resultSet.extractFields(any()) } returns fields
        every { connection.executeQuery(any(), any(), any()) } returns resultSet
        every { mapper.invoke(resultSet, fields) } returns result
    }

    data class Hero(val id: Int, val name: String)

    @Test
    fun `when query emit each row as a flow item`() {
        runBlocking {
            connection.queryAsFlow<Hero>(
                queryTemplate,
                mapper,
                "id" to 1,
            ).toList() shouldBe listOf(result)
        }
    }

    @Test
    fun `when query with automapper emit each row as a flow item`() {
        val mapper =
            mockk<Mapper<Hero>> {
                every { createInstance(any(), any()) } returns result
            }
        Kapper.mapperRegistry.registerIfAbsent(Hero::class.java, mapper)
        runBlocking {
            connection.queryAsFlow<Hero>(
                queryTemplate,
                "id" to 1,
            ).toList() shouldBe listOf(result)
        }
    }

    @Test
    fun `when query emit close after collection`() {
        runBlocking {
            connection.queryAsFlow<Hero>(
                queryTemplate,
                mapper,
                "id" to 1,
            ).collect {}
        }
        verify { resultSet.close() }
    }

    @Test
    fun `when except close`() {
        val ex = Exception("test")
        runBlocking {
            try {
                connection.queryAsFlow<Hero>(
                    queryTemplate,
                    mapper,
                    "id" to 1,
                ).collect {
                    throw ex
                }
            } catch (e: Exception) {
                e shouldBe ex
            }
        }
        verify { resultSet.close() }
    }

    @Test
    fun `when cancel close`() {
        // never finish the queryAsFlow
        every { resultSet.next() } returns true
        runBlocking {
            var count = 0
            val job =
                async {
                    connection.queryAsFlow<Hero>(
                        queryTemplate,
                        mapper,
                        "id" to 1,
                    ).collect {
                        // cancel after 10 items
                        count++
                        if (count > 10) cancel()
                        println("Hero $count $it")
                    }
                }
            println("Waiting to be cancelled")
            job.join()
        }
        verify { resultSet.close() }
    }

    @Test
    fun `when result throws rethrow`() {
        val ex = SQLException("test")
        every { resultSet.next() } throws ex
        runBlocking {
            shouldThrow<KapperQueryException> {
                connection.queryAsFlow<Hero>(
                    queryTemplate,
                    mapper,
                    "id" to 1,
                ).collect {}
            }.cause shouldBe ex
        }
    }

    @Test
    fun `when sql blank throw`() {
        runBlocking {
            shouldThrow<IllegalArgumentException> {
                connection.queryAsFlow<Hero>(
                    "",
                    mapper,
                    "id" to 1,
                ).collect {}
            }
        }
    }

    @Test
    fun `when fetchsize set`() {
        runBlocking {
            connection.queryAsFlow<Hero>(
                queryTemplate,
                mapper,
                "id" to 1,
                fetchSize = 10,
            ).toList()
        }
        verify { connection.executeQuery(any(), any(), 10) }
    }

    @Test
    fun `when fetchsize not set use default`() {
        runBlocking {
            connection.queryAsFlow<Hero>(
                queryTemplate,
                mapper,
                "id" to 1,
            ).toList()
        }
        verify { connection.executeQuery(any(), any(), 1000) }
    }
}
