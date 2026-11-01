package net.samyn.kapper.coroutines

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

/**
 * Exercises the real [net.samyn.kapper.internal.executeQuery] (rather than a static mock) so that
 * the statement/result-set lifecycle of the flow can be asserted end to end.
 *
 * `Connection.executeQuery` returns a result set whose `close()` also cancels and closes the
 * underlying statement, so closing the result set is what closes both resources.
 */
@Isolated // uses the real executeQuery, which other tests static-mock
class FlowQueryResourceTest {
    private val result = Hero(1, "Superman")
    private val queryTemplate = "SELECT * FROM super_heroes where id = :id"

    data class Hero(val id: Int, val name: String)

    private class Mocks(
        val connection: Connection,
        val statement: PreparedStatement,
        val resultSet: ResultSet,
    )

    private fun mockConnection(): Mocks {
        val statement = mockk<PreparedStatement>(relaxed = true)
        val resultSet = mockk<ResultSet>(relaxed = true)
        val connection =
            mockk<Connection>(relaxed = true) {
                every { prepareStatement(any()) } returns statement
            }
        every { statement.executeQuery() } returns resultSet
        every { statement.isClosed } returns false
        return Mocks(connection, statement, resultSet)
    }

    @Test
    fun `when flow is never collected no statement or result set is opened`() {
        val mocks = mockConnection()

        mocks.connection.queryAsFlow<Hero>(queryTemplate, { _, _ -> result })

        verify(exactly = 0) { mocks.connection.prepareStatement(any()) }
        verify(exactly = 0) { mocks.statement.executeQuery() }
        verify(exactly = 0) { mocks.resultSet.close() }
    }

    @Test
    fun `when cancelled mid collection closes result set and statement`() {
        val mocks = mockConnection()
        every { mocks.resultSet.next() } returns true

        runBlocking {
            var count = 0
            val job =
                async {
                    mocks.connection
                        .queryAsFlow<Hero>(queryTemplate, { _, _ -> result })
                        .collect {
                            count++
                            if (count > 10) cancel()
                        }
                }
            job.join()
        }

        verify { mocks.resultSet.close() }
        verify { mocks.statement.cancel() }
        verify { mocks.statement.close() }
    }
}
