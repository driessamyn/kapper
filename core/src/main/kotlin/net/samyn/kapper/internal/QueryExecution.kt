@file:JvmSynthetic

package net.samyn.kapper.internal

import net.samyn.kapper.Args
import net.samyn.kapper.internal.DbConnectionUtils.getDbFlavour
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.ResultSet

val logger: Logger = LoggerFactory.getLogger("net.samyn.kapper.coroutines")

fun Connection.executeQuery(
    sql: String,
    args: Args,
): ResultSet {
    // TODO: cache query
    val query = Query(sql)
    this.prepareStatement(query.sql).let { stmt ->
        args.setParameters(query, stmt, this.getDbFlavour())
        logger.debug("Executing prepared statement for query: {}", stmt)
        // TODO: cache data
        // TODO: cash fields (persist in query?)
        return CloseableResultSet(stmt.executeQuery()) {
            // close the statement when the ResultSet is closed
            if (!stmt.isClosed) stmt.close()
        }
    }
}

private class CloseableResultSet(
    private val resultSet: ResultSet,
    private val onClose: () -> Unit = {},
) : AutoCloseable, ResultSet by resultSet {
    override fun close() {
        if (!resultSet.isClosed) resultSet.close()
        onClose()
    }
}
