package net.samyn.kapper.snippets

import net.samyn.kapper.query
import net.samyn.kapper.queryScalar
import net.samyn.kapper.querySingle
import java.sql.Connection
import java.util.UUID

// region: select-many
fun selectMany(connection: Connection, minAge: Int): List<User> =
    connection.query<User>(
        "SELECT id, name, email, status, created_at FROM users WHERE age > :age",
        "age" to minAge,
    )
// endregion

// region: select-one
fun selectOne(connection: Connection, id: UUID): User? =
    connection.querySingle<User>(
        "SELECT id, name, email, status, created_at FROM users WHERE id = :id",
        "id" to id,
    )
// endregion

// region: select-scalar
fun countActive(connection: Connection): Long =
    connection.queryScalar<Long>("SELECT COUNT(*) FROM users WHERE status = 'ACTIVE'")
// endregion

// region: in-list
fun selectByIds(connection: Connection, ids: List<UUID>): List<User> =
    connection.query<User>(
        "SELECT id, name, email, status, created_at FROM users WHERE id IN (:ids)",
        "ids" to ids,
    )
// endregion

// region: join
fun selectUsersWithOrderCount(connection: Connection): List<UserWithOrders> =
    connection.query<UserWithOrders>(
        """
        SELECT u.id, u.name, COUNT(o.id) AS order_count
        FROM users u
        LEFT JOIN orders o ON o.user_id = u.id
        GROUP BY u.id, u.name
        """.trimIndent(),
    )
// endregion

// region: streaming
fun streamUsers(connection: Connection): Sequence<User> =
    connection.queryAsSequence<User>("SELECT id, name, email, status, created_at FROM users")
// endregion
