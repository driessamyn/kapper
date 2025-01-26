package net.samyn.kapper

import net.samyn.kapper.internal.KapperImpl
import java.sql.Connection
import java.sql.ResultSet

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
