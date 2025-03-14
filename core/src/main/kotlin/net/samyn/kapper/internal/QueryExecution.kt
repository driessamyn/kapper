@file:JvmSynthetic

package net.samyn.kapper.internal

import net.samyn.kapper.Args
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.ResultSet

val logger: Logger = LoggerFactory.getLogger("net.samyn.kapper.coroutines.QueryExecution")

fun Connection.executeQuery(
    query: Query,
    args: Args,
    // [fetch size 0 means batching is disabled](https://docs.oracle.com/en/java/javase/17/docs/api/java.sql/java/sql/Statement.html#setFetchSize(int)).
    fetchSize: Int = 0,
): ResultSet {
    this.prepareStatement(query.sql).let { stmt ->
        try {
            args.setParameters(query, stmt, this.getDbFlavour())
            logger.warn("Executing prepared statement for query: {}", stmt)
            stmt.fetchSize = fetchSize
            return CloseableResultSet(stmt.executeQuery()) {
                // cancel the statement when the ResultSet is closed and the statement isn't yet.
                if (!stmt.isClosed) stmt.cancel()
                // close the statement when the ResultSet is closed
                if (!stmt.isClosed) stmt.close()
            }
        } catch (e: Exception) {
            logger.warn("Failed to execute query: ${query.sql}", e)
            stmt.close()
            throw e
        }
    }
}

private class CloseableResultSet(
    private val resultSet: ResultSet,
    private val onClose: () -> Unit = {},
) : AutoCloseable, ResultSet by resultSet {
    override fun close() {
        resultSet.close()
        onClose()
    }
}
