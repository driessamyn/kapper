package net.samyn.kapper.internal

import net.samyn.kapper.Kapper
import net.samyn.kapper.KapperParseException
import java.sql.Connection
import java.sql.ResultSet
import java.util.HashMap

// TODO: this is only covered by the TestContainer integration tests.
//  Break this up further and ensure unit test coverage.
class KapperImpl(
    val sqlTypesConverter: (Int, String, ResultSet, String) -> Any = SQLTypesConverter::convert,
) : Kapper {
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
        args: Map<String, Any?>,
    ): List<T> {
        // TODO: cache query
        val query = Query(sql)
        // TODO: cash mapper/type reflection
        // TODO: allow multiple constructors and non-data classes
        val mapper = Mapper(clazz)
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
            // TODO: create overload that takes custom conversion function and defaults to this
            stmt.executeQuery().use { rs ->
                // TODO: cache data
                // TODO: structure nicer
                val fields =
                    (1..rs.metaData.columnCount).map {
                        rs.metaData.getColumnName(it) to
                            Pair(rs.metaData.getColumnType(it), rs.metaData.getColumnTypeName(it))
                    }
                while (rs.next()) {
                    results.add(
                        mapper.createInstance(
                            fields.map { field ->
                                Mapper.ColumnValue(
                                    field.first,
                                    sqlTypesConverter(field.second.first, field.second.second, rs, field.first),
                                )
                            },
                        ),
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
