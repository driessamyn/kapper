package net.samyn.kapper.internal

import net.samyn.kapper.Kapper
import net.samyn.kapper.KapperParseException
import net.samyn.kapper.internal.Mapper.Field
import java.sql.Connection
import java.sql.ResultSet
import java.util.HashMap

// TODO: this is only covered by the TestContainer integration tests.
//  Break this up further and ensure unit test coverage.
class KapperImpl : Kapper {
    override fun <T : Any> query(
        clazz: Class<T>,
        connection: Connection,
        sql: String,
        args: HashMap<String, Any?>,
    ): List<T> = query(clazz, connection, sql, args.toMap())

    override fun <T : Any> query(
        clazz: Class<T>,
        connection: Connection,
        sql: String,
        args: HashMap<String, Any?>,
        mapper: (ResultSet, Map<String, Field>) -> T,
    ): List<T> = query(clazz, connection, sql, args.toMap(), mapper)

    override fun <T : Any> query(
        clazz: Class<T>,
        connection: Connection,
        sql: String,
        args: Map<String, Any?>,
    ): List<T> =
        // TODO: cash mapper
        query(clazz, connection, sql, args.toMap(), Mapper(clazz)::createInstance)

    override fun <T : Any> query(
        clazz: Class<T>,
        connection: Connection,
        sql: String,
        args: Map<String, Any?>,
        mapper: (ResultSet, Map<String, Field>) -> T,
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
                    stmt.setObject(i, a.value)
                }
            }
            // TODO: introduce SLF4J
            println(stmt)
            // TODO: refactor
            stmt.executeQuery().use { rs ->
                // TODO: cache data
                // TODO: cash fields (persist in query?)
                val fields =
                    (1..rs.metaData.columnCount).map {
                        rs.metaData.getColumnName(it) to
                            Field(rs.metaData.getColumnType(it), rs.metaData.getColumnTypeName(it))
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
        args: HashMap<String, Any?>,
    ): T = querySingle(clazz, connection, sql, args.toMap())

    override fun <T : Any> querySingle(
        clazz: Class<T>,
        connection: Connection,
        sql: String,
        args: Map<String, Any?>,
    ): T {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }
}
