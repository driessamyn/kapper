package net.samyn.kapper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.sql.Connection

class ConnectionTransactionTest {
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

    @Test
    fun `when rollback throws, rethrow`() {
        val exception = Exception("test")
        val rollbackException = Exception("rollback failed")
        every { connection.rollback() } throws rollbackException
        val caught =
            shouldThrow<Exception> {
                connection.withTransaction {
                    throw exception
                }
            }
        caught shouldBe exception
        caught.suppressed shouldContain rollbackException
        verify { connection.rollback() }
    }
}
