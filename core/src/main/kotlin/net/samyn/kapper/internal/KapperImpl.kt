@file:JvmSynthetic

package net.samyn.kapper.internal

import net.samyn.kapper.Args
import net.samyn.kapper.Field
import net.samyn.kapper.Kapper
import net.samyn.kapper.KapperResultException
import net.samyn.kapper.internal.DbConnectionUtils.getDbFlavour
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.ResultSet

internal class KapperImpl(
    private val queryBuilder: (String) -> Query = { Query(it) },
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
        val results = mutableListOf<T>()
        connection.executeQuery(sql, args).use { rs ->
            val fields = rs.extractFields()
            while (rs.next()) {
                results.add(mapper(rs, fields))
            }
        }
        return results
    }

    override fun <T : Any> querySingle(
        clazz: Class<T>,
        connection: Connection,
        sql: String,
        args: Args,
    ): T? =
        // TODO: cash mapper
        querySingle(clazz, connection, sql, Mapper(clazz)::createInstance, args.toMap())

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
        args: HashMap<String, Any?>,
    ): Int = execute(connection, sql, args.toMap())

    override fun execute(
        connection: Connection,
        sql: String,
        args: Args,
    ): Int {
        // TODO: cache query
        val query = queryBuilder(sql)
        connection.prepareStatement(query.sql).use { stmt ->
            args.setParameters(query, stmt, connection.getDbFlavour())
            logger.debug("Executing prepared statement: {}", stmt)
            return stmt.executeUpdate()
        }
    }
}
