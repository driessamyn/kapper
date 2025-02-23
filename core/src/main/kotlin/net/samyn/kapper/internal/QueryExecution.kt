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
): ResultSet {
    this.prepareStatement(query.sql).let { stmt ->
        try {
            args.setParameters(query, stmt, this.getDbFlavour())
            logger.debug("Executing prepared statement for query: {}", stmt)
            return CloseableResultSet(stmt.executeQuery()) {
                // close the statement when the ResultSet is closed
                if (!stmt.isClosed) stmt.close()
            }
        } catch (e: Exception) {
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
