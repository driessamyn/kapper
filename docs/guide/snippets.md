# Snippets

One blessed way to do each common task. Generated from compiled code; do not edit by hand.

## Setup

The types and imports the snippets below rely on.

```kotlin
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource
import kotlinx.coroutines.flow.collect
import net.samyn.kapper.KapperMappingException
import net.samyn.kapper.coroutines.queryAsFlow
import net.samyn.kapper.coroutines.withConnection
import net.samyn.kapper.execute
import net.samyn.kapper.executeAll
import net.samyn.kapper.executeReturning
import net.samyn.kapper.query
import net.samyn.kapper.querySingle

data class User(
    val id: UUID,
    val name: String,
    val email: String?,
    val status: String,
    val createdAt: Instant,
)

data class UserWithOrders(
    val id: UUID,
    val name: String,
    val orderCount: Int,
)
```

## Select many rows

Map a result set to a list of data classes.

```kotlin
fun selectMany(
    connection: Connection,
    minAge: Int,
): List<User> =
    connection.query<User>(
        "SELECT id, name, email, status, created_at FROM users WHERE age > :age",
        "age" to minAge,
    )
```

## Select one row

Return a single row, or null if none.

```kotlin
fun selectOne(
    connection: Connection,
    id: UUID,
): User? =
    connection.querySingle<User>(
        "SELECT id, name, email, status, created_at FROM users WHERE id = :id",
        "id" to id,
    )
```

## Join

Map a joined result set to a flat DTO.

```kotlin
fun selectUsersWithOrderCount(connection: Connection): List<UserWithOrders> =
    connection.query<UserWithOrders>(
        """
        SELECT u.id, u.name, COUNT(o.id) AS order_count
        FROM users u
        LEFT JOIN orders o ON o.user_id = u.id
        GROUP BY u.id, u.name
        """.trimIndent(),
    )
```

## Insert

Insert a row and return the affected count.

```kotlin
fun insert(
    connection: Connection,
    user: User,
): Int =
    connection.execute(
        "INSERT INTO users(id, name, email, status, created_at) VALUES(:id, :name, :email, :status, :createdAt)",
        "id" to user.id,
        "name" to user.name,
        "email" to user.email,
        "status" to user.status,
        "createdAt" to user.createdAt,
    )
```

## Insert and return the id

Insert a row and read back the generated id.

```kotlin
fun insertReturningId(
    connection: Connection,
    name: String,
): UUID =
    connection.executeReturning(
        "INSERT INTO users(name) VALUES(:name) RETURNING id",
        mapper = { rs, _ -> rs.getObject("id", UUID::class.java) },
        "name" to name,
    ).single()
```

## Update

Update a row.

```kotlin
fun rename(
    connection: Connection,
    id: UUID,
    name: String,
): Int = connection.execute("UPDATE users SET name = :name WHERE id = :id", "name" to name, "id" to id)
```

## Delete

Delete a row.

```kotlin
fun delete(
    connection: Connection,
    id: UUID,
): Int = connection.execute("DELETE FROM users WHERE id = :id", "id" to id)
```

## Batch insert

Insert many rows in one JDBC batch.

```kotlin
fun batchInsert(
    connection: Connection,
    users: List<User>,
): IntArray =
    connection.executeAll(
        "INSERT INTO users(id, name, email, status, created_at) VALUES(:id, :name, :email, :status, :createdAt)",
        users,
        "id" to User::id,
        "name" to User::name,
        "email" to User::email,
        "status" to User::status,
        "createdAt" to User::createdAt,
    )
```

## Nullable column

Map a NULL column to a nullable property.

```kotlin
// email is nullable; a NULL column maps to null.
fun selectNullable(connection: Connection): List<User> = connection.query<User>("SELECT id, name, email, status, created_at FROM users")
```

## Coroutines

Run a query on a coroutine dispatcher.

```kotlin
suspend fun listUsers(dataSource: DataSource): List<User> =
    dataSource.withConnection { connection ->
        connection.query<User>("SELECT id, name, email, status, created_at FROM users")
    }
```

## Flow

Stream results as a cold Flow.

```kotlin
suspend fun streamUsers(
    dataSource: DataSource,
    onUser: suspend (User) -> Unit,
) = dataSource.withConnection { connection ->
    // queryAsFlow is cold: the query runs when the flow is collected,
    // and the connection stays open for the duration of the collection.
    connection.queryAsFlow<User>("SELECT id, name, email, status, created_at FROM users")
        .collect { onUser(it) }
}
```

## Connection setup

Create a pooled DataSource.

```kotlin
fun dataSource(
    url: String,
    user: String,
    secret: String,
): DataSource =
    HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = url
            username = user
            password = secret
        },
    )
```

## Error handling

Handle a mapping failure.

```kotlin
fun loadUsers(dataSource: DataSource): List<User> =
    try {
        dataSource.connection.use {
            it.query<User>("SELECT id, name, email, status, created_at FROM users")
        }
    } catch (e: KapperMappingException) {
        // The message names the target class, the columns returned and the mismatch.
        // Add context of your own and let it propagate rather than swallowing it.
        throw KapperMappingException("Could not load users: ${e.message}", e)
    }
```

## Next Steps

- Learn about [Queries](./queries.md) for the full query API
- Explore [Mapping](./mapping.md) for mapping result sets to data classes and records
- See [Coroutines](./coroutines.md) for running queries on a coroutine dispatcher
