package net.samyn.kapper.snippets

import net.samyn.kapper.query
import java.sql.Connection

// region: enum-property
// Status maps from a VARCHAR column by name, case-insensitively.
fun selectWithEnum(connection: Connection): List<User> =
    connection.query<User>("SELECT id, name, email, status, created_at FROM users")
// endregion

// region: nullable-column
// email is nullable; a NULL column maps to null.
fun selectNullable(connection: Connection): List<User> =
    connection.query<User>("SELECT id, name, email, status, created_at FROM users")
// endregion

// region: custom-mapper
fun selectNames(connection: Connection): List<String> =
    connection.query("SELECT name FROM users") { row, _ -> row.string("name") }
// endregion

// region: jsonb
// A jsonb column maps to String out of the box on PostgreSQL.
fun selectMetadata(connection: Connection): List<String> =
    connection.query("SELECT metadata FROM users") { row, _ -> row.string("metadata") }
// endregion
