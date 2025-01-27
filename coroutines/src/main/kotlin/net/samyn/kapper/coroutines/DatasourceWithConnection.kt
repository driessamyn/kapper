package net.samyn.kapper.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import javax.sql.DataSource
import kotlin.coroutines.CoroutineContext

/**
 * create a DB connection and execute query/aueries using the `Dispatchers.IO` CoroutineScope.
 * The connection is closed after the block is executed.
 * @param block The block of code to execute.
 */
suspend inline fun <T> DataSource.withConnection(crossinline block: suspend (Connection) -> T): T = withConnection(Dispatchers.IO, block)

/**
 * create a DB connection and execute query/aueries using the given CoroutineScope.
 * The connection is closed after the block is executed.
 * @param context The CoroutineContext to use.
 * @param block The block of code to execute.
 */
suspend inline fun <T> DataSource.withConnection(
    context: CoroutineContext,
    crossinline block: suspend (Connection) -> T,
): T {
    this.connection.use { connection ->
        return withContext(context) {
            block(connection)
        }
    }
}
