package net.samyn.kapper

import java.sql.Connection

/**
 * Execute a Batch of SQL statement using a collection of objects and argument mapper functions to specify the query arguments.
 *
 * This function is typically used for non-query SQL statements, such as `INSERT`, `UPDATE`, or `DELETE` and will
 * result in a JDBC batch operation.
 *
 * **Example**:
 * ```kotlin
 * // Assuming we have a class
 * data class User(val id: Int, val active: Boolean)
 * // Update the "active" status of a user
 * val user = User(id = 1, active = false)
 * val rowsAffected
 * = connection.execute(
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
 * @param objects The objects containing the values to be used in the SQL statement.
 * @param args A map where the keys are the names of the parameters in the SQL statement, and the values are functions that extract the corresponding values from the object.
 * @return The number of rows affected by each execution in the batch.
 * @throws java.sql.SQLException If there's a database error.
 */
inline fun <reified T : Any> Connection.executeAll(
    sql: String,
    objects: Iterable<T>,
    vararg args: ArgMapper<T>,
): IntArray {
    return Kapper.instance.executeAll(T::class.java, this, sql, objects, args.toMap())
}
