package net.samyn.kapper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.sql.Connection
import javax.sql.DataSource

class DataSourceTransactionTest {
    private val connectionMock = mockk<Connection>(relaxed = true)
    private val dataSourceMock =
        mockk<DataSource>(relaxed = true) {
            every { connection } returns connectionMock
        }

    @Test
    fun `set autocommit correctly`() {
        dataSourceMock.withTransaction {
            verify { connectionMock.autoCommit = false }
        }
        verify(exactly = 1) { dataSourceMock.connection }
        verify { connectionMock.autoCommit = true }
        verify { connectionMock.close() }
    }

    @Test
    fun `commit on success`() {
        dataSourceMock.withTransaction {
            // no-op
        }
        verify { connectionMock.commit() }
        verify { connectionMock.close() }
    }

    @Test
    fun `don't commit on fail`() {
        shouldThrow<Exception> {
            dataSourceMock.withTransaction {
                throw Exception("test")
            }
        }
        verify(exactly = 0) { connectionMock.commit() }
        verify { connectionMock.close() }
    }

    @Test
    fun `rollback and rethrow on fail`() {
        val exception = Exception("test")
        shouldThrow<Exception> {
            dataSourceMock.withTransaction {
                throw exception
            }
        } shouldBe exception
        verify { connectionMock.rollback() }
        verify { connectionMock.close() }
    }
}
