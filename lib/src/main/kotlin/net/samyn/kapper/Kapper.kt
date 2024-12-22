package net.samyn.kapper

import net.samyn.kapper.internal.KapperImpl
import java.sql.Connection
import java.sql.ResultSet
import java.util.HashMap
import kotlin.reflect.KClass

val impl: Kapper by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    Kapper.getInstance()
}

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
): List<T> = impl.query(clazz.java, this, sql, args.toMap())

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
): List<T> = impl.query(clazz.java, this, sql, mapper, args)

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
): T? = impl.querySingle(clazz.java, this, sql, args.toMap())

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
): T? = impl.querySingle(clazz.java, this, sql, mapper, args.toMap())

/**
 * Execute a SQL statement and return the number of affected rows.
 *
 * This function is typically used for non-query SQL statements, such as `INSERT`, `UPDATE`, or `DELETE`.
 *
 * **Example**:
 * ```kotlin
 * // Update the "active" status of a user
 * val rowsAffected = connection.execute(
 *     sql = "UPDATE users SET active = :active WHERE id = :id",
 *     "active" to false,
 *     "id" to 1
 * )
 *
 * println("Rows affected: $rowsAffected")
 * ```
 *
 * **Edge Cases**:
 * - Returns `0` if no rows are affected by the statement.
 * - Throws an exception if there's an issue with the database connection or query syntax.
 *
 * @param sql The SQL statement to execute.
 * @param args Optional key-value pairs representing named parameters to substitute into the statement.
 * @return The number of rows affected by the execution of the SQL statement.
 * @throws SQLException If there's a database error.
 */
fun Connection.execute(
    sql: String,
    vararg args: Pair<String, Any?>,
): Int {
    return impl.execute(this, sql, args.toMap())
}

/**
 * Kapper API interface for executing SQL statements and queries.
 * Used in cases where the extension methods cannot be used or are not preferred.
 */
interface Kapper {
    companion object {
        @JvmStatic
        fun getInstance(): Kapper = KapperImpl()
    }

    /**
     * Execute a SQL query and map the results to a list of instances of the specified class.
     *
     * @param clazz The class to map the results to.
     * @param connection The SQL connection to use.
     * @param sql The SQL query to execute.
     * @param args Optional parameters to be substituted in the SQL query during execution. Parameter substitution is based on the Map keys.
     * @return The query result as a list of [T] instances.
     */
    fun <T : Any> query(
        clazz: Class<T>,
        connection: Connection,
        sql: String,
        args: Map<String, Any?>,
    ): List<T>

    /**
     * Execute a SQL query and map the results to a list of instances of the specified class.
     *
     * @param clazz The class to map the results to.
     * @param connection The SQL connection to use.
     * @param sql The SQL query to execute.
     * @param mapper Optional mapping function to map the [ResultSet] to the target class.
     * @param args Optional parameters to be substituted in the SQL query during execution. Parameter substitution is based on the Map keys.
     * @return The query result as a list of [T] instances.
     */
    fun <T : Any> query(
        clazz: Class<T>,
        connection: Connection,
        sql: String,
        mapper: (ResultSet, Map<String, Field>) -> T,
        args: Map<String, Any?>,
    ): List<T>

    /**
     * Execute a SQL query and map the results to a list of instances of the specified class.
     *
     * @param clazz The class to map the results to.
     * @param connection The SQL connection to use.
     * @param sql The SQL query to execute.
     * @param args Optional parameters to be substituted in the SQL query during execution. Parameter substitution is based on the HashMap keys.
     * @return The query result as a list of [T] instances.
     */
    fun <T : Any> query(
        clazz: Class<T>,
        connection: Connection,
        sql: String,
        args: HashMap<String, Any?>,
    ): List<T> = query(clazz, connection, sql, args.toMap())

