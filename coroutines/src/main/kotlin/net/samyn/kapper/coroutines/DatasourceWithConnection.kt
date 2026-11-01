package net.samyn.kapper.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.sql.Connection
import javax.sql.DataSource
import kotlin.coroutines.CoroutineContext

/**
 * create a DB connection and execute query/queries using the `Dispatchers.IO` CoroutineScope.
 * The connection is closed after the block is executed.
 *
 * The connection is acquired and closed on the given dispatcher, and must not escape the block:
 * once the block returns, the connection is closed and must not be used again.
 * @param block The block of code to execute.
 */
suspend inline fun <T> DataSource.withConnection(crossinline block: suspend (Connection) -> T): T = withConnection(Dispatchers.IO, block)

/**
 * create a DB connection and execute query/queries using the given CoroutineScope.
 * The connection is closed after the block is executed.
 *
 * Both acquiring the connection and closing it happen on [context], so a blocking pool checkout
 * never runs on the caller's thread. The connection is owned by the block and must not escape it:
 * once the block returns, the connection is closed and must not be used again.
 * @param context The CoroutineContext to use.
 * @param block The block of code to execute.
 */
suspend inline fun <T> DataSource.withConnection(
    context: CoroutineContext,
    crossinline block: suspend (Connection) -> T,
): T {
    var connection: Connection? = null
    return try {
        // Acquire inside the same scope as the block: if the caller is cancelled as this
        // `withContext` resumes, the connection is already recorded and the `finally` still
        // closes it. Acquiring in a separate `withContext` would let the result be discarded.
        withContext(context) {
            val conn = this@withConnection.connection
            connection = conn
            block(conn)
        }
    } finally {
        // Close on the dispatcher too, and even if the block was cancelled.
        connection?.let { withContext(context + NonCancellable) { it.close() } }
    }
}
