package net.samyn.kapper.coroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.samyn.kapper.Field
import net.samyn.kapper.internal.Mapper
import net.samyn.kapper.internal.executeQuery
import net.samyn.kapper.internal.extractFields
import net.samyn.kapper.internal.logger
import java.sql.Connection
import java.sql.ResultSet

/**
 * Execute a SQL query and map the results to a Flow of instances of the specified class.
 *
 * This function uses reflection to automatically map the result set columns to the properties of the specified class.
 * For advanced mappings, use the overloaded version with a custom `mapper` function.
 *
 * **Example**:
 * ```kotlin
 * // Assuming we have a data class
 * data class User(val id: Int, val name: String)
 *
 * // Fetch all users where "active" is true
 * val users: Flow<User> = connection.query(
 *     sql = "SELECT id, name FROM users WHERE active = :active",
 *     "active" to true
 * )
 *
 * // Iterate over the user list and print each user
 * users.collect { println(it) }
 * ```
 *
 * @param sql The SQL query to execute.
 * @param args Optional key-value pairs representing named parameters to substitute into the query.
 * @return The query result as a [Flow] of [T] instances.
 * @throws java.sql.SQLException If there's a database error.
 */
inline fun <reified T : Any> Connection.query(
    sql: String,
    vararg args: Pair<String, Any?>,
): Flow<T> =
    query(
        sql,
        Mapper(T::class.java)::createInstance,
        *args,
    )

/**
 * Execute a SQL query and map the results to a Flow of instances of the specified class with a custom mapper.
 *
 * **Example**:
 * ```kotlin
 * val users: Flow<User> = connection.query(
 *     sql = "SELECT id, name FROM users",
 *     mapper = { resultSet, _ ->
 *         User(
 *             id = resultSet.getInt("id"),
 *             name = resultSet.getString("name")
 *         )
 *     }
 * )
 * users.collect { println(it) }
 * ```
 *
 * @param sql The SQL query to execute.
 * @param mapper Custom mapping function to transform the [ResultSet] into the target class.
 * @param args Optional parameters to be substituted in the SQL query during execution.
 * @return The query result as a [Flow] of [T] instances.
 * @throws java.sql.SQLException If there's a database error.
 */
inline fun <reified T : Any> Connection.query(
    sql: String,
    noinline mapper: (ResultSet, Map<String, Field>) -> T,
    vararg args: Pair<String, Any?>,
): Flow<T> {
    this.executeQuery(sql, args.toMap()).let { rs ->
        val fields = rs.extractFields()
        return flow {
            try {
                while (rs.next()) {
                    emit(mapper(rs, fields))
                }
            } catch (e: CancellationException) {
                logger.info("Query results processing cancelled: ${e.message}")
                throw e
            } finally {
                rs.close()
            }
        }
    }
}
