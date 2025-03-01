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
