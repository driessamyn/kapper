package net.samyn.kapper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.sql.Connection

class TransactionTest {
    val connection = mockk<Connection>(relaxed = true)

    @Test
    fun `set autocommit correctly`() {
        connection.withTransaction {
            verify { connection.autoCommit = false }
        }
        verify { connection.autoCommit = true }
    }

    @Test
    fun `commit on success`() {
        connection.withTransaction {
            // no-op
        }
        verify { connection.commit() }
    }

    @Test
    fun `don't commit on fail`() {
        shouldThrow<Exception> {
            connection.withTransaction {
                throw Exception("test")
            }
        }
        verify(exactly = 0) { connection.commit() }
    }

    @Test
    fun `rollback and rethrow on fail`() {
        val exception = Exception("test")
        shouldThrow<Exception> {
            connection.withTransaction {
                throw exception
            }
        } shouldBe exception
        verify { connection.rollback() }
    }
}
