package net.samyn.kapper.internal

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import net.samyn.kapper.Args
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import java.sql.Connection
import java.sql.PreparedStatement

@Isolated // avoid running in parallel as this test uses static mock for KapperInstance
class QueryExecutionTest {
    private val statement = mockk<PreparedStatement>(relaxed = true)
    private val connection =
        mockk<Connection>(relaxed = true) {
            every { prepareStatement(any()) } returns statement
        }
    private val query =
        mockk<Query>(relaxed = true) {
            every { sql } returns "SELECT * FROM table"
            every { tokens } returns mapOf("arg1" to listOf(1), "arg2" to listOf(2))
        }
    private val args = mockk<Args>(relaxed = true)

    companion object {
        @AfterAll
        @JvmStatic
        fun tearDown() {
            unmockkAll()
        }
    }

    init {
        mockkStatic(Connection::getDbFlavour)
        mockkStatic(Args::setParameters)
        every { connection.getDbFlavour() } returns DbFlavour.UNKNOWN
    }

    @Test
    fun `when executeQuery prepare statement`() {
        connection.executeQuery(query, args)
        verify { connection.prepareStatement("SELECT * FROM table") }
    }

    @Test
    fun `when executeQuery set params`() {
        connection.executeQuery(query, args)
        verify { args.setParameters(query, statement, DbFlavour.UNKNOWN) }
    }

    @Test
    fun `when executeQuery execute`() {
        connection.executeQuery(query, args)
        verify { statement.executeQuery() }
    }

    @Test
    fun `when executeQuery close statement when ResultSet closed`() {
        val rs = connection.executeQuery(query, args)
        rs.close()
        verify { statement.close() }
    }

    @Test
    fun `when executeQuery do not close statement if already closed`() {
        every { statement.isClosed } returns true
        val rs = connection.executeQuery(query, args)
        rs.close()
        verify(exactly = 0) { statement.close() }
    }

    @Test
    fun `when exception close statement`() {
        val ex = Exception("test")
        every { args.setParameters(any(), any(), any()) } throws ex
        shouldThrow<Exception> {
            connection.executeQuery(query, args)
        }.shouldBe(ex)
        verify { statement.close() }
    }
}
