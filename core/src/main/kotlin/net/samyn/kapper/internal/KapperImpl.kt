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
    private val queryFactory: (String) -> Query = { Query(it) },
) : Kapper {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun <T : Any> query(
        clazz: Class<T>,
        connection: Connection,
        sql: String,
        mapper: (ResultSet, Map<String, Field>) -> T,
        args: Args,
    ): List<T> {
        require(sql.isNotBlank()) { "SQL query cannot be empty or blank" }
        return buildList {
            connection.executeQuery(queryFactory(sql), args).use { rs ->
                try {
                    val fields = rs.extractFields()
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
        val results = query(clazz, connection, sql, mapper, args)
        if (results.size > 1) {
            throw KapperResultException("Expected a single result but found ${results.size}")
        }
        return results.firstOrNull()
    }

    override fun execute(
        connection: Connection,
        sql: String,
        args: Args,
    ): Int {
        val query = queryFactory(sql)
        connection.prepareStatement(query.sql).use { stmt ->
            args.setParameters(query, stmt, connection.getDbFlavour())
            logger.debug("Executing prepared statement: {}", stmt)
            return stmt.executeUpdate()
        }
    }
}
