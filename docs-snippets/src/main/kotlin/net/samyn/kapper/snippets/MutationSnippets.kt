package net.samyn.kapper.snippets

import net.samyn.kapper.execute
import net.samyn.kapper.executeAll
import net.samyn.kapper.executeReturning
import java.sql.Connection
import java.util.UUID

// region: insert
fun insert(connection: Connection, user: User): Int =
    connection.execute(
        "INSERT INTO users(id, name, email, status, created_at) VALUES(:id, :name, :email, :status, :createdAt)",
        "id" to user.id,
        "name" to user.name,
        "email" to user.email,
        "status" to user.status,
        "createdAt" to user.createdAt,
    )
// endregion

// region: insert-returning-id
fun insertReturningId(connection: Connection, name: String): Long =
    connection.executeReturning<Long>(
        "INSERT INTO users(name) VALUES(:name) RETURNING id",
        "name" to name,
    ).single()
// endregion

// region: update
fun rename(connection: Connection, id: UUID, name: String): Int =
    connection.execute("UPDATE users SET name = :name WHERE id = :id", "name" to name, "id" to id)
// endregion

// region: delete
fun delete(connection: Connection, id: UUID): Int =
    connection.execute("DELETE FROM users WHERE id = :id", "id" to id)
// endregion

// region: batch-insert
fun batchInsert(connection: Connection, users: List<User>): IntArray =
    connection.executeAll(
        "INSERT INTO users(id, name, email, status, created_at) VALUES(:id, :name, :email, :status, :createdAt)",
        users,
        "id" to User::id,
        "name" to User::name,
        "email" to User::email,
        "status" to User::status,
        "createdAt" to User::createdAt,
    )
// endregion
