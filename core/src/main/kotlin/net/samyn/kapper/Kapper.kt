package net.samyn.kapper

import net.samyn.kapper.internal.KapperImpl
import net.samyn.kapper.internal.logger
import java.sql.Connection
import java.sql.ResultSet

typealias Args = Map<String, Any?>

/**
 * Type alias for mapping functions from an object to SQL parameter values.
 */
typealias ArgMapper<T> = Pair<String, (T) -> Any?>

/**
 * Kapper API interface for executing SQL statements and queries.
 * Used in cases where the extension methods cannot be used or are not preferred.
 */
interface Kapper {
    companion object {
        /**
         * Create a new instance of the Kapper.
         *
         * @return A new instance of [Kapper].
         */
        @JvmStatic
        fun createInstance(): Kapper = KapperImpl()

        /**
         * Singleton instance of the Kapper.
         */
        @JvmStatic
        val instance: Kapper by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            createInstance()
        }.also { logger.info("Kapper instance created") }

        @JvmStatic
        val mapperRegistry: MapperRegistry = MapperRegistry()
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
        args: Args,
    ): List<T> {
        val mapper =
            try {
                mapperRegistry.get(clazz)
            } catch (e: Exception) {
                logger.error("Error creating instance of $clazz", e)
                throw KapperMappingException("Error creating mapper for $clazz", e)
            }
        return query(clazz, connection, sql, mapper::createInstance, args.toMap())
    }

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
        args: Args,
    ): List<T>

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
    ): T? {
        val mapper =
            try {
                mapperRegistry.get(clazz)
            } catch (e: Exception) {
                logger.error("Error creating instance of $clazz", e)
                throw KapperMappingException("Error creating auto-mapper for $clazz", e)
            }
        return querySingle(clazz, connection, sql, mapper::createInstance, args.toMap())
    }

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
        args: Args,
    ): T?

    /**
     * Execute a SQL statement and return the number of affected rows.
     *
     * @param connection The SQL connection to use.
     * @param sql The SQL statement to execute.
     * @param args Optional parameters to be substituted in the SQL statement. Parameter substitution is based on the Map keys.
     * @return The number of rows affected by the statement.
     */
    fun execute(
        connection: Connection,
        sql: String,
        args: Args,
    ): Int

    /**
     * Execute a SQL statement using an object and argument mapper functions to specify the query arguments.
     *
     * @param T The type of the object used to provide values for the SQL statement.
     * @param connection The SQL connection to use.
     * @param sql The SQL statement to execute.
     * @param obj The object containing the values to be used in the SQL statement.
     * @param args A map where the keys are the names of the parameters in the SQL statement, and the values are functions that extract the corresponding values from the object.
     * @return The number of rows affected by the execution of the SQL statement.
     */
    fun <T : Any> execute(
        clazz: Class<T>,
        connection: Connection,
        sql: String,
        obj: T,
        args: Map<String, (T) -> Any?>,
    ): Int

    /**
     * Execute a SQL statement using an object and argument mapper functions to specify the query arguments.
     *
     * @param T The type of the object used to provide values for the SQL statement.
     * @param connection The SQL connection to use.
     * @param sql The SQL statement to execute.
     * @param objects The objects containing the values to be used in the SQL statement.
     * @param args A map where the keys are the names of the parameters in the SQL statement, and the values are functions that extract the corresponding values from the object.
     * @return The number of rows affected by each execution in the batch.
     */
    fun <T : Any> executeAll(
        clazz: Class<T>,
        connection: Connection,
        sql: String,
        objects: Iterable<T>,
        args: Map<String, (T) -> Any?>,
    ): IntArray
}