    /**
     * Execute a SQL query and map the results to a list of instances of the specified class.
     *
     * @param clazz The class to map the results to.
     * @param connection The SQL connection to use.
     * @param sql The SQL query to execute.
     * @param mapper Optional mapping function to map the [ResultSet] to the target class.
     * @param args Optional parameters to be substituted in the SQL query during execution. Parameter substitution is based on the HashMap keys.
     * @return The query result as a list of [T] instances.
     */
    fun <T : Any> query(
        clazz: Class<T>,
        connection: Connection,
        sql: String,
        mapper: (ResultSet, Map<String, Field>) -> T,
        args: HashMap<String, Any?>,
    ): List<T> = query(clazz, connection, sql, mapper, args.toMap())

    /**
     * Execute a SQL query and map the result to a single instance of the specified class, or null if no results are found.
     *
     * @param clazz The class to map the result to.
     * @param connection The SQL connection to use.
     * @param sql The SQL query to execute.
     * @param args Optional parameters to be substituted in the SQL query during execution. Parameter substitution is based on the HashMap keys.
     * @return Returns a single result or `null` if no results are found. Throws an exception if more than one result is present.
     */
    fun <T : Any> querySingle(
        clazz: Class<T>,
        connection: Connection,
        sql: String,
        args: HashMap<String, Any?>,
    ): T? = querySingle(clazz, connection, sql, args.toMap())

    /**
     * Execute a SQL query and map the result to a single instance of the specified class, or null of no results found.
     *
     * @param clazz The class to map the result to.
     * @param connection The SQL connection to use.
     * @param sql The SQL query to execute.
     * @param mapper Optional mapping function to map the [ResultSet] to the target class.
     * @param args Optional parameters to be substituted in the SQL query during execution. Parameter substitution is based on the HashMap keys.
     * @return Returns a single result or `null` if no results are found. Throws an exception if more than one result is present.
     */
    fun <T : Any> querySingle(
        clazz: Class<T>,
        connection: Connection,
        sql: String,
        mapper: (ResultSet, Map<String, Field>) -> T,
        args: HashMap<String, Any?>,
    ): T? = querySingle(clazz, connection, sql, mapper, args.toMap())

    /**
     * Execute a SQL query and map the result to a single instance of the specified class, or null of no results found.
     *
     * @param clazz The class to map the result to.
     * @param connection The SQL connection to use.
     * @param sql The SQL query to execute.
     * @param args Optional parameters to be substituted in the SQL query during execution. Parameter substitution is based on the Map keys.
     * @return Returns a single result or `null` if no results are found. Throws an exception if more than one result is present.
     */
    fun <T : Any> querySingle(
        clazz: Class<T>,
        connection: Connection,
        sql: String,
        args: Map<String, Any?>,
    ): T?

    /**
     * Execute a SQL query and map the result to a single instance of the specified class, or null of no results found.
     *
     * @param clazz The class to map the result to.
     * @param connection The SQL connection to use.
     * @param sql The SQL query to execute.
     * @param mapper Optional mapping function to map the [ResultSet] to the target class.
     * @param args Optional parameters to be substituted in the SQL query during execution. Parameter substitution is based on the Map keys.
     * @return Returns a single result or `null` if no results are found. Throws an exception if more than one result is present.
     */
    fun <T : Any> querySingle(
        clazz: Class<T>,
        connection: Connection,
        sql: String,
        mapper: (ResultSet, Map<String, Field>) -> T,
        args: Map<String, Any?>,
    ): T?

    /**
     * Execute a SQL statement that does not return a result set.
     *
     * @param connection The SQL connection to use.
     * @param sql The SQL statement to execute.
     * @param args Optional parameters to be substituted in the SQL statement. Parameter substitution is based on the HashMap keys.
     * @return The number of rows affected by the statement.
     */
    fun execute(
        connection: Connection,
        sql: String,
        args: HashMap<String, Any?>,
    ): Int

    /**
     * Execute a SQL statement that does not return a result set.
     *
     * @param connection The SQL connection to use.
     * @param sql The SQL statement to execute.
     * @param args Optional parameters to be substituted in the SQL statement. Parameter substitution is based on the Map keys.
     * @return The number of rows affected by the statement.
     */
    fun execute(
        connection: Connection,
        sql: String,
        args: Map<String, Any?>,
    ): Int
}
