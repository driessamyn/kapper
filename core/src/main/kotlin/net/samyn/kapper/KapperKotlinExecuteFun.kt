package net.samyn.kapper

import java.sql.Connection

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
 * @throws java.sql.SQLException If there's a database error.
 */
fun Connection.execute(
    sql: String,
    vararg args: Pair<String, Any?>,
): Int {
    return Kapper.instance.execute(this, sql, args.toMap())
}

/**
 * Execute a SQL statement using an object and argument mapper functions to specify the query arguments.
 *
 * This function is typically used for non-query SQL statements, such as `INSERT`, `UPDATE`, or `DELETE`.
 *
 * **Example**:
 * ```kotlin
 * // Assuming we have a class
 * data class User(val id: Int, val active: Boolean)
 * // Update the "active" status of a user
 * val user = User(id = 1, active = false)
 * val rowsAffected = connection.execute(
 *     sql = "UPDATE users SET active = :active WHERE id = :id",
 *     obj = user,
 *     "active" to User::active,
 *     "id" to User::id
 *     )
 *
 * println("Rows affected: $rowsAffected")
 *
 * @param T The type of the object used to provide values for the SQL statement.
 * @param sql The SQL statement to execute.
 * @param obj The object containing the values to be used in the SQL statement.
 * @param args A map where the keys are the names of the parameters in the SQL statement, and the values are functions that extract the corresponding values from the object.
 * @return The number of rows affected by the execution of the SQL statement.
 * @throws java.sql.SQLException If there's a database error.
 */
inline fun <reified T : Any> Connection.execute(
    sql: String,
    obj: T,
    vararg args: ArgMapper<T>,
): Int {
    return Kapper.instance.execute(T::class.java, this, sql, obj, args.toMap())
}
