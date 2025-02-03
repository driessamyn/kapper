package net.samyn.kapper

import java.sql.Connection

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
        rollback()
        throw e
    } finally {
        autoCommit = true
    }
}
