@file:JvmSynthetic

package net.samyn.kapper.internal

import net.samyn.kapper.Args
import net.samyn.kapper.Field
import net.samyn.kapper.Kapper
import net.samyn.kapper.KapperQueryException
import net.samyn.kapper.KapperResultException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException

internal class KapperImpl(
    private val queryFactory: (String) -> Query = QueryCache(),
) : Kapper {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
        private const val SQL_BLANK_ERROR = "SQL query cannot be empty or blank"
        private const val EXECUTING_STMT_LOG = "Executing prepared statement: {}"
    }

    override fun <T : Any> query(
        clazz: Class<T>,
        connection: Connection,
        sql: String,
        mapper: (ResultSet, Map<String, Field>) -> T,
        args: Args,
    ): List<T> {
        require(sql.isNotBlank()) { SQL_BLANK_ERROR }
        val dbFlavour = connection.getDbFlavour()
        return buildList {
            connection.executeQuery(queryFactory(sql), args, dbFlavour = dbFlavour).use { rs ->
                try {
                    val fields = rs.extractFields(dbFlavour)
                    while (rs.next()) {
                        add(mapper(rs, fields))
                    }
                } catch (e: SQLException) {
                    "Failed to execute query: $sql".also {
                        logger.warn(it, e)
                        throw KapperQueryException(it, e)
                    }
                }
            }
        }
    }

    override fun <T : Any> querySingle(
        clazz: Class<T>,
        connection: Connection,
        sql: String,
        mapper: (ResultSet, Map<String, Field>) -> T,
        args: Args,
    ): T? {
        require(sql.isNotBlank()) { SQL_BLANK_ERROR }
        val dbFlavour = connection.getDbFlavour()
        // Stop at two rows: a single result is expected, so there is no need to materialise the rest.
        connection.executeQuery(queryFactory(sql), args, dbFlavour = dbFlavour).use { rs ->
            try {
                val fields = rs.extractFields(dbFlavour)
                if (!rs.next()) return null
                val result = mapper(rs, fields)
                if (rs.next()) {
                    throw KapperResultException("Expected a single result but found more than one")
                }
                return result
            } catch (e: SQLException) {
                "Failed to execute query: $sql".also {
                    logger.warn(it, e)
                    throw KapperQueryException(it, e)
                }
            }
        }
    }

    override fun execute(
        connection: Connection,
        sql: String,
        args: Args,
    ): Int {
        val query = queryFactory(sql)
        connection.prepareStatement(query.sql).use { stmt ->
            val cleanup = args.setParameters(query, stmt, connection.getDbFlavour())
            try {
                logger.debug(EXECUTING_STMT_LOG, stmt)
                return stmt.executeUpdate()
            } finally {
                cleanup()
            }
        }
    }

    override fun <T : Any> execute(
        clazz: Class<T>,
        connection: Connection,
        sql: String,
        obj: T,
        args: Map<String, (T) -> Any?>,
    ): Int {
        val query = queryFactory(sql)
        connection.prepareStatement(query.sql).use { stmt ->
            val cleanup = args.setParameters(query.tokens, stmt, obj, connection.getDbFlavour())
            try {
                logger.debug(EXECUTING_STMT_LOG, stmt)
                return stmt.executeUpdate()
            } finally {
                cleanup()
            }
        }
    }

    override fun <T : Any> executeReturning(
        clazz: Class<T>,
        connection: Connection,
        sql: String,
        mapper: (ResultSet, Map<String, Field>) -> T,
        args: Args,
    ): List<T> {
        require(sql.isNotBlank()) { SQL_BLANK_ERROR }
        val dbFlavour = connection.getDbFlavour()
        return buildList {
            connection.executeQuery(queryFactory(sql), args, dbFlavour = dbFlavour).use { rs ->
                try {
                    val fields = rs.extractFields(dbFlavour)
                    while (rs.next()) {
                        add(mapper(rs, fields))
                    }
                } catch (e: SQLException) {
                    "Failed to execute statement: $sql".also {
                        logger.warn(it, e)
                        throw KapperQueryException(it, e)
                    }
                }
            }
        }
    }

    override fun <R : Any, A : Any> executeReturning(
        clazz: Class<R>,
        connection: Connection,
        sql: String,
        mapper: (ResultSet, Map<String, Field>) -> R,
        obj: A,
        args: Map<String, (A) -> Any?>,
    ): List<R> {
        require(sql.isNotBlank()) { SQL_BLANK_ERROR }
        val query = queryFactory(sql)
        val dbFlavour = connection.getDbFlavour()
        connection.prepareStatement(query.sql).use { stmt ->
            val cleanup = args.setParameters(query.tokens, stmt, obj, dbFlavour)
            try {
                logger.debug(EXECUTING_STMT_LOG, stmt)
                return buildList {
                    stmt.executeQuery().use { rs ->
                        try {
                            val fields = rs.extractFields(dbFlavour)
                            while (rs.next()) {
                                add(mapper(rs, fields))
                            }
                        } catch (e: SQLException) {
                            "Failed to execute statement: $sql".also {
                                logger.warn(it, e)
                                throw KapperQueryException(it, e)
                            }
                        }
                    }
                }
            } finally {
                cleanup()
            }
        }
    }

    override fun <T : Any> executeAll(
        clazz: Class<T>,
        connection: Connection,
        sql: String,
        objects: Iterable<T>,
        args: Map<String, (T) -> Any?>,
    ): IntArray {
        val query = queryFactory(sql)
        connection.prepareStatement(query.sql).use { stmt ->
            val dbFlavour = connection.getDbFlavour()
            val cleanups = mutableListOf<() -> Unit>()
            for (obj in objects) {
                cleanups.add(args.setParameters(query.tokens, stmt, obj, dbFlavour))
                logger.debug("Adding to batch: {}", stmt)
                stmt.addBatch()
            }
            try {
                return stmt.executeBatch()
            } finally {
                cleanups.forEach { it() }
            }
        }
    }
}
