package net.samyn.kapper

import java.sql.Connection
import java.sql.ResultSet
import kotlin.reflect.KClass

/**
 * Execute a SQL query and map the results to a list of instances of the specified class.
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
 * val users: List<User> = connection.query(
 *     sql = "SELECT id, name FROM users WHERE active = :active",
 *     "active" to true
 * )
 *
 * // Iterate over the user list and print each user
 * users.forEach { println(it) }
 * ```
 *
 * **Edge Cases**:
 * - Returns an empty list if the query yields no results.
 * - Throws an exception if the query is invalid or if there's an issue with the database connection.
 *
 * @param sql The SQL query to execute.
 * @param args Optional key-value pairs representing named parameters to substitute into the query.
 * @return The query result as a list of [T] instances.
 * @throws java.sql.SQLException If there's a database error.
 */
inline fun <reified T : Any> Connection.query(
    sql: String,
    vararg args: Pair<String, Any?>,
): List<T> {
    return query(T::class, sql, *args)
}

/**
 * Execute a SQL query and map the results to a list of instances of the specified class with a custom mapper.
 *
 * **Example**:
 * ```kotlin
 * val users: List<User> = connection.query(
 *     sql = "SELECT id, name FROM users",
 *     mapper = { resultSet, _ ->
 *         User(
 *             id = resultSet.getInt("id"),
 *             name = resultSet.getString("name")
 *         )
 *     }
 * )
 * users.forEach { println(it) }
 * ```
 *
 * @param sql The SQL query to execute.
 * @param mapper Custom mapping function to transform the [ResultSet] into the target class.
 * @param args Optional parameters to be substituted in the SQL query during execution.
 * @return The query result as a list of [T] instances.
 */
inline fun <reified T : Any> Connection.query(
    sql: String,
    noinline mapper: (ResultSet, Map<String, Field>) -> T,
    vararg args: Pair<String, Any?>,
): List<T> = query(T::class, sql, mapper, args.toMap())

/**
 * Execute a SQL query and map the results to a list of instances of the specified class.
 *
 * @param clazz The class to map the results to.
 * @param sql The SQL query to execute.
 * @param args Optional parameters to be substituted in the SQL query during execution.
 * @return The query result as a list of [T] instances.
 */
fun <T : Any> Connection.query(
    clazz: KClass<T>,
    sql: String,
    vararg args: Pair<String, Any?>,
): List<T> = KapperInstance.get().query(clazz.java, this, sql, args.toMap())

/**
 * Execute a SQL query and map the results to a list of instances of the specified class with a custom mapper.
 *
 * @param clazz The class to map the results to.
 * @param sql The SQL query to execute.
 * @param mapper Custom mapping function to transform the [ResultSet] into the target class.
 * @param args Optional parameters to be substituted in the SQL query during execution.
 * @return The query result as a list of [T] instances.
 */
fun <T : Any> Connection.query(
    clazz: KClass<T>,
    sql: String,
    mapper: (ResultSet, Map<String, Field>) -> T,
    args: Map<String, Any?>,
): List<T> = KapperInstance.get().query(clazz.java, this, sql, mapper, args)
