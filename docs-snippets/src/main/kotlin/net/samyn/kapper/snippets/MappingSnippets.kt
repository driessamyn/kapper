package net.samyn.kapper.snippets

import net.samyn.kapper.query
import java.sql.Connection

// region: nullable-column
// email is nullable; a NULL column maps to null.
fun selectNullable(connection: Connection): List<User> = connection.query<User>("SELECT id, name, email, status, created_at FROM users")
// endregion
