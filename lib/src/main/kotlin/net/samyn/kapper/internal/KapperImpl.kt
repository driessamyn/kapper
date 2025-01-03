package net.samyn.kapper.internal

import net.samyn.kapper.Field
import net.samyn.kapper.Kapper
import net.samyn.kapper.KapperParseException
import net.samyn.kapper.KapperResultException
import net.samyn.kapper.internal.DbConnectionUtils.getDbFlavour
import net.samyn.kapper.internal.SQLTypesConverter.setParameter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.JDBCType
import java.sql.ResultSet
import java.util.HashMap

// TODO: this is only covered by the TestContainer integration tests.
//  Break this up further and ensure unit test coverage.
internal class KapperImpl : Kapper {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun <T : Any> query(
        clazz: Class<T>,
        connection: Connection,
        sql: String,
        args: Map<String, Any?>,
    ): List<T> =
        // TODO: cash mapper
        query(clazz, connection, sql, Mapper(clazz)::createInstance, args.toMap())

    override fun <T : Any> query(
        clazz: Class<T>,
        connection: Connection,
        sql: String,
        mapper: (ResultSet, Map<String, Field>) -> T,
        args: Map<String, Any?>,
    ): List<T> {
        // TODO: cache query
        val query = Query(sql)
        val results = mutableListOf<T>()
        connection.prepareStatement(query.sql).use { stmt ->
            args.forEach { a ->
                val indexes =
                    query.tokens[a.key]
                        ?: throw KapperParseException("Token with name `${a.key}' not found in template")
                indexes.forEach { i ->
                    // TODO: allow custom SQL type conversion?
                    stmt.setParameter(i, a.value, connection.getDbFlavour())
                }
            }
            logger.debug("Executing prepared statement: $stmt")
            // TODO: refactor
            stmt.executeQuery().use { rs ->
                // TODO: cache data
                // TODO: cash fields (persist in query?)
                val fields =
                    (1..rs.metaData.columnCount).map {
                        rs.metaData.getColumnName(it) to
                            Field(JDBCType.valueOf(rs.metaData.getColumnType(it)), rs.metaData.getColumnTypeName(it))
                    }.toMap()
                while (rs.next()) {
                    results.add(
                        mapper(rs, fields),
                    )
                }
            }
        }

        return results
    }

    override fun <T : Any> querySingle(
        clazz: Class<T>,
        connection: Connection,
        sql: String,
        args: Map<String, Any?>,
    ): T? =
        // TODO: cash mapper
        querySingle(clazz, connection, sql, Mapper(clazz)::createInstance, args.toMap())

    override fun <T : Any> querySingle(
        clazz: Class<T>,
        connection: Connection,
        sql: String,
        mapper: (ResultSet, Map<String, Field>) -> T,
        args: Map<String, Any?>,
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
        args: Map<String, Any?>,
    ): Int {
        // TODO: cache query
        val query = Query(sql)
        connection.prepareStatement(query.sql).use { stmt ->
            args.forEach { a ->
                val indexes =
                    query.tokens[a.key]
                        ?: throw KapperParseException("Token with name `${a.key}' not found in template")
                indexes.forEach { i ->
                    // TODO: allow custom SQL type conversion?
                    stmt.setParameter(i, a.value, connection.getDbFlavour())
                }
            }
            logger.debug("Executing prepared statement: $stmt")
            return stmt.executeUpdate()
        }
    }
}
