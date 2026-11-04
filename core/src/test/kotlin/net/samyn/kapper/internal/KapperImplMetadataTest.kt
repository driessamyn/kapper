package net.samyn.kapper.internal

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import net.samyn.kapper.DbFlavour
import net.samyn.kapper.Field
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.JDBCType
import java.sql.ResultSet

// A single query resolves the DbFlavour (and therefore reads the connection metadata) at most once.
// getDbFlavour is deliberately NOT mocked here so the real implementation runs.
@Isolated
class KapperImplMetadataTest {
    private val resultSet = mockk<ResultSet>(relaxed = true)
    private val mapper = mockk<(ResultSet, Map<String, Field>) -> TestEntity>(relaxed = true)
    private val fields = mapOf("id" to Field(1, JDBCType.INTEGER, "INTEGER", DbFlavour.UNKNOWN))

    companion object {
        @AfterAll
        @JvmStatic
        fun tearDown() = unmockkAll()
    }

    init {
        mockkStatic(Connection::executeQuery)
        mockkStatic(ResultSet::extractFields)
        every { resultSet.extractFields(any()) } returns fields
        every { resultSet.next() } returns false
    }

    data class TestEntity(val id: Int, val name: String)

    private fun connectionWithProductName(productName: String): Connection {
        val dbMetaData =
            mockk<DatabaseMetaData>(relaxed = true) {
                every { databaseProductName } returns productName
            }
        val connection = mockk<Connection>(relaxed = true)
        every { connection.metaData } returns dbMetaData
        every { connection.executeQuery(any(), any(), any(), any()) } returns resultSet
        return connection
    }

    @Test
    fun `query reads connection metadata at most once`() {
        val connection = connectionWithProductName("PostgreSQL")
        KapperImpl().query(TestEntity::class.java, connection, "SELECT 1", mapper, emptyMap<String, Any?>())
        verify(exactly = 1) { connection.metaData }
    }

    @Test
    fun `querySingle reads connection metadata at most once`() {
        val connection = connectionWithProductName("PostgreSQL")
        KapperImpl().querySingle(TestEntity::class.java, connection, "SELECT 1", mapper, emptyMap<String, Any?>())
        verify(exactly = 1) { connection.metaData }
    }

    @Test
    fun `executeReturning reads connection metadata at most once`() {
        val connection = connectionWithProductName("PostgreSQL")
        KapperImpl().executeReturning(TestEntity::class.java, connection, "SELECT 1", mapper, emptyMap<String, Any?>())
        verify(exactly = 1) { connection.metaData }
    }
}
