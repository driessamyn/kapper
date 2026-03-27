package net.samyn.kapper

import java.sql.Connection
import java.sql.ResultSet
import kotlin.reflect.KClass

/**
 * Execute a SQL statement with a RETURNING clause and map the results to a list of instances of the specified class.
 *
 * This function uses reflection to automatically map the result set columns to the properties of the specified class.
 * For advanced mappings, use the overloaded version with a custom `mapper` function.
 *
 * **Example**:
 * ```kotlin
 * data class Hero(val id: UUID, val name: String, val updatedAt: Instant)
 *
 * val heroes: List<Hero> = connection.executeReturning(
 *     sql = """
 *         INSERT INTO heroes (id, name) VALUES (:id, :name)
 *         ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name
 *         RETURNING *
 *     """,
 *     "id" to UUID.randomUUID(),
 *     "name" to "Superman"
 * )
 * ```
 *
 * @param sql The SQL statement to execute, including a RETURNING clause.
 * @param args Optional key-value pairs representing named parameters to substitute into the statement.
 * @return The rows returned by the RETURNING clause as a list of [T] instances.
 * @throws java.sql.SQLException If there's a database error.
 */
inline fun <reified T : Any> Connection.executeReturning(
    sql: String,
    vararg args: Pair<String, Any?>,
): List<T> = executeReturning(T::class, sql, *args)

/**
 * Execute a SQL statement with a RETURNING clause and map the results to a list of instances of the specified class
 * with a custom mapper.
 *
 * **Example**:
 * ```kotlin
 * val heroes: List<Hero> = connection.executeReturning(
 *     sql = "INSERT INTO heroes (id, name) VALUES (:id, :name) ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name RETURNING *",
 *     mapper = { resultSet, _ ->
 *         Hero(
 *             id = resultSet.getObject("id", UUID::class.java),
 *             name = resultSet.getString("name")
 *         )
 *     },
 *     "id" to UUID.randomUUID(),
 *     "name" to "Superman"
 * )
 * ```
 *
 * @param sql The SQL statement to execute, including a RETURNING clause.
 * @param mapper Custom mapping function to transform the [ResultSet] into the target class.
 * @param args Optional parameters to be substituted in the SQL statement during execution.
 * @return The rows returned by the RETURNING clause as a list of [T] instances.
 */
inline fun <reified T : Any> Connection.executeReturning(
    sql: String,
    noinline mapper: (ResultSet, Map<String, Field>) -> T,
    vararg args: Pair<String, Any?>,
): List<T> = executeReturning(T::class, sql, mapper, args.toMap())

/**
 * Execute a SQL statement with a RETURNING clause and map the results to a list of instances of the specified class.
 *
 * @param clazz The class to map the results to.
 * @param sql The SQL statement to execute, including a RETURNING clause.
 * @param args Optional parameters to be substituted in the SQL statement during execution.
 * @return The rows returned by the RETURNING clause as a list of [T] instances.
 */
fun <T : Any> Connection.executeReturning(
    clazz: KClass<T>,
    sql: String,
    vararg args: Pair<String, Any?>,
): List<T> = Kapper.instance.executeReturning(clazz.java, this, sql, args.toMap())

/**
 * Execute a SQL statement with a RETURNING clause and map the results to a list of instances of the specified class
 * with a custom mapper.
 *
 * @param clazz The class to map the results to.
 * @param sql The SQL statement to execute, including a RETURNING clause.
 * @param mapper Custom mapping function to transform the [ResultSet] into the target class.
 * @param args Optional parameters to be substituted in the SQL statement during execution.
 * @return The rows returned by the RETURNING clause as a list of [T] instances.
 */
fun <T : Any> Connection.executeReturning(
    clazz: KClass<T>,
    sql: String,
    mapper: (ResultSet, Map<String, Field>) -> T,
    args: Map<String, Any?>,
): List<T> = Kapper.instance.executeReturning(clazz.java, this, sql, mapper, args)

/**
 * Execute a SQL statement with a RETURNING clause using an object and argument mapper functions,
 * and map the results to a list of instances of the specified class.
 *
 * The argument object type [A] and the returned row type [R] are intentionally separate,
 * allowing patterns such as passing a `CreateFoo` request and receiving a `Foo` result.
 *
 * **Example**:
 * ```kotlin
 * data class Hero(val id: UUID, val name: String)
 *
 * val hero = Hero(id = UUID.randomUUID(), name = "Superman")
 * val result: List<Hero> = connection.executeReturning(
 *     sql = """
 *         INSERT INTO heroes (id, name) VALUES (:id, :name)
 *         ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name
 *         RETURNING *
 *     """,
 *     obj = hero,
 *     "id" to Hero::id,
 *     "name" to Hero::name
 * )
 * ```
 *
 * @param sql The SQL statement to execute, including a RETURNING clause.
 * @param obj The object containing the values to be used in the SQL statement.
 * @param args Argument mappers that extract values from the object for parameter substitution.
 * @return The rows returned by the RETURNING clause as a list of [R] instances.
 * @throws java.sql.SQLException If there's a database error.
 */
inline fun <reified R : Any, A : Any> Connection.executeReturning(
    sql: String,
    obj: A,
    vararg args: ArgMapper<A>,
): List<R> = Kapper.instance.executeReturning(R::class.java, this, sql, obj, args.toMap())

/**
 * Execute a SQL statement with a RETURNING clause using an object, argument mapper functions, and a custom result mapper.
 *
 * The argument object type [A] and the returned row type [R] are intentionally separate,
 * allowing patterns such as passing a `CreateFoo` request and receiving a `Foo` result.
 *
 * @param sql The SQL statement to execute, including a RETURNING clause.
 * @param mapper Custom mapping function to transform the [ResultSet] into the target class.
 * @param obj The object containing the values to be used in the SQL statement.
 * @param args Argument mappers that extract values from the object for parameter substitution.
 * @return The rows returned by the RETURNING clause as a list of [R] instances.
 * @throws java.sql.SQLException If there's a database error.
 */
inline fun <reified R : Any, A : Any> Connection.executeReturning(
    sql: String,
    noinline mapper: (ResultSet, Map<String, Field>) -> R,
    obj: A,
    vararg args: ArgMapper<A>,
): List<R> = Kapper.instance.executeReturning(R::class.java, this, sql, mapper, obj, args.toMap())
