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
 * @param sql The SQL query to execute.
 * @param args Optional parameters to be substituted in the SQL query.
 * @return The query result as a list of [T] instances.
 */
inline fun <reified T : Any> Connection.query(
    sql: String,
    vararg args: Pair<String, Any?>,
): List<T> {
    return query(T::class, sql, *args)
}

/**
 * Execute a SQL query and map the results to a list of instances of the specified class.
 *
 * @param sql The SQL query to execute.
 * @param mapper Optional mapping function to map the [ResultSet] to the target class.
 * @param args Optional parameters to be substituted in the SQL query.
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
 * @param clazz The lass to map the results to.
 * @param sql The SQL query to execute.
 * @param args Optional parameters to be substituted in the SQL query.
 * @return The query result as a list of [T] instances.
 */
fun <T : Any> Connection.query(
    clazz: KClass<T>,
    sql: String,
    vararg args: Pair<String, Any?>,
): List<T> = impl.query(clazz.java, this, sql, args.toMap())

/**
 * Execute a SQL query and map the results to a list of instances of the specified class.
 *
 * @param clazz The class to map the results to.
 * @param sql The SQL query to execute.
 * @param mapper Optional mapping function to map the [ResultSet] to the target class.
 * @param args Optional parameters to be substituted in the SQL query.
 * @return The query result as a list of [T] instances.
 */
fun <T : Any> Connection.query(
    clazz: KClass<T>,
    sql: String,
    mapper: (ResultSet, Map<String, Field>) -> T,
    args: Map<String, Any?>,
): List<T> = impl.query(clazz.java, this, sql, mapper, args)

/**
 * Execute a SQL query and map the results to a list of instances of the specified class.
 *
 * @param sql The SQL query to execute.
 * @param clazz The class to map the result to.
 * @param args Optional parameters to be substituted in the SQL query.
 * @return The query result single [T] instance or null if 0 results found.
 */
inline fun <reified T : Any> Connection.querySingle(
    sql: String,
    vararg args: Pair<String, Any?>,
): T? {
    return querySingle(T::class, sql, *args)
}

/**
 * Execute a SQL query and map the result to a single instance of the specified class, or null of no results found.
 *
 * @param sql The SQL query to execute.
 * @param mapper Optional mapping function to map the [ResultSet] to the target class.
 * @param args Optional parameters to be substituted in the SQL query.
 * @return The query result single [T] instance or null if 0 results found.
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
 * @param args Optional parameters to be substituted in the SQL query.
 * @return The query result single [T] instance or null if 0 results found.
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
 * @param args Optional parameters to be substituted in the SQL query.
 * @return The query result single [T] instance or null if 0 results found.
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
 * @param sql The SQL statement to execute.
 * @param args Optional parameters to be substituted in the SQL statement.
 * @return The number of affected rows.
 */
fun Connection.execute(
    sql: String,
    vararg args: Pair<String, Any?>,
): Int {
    return impl.execute(this, sql, args.toMap())
}

/**
 * Kapper API interface for executing SQL statements and queries.
 * Used in cases where the extension methods cannot be used or ar not preferred.
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
     * @param args Optional parameters to be substituted in the SQL query.
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
     * @param args Optional parameters to be substituted in the SQL query.
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
     * @param args Optional parameters to be substituted in the SQL query.
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
     * @param args Optional parameters to be substituted in the SQL query.
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
     * Execute a SQL query and map the result to a single instance of the specified class, or null of no results found.
     *
     * @param clazz The class to map the result to.
     * @param connection The SQL connection to use.
     * @param sql The SQL query to execute.
     * @param args Optional parameters to be substituted in the SQL query.
     * @return The query result single [T] instance or null if 0 results found.
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
     * @param args Optional parameters to be substituted in the SQL query.
     * @return The query result single [T] instance or null if 0 results found.
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
     * @param args Optional parameters to be substituted in the SQL query.
     * @return The query result single [T] instance or null if 0 results found.
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
     * @param args Optional parameters to be substituted in the SQL query.
     * @return The query result single [T] instance or null if 0 results found.
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
     * @param args Optional parameters to be substituted in the SQL statement.
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
     * @param args Optional parameters to be substituted in the SQL statement.
     * @return The number of rows affected by the statement.
     */
    fun execute(
        connection: Connection,
        sql: String,
        args: Map<String, Any?>,
    ): Int
}
