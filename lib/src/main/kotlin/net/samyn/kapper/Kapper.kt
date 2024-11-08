package net.samyn.kapper

import java.sql.Connection
import kotlin.reflect.KClass

/**
 * Execute a SQL statement and return the number of affected rows.
 *
 * @param sql The SQL statement to execute.
 * @param args Optional parameters to be substituted in the SQL statement.
 * @return The number of affected rows.
 */
fun Connection.execute(
    sql: String,
    vararg args: Any?,
): Int {
    // Implementation to execute the SQL statement and return the number of affected rows
    TODO()
}

/**
 * Execute a SQL query and map the results to a list of Kotlin data class instances.
 *
 * @param sql The SQL query to execute.
 * @param args Optional parameters to be substituted in the SQL query.
 * @return A list of Kotlin data class instances.
 */
inline fun <reified T : Any> Connection.query(
    sql: String,
    vararg args: Pair<String, Any?>,
): List<T> {
    return query(T::class, sql, *args)
}

/**
 * Execute a SQL query and map the results to a list of instances of the specified Kotlin data class.
 *
 * @param clazz The Kotlin data class to map the results to.
 * @param sql The SQL query to execute.
 * @param args Optional parameters to be substituted in the SQL query.
 * @return A list of Kotlin data class instances.
 */
fun <T : Any> Connection.query(
    clazz: KClass<T>,
    sql: String,
    vararg args: Pair<String, Any?>,
): List<T> {
    // Implementation to execute the SQL query and map the results to a list of Kotlin data class instances
    TODO()
}

/**
 * Execute a SQL query and map the result to a single Kotlin data class instance.
 *
 * @param sql The SQL query to execute.
 * @param clazz The Kotlin data class to map the result to.
 * @param args Optional parameters to be substituted in the SQL query.
 * @return A single Kotlin data class instance.
 */
inline fun <reified T : Any> Connection.querySingle(
    sql: String,
    vararg args: Pair<String, Any?>,
): T {
    return querySingle(T::class, sql, *args)
}

/**
 * Execute a SQL query and map the result to a single instance of the specified Kotlin data class.
 *
 * @param clazz The Kotlin data class to map the result to.
 * @param sql The SQL query to execute.
 * @param args Optional parameters to be substituted in the SQL query.
 * @return A single Kotlin data class instance.
 */
fun <T : Any> Connection.querySingle(
    clazz: KClass<T>,
    sql: String,
    vararg args: Pair<String, Any?>,
): T {
    // Implementation to execute the SQL query and map the single result to a Kotlin data class instance
    TODO()
}

/**
 * Kapper API interface for executing SQL statements and queries.
 * Used in cases where the extension methods cannot be used or ar not preferred.
 */
interface Kapper {
    /**
     * Execute a SQL query and map the results to a list of instances of the specified class.
     *
     * @param clazz The class to map the results to.
     * @param connection The SQL connection to use.
     * @param sql The SQL query to execute.
     * @param args Optional parameters to be substituted in the SQL query.
     * @return A list of Kotlin data class instances.
     */
    fun <T : Any> query(
        clazz: Class<T>,
        connection: Connection,
        sql: String,
        args: java.util.Map<String, Any?>,
    ): List<T>

    fun <T : Any> querySingle(
        clazz: Class<T>,
        connection: Connection,
        sql: String,
        args: java.util.Map<String, Any?>,
    ): T

    fun execute(
        connection: Connection,
        sql: String,
        args: java.util.Map<String, Any?>,
    ): Int
}
