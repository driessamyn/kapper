package net.samyn.kapper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test
import java.sql.Connection

class TransactionFunTest {
    private fun makeConnection(initialAutoCommit: Boolean = true): Connection =
        mockk<Connection>(relaxed = true) {
            every { autoCommit } returns initialAutoCommit
        }

    @Test
    fun `set autocommit to false and restore to true when initially true`() {
        val connection = makeConnection(initialAutoCommit = true)
        connection.withTransaction {
            // no-op
        }
        verifyOrder {
            connection.autoCommit = false
            connection.autoCommit = true
        }
    }

    @Test
    fun `restore autocommit to false when it was false before transaction`() {
        val connection = makeConnection(initialAutoCommit = false)
        connection.withTransaction {
            // no-op
        }
        verifyOrder {
            connection.autoCommit = false
            connection.autoCommit = false
        }
        // ensure autoCommit is never set to true
        verify(exactly = 0) { connection.autoCommit = true }
    }

    @Test
    fun `commit on success`() {
        val connection = makeConnection()
        connection.withTransaction {
            // no-op
        }
        verify { connection.commit() }
    }

    @Test
    fun `don't commit on fail`() {
        val connection = makeConnection()
        shouldThrow<Exception> {
            connection.withTransaction {
                throw Exception("test")
            }
        }
        verify(exactly = 0) { connection.commit() }
    }

    @Test
    fun `rollback and rethrow on fail`() {
        val connection = makeConnection()
        val exception = Exception("test")
        shouldThrow<Exception> {
            connection.withTransaction {
                throw exception
            }
        } shouldBe exception
        verify { connection.rollback() }
    }

    @Test
    fun `when rollback throws, rethrow with suppressed`() {
        val connection = makeConnection()
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

    @Test
    fun `restore autocommit to true even when exception is thrown`() {
        val connection = makeConnection(initialAutoCommit = true)
        shouldThrow<Exception> {
            connection.withTransaction {
                throw Exception("test")
            }
        }
        verify { connection.autoCommit = true }
    }

    @Test
    fun `restore autocommit to false even when exception is thrown`() {
        val connection = makeConnection(initialAutoCommit = false)
        shouldThrow<Exception> {
            connection.withTransaction {
                throw Exception("test")
            }
        }
        verify(exactly = 0) { connection.autoCommit = true }
        verify { connection.autoCommit = false }
    }

    @Test
    fun `when restore autocommit throws during failure, add as suppressed`() {
        val connection = makeConnection(initialAutoCommit = true)
        val exception = Exception("test")
        val restoreException = RuntimeException("restore failed")
        // Allow the first set (autoCommit = false), then throw on restore (autoCommit = true)
        every { connection.autoCommit = false } returns Unit
        every { connection.autoCommit = true } throws restoreException
        val caught =
            shouldThrow<Exception> {
                connection.withTransaction {
                    throw exception
                }
            }
        caught shouldBe exception
        caught.suppressed shouldContain restoreException
    }

    @Test
    fun `when restore autocommit throws on success, propagate restore exception`() {
        val connection = makeConnection(initialAutoCommit = true)
        val restoreException = RuntimeException("restore failed")
        every { connection.autoCommit = false } returns Unit
        every { connection.autoCommit = true } throws restoreException
        val caught =
            shouldThrow<RuntimeException> {
                connection.withTransaction {
                    // no-op — success path
                }
            }
        caught shouldBe restoreException
    }
}
