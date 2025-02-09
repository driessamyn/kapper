package net.samyn.kapper.coroutines

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.samyn.kapper.Field
import net.samyn.kapper.internal.executeQuery
import net.samyn.kapper.internal.extractFields
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.ResultSet

class FlowQueryTest {
    init {
        mockkStatic(Connection::executeQuery)
        mockkStatic(ResultSet::extractFields)
    }

    private val fields =
        mapOf(
            "id" to Field(1, java.sql.JDBCType.INTEGER, "id"),
        )
    private val result = Hero(1, "Superman")
    private val resultSet =
        mockk<ResultSet>(relaxed = true) {
            every { next() } returns true andThen false
        }.also {

            every { it.extractFields() } returns fields
        }
    private val connection =
        mockk<Connection>(relaxed = true).also {
            every {
                it.executeQuery("SELECT * FROM super_heroes where id = :id", mapOf("id" to 1))
            } returns resultSet
        }
    private val mapper: (ResultSet, Map<String, Field>) -> Hero =
        mockk<(ResultSet, Map<String, Field>) -> Hero>().also {
            every { it.invoke(resultSet, fields) } returns result
        }

    data class Hero(val id: Int, val name: String)

    @Test
    fun `when query emit each row as a flow item`() {
        runTest {
            connection.query<Hero>(
                "SELECT * FROM super_heroes where id = :id",
                mapper,
                "id" to 1,
            ).toList() shouldBe listOf(Hero(1, "Superman"))
        }
    }

    @Test
    fun `when query emit close after collection`() {
        runTest {
            connection.query<Hero>(
                "SELECT * FROM super_heroes where id = :id",
                mapper,
                "id" to 1,
            ).collect {}
        }
        verify { resultSet.close() }
    }

    @Test
    fun `when except close`() {
        val ex = Exception("test")
        runTest {
            try {
                connection.query<Hero>(
                    "SELECT * FROM super_heroes where id = :id",
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
        // never finish the query
        every { resultSet.next() } returns true
        runTest {
            var count = 0
            val job =
                async {
                    connection.query<Hero>(
                        "SELECT * FROM super_heroes where id = :id",
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
}
