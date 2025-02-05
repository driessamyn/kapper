package net.samyn.kapper.internal

import net.samyn.kapper.Args
import net.samyn.kapper.Field
import net.samyn.kapper.Kapper
import net.samyn.kapper.KapperResultException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.ResultSet
import java.util.HashMap

// TODO: this is only covered by the TestContainer integration tests.
//  Break this up further and ensure unit test coverage.
internal class KapperImpl(
    private val queryBuilder: (String) -> Query = { Query(it) },
    private val fieldExtractor: (ResultSet) -> Map<String, Field> = { rs -> rs.extractFields() },
) : Kapper {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun <T : Any> query(
        clazz: Class<T>,
        connection: Connection,
        sql: String,
        args: Args,
    ): List<T> =
        // TODO: cash mapper
        query(clazz, connection, sql, Mapper(clazz)::createInstance, args.toMap())

    override fun <T : Any> query(
        clazz: Class<T>,
        connection: Connection,
        sql: String,
        mapper: (ResultSet, Map<String, Field>) -> T,
        args: Args,
    ): List<T> {
        // TODO: cache query
        val query = queryBuilder(sql)
        val results = mutableListOf<T>()
        connection.prepareStatement(query.sql).use { stmt ->
            args.setParameters(query, stmt, connection)
            logger.debug("Executing prepared statement: {}", stmt)
            // TODO: refactor
            stmt.executeQuery().use { rs ->
                // TODO: cache data
                // TODO: cash fields (persist in query?)
                val fields = fieldExtractor(rs)

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
            args.setParameters(query, stmt, connection)
            logger.debug("Executing prepared statement: {}", stmt)
            return stmt.executeUpdate()
        }
    }
}
