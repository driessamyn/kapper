package net.samyn.kapper

import java.sql.Connection
import javax.sql.DataSource

/**
 * Execute a block of code within a DB transaction.
 * Attempt to commit the transaction and roll back in case of an exception.
 */
inline fun Connection.withTransaction(block: Connection.() -> Unit) {
    autoCommit = false
    try {
        block()
        commit()
    } catch (e: Exception) {
        try {
            rollback()
        } catch (rollbackException: Exception) {
            e.addSuppressed(rollbackException)
        }
        throw e
    } finally {
        autoCommit = true
    }
}

/**
 * Execute a block of code within a DB transaction.
 * Attempt to commit the transaction and roll back in case of an exception.
 * The function will create a new connection from the data source and ensure
 * it is closed when the code block completes
 */
inline fun DataSource.withTransaction(block: (Connection) -> Unit) {
    this.connection.use { it.withTransaction(block) }
}
