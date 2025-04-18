package net.samyn.kapper

import java.sql.Connection
import java.sql.ResultSet
import kotlin.reflect.KClass

/**
 * Execute a SQL query and map the result to a single instance of the specified class, or `null` if no results are found.
 *
 * This function is useful for queries that are expected to return only one result, such as fetching a specific record by ID.
 *
 * **Example**:
 * ```kotlin
 * // Assuming we have a data class
 * data class User(val id: Int, val name: String)
 *
 * // Fetch a user by ID
 * val user: User? = connection.querySingle(
 *     sql = "SELECT id, name FROM users WHERE id = :id",
 *     "id" to 1
 * )
 *
 * // Handle the result
 * if (user == null) {
 *     println("User not found!")
 * } else {
 *     println("Found user: $user")
 * }
 * ```
 *
 * **Edge Cases**:
 * - Returns `null` if the query yields no results.
 * - Throws an exception if the query returns more than one result.
 * - Throws an exception if the query is invalid or if there's a database connection issue.
 *
 * @param sql The SQL query to execute.
 * @param args Optional key-value pairs representing named parameters to substitute into the query.
 * @return A single instance of [T] if exactly one result is found, or `null` otherwise.
 * @throws java.sql.SQLException If there's a database error.
 * @throws IllegalStateException If the query yields more than one result.
 */
inline fun <reified T : Any> Connection.querySingle(
    sql: String,
    vararg args: Pair<String, Any?>,
): T? {
    return querySingle(T::class, sql, *args)
}

/**
 * Execute a SQL query and map the result to a single instance of the specified class using a custom mapper or null if no results are found.
 *
 * **Example**:
 * ```kotlin
 * val customUser: User? = connection.querySingle(
 *     sql = "SELECT id, name FROM users WHERE id = :id",
 *     mapper = { resultSet, _ ->
 *         User(
 *             id = resultSet.getInt("id"),
 *             name = resultSet.getString("name")
 *         )
 *     },
 *     "id" to 1
 * )
 * println(customUser)
 * ```
 *
 * @param sql The SQL query to execute.
 * @param mapper Custom mapping function to transform the [ResultSet] into the target class.
 * @param args Optional parameters to be substituted in the SQL query during execution.
 * @return The query result as a single instance of [T], or null if no results are found.
 */
inline fun <reified T : Any> Connection.querySingle(
    sql: String,
    noinline mapper: (ResultSet, Map<String, Field>) -> T,
    vararg args: Pair<String, Any?>,
): T? {
    return querySingle(T::class, sql, mapper, *args)
}

/**
 * Execute a SQL query and map the result to a single instance of the specified class, or null of no results found.
 *
 * @param clazz The class to map the result to.
 * @param sql The SQL query to execute.
 * @param args Optional parameters to be substituted in the SQL query during execution. Parameter substitution is based on the first item in each Pair.
 * @return Returns a single result or `null` if no results are found. Throws an exception if more than one result is present.
 */
fun <T : Any> Connection.querySingle(
    clazz: KClass<T>,
    sql: String,
    vararg args: Pair<String, Any?>,
): T? = Kapper.instance.querySingle(clazz.java, this, sql, args.toMap())

/**
 * Execute a SQL query and map the result to a single instance of the specified class, or null of no results found.
 *
 * @param clazz The class to map the result to.
 * @param sql The SQL query to execute.
 * @param mapper Optional mapping function to map the [ResultSet] to the target class.
 * @param args Optional parameters to be substituted in the SQL query during execution. Parameter substitution is based on the first item in each Pair.
 * @return Returns a single result or `null` if no results are found. Throws an exception if more than one result is present.
 */
fun <T : Any> Connection.querySingle(
    clazz: KClass<T>,
    sql: String,
    mapper: (ResultSet, Map<String, Field>) -> T,
    vararg args: Pair<String, Any?>,
): T? = Kapper.instance.querySingle(clazz.java, this, sql, mapper, args.toMap())
